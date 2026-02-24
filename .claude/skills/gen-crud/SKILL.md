---
name: gen-crud
description: Generate complete CRUD module (Entity, Mapper, Service, Controller, and Vue UI) from SQL schema and module name, following project conventions.
---

# Generate CRUD Module

Use this skill when the user asks to generate a complete CRUD module from a database table definition.

## Required Inputs

1. **SQL CREATE TABLE statement** - The table schema to generate code from
2. **Java module name** - The existing module (e.g., `pig-gym`, `pig-upms`)
3. **Frontend module path** - Path under `pig-ui/src/views/admin/` (e.g., `gym`, `sys`)

## What This Skill Generates

### Backend (Java)
1. **Entity** - MyBatis Plus entity class in `{module}-api/src/main/java/com/pig4cloud/pig/{name}/api/entity/`
2. **Mapper** - MyBatis Plus mapper interface in `{module}-biz/src/main/java/com/pig4cloud/pig/{name}/mapper/`
3. **Service** - Service interface in `{module}-biz/src/main/java/com/pig4cloud/pig/{name}/service/`
4. **ServiceImpl** - Service implementation in `{module}-biz/src/main/java/com/pig4cloud/pig/{name}/service/impl/`
5. **Controller** - REST controller in `{module}-biz/src/main/java/com/pig4cloud/pig/{name}/controller/`

### Frontend (Vue 3 + TypeScript)
1. **index.vue** - List page with pagination and CRUD operations
2. **form.vue** - Form dialog for add/edit
3. **API file** - TypeScript API client functions
4. **i18n files** - Internationalization (zh-cn.ts, en.ts)

## Workflow

### Step 1: Parse SQL Schema
- Extract table name, column definitions, primary key
- Map SQL types to Java types (use `Instant` for new time fields; keep `LocalDateTime` for existing)
- **IMPORTANT**: Every table MUST include `create_time`, `update_time`, `create_by`, `update_by`. If missing from the input SQL, add them automatically.
- Identify auto-fill fields: `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`

### Step 2: Generate Entity Class
Rules:
- **Extend `BaseEntity`** (`com.pig4cloud.pig.common.mybatis.base.BaseEntity`)
  - `BaseEntity` already provides: `createTime`, `updateTime`, `createBy`, `updateBy`
  - **Do NOT generate** these four fields in the entity body — they are inherited
- Use `@TableId(type = IdType.ASSIGN_ID)` for primary key
- Use `@Schema` annotations for Swagger documentation
- Apply `@TableLogic` + `@TableField(fill = FieldFill.INSERT)` for `delFlag`
- Place in `{module}-api` submodule

### Step 3: Generate Mapper Interface
- Extend `BaseMapper<Entity>`, annotated with `@Mapper`
- Place in `{module}-biz/mapper/`

### Step 4: Generate Service Interface
- Extend `IService<Entity>`
- Place in `{module}-biz/service/`

### Step 5: Generate ServiceImpl
- Extend `ServiceImpl<Mapper, Entity>`, annotated with `@Service` and `@AllArgsConstructor`
- Place in `{module}-biz/service/impl/`

### Step 6: Generate Controller
REST controller following project conventions:
- Annotated with `@RestController`, `@RequestMapping`, `@Tag`, `@SecurityRequirement`, `@AllArgsConstructor`
- Required endpoints:
  - `GET /page` - Pagination query with filters
  - `GET /details/{id}` - Get by ID
  - `POST /` - Create new record
  - `PUT /` - Update existing record
  - `DELETE /` - Delete by ID array
- Use `@HasPermission` for authorization
- Use `@SysLog` for audit logging
- Use `@ParameterObject` for query parameters
- Follow `SysPublicParamController` pattern

### Step 7: Generate Vue Index Page
- Search form with filters, data table with pagination
- Action buttons (Add, Delete, Export, Refresh) and row actions (Edit, Delete)
- i18n support

