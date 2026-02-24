-- 为「健身房管理」新增「训练动作素材」菜单（trainingexercisemedia）
-- 说明：
-- 1) 自动按名称定位父菜单：健身房管理
-- 2) 幂等执行：重复执行不会重复插入
-- 3) 自动补齐按钮权限并赋权给管理员角色（固定 role_id=1）
-- 4) 递归授权整条祖先菜单链，确保菜单可见

USE `pig`;

SET @parent_menu := (
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
	WHERE parent_id = @parent_menu
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
	@menu_id_candidate, '训练动作素材', 'trainingexercisemedia', NULL, '/admin/gym/trainingexercisemedia/index', @parent_menu, 'ele-Film',
	'1', @menu_sort, '0', '0', '0', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @parent_menu IS NOT NULL
	AND NOT EXISTS (
		SELECT 1
		FROM sys_menu
		WHERE parent_id = @parent_menu
			AND path = '/admin/gym/trainingexercisemedia/index'
			AND del_flag = '0'
	);

-- 获取最终主菜单ID（无论是新建还是已存在）
SET @menu_entity := (
	SELECT menu_id
	FROM sys_menu
	WHERE parent_id = @parent_menu
		AND path = '/admin/gym/trainingexercisemedia/index'
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
	@btn_add_id, '训练动作素材新增', NULL, 'gym_trainingexercisemedia_add', NULL, @menu_entity, NULL,
	'1', 1, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_entity IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_add' AND del_flag = '0');

SET @btn_edit_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_edit_id, '训练动作素材修改', NULL, 'gym_trainingexercisemedia_edit', NULL, @menu_entity, NULL,
	'1', 2, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_entity IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_edit' AND del_flag = '0');

SET @btn_del_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_del_id, '训练动作素材删除', NULL, 'gym_trainingexercisemedia_del', NULL, @menu_entity, NULL,
	'1', 3, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_entity IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_del' AND del_flag = '0');

SET @btn_export_id := (SELECT IFNULL(MAX(menu_id), 10000) + 1 FROM sys_menu);
INSERT INTO sys_menu (
	menu_id, name, en_name, permission, path, parent_id, icon, visible,
	sort_order, keep_alive, embedded, menu_type, create_by, create_time, update_by, update_time, del_flag
)
SELECT
	@btn_export_id, '训练动作素材导出', NULL, 'gym_trainingexercisemedia_export', NULL, @menu_entity, NULL,
	'1', 4, '0', NULL, '1', 'admin', NOW(), 'admin', NOW(), '0'
FROM dual
WHERE @menu_entity IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_export' AND del_flag = '0');

-- -------------------------------------------------------------------------
-- 授权给管理员角色（role_id=1）
-- 使用递归 CTE 遍历菜单本身及所有祖先，确保整条菜单链都已授权，
-- 否则即使子菜单授权了，父级未授权时前端也不会显示。
-- -------------------------------------------------------------------------
SET @admin_role_id := 1;

-- 1. 递归授权菜单本身及全部祖先节点（parent_id=0 为根，停止）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
WITH RECURSIVE menu_ancestors AS (
	SELECT menu_id, parent_id
	FROM sys_menu
	WHERE menu_id = @menu_entity
		AND del_flag = '0'
	UNION ALL
	SELECT m.menu_id, m.parent_id
	FROM sys_menu m
	INNER JOIN menu_ancestors a ON m.menu_id = a.parent_id
	WHERE m.parent_id != 0
		AND m.del_flag = '0'
)
SELECT @admin_role_id, menu_id FROM menu_ancestors;

-- 2. 授权四个按钮权限菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT @admin_role_id, m.menu_id
FROM sys_menu m
WHERE m.del_flag = '0'
	AND m.permission IN (
		'gym_trainingexercisemedia_add',
		'gym_trainingexercisemedia_edit',
		'gym_trainingexercisemedia_del',
		'gym_trainingexercisemedia_export'
	);

-- 结果汇总
SELECT
	@parent_menu AS parent_menu_id,
	@menu_entity AS entity_menu_id,
	@admin_role_id AS authorized_admin_role_id,
	(
		SELECT COUNT(1)
		FROM sys_role_menu
		WHERE role_id = @admin_role_id
			AND menu_id IN (
				@parent_menu,
				@menu_entity,
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_add' AND del_flag = '0' LIMIT 1),
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_edit' AND del_flag = '0' LIMIT 1),
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_del' AND del_flag = '0' LIMIT 1),
				(SELECT menu_id FROM sys_menu WHERE permission = 'gym_trainingexercisemedia_export' AND del_flag = '0' LIMIT 1)
			)
	) AS granted_menu_count,
	IF(@parent_menu IS NULL, '未找到父菜单【健身房管理】', '处理完成') AS result;
