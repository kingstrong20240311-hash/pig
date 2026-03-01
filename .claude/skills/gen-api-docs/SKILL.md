---
name: gen-api-docs
description: Generate or update Controller API documentation under back/docs/api/. Creates docs from scratch if missing; in update mode, refreshes changed Controllers and also backfills missing Controller docs inside affected modules.
---

# Gen API Docs

## Purpose

Generate or incrementally update the REST API documentation for business Controllers.

**Announce at start:** "I'm using the gen-api-docs skill to generate/update Controller API documentation."

---

## Documentation Structure

```text
back/
└── docs/
    ├── common.md             # Shared field conventions
    └── api/
        ├── index.md          # Unified response structure + module index table
        └── {module}/
            ├── index.md      # Request summary (path + method + description + doc link)
            └── {ControllerSimpleName}@{Tag.name}.md  # Detailed per-controller doc
```

The `{module}` folder name comes from the Maven module: `pig-{module}` → `{module}`.

---

## Step 1 — Determine Mode

Check whether module subdirectories already exist in `back/docs/api/`:

```bash
ls back/docs/api/
```

- **Create mode** — only `index.md` exists, no module subdirectories → run **Step 2A**.
- **Update mode** — at least one module directory exists → run **Step 2B**.

---

## Step 2A — Create Mode (full generation)

### 2A.1 Find business Controllers

Scan for `**/controller/*Controller.java` files. **Include only** business modules with a `-biz` submodule:

| Include | Path pattern |
|---|---|
| gym | `pig-gym/pig-gym-biz/**/*Controller.java` |
| order | `pig-order/pig-order-biz/**/*Controller.java` |
| vault | `pig-vault/pig-vault-biz/**/*Controller.java` |
| any future `pig-{name}` | `pig-{name}/pig-{name}-biz/**/*Controller.java` |

**Skip** infrastructure/framework controllers:
- `pig-upms/` (Sys* controllers — framework RBAC, not business API)
- `pig-visual/` (codegen, quartz, monitor)
- `pig-common/` (error handling utilities)
- `pig-auth/` (OAuth2 server internals)
- `pig-gateway/` (gateway filters)

### 2A.2 Derive module name

From the Maven module directory in the path:
```
back/pig-{module}/pig-{module}-biz/src/main/.../controller/XController.java
                   ↑ extract this
```

Examples:
- `pig-gym/pig-gym-biz/.../GymController.java` → module `gym`
- `pig-order/pig-order-biz/.../OrderController.java` → module `order`
- `pig-vault/pig-vault-biz/.../VaultController.java` → module `vault`

### 2A.3 Parse each Controller

Read the `.java` file. Extract:

**Class-level:**
- `@Tag(name = "Tag名")` → Tag name (used in filename and heading)
- `@RequestMapping("path")` on the class → base path (may be SpEL like `${gym.api-prefix:}`)
- `@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)` → note if auth required

**Per method** (only methods annotated with `@Operation`):
- HTTP method: `@PostMapping`, `@GetMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`
- Endpoint path: value of the mapping annotation (e.g. `"/session/create"`)
- `@Operation(summary = "...", description = "...")`
- `@PreAuthorize("...")` → permission requirement if present
- Parameters:
  - `@RequestBody XxxRequest request` → read DTO fields
  - `@PathVariable("name") Type name`
  - `@RequestParam(value = "name", required = false) Type name`
- Return type: `R<ReturnType>` → document the `data` field type

### 2A.4 Read DTO and Entity classes

When a method uses a DTO or returns an entity, locate and read that class:

**Search paths** (in order):
1. `back/pig-{module}/pig-{module}-api/src/main/java/.../api/dto/`
2. `back/pig-{module}/pig-{module}-api/src/main/java/.../api/entity/`
3. `back/pig-{module}/pig-{module}-biz/src/main/java/.../entity/`
4. `back/pig-common/pig-common-core/src/main/java/.../`

For each field in the DTO/Entity:
- **Name**: camelCase field name
- **Type**: map using the Type Mapping table below
- **Required**: `是` if annotated with `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Valid` + `@Size(min>0)` — otherwise `否`
- **Description**: from `@Schema(description = "...")`, or inline `// comment`, or field name if none

**Type Mapping:**

| Java Type | Doc Type |
|---|---|
| `Long`, `long` | `long` |
| `Integer`, `int` | `integer` |
| `String` | `string` |
| `Boolean`, `boolean` | `boolean` |
| `BigDecimal` | `number` |
| `Instant` | `timestamp（epoch ms）` |
| `LocalDateTime` | `string（ISO-8601）` |
| `List<X>`, `Set<X>` | `array` |
| `Map<K,V>` | `object` |
| Custom DTO/Entity | `object（展开字段）` |
| Enum | `string（枚举值见说明）` |
| `Void` | *(omit data field or write `null`)* |

### 2A.5 Generate `{ControllerSimpleName}@{Tag.name}.md`

File path: `back/docs/api/{module}/{ControllerSimpleName}@{Tag.name}.md`

Template (follow `GymController@健身管理.md` as the canonical example):

