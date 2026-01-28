# K6 Load Testing

This directory contains k6 load tests for the Polymarket backend system.

## 📚 Documentation

- **[FAQ.md](docs/FAQ.md)** - 常见问题解答
  - 并发数配置
  - Outcome 资产异步生成处理
  - test-data.json 生成原理
  - test-data.json 的多种用途

## Quick Start

```bash
# Run everything with defaults (10 markets, 100 users, 50 VUs)
./run-load-test.sh

# Custom configuration
./run-load-test.sh --markets 50 --users 500 --vus 100

# Setup data only
./run-load-test.sh --setup-only --markets 20 --users 1000

# Run load test only (reuse existing data)
./run-load-test.sh --test-only --vus 200
```

## Prerequisites

1. **k6 installed**:
   ```bash
   brew install k6  # macOS
   ```

2. **Backend services running** at `http://localhost:9999` (or specify with `--base-url`)

3. **Database initialized** with required schema

## Directory Structure

```
k6/
├── run-load-test.sh          # Main runner script
├── config.js                 # Configuration and thresholds
├── run.js                    # Main k6 entrypoint
├── helpers/
│   ├── auth.js              # OAuth2 authentication & password encryption
│   └── http.js              # HTTP request wrappers with auth
├── setup/
│   ├── data-setup.js        # Data preparation script
│   └── test-data.json       # Generated test data (users, markets)
├── scenarios/
│   ├── order-create.js      # Order creation scenario
│   ├── order-cancel.js      # Order cancellation scenario
│   ├── order-query.js       # Order query scenario
│   ├── vault-balance.js     # Balance query scenario
│   └── mixed.js             # Mixed workload (70% create, 30% cancel)
└── checks/
    └── validators.js         # Response validation helpers
```

## Test Scenarios

The load test runs **3 concurrent scenarios**:

### 1. Mixed Load (ramping VUs)
- **70%** order creation
- **30%** order cancellation
- Stages: warmup → steady → peak → cooldown
- Duration: 40 minutes

### 2. Read Orders (constant VUs)
- 20% of total VUs
- Queries order details
- Duration: 40 minutes

### 3. Read Balances (constant VUs)
- 10% of total VUs
- Queries USDC and outcome asset balances
- Duration: 40 minutes

## Configuration

### Command-line Options

```bash
./run-load-test.sh [options]

Options:
  -h, --help              Show help message
  -s, --setup-only        Run data setup only
  -t, --test-only         Run load test only
  -c, --clean             Remove existing test data

  --base-url URL          Backend URL (default: http://localhost:9999)
  --markets N             Markets to create (default: 10)
  --users N               Users to create (default: 100)
  --sellers-pct PCT       % of users with assets (default: 0.1)
  --vus N                 Virtual users (default: 50)
  --p95 MS                P95 latency threshold (default: 500ms)
  --p99 MS                P99 latency threshold (default: 1000ms)
```

### Environment Variables

All options can be set via environment variables:

```bash
export K6_BASE_URL=http://localhost:9999
export K6_MARKETS=50
export K6_USERS=1000
export K6_VUS=200
export K6_P95_MS=300
export K6_P99_MS=800
export K6_CLIENT_ID=test
export K6_CLIENT_SECRET=test
export K6_ENCODE_KEY=testencryptkey

./run-load-test.sh
```

### Advanced k6 Options

For advanced k6 configuration, edit `config.js` or `run.js` directly.

## Data Setup

The `data-setup.js` script prepares test data in 5 phases:

1. **Ensure USDC asset** - Creates USDC if it doesn't exist
2. **Create markets** - Creates N prediction markets, waits for ACTIVE status
3. **Wait for outcome assets** - Polls each market until YES/NO assets are generated asynchronously
4. **Register users** - Creates M user accounts with encrypted passwords
5. **Deposit USDC** - Gives all users USDC balance (100 USDC each)
6. **Deposit outcome assets** - Gives seller subset YES/NO assets (50 each)

