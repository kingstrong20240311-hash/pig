# gen-crud Skill

This skill generates a complete CRUD module including:

## Backend Components
- Entity (JPA/MyBatis Plus entity class)
- Mapper (MyBatis Plus mapper interface)
- Service (Service interface)
- ServiceImpl (Service implementation)
- Controller (REST controller with full CRUD endpoints)

## Frontend Components
- index.vue (List page with search, pagination, and actions)
- form.vue (Form dialog for add/edit)
- API file (TypeScript API client functions)
- i18n files (Chinese and English translations)

## Usage

```
/gen-crud
```

Then provide:
1. SQL CREATE TABLE statement
2. Java module name (e.g., pig-gym)
3. Frontend module path (e.g., gym)

## Templates

The skill uses templates located in `templates/` directory:
- Entity.java.template
- Mapper.java.template
- Service.java.template
- ServiceImpl.java.template
- Controller.java.template
- index.vue.template
- form.vue.template
- api.ts.template

## Type Mappings

### SQL to Java
- BIGINT → Long
- VARCHAR/TEXT → String
- INT/INTEGER → Integer
- TINYINT(1) → String (for flags)
- DECIMAL → BigDecimal
- DATETIME/TIMESTAMP → Instant (new) or LocalDateTime (existing)
- DATE → LocalDate
- BIT/BOOLEAN → Boolean

### Java to TypeScript
- Long → number
- String → string
- Integer → number
- Boolean → boolean
- Instant/LocalDateTime → string

## Auto-detected Fields

The following fields are automatically configured with MyBatis Plus annotations:
- `create_time` - @TableField(fill = FieldFill.INSERT)
- `update_time` - @TableField(fill = FieldFill.UPDATE)
- `create_by` - @TableField(fill = FieldFill.INSERT)
- `update_by` - @TableField(fill = FieldFill.UPDATE)
- `del_flag` - @TableLogic

## Post-generation

After code generation, the skill will:
1. Format Java code using `mvn spring-javaformat:apply`
2. Verify all files were created successfully
3. Display a checklist of next steps

## Example

Input:
```sql
CREATE TABLE `gym_member` (
  `id` bigint NOT NULL COMMENT 'ID',
  `name` varchar(64) NOT NULL COMMENT 'Member Name',
  `phone` varchar(20) COMMENT 'Phone',
  `status` char(1) DEFAULT '0' COMMENT 'Status',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(64),
  `update_by` varchar(64),
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`)
);
```

Module: `pig-gym`
Frontend: `gym`

Output:
- Entity: `pig-gym-api/.../entity/GymMember.java`
- Mapper: `pig-gym-biz/.../mapper/GymMemberMapper.java`
- Service: `pig-gym-biz/.../service/GymMemberService.java`
- ServiceImpl: `pig-gym-biz/.../service/impl/GymMemberServiceImpl.java`
- Controller: `pig-gym-biz/.../controller/GymMemberController.java`
- Vue pages: `pig-ui/src/views/admin/gym/member/`
- API: `pig-ui/src/api/admin/gymmember.ts`
