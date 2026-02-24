# gen-crud Skill Usage Example

## Complete Example: Gym Member Management

### Step 1: Invoke the Skill

```
/gen-crud
```

### Step 2: Provide Required Information

#### SQL Schema
```sql
CREATE TABLE `gym_member` (
  `id` bigint NOT NULL COMMENT 'Member ID',
  `member_name` varchar(64) NOT NULL COMMENT 'Member Name',
  `phone` varchar(20) COMMENT 'Phone Number',
  `email` varchar(64) COMMENT 'Email',
  `gender` char(1) DEFAULT '0' COMMENT 'Gender 0:Male 1:Female',
  `birth_date` date COMMENT 'Birth Date',
  `status` char(1) DEFAULT '0' COMMENT 'Status 0:Active 1:Inactive',
  `membership_type` varchar(32) COMMENT 'Membership Type',
  `join_date` datetime COMMENT 'Join Date',
  `expire_date` datetime COMMENT 'Membership Expire Date',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
  `create_by` varchar(64) COMMENT 'Created By',
  `update_by` varchar(64) COMMENT 'Updated By',
  `del_flag` char(1) DEFAULT '0' COMMENT 'Delete Flag 0:Normal 1:Deleted',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Gym Member Information';
```

#### Module Information
- **Java Module**: `pig-gym`
- **Frontend Path**: `gym`

### Step 3: Generated Files

The skill will generate the following files:

#### Backend (Java)

1. **Entity**: `pig-gym/pig-gym-api/src/main/java/com/pig4cloud/pig/gym/api/entity/GymMember.java`
```java
// Extends BaseEntity — createTime, updateTime, createBy, updateBy are inherited, do NOT redeclare them
@Data
@Schema(description = "Gym Member Information")
@EqualsAndHashCode(callSuper = true)
public class GymMember extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Member ID")
    private Long id;

    @Schema(description = "Member Name", required = true)
    private String memberName;

    @Schema(description = "Phone Number")
    private String phone;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Gender 0:Male 1:Female")
    private String gender;

    @Schema(description = "Birth Date")
    private LocalDate birthDate;

    @Schema(description = "Status 0:Active 1:Inactive")
    private String status;

    @Schema(description = "Membership Type")
    private String membershipType;

    @Schema(description = "Join Date")
    private Instant joinDate;

    @Schema(description = "Membership Expire Date")
    private Instant expireDate;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Delete Flag")
    private String delFlag;
}
```

2. **Mapper**: `pig-gym/pig-gym-biz/src/main/java/com/pig4cloud/pig/gym/mapper/GymMemberMapper.java`
```java
@Mapper
public interface GymMemberMapper extends BaseMapper<GymMember> {
}
```

3. **Service**: `pig-gym/pig-gym-biz/src/main/java/com/pig4cloud/pig/gym/service/GymMemberService.java`
```java
public interface GymMemberService extends IService<GymMember> {
}
```

4. **ServiceImpl**: `pig-gym/pig-gym-biz/src/main/java/com/pig4cloud/pig/gym/service/impl/GymMemberServiceImpl.java`
```java
@Service
@AllArgsConstructor
public class GymMemberServiceImpl extends ServiceImpl<GymMemberMapper, GymMember>
        implements GymMemberService {
}
```

5. **Controller**: `pig-gym/pig-gym-biz/src/main/java/com/pig4cloud/pig/gym/controller/GymMemberController.java`
```java
@RestController
@AllArgsConstructor
@RequestMapping("/member")
@Tag(description = "member", name = "Gym Member Information管理")
@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
public class GymMemberController {

    private final GymMemberService gymMemberService;

    @GetMapping("/page")
    @Operation(description = "分页查询", summary = "分页查询")
    public R getGymMemberPage(@ParameterObject Page page, @ParameterObject GymMember gymMember) {
        LambdaQueryWrapper<GymMember> wrapper = Wrappers.<GymMember>lambdaQuery()
            .like(StrUtil.isNotBlank(gymMember.getMemberName()), GymMember::getMemberName, gymMember.getMemberName())
            .eq(StrUtil.isNotBlank(gymMember.getPhone()), GymMember::getPhone, gymMember.getPhone())
            .eq(StrUtil.isNotBlank(gymMember.getStatus()), GymMember::getStatus, gymMember.getStatus());
        return R.ok(gymMemberService.page(page, wrapper));
    }

    @GetMapping("/details/{id}")
    @Operation(description = "通过ID查询", summary = "通过ID查询")
    public R getById(@PathVariable("id") Long id) {
        return R.ok(gymMemberService.getById(id));
    }

    @PostMapping
    @SysLog("新增Gym Member Information")
    @HasPermission("gym_gymmember_add")
    @Operation(description = "新增Gym Member Information", summary = "新增Gym Member Information")
    public R save(@RequestBody GymMember gymMember) {
        return R.ok(gymMemberService.save(gymMember));
    }

    @PutMapping
    @SysLog("修改Gym Member Information")
    @HasPermission("gym_gymmember_edit")
    @Operation(description = "修改Gym Member Information", summary = "修改Gym Member Information")
    public R updateById(@RequestBody GymMember gymMember) {
        return R.ok(gymMemberService.updateById(gymMember));
    }

    @DeleteMapping
    @SysLog("删除Gym Member Information")
    @HasPermission("gym_gymmember_del")
    @Operation(description = "删除Gym Member Information", summary = "删除Gym Member Information")
    public R removeById(@RequestBody Long[] ids) {
        return R.ok(gymMemberService.removeBatchByIds(Arrays.asList(ids)));
    }
}
```