**Important**: Markets are created via API, but the outcome YES/NO assets are generated **asynchronously** by the backend. The setup script polls each market until `symbolIdYes` and `symbolIdNo` are populated before proceeding to deposits.

Output is saved to `setup/test-data.json`:

```json
{
  "users": [
    {"userId": "1", "username": "k6user_0001", "password": "encrypted..."}
  ],
  "markets": [123, 456, 789],
  "marketDetails": {
    "123": {
      "marketId": 123,
      "status": "ACTIVE",
      "symbolIdYes": 789,
      "symbolIdNo": 790,
      "symbolYes": "M123_YES",
      "symbolNo": "M123_NO"
    }
  }
}
```

## Metrics

### Standard k6 Metrics
- `http_req_duration` - Total request duration
- `http_req_failed` - Failed requests
- `http_reqs` - Total HTTP requests
- `vus` - Active virtual users
- `iterations` - Completed iterations

### Custom Metrics
- `order_create_duration` - Order creation latency
- `order_create_errors` - Order creation failures
- `order_cancel_duration` - Cancellation latency
- `order_cancel_errors` - Cancellation failures
- `order_query_duration` - Query latency
- `order_query_errors` - Query failures
- `vault_balance_duration` - Balance query latency
- `vault_balance_errors` - Balance query failures

### Thresholds

Default thresholds (configurable):
- `http_req_duration{p(95)} < 500ms`
- `http_req_duration{p(99)} < 1000ms`
- `http_req_failed < 0.01` (1% error rate)

## Output

Results are written to:
- **Console**: Text summary with colors
- **k6-summary.json**: Complete JSON report for post-processing

## Examples

### Quick smoke test
```bash
./run-load-test.sh --markets 5 --users 50 --vus 10
```

### Production-level test
```bash
./run-load-test.sh --markets 100 --users 10000 --vus 500 --p95 200 --p99 500
```

### Regenerate test data
```bash
./run-load-test.sh --clean --setup-only --markets 50 --users 2000
```

### Run against staging
```bash
./run-load-test.sh --base-url https://staging.example.com --vus 100
```

### InfluxDB + Grafana integration
```bash
k6 run run.js \
  --out influxdb=http://localhost:8086/k6 \
  -e K6_BASE_URL=http://localhost:9999
```

## Troubleshooting

### "Backend is not responding"
- Check services are running: `curl http://localhost:9999/actuator/health`
- Verify correct port/URL with `--base-url`

### "Test data not found"
- Run setup first: `./run-load-test.sh --setup-only`
- Or run full pipeline: `./run-load-test.sh`

### High error rates
- Check backend logs for errors
- Reduce VU count: `--vus 10`
- Increase thresholds: `--p95 1000 --p99 2000`

### "Login failed" errors
- Verify OAuth2 credentials match backend config
- Check `K6_CLIENT_ID`, `K6_CLIENT_SECRET`, `K6_ENCODE_KEY` env vars
- Ensure users were created successfully in data-setup phase

### Memory issues
- Reduce user/market count in data setup
- Lower VU count
- Run on machine with more RAM

## Best Practices

1. **Always run data-setup first** - Don't skip this step
2. **Start small** - Test with 10-50 VUs before scaling up
3. **Monitor backend** - Watch CPU, memory, DB connections during test
4. **Adjust thresholds** - Set realistic p95/p99 based on your requirements
5. **Clean data periodically** - Use `--clean` to regenerate fresh test data
6. **Version control** - Keep test-data.json in .gitignore (it's generated)

## CI/CD Integration

```yaml
# Example GitHub Actions workflow
- name: Run load tests
  run: |
    cd pig-e2e-test/k6
    ./run-load-test.sh --markets 20 --users 200 --vus 50 --p95 300
```

## References

### Project Documentation
- [FAQ.md](docs/FAQ.md) - 常见问题解答（并发配置、异步资产生成、test-data.json 详解）

### External Resources
- [k6 Documentation](https://k6.io/docs/)
- [k6 Best Practices](https://k6.io/docs/misc/best-practices/)
- [OAuth2 Password Grant](https://oauth.net/2/grant-types/password/)
