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
1. **Entity** - JPA entity class in `{module}-api/src/main/java/com/pig4cloud/pig/{name}/api/entity/`
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
- Extract table name
- Extract column definitions with types, constraints
- Identify primary key
- Map SQL types to Java types (following project constraint: use `Instant` for new time fields, keep existing `LocalDateTime`)
- Detect auto-fill fields: `create_time`, `update_time`, `create_by`, `update_by`, `del_flag`

### Step 2: Generate Entity Class
Template-based generation following conventions:
- Extend `Model<T>` for ActiveRecord pattern
- Use `@TableId(type = IdType.ASSIGN_ID)` for primary key
- Use `@Schema` annotations for Swagger documentation
- Apply `@TableField(fill = FieldFill.INSERT)` for create fields
- Apply `@TableField(fill = FieldFill.UPDATE)` for update fields
- Apply `@TableLogic` for `delFlag`
- Place in `{module}-api` submodule

### Step 3: Generate Mapper Interface
Simple interface extending `BaseMapper<Entity>`:
- Annotated with `@Mapper`
- Place in `{module}-biz/mapper/`
- Usually no custom methods needed (MyBatis Plus provides CRUD)

### Step 4: Generate Service Interface
Service interface extending `IService<Entity>`:
- Define business methods if needed
- Standard CRUD provided by MyBatis Plus
- Place in `{module}-biz/service/`

### Step 5: Generate ServiceImpl
Implementation extending `ServiceImpl<Mapper, Entity>`:
- Annotated with `@Service` and `@AllArgsConstructor`
- Implement custom business methods
- Add cache annotations if needed
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
  - Optional: `GET /export` - Export Excel
- Use `@HasPermission` for authorization
- Use `@SysLog` for audit logging
- Use `@ParameterObject` for query parameters
- Follow `SysPublicParamController` pattern

### Step 7: Generate Vue Index Page
List page with:
- Search form with filters
- Data table with pagination
- Action buttons (Add, Delete, Export, Refresh)
- Row actions (Edit, Delete)
- Selection support
- i18n support
- Integration with form dialog

### Step 8: Generate Vue Form Dialog
Form dialog with:
- Reactive form with validation rules
- Support for add/edit modes
- i18n support
- API integration
- Submit and cancel handling

### Step 9: Generate TypeScript API Client
API functions:
- `fetchList(query)` - Paginated list
- `getObj(id)` - Get by ID
- `addObj(obj)` - Create
- `putObj(obj)` - Update
- `delObj(ids)` - Delete

### Step 10: Generate i18n Files
Chinese and English translations for:
- Field labels
- Placeholders
- Validation messages
- Button labels

## Code Formatting

After generation, apply Spring Java Format:
```bash
mvn spring-javaformat:apply
```

## References

### Example Entities
- `SysPublicParam` - Standard entity with all common fields

### Example Controllers
- `SysPublicParamController` - Standard CRUD controller pattern

### Example Vue Pages
- `pig-ui/src/views/admin/sys/param/` - Complete reference implementation

### Type Mapping (SQL → Java)

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

## Best Practices

1. **Entity Design**
   - Always include audit fields: `createTime`, `updateTime`, `createBy`, `updateBy`
   - Always include `delFlag` for soft delete
   - Use `@Schema` for comprehensive API documentation
   - Follow naming: entity name should be `{TableName}` in PascalCase

2. **Service Layer**
   - Keep service interface simple, add methods only when needed
   - Implement custom business logic in ServiceImpl
   - Use cache annotations for frequently accessed data

3. **Controller Layer**
   - Follow REST conventions
   - Use `@ParameterObject` for complex query objects
   - Return `R<T>` wrapper for all responses
   - Add permission checks with `@HasPermission`
   - Use `LambdaUpdateWrapper` for dynamic query conditions

4. **Frontend**
   - Use composition API (setup script)
   - Follow existing i18n patterns
   - Reuse components: `right-toolbar`, `pagination`, `dict-tag`
   - Use `useTable` hook for table management
   - Use `useDict` hook for dictionary data

5. **API Client**
   - Use consistent naming: `fetchList`, `getObj`, `addObj`, `putObj`, `delObj`
   - Follow request utility patterns
   - Add validation functions when needed

## Example Usage

User provides:
```sql
CREATE TABLE `gym_member` (
  `id` bigint NOT NULL COMMENT 'ID',
  `name` varchar(64) NOT NULL COMMENT 'Member Name',
  `phone` varchar(20) COMMENT 'Phone',
  `status` char(1) DEFAULT '0' COMMENT 'Status 0:Active 1:Inactive',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  `create_by` varchar(64) COMMENT 'Created By',
  `update_by` varchar(64) COMMENT 'Updated By',
  `del_flag` char(1) DEFAULT '0' COMMENT 'Delete Flag',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gym Member';
```

Module: `pig-gym`
Frontend path: `gym`

Skill generates:
- `pig-gym-api/.../entity/GymMember.java`
- `pig-gym-biz/.../mapper/GymMemberMapper.java`
- `pig-gym-biz/.../service/GymMemberService.java`
- `pig-gym-biz/.../service/impl/GymMemberServiceImpl.java`
- `pig-gym-biz/.../controller/GymMemberController.java`
- `pig-ui/src/views/admin/gym/member/index.vue`
- `pig-ui/src/views/admin/gym/member/form.vue`
- `pig-ui/src/api/admin/gymmember.ts`
- `pig-ui/src/views/admin/gym/member/i18n/zh-cn.ts`
- `pig-ui/src/views/admin/gym/member/i18n/en.ts`

## Post-Generation Checklist

- [ ] Run `mvn spring-javaformat:apply` to format Java code
- [ ] Verify entity annotations are correct
- [ ] Check if additional business methods are needed in Service
- [ ] Review permission strings in Controller
- [ ] Test API endpoints
- [ ] Verify frontend routing is configured
- [ ] Test CRUD operations in UI
- [ ] Add menu entry if needed
