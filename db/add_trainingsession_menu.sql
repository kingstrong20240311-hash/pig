-- 为「健身房管理」新增「训练课程」菜单（trainingsession）
-- 说明：
-- 1) 自动按名称定位父菜单：健身房管理
-- 2) 幂等执行：重复执行不会重复插入
-- 3) 自动补齐按钮权限并赋权给管理员角色（按角色编码/名称匹配，不写死 role_id）

USE `pig`;

SET @parent_gym := (
	SELECT menu_id
	FROM sys_menu
	WHERE name = '健身房管理'
		AND menu_type = '0'
		AND del_flag = '0'
	ORDER BY menu_id
	LIMIT 1
);

-- 目录下菜单排序
SET @menu_sort := (
	SELECT IFNULL(MAX(sort_order), 0) + 1
	FROM sys_menu
	WHERE parent_id = @parent_gym
		AND menu_type IN ('0', '1')
		AND del_flag = '0'
);

-- 若不存在主菜单则创建
SET @menu_id_candidate := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@menu_id_candidate, '训练课程', 'trainingsession', NULL, '/admin/gym/trainingsession/index', @parent_gym, 'ele-Calendar',
	'1', @menu_sort, '0', '0', '0', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @parent_gym IS NOT NULL
	AND NOT EXISTS (
		SELECT 1
		FROM sys_menu
		WHERE parent_id = @parent_gym
			AND path = '/admin/gym/trainingsession/index'
			AND del_flag = '0'
	);

-- 获取最终主菜单ID（无论是新建还是已存在）
SET @menu_trainingsession := (
	SELECT menu_id
	FROM sys_menu
	WHERE parent_id = @parent_gym
		AND path = '/admin/gym/trainingsession/index'
		AND del_flag = '0'
	ORDER BY menu_id
	LIMIT 1
);

-- 新增按钮权限
SET @btn_add_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_add_id, '训练课程新增', NULL, 'gym_trainingsession_add', NULL, @menu_trainingsession, NULL,
	'1', 1, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_trainingsession IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingsession_add' AND del_flag = '0');

SET @btn_edit_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_edit_id, '训练课程修改', NULL, 'gym_trainingsession_edit', NULL, @menu_trainingsession, NULL,
	'1', 2, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_trainingsession IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingsession_edit' AND del_flag = '0');

SET @btn_del_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_del_id, '训练课程删除', NULL, 'gym_trainingsession_del', NULL, @menu_trainingsession, NULL,
	'1', 3, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_trainingsession IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingsession_del' AND del_flag = '0');

SET @btn_export_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_export_id, '训练课程导出', NULL, 'gym_trainingsession_export', NULL, @menu_trainingsession, NULL,
	'1', 4, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_trainingsession IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingsession_export' AND del_flag = '0');

-- 赋权给管理员角色（固定 role_id=1）
SET @admin_role_id := 1;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, m.menu_id
FROM sys_menu m
WHERE m.del_flag = '0'
	AND (
		m.menu_id = @parent_gym
		OR
		m.menu_id = @menu_trainingsession
		OR m.permission IN (
			'gym_trainingsession_add',
			'gym_trainingsession_edit',
			'gym_trainingsession_del',
			'gym_trainingsession_export'
		)
	);

SELECT
	@parent_gym AS gym_parent_menu_id,
	@menu_trainingsession AS trainingsession_menu_id,
	@admin_role_id AS authorized_admin_role_id,
	(
		SELECT COUNT(1)
		FROM sys_role_menu
		WHERE role_id = @admin_role_id
			AND menu_id IN (
				@parent_gym,
				@menu_trainingsession,
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingsession_add' AND del_flag = '0' LIMIT 1),
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingsession_edit' AND del_flag = '0' LIMIT 1),
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingsession_del' AND del_flag = '0' LIMIT 1),
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingsession_export' AND del_flag = '0' LIMIT 1)
			)
	) AS granted_menu_count,
	IF(@parent_gym IS NULL, '未找到父菜单【健身房管理】', '处理完成') AS result;
