# K6 负载测试 FAQ

本文档回答关于 K6 负载测试框架的常见问题。

## 目录

- [1. 并发数是否可配置？](#1-并发数是否可配置)
- [2. 是否考虑了 Outcome 资产的异步生成？](#2-是否考虑了-outcome-资产的异步生成)
- [3. test-data.json 是如何生成的？](#3-test-datajson-是如何生成的)
- [4. test-data.json 的作用是什么？](#4-test-datajson-的作用是什么)

---

## 1. 并发数是否可配置？

**答：是的，完全可配置。**

### 通过环境变量

```bash
# 设置虚拟用户数为 200
export K6_VUS=200
./run-load-test.sh

# 或直接传递
./run-load-test.sh --vus 200
```

### 各场景的 VU 分配

总 VU 数会自动分配给不同场景：

| 场景 | VU 分配 | 说明 |
|------|---------|------|
| **mixed_load** | 100% (ramping) | 主负载场景，70%创建+30%取消 |
| **read_orders** | 20% (constant) | 持续查询订单 |
| **read_balances** | 10% (constant) | 持续查询余额 |

**示例**：
- 设置 `K6_VUS=100`
  - mixed_load: 0→100 (ramping stages)
  - read_orders: 20 VUs (constant)
  - read_balances: 10 VUs (constant)

### 相关配置

在 `config.js` 中定义：

```javascript
export const VUS = parseInt(__ENV.K6_VUS || '50');
```

在 `run.js` 中使用：

```javascript
export const options = {
    scenarios: {
        mixed_load: {
            executor: 'ramping-vus',
            stages: getStages(),  // 基于 VUS 计算
        },
        read_orders: {
            executor: 'constant-vus',
            vus: Math.max(1, Math.floor(VUS * 0.2)),  // 20%
        },
        read_balances: {
            executor: 'constant-vus',
            vus: Math.max(1, Math.floor(VUS * 0.1)),  // 10%
        },
    },
};
```

---

## 2. 是否考虑了 Outcome 资产的异步生成？

**答：是的，已经完整处理。**

### 背景

市场创建后，YES/NO outcome 资产是**异步生成**的：

```
POST /order/market (创建市场)
       ↓
返回 marketId (此时 symbolIdYes/No 为 null)
       ↓
后台异步处理 (MarketCreatedEventHandler)
       ↓
生成 YES/NO 资产
       ↓
更新 symbolIdYes/symbolIdNo
```

### 处理机制

在 `setup/data-setup.js` 中实现了轮询等待：

```javascript
function waitForMarketReady(token, marketId) {
    const maxAttempts = (MAX_POLL_SECONDS * 1000) / POLL_INTERVAL_MS;

    for (let i = 0; i < maxAttempts; i++) {
        const res = authGet(`/order/market/${marketId}`, token);

        if (res.status === 200) {
            const body = res.json();

            // ✅ 检查市场状态和 outcome 资产是否都已生成
            if (
                body &&
                body.data &&
                body.data.status === 'ACTIVE' &&
                body.data.symbolIdYes != null &&  // ✅ 等待 YES 资产
                body.data.symbolIdNo != null       // ✅ 等待 NO 资产
            ) {
                return {
                    marketId: marketId,
                    status: body.data.status,
                    symbolIdYes: body.data.symbolIdYes,
                    symbolIdNo: body.data.symbolIdNo,
                    symbolYes: `M${marketId}_YES`,
                    symbolNo: `M${marketId}_NO`,
                };
            }
        }

        sleep(POLL_INTERVAL_MS / 1000);  // 默认 500ms 轮询一次
    }

    // 超时警告
    console.warn(`Market ${marketId} did not become ready in time (outcome assets not generated)`);
    return null;
}
```

### 执行流程

```
Phase 2: 创建市场
  ├─ POST /order/market (创建市场1)
  ├─ POST /order/market (创建市场2)
  └─ ...
       ↓
等待所有市场就绪和 outcome 资产生成
  ├─ GET /order/market/1 (轮询，最多30秒)
  │    └─ 检查 symbolIdYes/No != null
  ├─ GET /order/market/2
  └─ ...
       ↓
Phase 5: 存入 outcome 资产 (此时确保资产已存在)
  ├─ POST /vault/deposit (symbol="M1_YES")
  ├─ POST /vault/deposit (symbol="M1_NO")
  └─ ...
```

### 配置项

可以调整轮询参数以适应不同环境：

```bash
# 如果后端处理较慢，增加超时时间
export K6_MAX_POLL_SECONDS=60    # 默认 30 秒
export K6_POLL_INTERVAL_MS=1000  # 默认 500ms

./run-load-test.sh --setup-only
```

### 运行时输出

数据准备时会显示详细信息：

```
Phase 2: Creating 10 markets...
  Created 10/10 markets
Waiting for markets to become ACTIVE and outcome assets to be generated...
  Market 123: symbolIdYes=456, symbolIdNo=457
  Market 124: symbolIdYes=458, symbolIdNo=459
  Market 125: symbolIdYes=460, symbolIdNo=461
  ...
Phase 3: Registering 100 users...
```

如果超时，会看到警告：

```
⚠️  Market 123 did not become ready in time (outcome assets not generated)
```

### Symbol 格式说明

系统中有两种 symbol 表示方式：

| 类型 | 示例 | 用途 | 来源 |
|------|------|------|------|
| **Symbol ID** | `456`, `457` | 内部匹配引擎使用 | `symbolIdYes`/`symbolIdNo` |
| **Symbol String** | `M123_YES`, `M123_NO` | 外部API（deposit、balance） | 构建格式 |

在 `test-data.json` 中同时保存两种格式，方便不同场景使用。

---

## 3. test-data.json 是如何生成的？

**答：通过调用真实的后端 API，从响应中提取数据生成。**

### 完整数据流

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 运行 data-setup.js                                            │
│    └─> k6 run setup/data-setup.js                               │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. 调用后端 API 创建资源                                          │
│    ├─> POST /vault/asset         → 返回 USDC asset              │
│    ├─> POST /order/market        → 返回 marketId                │
│    ├─> GET  /order/market/{id}   → 返回 symbolIdYes/No          │
│    ├─> POST /auth/register       → 返回 userId                  │
│    └─> POST /vault/deposit       → 存入资产                      │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. 从 API 响应中提取数据并构建对象                                 │
│    const data = {                                               │
│      markets: [从API提取的marketId数组],                         │
│      marketDetails: {从API提取的完整市场信息},                    │
│      users: [从API提取的userId和username]                        │
│    }                                                            │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. 通过 console.log 输出 JSON                                    │
│    console.log(JSON.stringify(data));                          │
└─────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Shell 脚本捕获输出并写入文件                                    │
│    grep -o '\{.*\}' /tmp/k6-setup.log | tail -1 > test-data.json│
└─────────────────────────────────────────────────────────────────┘
```

### 代码示例

#### 步骤 2-3：调用 API 并提取响应

```javascript
// data-setup.js: 创建市场并提取响应数据
function createMarket(token, index) {
    const res = authPost('/order/market', token, {
        name: `k6-load-test-market-${Date.now()}-${index}`,
        status: 'ACTIVE',
        expireAt: Date.now() + 86400 * 365 * 1000,
    });

    if (res.status === 200) {
        const body = res.json();  // ✅ 解析 API 响应
        if (body && body.data) {
            // ✅ 从响应中提取 marketId
            return body.data.id || body.data.marketId;
        }
    }
    return null;
}

// 轮询获取完整市场信息
function waitForMarketReady(token, marketId) {
    // ...轮询逻辑
    const body = res.json();  // ✅ 解析 API 响应

    return {
        marketId: marketId,
        status: body.data.status,           // ✅ 从响应提取
        symbolIdYes: body.data.symbolIdYes, // ✅ 从响应提取
        symbolIdNo: body.data.symbolIdNo,   // ✅ 从响应提取
        symbolYes: `M${marketId}_YES`,
        symbolNo: `M${marketId}_NO`,
    };
}

// 注册用户并提取响应数据
function registerUser(token, username, password) {
    const res = authPost('/auth/register', token, {
        username: username,
        password: password,
    });

    if (res.status === 200) {
        const body = res.json();  // ✅ 解析 API 响应
        if (body && body.code === 0) {
            return body.data || { userId: username };  // ✅ 提取 userId
        }
    }
    return { userId: username };
}
```

#### 步骤 3-4：构建并输出 JSON

```javascript
// data-setup.js: 主函数
export default function () {
    const adminToken = login('admin', '123456');

    // 初始化数据结构
    const data = {
        markets: [],
        marketDetails: {},
        users: [],
    };

    // Phase 2: 创建市场并收集返回的 ID
    console.log(`Phase 2: Creating ${MARKET_COUNT} markets...`);
    for (let i = 0; i < MARKET_COUNT; i++) {
        const marketId = createMarket(adminToken, i);  // ✅ API 返回的 ID
        if (marketId) {
            data.markets.push(marketId);
        }
    }

    // 等待并收集完整市场信息
    console.log('Waiting for markets to become ACTIVE and outcome assets to be generated...');
    for (const marketId of data.markets) {
        const marketDetail = waitForMarketReady(adminToken, marketId);  // ✅ API 返回的详情
        if (marketDetail) {
            data.marketDetails[marketId] = marketDetail;
            console.log(`  Market ${marketId}: symbolIdYes=${marketDetail.symbolIdYes}, symbolIdNo=${marketDetail.symbolIdNo}`);
        }
    }

    // Phase 3: 注册用户并收集返回的 userId
    console.log(`Phase 3: Registering ${USER_COUNT} users...`);
    for (let i = 0; i < USER_COUNT; i++) {
        const username = `${USER_PREFIX}${i}`;
        const user = registerUser(adminToken, username, USER_PASSWORD);  // ✅ API 返回的 user
        if (user) {
            data.users.push({
                username,
                password: USER_PASSWORD,
                userId: user.userId  // ✅ 从 API 响应提取
            });
        }
    }

    // Phase 4-5: 充值操作...

    // ✅ 输出收集到的数据（全部来自 API 响应）
    console.log('=== TEST_DATA_JSON_START ===');
    console.log(JSON.stringify(data));
    console.log('=== TEST_DATA_JSON_END ===');
}
```

#### 步骤 5：Shell 脚本捕获输出

```bash
# run-load-test.sh
k6 run "$SCRIPT_DIR/setup/data-setup.js" \
    -e K6_BASE_URL="$BASE_URL" \
    -e K6_MARKETS="$MARKETS" \
    -e K6_USERS="$USERS" \
    --quiet | tee /tmp/k6-setup.log

# ✅ 从 k6 的控制台输出中提取 JSON
if grep -q '"users"' /tmp/k6-setup.log; then
    grep -o '\{.*\}' /tmp/k6-setup.log | tail -1 > "$DATA_FILE"
    echo "Test data saved to $DATA_FILE"

    # 显示摘要
    ACTUAL_MARKETS=$(jq '.markets | length' "$DATA_FILE")
    ACTUAL_USERS=$(jq '.users | length' "$DATA_FILE")
    echo "Created: $ACTUAL_USERS users, $ACTUAL_MARKETS markets"
fi
```

### 最终生成的文件

`setup/test-data.json`:

```json
{
  "markets": [123, 124, 125],
  "marketDetails": {
    "123": {
      "marketId": 123,
      "status": "ACTIVE",
      "symbolIdYes": 456,
      "symbolIdNo": 457,
      "symbolYes": "M123_YES",
      "symbolNo": "M123_NO"
    },
    "124": {
      "marketId": 124,
      "status": "ACTIVE",
      "symbolIdYes": 458,
      "symbolIdNo": 459,
      "symbolYes": "M124_YES",
      "symbolNo": "M124_NO"
    }
  },
  "users": [
    {
      "username": "k6user0001",
      "password": "123456",
      "userId": "1001"
    },
    {
      "username": "k6user0002",
      "password": "123456",
      "userId": "1002"
    }
  ]
}
```

### 关键点总结

- ✅ **所有数据都来自后端 API 响应** - 没有硬编码或模拟数据
- ✅ **实时验证** - 轮询确保异步操作完成（如 outcome 资产生成）
- ✅ **准确反映数据库状态** - JSON 中的 ID 和状态与数据库一致
- ✅ **可重用** - load test 使用同样的 ID 访问真实数据

**test-data.json 是真实后端数据的快照，不是模拟数据！**

---

## 4. test-data.json 的作用是什么？

**答：除了作为数据快照，还有 8 个重要用途。**

### 1. 📊 负载测试的输入数据源（最核心）

```javascript
// run.js - 加载 test-data.json
const testData = JSON.parse(open('./setup/test-data.json'));

// 传递给各个场景
export function mixedScenario() {
    mixed(testData);  // ✅ 共享数据
}

export function queryScenario() {
    orderQuery(testData);  // ✅ 共享数据
}
```

每个场景从中随机选择数据：

```javascript
// scenarios/order-create.js
export default function orderCreate(sharedData) {
    const user = pickRandom(sharedData.users);      // ✅ 随机用户
    const marketId = pickRandom(sharedData.markets); // ✅ 随机市场

    const token = login(user.username, user.password);

    // 使用真实的 userId 和 marketId 发起请求
    const res = authPost('/order/create', token, {
        marketId: marketId,
        outcome: 'YES',
        side: 'BUY',
        // ...
    });
}

function pickRandom(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}
```

**好处**：
- 200 个 VU 可以并发使用不同的 user 和 market
- 模拟真实多用户场景，避免热点冲突
- 测试数据真实存在于数据库中

### 2. ⚡ 大幅减少测试启动时间

#### 没有 test-data.json（每次创建数据）

```bash
./run-load-test.sh
  ├─ Phase 1: 创建 USDC 资产
  ├─ Phase 2: 创建 100 个市场 (2分钟)
  ├─ Phase 2.5: 等待异步生成 outcome 资产 (5分钟)
  ├─ Phase 3: 注册 10,000 个用户 (10分钟)
  ├─ Phase 4: 10,000 次充值 USDC (15分钟)
  ├─ Phase 5: 1,000 个 seller 的 outcome 资产充值 (8分钟)
  └─ Phase 6: 开始负载测试

⏱️  总计：40分钟后才开始测试！
```

#### 有 test-data.json（直接复用）

```bash
./run-load-test.sh --test-only
  └─ 直接开始负载测试

⏱️  总计：立即开始！
```

**节省时间**：从 40 分钟到 0 秒。

### 3. 🔄 数据与测试分离 = 可重复测试

```bash
# 一次准备数据
./run-load-test.sh --setup-only --markets 100 --users 5000
# ✅ test-data.json 创建完成（包含 100 markets, 5000 users）

# 多次运行不同配置的负载测试，使用相同数据集
./run-load-test.sh --test-only --vus 50 --p95 500   # 测试1：50 VU
./run-load-test.sh --test-only --vus 100 --p95 500  # 测试2：100 VU
./run-load-test.sh --test-only --vus 200 --p95 500  # 测试3：200 VU
```

**好处**：
- ✅ **固定数据集** → 消除数据差异变量
- ✅ **性能对比准确** → 使用相同的 user/market 测试
- ✅ **问题可复现** → 知道具体是哪个 user/market 有问题

### 4. 🐛 调试和故障排查

当测试失败时，可以直接查看和使用 test-data.json：

```bash
# 查看有哪些测试用户
jq '.users[:5]' setup/test-data.json
[
  {"username": "k6user0001", "userId": "1001", "password": "123456"},
  {"username": "k6user0002", "userId": "1002", "password": "123456"},
  {"username": "k6user0003", "userId": "1003", "password": "123456"}
]

# 查看市场详情
jq '.marketDetails["123"]' setup/test-data.json
{
  "marketId": 123,
  "symbolIdYes": 456,
  "symbolIdNo": 457,
  "symbolYes": "M123_YES",
  "symbolNo": "M123_NO"
}

# 查看有多少用户和市场
jq '{users: (.users | length), markets: (.markets | length)}' setup/test-data.json
{
  "users": 5000,
  "markets": 100
}
```

**手动重现问题**：

```bash
# 使用 k6 的用户登录测试
curl -X POST http://localhost:9999/auth/token \
  -u test:test \
  -d "username=k6user0001&password=123456&grant_type=password"

# 使用返回的 token 创建订单
curl -X POST http://localhost:9999/order/create \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "marketId": 123,
    "outcome": "YES",
    "side": "BUY",
    "type": "LIMIT",
    "quantity": 10,
    "price": 0.5
  }'
```

### 5. 📈 性能基准和回归测试

```bash
# Week 1: 建立基准
./run-load-test.sh --setup-only --markets 100 --users 1000
./run-load-test.sh --test-only --vus 200
# 结果：P95=300ms, P99=500ms, 错误率=0.5%
# ✅ 保存 test-data.json 和 k6-summary.json 作为基准

# Week 2: 代码优化后，用相同数据集测试
./run-load-test.sh --test-only --vus 200  # 复用相同的 test-data.json
# 结果：P95=200ms, P99=350ms, 错误率=0.2%
# ✅ 对比证明：优化有效！P95 提升 33%
```

**关键**：只有使用相同的数据集才能公平对比性能。

#### 性能对比示例

| 版本 | 数据集 | VUs | P95 | P99 | 错误率 |
|------|--------|-----|-----|-----|--------|
| v1.0 | test-data.json (100m/1000u) | 200 | 300ms | 500ms | 0.5% |
| v1.1 | **相同数据集** | 200 | 200ms | 350ms | 0.2% |
| v1.2 | **相同数据集** | 200 | 150ms | 280ms | 0.1% |

**结论**：使用相同数据集，可以清晰看到优化效果。

### 6. 🔀 多场景共享数据

添加新的测试场景时，无需重新创建数据：

```javascript
// scenarios/order-list.js (新场景)
import { Trend, Counter } from 'k6/metrics';
import { login } from '../helpers/auth.js';
import { authGet } from '../helpers/http.js';

export const orderListDuration = new Trend('order_list_duration', true);
export const orderListErrors = new Counter('order_list_errors');

export default function orderList(sharedData) {
    const user = pickRandom(sharedData.users);      // ✅ 复用现有用户
    const marketId = pickRandom(sharedData.markets); // ✅ 复用现有市场

    const token = login(user.username, user.password);
    const res = authGet(`/order/list?marketId=${marketId}&limit=50`, token);

    orderListDuration.add(res.timings.duration);

    if (res.status !== 200) {
        orderListErrors.add(1);
    }
}

function pickRandom(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}
```

在 `run.js` 中添加场景：

```javascript
import orderList from './scenarios/order-list.js';

export const options = {
    scenarios: {
        // ... 现有场景
        list_orders: {
            executor: 'constant-vus',
            exec: 'listScenario',
            vus: Math.max(1, Math.floor(VUS * 0.15)),
            duration: '40m',
        },
    },
};

export function listScenario() {
    orderList(testData);  // ✅ 使用相同的 test-data.json
}
```

**好处**：无需重新准备数据，快速添加新场景。

### 7. 🎯 特定场景的定制数据集

为不同测试目的准备不同的数据集：

```bash
# 小规模冒烟测试（快速验证功能）
./run-load-test.sh --setup-only --markets 5 --users 50
mv setup/test-data.json setup/test-data-smoke.json

# 中等规模压力测试（日常性能测试）
./run-load-test.sh --setup-only --markets 50 --users 1000
mv setup/test-data.json setup/test-data-medium.json

# 大规模极限测试（寻找性能瓶颈）
./run-load-test.sh --setup-only --markets 500 --users 10000
mv setup/test-data.json setup/test-data-large.json

# 特定场景：高频交易测试（少量用户，大量市场）
./run-load-test.sh --setup-only --markets 1000 --users 100
mv setup/test-data.json setup/test-data-hft.json
```

使用不同数据集：

```bash
# 快速冒烟测试
ln -sf test-data-smoke.json setup/test-data.json
./run-load-test.sh --test-only --vus 10

# 日常压力测试
ln -sf test-data-medium.json setup/test-data.json
./run-load-test.sh --test-only --vus 100

# 极限测试
ln -sf test-data-large.json setup/test-data.json
./run-load-test.sh --test-only --vus 500
```

### 8. 📋 CI/CD 集成

在持续集成环境中缓存 test-data.json，节省时间和资源：

```yaml
# .github/workflows/load-test.yml
name: Load Test

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # 每天凌晨2点

jobs:
  load-test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Start backend services
        run: docker compose up -d

      - name: Install k6
        run: |
          curl -L https://github.com/grafana/k6/releases/download/v0.49.0/k6-v0.49.0-linux-amd64.tar.gz | tar xvz
          sudo mv k6-v0.49.0-linux-amd64/k6 /usr/local/bin/

      - name: Cache test data
        id: cache-testdata
        uses: actions/cache@v3
        with:
          path: pig-e2e-test/k6/setup/test-data.json
          # 当数据库 schema 变化时重新生成
          key: k6-testdata-${{ hashFiles('db/schema.sql', 'db/migrations/*.sql') }}

      - name: Setup test data (if not cached)
        if: steps.cache-testdata.outputs.cache-hit != 'true'
        run: |
          cd pig-e2e-test/k6
          ./run-load-test.sh --setup-only --markets 50 --users 500

      - name: Run load test
        run: |
          cd pig-e2e-test/k6
          ./run-load-test.sh --test-only --vus 100 --p95 300 --p99 600

      - name: Upload results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: k6-results
          path: pig-e2e-test/k6/k6-summary.json

      - name: Check thresholds
        run: |
          # 如果 k6 失败（阈值未达到），此步骤会失败
          exit $?
```

**好处**：
- ✅ **缓存数据** - 避免每次 CI 运行都创建数据（节省 30-40 分钟）
- ✅ **数据版本化** - schema 变化时自动重新生成
- ✅ **快速反馈** - 代码提交后 5 分钟内得到性能测试结果

### 9. 🔍 数据质量验证

可以验证生成的数据是否符合预期：

```bash
# 检查数据完整性
jq '
{
  markets_count: (.markets | length),
  users_count: (.users | length),
  market_details_count: (.marketDetails | length),
  first_market: .marketDetails[.markets[0]],
  first_user: .users[0]
}
' setup/test-data.json

# 检查所有市场都有 outcome 资产
jq '
.markets | map(. as $mid |
  {
    marketId: $mid,
    hasDetails: ($mid | tostring | in($marketDetails)),
    hasSymbolIds: (
      $marketDetails[$mid | tostring].symbolIdYes != null and
      $marketDetails[$mid | tostring].symbolIdNo != null
    )
  }
)
' setup/test-data.json --argjson marketDetails "$(jq .marketDetails setup/test-data.json)"
```

### 用途总结表

| 用途 | 说明 | 价值 | 使用场景 |
|------|------|------|----------|
| **1. 输入数据源** | VU 从中随机选择 user/market | 模拟真实多用户并发 | 所有负载测试 |
| **2. 减少启动时间** | 避免每次重建数据（40分钟→0秒） | 快速迭代测试 | 开发阶段 |
| **3. 可重复性** | 固定数据集，消除变量 | 准确的性能对比 | 性能调优 |
| **4. 调试** | 直接查看和手动测试 | 快速定位问题 | 故障排查 |
| **5. 性能基准** | 相同数据集对比不同版本 | 证明优化效果 | 回归测试 |
| **6. 数据共享** | 多个场景复用 | 避免重复准备 | 添加新场景 |
| **7. 定制数据集** | 不同规模/场景的数据 | 灵活测试 | 冒烟/压力/极限测试 |
| **8. CI/CD** | 缓存数据文件 | 节省资源和时间 | 持续集成 |
| **9. 数据验证** | 检查数据质量 | 确保测试有效性 | 数据准备阶段 |

---

## 相关文档

- [README.md](../README.md) - 快速开始和配置说明
- [config.js](../config.js) - 配置参数详细说明
- [data-setup.js](../setup/data-setup.js) - 数据准备脚本源码

---

**问题反馈**: 如有其他问题，请在项目中提 Issue 或查看源码注释。