```markdown
# {Tag.name}（{ControllerSimpleName}）

- Controller：`{ControllerSimpleName}`
- Tag：`{Tag.name}`
- Base Path：`{basePath}`（如 SpEL 则说明默认值）
[- 鉴权：需要 Bearer Token]  ← include only if @SecurityRequirement present

## 1. {operation.summary}

- 方法/路径：`{HTTP_METHOD} {combined_path}`
- Summary：{summary}
- Description：{description}
[- 权限：`{@PreAuthorize value}`]  ← include only if @PreAuthorize present

### 请求体  ← section only if @RequestBody exists

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| field | type | 是/否 | description |

For nested arrays (e.g. `List<ItemDTO>`), add a sub-table:

`{fieldName}[]` 字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| ...

请求示例：

​```json
{
  realistic example JSON with representative values
}
​```

### 路径参数  ← section only if @PathVariable exists

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| name | type | description |

### 查询参数  ← section only if @RequestParam exists

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| name | type | 是/否 | - | description |

### 响应 data（`{ReturnTypeName}`）  ← skip section if R<Void>

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| field | type | description |

响应示例：

​```json
{
  "code": 0,
  "msg": "success",
  "data": {
    realistic response JSON
  }
}
​```

## 2. {next operation}
...
```

**Example value guidelines:**
- ID / Long fields: `1001`, `10001`, `20001` etc.
- String fields: realistic short Chinese or English values matching the field meaning
- Enum fields: use first valid enum value from the code
- Timestamp (epoch ms): use a plausible value like `1772330400000`
- `R<Void>` response: `"data": null`
- Omit optional fields that add no clarity (or show them as `null`)

### 2A.6 Generate `back/docs/api/{module}/index.md`

Aggregate all endpoints from all Controllers in the module:

```markdown
# {module} 模块接口摘要

- 模块：`{module}`

## 请求摘要

| 路径 | 方法 | 功能说明 | 文档 |
| --- | --- | --- | --- |
| `{combined_path}` | {METHOD} | {summary} | [`{ControllerSimpleName}@{Tag.name}.md`](./{ControllerSimpleName}@{Tag.name}.md) |
```

Sort rows by: Controller name alphabetically, then by method order within each Controller.

### 2A.7 Update `back/docs/api/index.md`

Append a row for each new module (keep existing rows) in the **模块索引** table:

```markdown
| {module} | {short description from @Tag(description)} | [`{module}/index.md`](./{module}/index.md) |
```

---

## Step 2B — Update Mode (incremental)

### 2B.1 Identify changed Controller files via git diff

```bash
git -C /path/to/repo diff --name-only HEAD
```

If that returns nothing (clean working tree), fall back to:

```bash
git -C /path/to/repo diff --name-only HEAD~1 HEAD
```

Filter the output to lines matching `*Controller.java`.

- If matched Controller files exist: continue with Step 2B.2 and Step 2B.3.
- If no Controller files appear: do **not** stop immediately. First run **Step 2B.1.1 (missing doc backfill check)**.
  - If Step 2B.1.1 finds missing docs, generate them and update module index files.
  - If Step 2B.1.1 finds nothing missing, inform the user and stop.

> **Note:** Use the actual git repo root that contains `.git` (in this project it is typically `back/`). File paths in diff output are relative to that repo root.

### 2B.1.1 Missing doc backfill check (required in update mode)

For each affected module (derived from changed Controllers), and also for user-targeted modules (if the user explicitly says "sync module X"):

1. Scan all business Controllers under `pig-{module}/pig-{module}-biz/**/controller/*Controller.java`.
2. For each Controller, read `@Tag(name = "...")` and derive expected doc filename:
   - `back/docs/api/{module}/{ControllerSimpleName}@{Tag.name}.md`
3. If expected file does not exist, mark as **missing doc Controller**.
4. Regenerate docs for all missing doc Controllers, then regenerate `back/docs/api/{module}/index.md`.

This rule ensures unchanged Controllers (for example `InbodyTestController`) are still documented when their doc file is missing.

### 2B.2 For each changed Controller (plus missing doc Controllers)

1. Build the target set = `changed Controllers` ∪ `missing doc Controllers` (from Step 2B.1.1).
2. Derive each Controller's **module** (same rule as Step 2A.2).
3. Read the current controller `.java` file.
4. Regenerate its `{ControllerSimpleName}@{Tag.name}.md` (full replacement of that file).
5. Regenerate the module's `index.md` (re-scan all Controllers in that module — don't just patch rows).
6. If this module has **no existing folder** in `back/docs/api/`:
   - Create the module folder and both files.
   - Add the module row to `back/docs/api/index.md`.

### 2B.3 Handle deleted or renamed Controllers

If `git diff` shows a Controller file was deleted (`deleted:`) or renamed:
- Remove the old `{ControllerSimpleName}@{Tag.name}.md`.
- Regenerate the module `index.md` without that Controller's rows.
- If the module folder is now empty (no controllers remain), remove it and clean up `api/index.md`.

---

## Combined Base Path

The full endpoint path shown in docs is: `{classMappingPath}/{methodMappingPath}`.

- If `classMappingPath` is empty or a SpEL expression like `${gym.api-prefix:}`, show the method path directly and note the SpEL in the header.
- Normalize: no trailing slash, leading slash on method path if absent.
- Examples:
  - Class `@RequestMapping("/market")` + method `@GetMapping("/{marketId}")` → `/market/{marketId}`
  - Class `@RequestMapping("${gym.api-prefix:}")` + method `@PostMapping("/session/create")` → `/session/create`

---

## Project Conventions to Follow

- All time fields use timestamps (epoch ms) — `createTime`, `updateTime`, `expireAt`, etc.
- Fields inherited from `BaseEntity`: `createTime`, `updateTime`, `createBy`, `updateBy`, `delFlag` — include in response tables.
- `R<T>` wrapper: always show the full `{"code": 0, "msg": "success", "data": ...}` structure in response examples.
- `R<Void>` endpoints: write `"data": null` in the example and omit the response data table.
- Security: note `Bearer Token required` in the Controller header when `@SecurityRequirement` is present.

---

## Output Summary

After completing, report:
- Number of modules documented
- Number of Controller doc files created/updated
- List of files created or modified