#### Frontend (Vue 3 + TypeScript)

1. **List Page**: `pig-ui/src/views/admin/gym/member/index.vue`
   - Search form with filters (name, phone, status)
   - Data table with pagination
   - Action buttons (Add, Delete)
   - Row actions (Edit, Delete)

2. **Form Dialog**: `pig-ui/src/views/admin/gym/member/form.vue`
   - Form fields for all editable properties
   - Validation rules
   - Add/Edit mode handling

3. **API Client**: `pig-ui/src/api/admin/gymmember.ts`
```typescript
import request from '/@/utils/request';

export function fetchList(query?: Object) {
    return request({
        url: '/gym/member/page',
        method: 'get',
        params: query,
    });
}

export function addObj(obj?: Object) {
    return request({
        url: '/gym/member',
        method: 'post',
        data: obj,
    });
}

export function getObj(id?: number) {
    return request({
        url: '/gym/member/details/' + id,
        method: 'get',
    });
}

export function delObj(ids?: Object) {
    return request({
        url: '/gym/member',
        method: 'delete',
        data: ids,
    });
}

export function putObj(obj?: Object) {
    return request({
        url: '/gym/member',
        method: 'put',
        data: obj,
    });
}
```

4. **i18n Files**:
   - `pig-ui/src/views/admin/gym/member/i18n/zh-cn.ts` (Chinese)
   - `pig-ui/src/views/admin/gym/member/i18n/en.ts` (English)

### Step 4: Post-generation Tasks

The skill will automatically:
1. ✅ Format Java code with `mvn spring-javaformat:apply`
2. ✅ Create all necessary directories
3. ✅ Generate all files from templates

### Step 5: Manual Configuration (if needed)

You may need to:
1. Add menu entry in the admin UI
2. Configure routing for the new page
3. Add permission records to the database
4. Customize query conditions in Controller
5. Add custom business logic to Service

## Another Example: Simple Configuration Table

### Input

```sql
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL,
  `config_key` varchar(128) NOT NULL COMMENT 'Config Key',
  `config_value` text COMMENT 'Config Value',
  `config_desc` varchar(255) COMMENT 'Description',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(64),
  `update_by` varchar(64),
  `del_flag` char(1) DEFAULT '0',
  PRIMARY KEY (`id`)
);
```

- **Module**: `pig-upms`
- **Frontend**: `sys`

### Generated Structure

```
pig-upms/
  pig-upms-api/
    src/main/java/com/pig4cloud/pig/admin/api/entity/
      SysConfig.java
  pig-upms-biz/
    src/main/java/com/pig4cloud/pig/admin/
      mapper/
        SysConfigMapper.java
      service/
        SysConfigService.java
        impl/
          SysConfigServiceImpl.java
      controller/
        SysConfigController.java

pig-ui/
  src/
    api/admin/
      sysconfig.ts
    views/admin/sys/config/
      index.vue
      form.vue
      i18n/
        zh-cn.ts
        en.ts
```

## Tips

1. **Table Naming**: Use lowercase with underscores (e.g., `gym_member`, `sys_config`)
2. **Primary Key**: Always use `id` as the primary key field name for consistency
3. **Comments**: Add SQL comments - they become JavaDoc and Schema descriptions
4. **Required Fields**: Mark NOT NULL fields in SQL - they get `required: true` in validation
5. **Field Types**: Follow project conventions for time fields (Instant for new, LocalDateTime for existing)
