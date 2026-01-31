#!/bin/bash

# Load Test Runner
# Usage: ./run-load-test.sh [options]

set -e

# Default configuration
BASE_URL="${K6_BASE_URL:-http://localhost:9999}"
MARKETS="${K6_MARKETS:-10}"
USERS="${K6_USERS:-100}"
SELLERS_PCT="${K6_SELLERS_PCT:-0.1}"
VUS="${K6_VUS:-50}"
P95_MS="${K6_P95_MS:-500}"
P99_MS="${K6_P99_MS:-1000}"
CLIENT_ID="${K6_CLIENT_ID:-test}"
CLIENT_SECRET="${K6_CLIENT_SECRET:-test}"
ENCODE_KEY="${K6_ENCODE_KEY:-thanks,pig4cloud}"
USER_PREFIX="${K6_USER_PREFIX:-k6user}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_FILE="$SCRIPT_DIR/setup/test-data.json"

# Usage
usage() {
    cat << EOF
Usage: $0 [options]

Options:
    -h, --help              Show this help message
    -s, --setup-only        Run data setup only (no load test)
    -t, --test-only         Run load test only (skip data setup)
    -c, --clean             Remove existing test data before setup

    --base-url URL          Backend base URL (default: http://localhost:9999)
    --markets N             Number of markets to create (default: 10)
    --users N               Number of users to create (default: 100)
    --sellers-pct PCT       Percentage of users with assets (default: 0.1)
    --vus N                 Virtual users for load test (default: 50)
    --p95 MS                P95 latency threshold (default: 500)
    --p99 MS                P99 latency threshold (default: 1000)

Environment variables can also be used (K6_BASE_URL, K6_MARKETS, K6_USERS, etc.)

Examples:
    # Full run with defaults
    $0

    # Setup only with custom values
    $0 --setup-only --markets 20 --users 500

    # Load test only (reuse existing data)
    $0 --test-only --vus 100

    # Custom load test
    $0 --markets 50 --users 1000 --vus 200 --p95 300
EOF
    exit 0
}

# Parse arguments
SETUP_ONLY=false
TEST_ONLY=false
CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            ;;
        -s|--setup-only)
            SETUP_ONLY=true
            shift
            ;;
        -t|--test-only)
            TEST_ONLY=true
            shift
            ;;
        -c|--clean)
            CLEAN=true
            shift
            ;;
        --base-url)
            BASE_URL="$2"
            shift 2
            ;;
        --markets)
            MARKETS="$2"
            shift 2
            ;;
        --users)
            USERS="$2"
            shift 2
            ;;
        --sellers-pct)
            SELLERS_PCT="$2"
            shift 2
            ;;
        --vus)
            VUS="$2"
            shift 2
            ;;
        --p95)
            P95_MS="$2"
            shift 2
            ;;
        --p99)
            P99_MS="$2"
            shift 2
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            usage
            ;;
    esac
done

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}Error: k6 is not installed${NC}"
    echo "Install with: brew install k6"
    exit 1
fi

# Check if backend is running
echo -e "${YELLOW}Checking backend availability...${NC}"
if ! curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" | grep -q "200"; then
    echo -e "${RED}Error: Backend at $BASE_URL is not responding${NC}"
    echo "Make sure your services are running"
    exit 1
fi
echo -e "${GREEN}Backend is running${NC}"

# Clean data if requested
if [ "$CLEAN" = true ] && [ -f "$DATA_FILE" ]; then
    echo -e "${YELLOW}Removing existing test data...${NC}"
    rm "$DATA_FILE"
fi