### Step 8: Generate Vue Form Dialog
- Reactive form with validation, add/edit mode, API integration

### Step 9: Generate TypeScript API Client
- `fetchList`, `getObj`, `addObj`, `putObj`, `delObj`

### Step 10: Generate i18n Files
- `zh-cn.ts` and `en.ts` with field labels, placeholders, validation messages

## Type Mapping (SQL → Java)

| SQL Type | Java Type | Notes |
|----------|-----------|-------|
| BIGINT | Long | For IDs |
| VARCHAR, TEXT | String | |
| INT, INTEGER | Integer | |
| TINYINT(1) | String | For flags (0/1) |
| DECIMAL | BigDecimal | |
| DATETIME, TIMESTAMP | Instant | **For new fields** |
| DATETIME, TIMESTAMP | LocalDateTime | **Keep for existing** |
| DATE | LocalDate | |
| BIT, BOOLEAN | Boolean | |

## Post-Generation Checklist

### 1. Format Java Code
```bash
mvn spring-javaformat:apply
```

### 2. Restart and Verify Build

```bash
./start-standalone.sh
```

**Success**: A new PID is printed in the output, e.g.:
```
Started PigBootApplication in X.XXX seconds
PID: <new-pid>
```

**If no new PID appears** — compilation failed:
1. Read the build log:
   ```bash
   cat logs/pig-boot/build.log
   ```
2. Fix all reported errors
3. Re-run `./start-standalone.sh` and repeat until a new PID appears

### 3. Verify Controller is Active via API Docs

Look up the OpenAPI group name for the module in:
`pig-boot/src/main/java/com/pig4cloud/pig/config/OpenApiGroupConfiguration.java`

Known group names:
| Module package | `.group(...)` value | API Docs URL |
|---|---|---|
| `com.pig4cloud.pig.gym` | `健身管理模块` | `http://localhost:9999/admin/v3/api-docs/健身管理模块` |
| `com.pig4cloud.pig.admin` | `用户管理模块` | `http://localhost:9999/admin/v3/api-docs/用户管理模块` |
| `com.pig4cloud.pig.auth` | `认证模块` | `http://localhost:9999/admin/v3/api-docs/认证模块` |

For new modules, read the file and use the `.group(...)` value as the URL path segment (URL-encode Chinese characters if needed).

Check that the response JSON contains the new controller's paths (e.g., `/member/page`).
If not found: check package scan config in `OpenApiGroupConfiguration.java` and restart again.

### 4. Add Menu Entry

After confirming the Controller is active, render `templates/add_menu.sql.template` by substituting all `{{PLACEHOLDER}}` values:

| Placeholder | Example value |
|---|---|
| `{{PARENT_MENU_NAME}}` | `健身房管理` |
| `{{ENTITY_DISPLAY_NAME}}` | `会员管理` |
| `{{ENTITY_EN_NAME}}` | `member` |
| `{{VUE_ROUTE_PATH}}` | `/admin/gym/member/index` |
| `{{ICON}}` | `ele-User` |
| `{{PERMISSION_PREFIX}}` | `gym_member` |

Save the rendered SQL as `db/add_{entity_snake}_menu.sql`, then execute:
```bash
mysql -u root -pJz123456 pig < db/add_{entity_snake}_menu.sql
```

If the password is wrong, ask the user for the correct password.

The template uses a **recursive CTE** to walk up the full ancestor chain and grant every ancestor menu to `role_id=1` as well — this is required because a child menu is invisible if any ancestor is not granted to the role.

## References

- Entity template: `templates/Entity.java.template`
- Menu SQL template: `templates/add_menu.sql.template`
- Example controller: `SysPublicParamController`
- Example Vue pages: `pig-ui/src/views/admin/sys/param/`
- Example menu SQL: `db/add_trainingsession_menu.sql`
- OpenAPI group config: `pig-boot/src/main/java/com/pig4cloud/pig/config/OpenApiGroupConfiguration.java`