# Step 1: Data Setup
if [ "$TEST_ONLY" = false ]; then
    if [ -f "$DATA_FILE" ]; then
        echo -e "${YELLOW}Test data already exists at $DATA_FILE${NC}"
        read -p "Regenerate? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Reusing existing test data"
        else
            rm "$DATA_FILE"
        fi
    fi

    if [ ! -f "$DATA_FILE" ]; then
        echo -e "${YELLOW}Step 1: Generating test data...${NC}"
        echo "  Markets: $MARKETS"
        echo "  Users: $USERS"
        echo "  Sellers: $(echo "$USERS * $SELLERS_PCT" | bc | cut -d. -f1)"
        echo ""

        # Ensure each setup run uses a unique user prefix to avoid legacy users
        RUN_ID="$(date +%Y%m%d%H%M%S)"
        EFFECTIVE_USER_PREFIX="${USER_PREFIX}-${RUN_ID}"

        k6 run "$SCRIPT_DIR/setup/data-setup.js" \
            -e K6_BASE_URL="$BASE_URL" \
            -e K6_MARKETS="$MARKETS" \
            -e K6_USERS="$USERS" \
            -e K6_SELLERS_PCT="$SELLERS_PCT" \
            -e K6_USER_PREFIX="$EFFECTIVE_USER_PREFIX" \
            -e K6_CLIENT_ID="$CLIENT_ID" \
            -e K6_CLIENT_SECRET="$CLIENT_SECRET" \
            -e K6_ENCODE_KEY="$ENCODE_KEY" \
            --quiet | tee /tmp/k6-setup.log

        # Extract JSON from k6 output (between TEST_DATA_JSON_START and END; unescape msg= quotes)
        if grep -q 'TEST_DATA_JSON_START' /tmp/k6-setup.log; then
            JSON_LINE=$(sed -n '/TEST_DATA_JSON_START/,/TEST_DATA_JSON_END/p' /tmp/k6-setup.log | sed -n '2p')
            if [ -n "$JSON_LINE" ]; then
                echo "$JSON_LINE" | sed 's/.*msg="//; s/" source=.*//' | sed 's/\\"/"/g' > "$DATA_FILE"
            fi
        fi
        if [ -f "$DATA_FILE" ] && jq -e '.users | length' "$DATA_FILE" >/dev/null 2>&1; then
            echo -e "${GREEN}Test data saved to $DATA_FILE${NC}"
            ACTUAL_MARKETS=$(jq '.markets | length' "$DATA_FILE")
            ACTUAL_USERS=$(jq '.users | length' "$DATA_FILE")
            echo -e "${GREEN}Created: $ACTUAL_USERS users, $ACTUAL_MARKETS markets${NC}"
        else
            echo -e "${RED}Error: Data setup failed or produced no output${NC}"
            cat /tmp/k6-setup.log
            exit 1
        fi
    fi

    if [ "$SETUP_ONLY" = true ]; then
        echo -e "${GREEN}Data setup complete. Run with --test-only to execute load test.${NC}"
        exit 0
    fi
fi

# Step 2: Load Test
if [ "$SETUP_ONLY" = false ]; then
    if [ ! -f "$DATA_FILE" ]; then
        echo -e "${RED}Error: Test data not found at $DATA_FILE${NC}"
        echo "Run with --setup-only first, or without --test-only flag"
        exit 1
    fi

    echo ""
    echo -e "${YELLOW}Step 2: Running load test...${NC}"
    echo "  Virtual Users: $VUS"
    echo "  P95 Threshold: ${P95_MS}ms"
    echo "  P99 Threshold: ${P99_MS}ms"
    echo ""

    k6 run "$SCRIPT_DIR/run.js" \
        -e K6_BASE_URL="$BASE_URL" \
        -e K6_VUS="$VUS" \
        -e K6_P95_MS="$P95_MS" \
        -e K6_P99_MS="$P99_MS" \
        -e K6_CLIENT_ID="$CLIENT_ID" \
        -e K6_CLIENT_SECRET="$CLIENT_SECRET" \
        -e K6_ENCODE_KEY="$ENCODE_KEY"

    echo ""
    echo -e "${GREEN}Load test complete!${NC}"

    if [ -f "$SCRIPT_DIR/k6-summary.json" ]; then
        echo -e "${GREEN}Summary saved to $SCRIPT_DIR/k6-summary.json${NC}"
    fi
fi
