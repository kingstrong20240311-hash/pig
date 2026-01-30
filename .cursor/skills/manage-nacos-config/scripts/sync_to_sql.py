#!/usr/bin/env python3
"""
Nacos 配置同步到 pig_config.sql 工具
从 Nacos 获取最新配置并更新到 SQL 文件
"""

import argparse
import hashlib
import os
import re
import sys
from datetime import datetime
from typing import Dict, List, Optional

try:
    import requests
except ImportError:
    print("错误: 缺少 requests 库")
    print("请运行: pip install requests")
    sys.exit(1)


class ConfigSyncTool:
    """配置同步工具"""

    def __init__(
        self,
        nacos_host: str = "127.0.0.1",
        nacos_port: int = 8848,
        namespace: str = "public",
        username: str = "nacos",
        password: str = "nacos",
        sql_file: str = "db/pig_config.sql",
    ):
        self.nacos_host = nacos_host
        self.nacos_port = nacos_port
        self.namespace = namespace
        self.username = username
        self.password = password
        self.sql_file = sql_file
        self.base_url = f"http://{nacos_host}:{nacos_port}/nacos/v1/cs"
        self.base_url_v3 = f"http://{nacos_host}:{nacos_port}/nacos/v3/console/cs"
        self.auth = (username, password) if username and password else None

    def get_config_from_nacos(
        self, data_id: str, group: str = "DEFAULT_GROUP"
    ) -> Optional[str]:
        """从 Nacos 获取配置"""
        url = f"{self.base_url}/configs"
        params = {
            "dataId": data_id,
            "group": group,
            "tenant": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.get(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            return response.text
        except requests.exceptions.RequestException as e:
            print(f"错误: 从 Nacos 获取配置失败: {e}")
            return None

    def list_configs_from_nacos(self) -> List[Dict]:
        """列出 Nacos 中所有配置 - 使用 v3 API"""
        url = f"{self.base_url_v3}/config/list"
        params = {
            "pageNo": 1,
            "pageSize": 500,
            "namespaceId": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.get(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            result = response.json()
            
            # v3 API 返回格式: {"code": 0, "data": {"totalCount": x, "pageItems": [...]}}
            if result.get("code") == 0:
                data = result.get("data", {})
                return data.get("pageItems", [])
            else:
                print(f"错误: Nacos 返回错误: {result.get('message', '未知错误')}")
                return []
        except requests.exceptions.RequestException as e:
            print(f"错误: 列出配置失败: {e}")
            return []

    def calculate_md5(self, content: str) -> str:
        """计算 MD5"""
        return hashlib.md5(content.encode("utf-8")).hexdigest()

    def escape_sql_string(self, s: str) -> str:
        """转义 SQL 字符串"""
        # 转义反斜杠和单引号
        s = s.replace("\\", "\\\\")
        s = s.replace("'", "\\'")
        return s

    def backup_sql_file(self):
        """备份 SQL 文件"""
        if not os.path.exists(self.sql_file):
            print(f"警告: SQL 文件不存在: {self.sql_file}")
            return

        backup_file = f"{self.sql_file}.bak"
        try:
            with open(self.sql_file, "r", encoding="utf-8") as f:
                content = f.read()
            with open(backup_file, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"✓ 已备份到: {backup_file}")
        except Exception as e:
            print(f"警告: 备份失败: {e}")

    def update_config_in_sql(
        self, data_id: str, group: str, content: str
    ) -> bool:
        """更新 SQL 文件中的配置"""
        if not os.path.exists(self.sql_file):
            print(f"错误: SQL 文件不存在: {self.sql_file}")
            return False

        # 读取 SQL 文件
        try:
            with open(self.sql_file, "r", encoding="utf-8") as f:
                sql_content = f.read()
        except Exception as e:
            print(f"错误: 读取 SQL 文件失败: {e}")
            return False

        # 计算 MD5
        md5 = self.calculate_md5(content)

        # 当前时间
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        # 转义配置内容
        escaped_content = self.escape_sql_string(content)

        # 查找并替换对应的配置记录
        # INSERT INTO `config_info` VALUES (id,'data_id','group','content','md5',...)
        
        # 使用正则匹配整个配置记录
        pattern = r"\((\d+),'{}','{}','[^']*(?:\\'[^']*)*','[^']*','[^']*','[^']*'".format(
            re.escape(data_id), re.escape(group)
        )

        match = re.search(pattern, sql_content)

        if not match:
            print(f"错误: 在 SQL 文件中未找到配置: {data_id} ({group})")
            return False

        # 提取 ID
        config_id = match.group(1)

        # 构建替换模式 - 匹配整个元组
        full_pattern = r"\({},'{}'.*?\)(?=,\(|\);)".format(
            config_id, re.escape(data_id)
        )

        # 查找完整的配置元组
        full_match = re.search(full_pattern, sql_content, re.DOTALL)

        if not full_match:
            print(f"错误: 无法匹配完整的配置记录")
            return False

        old_tuple = full_match.group(0)

        # 解析旧元组的字段
        # 格式: (id, data_id, group_id, content, md5, create_time, modify_time, ...)
        # 我们需要保留除 content, md5, modify_time 外的所有字段

        # 简单方法: 使用正则提取关键字段
        old_parts = re.match(
            r"\((\d+),'([^']+)','([^']+)','[^']*(?:\\'[^']*)*','[^']*','([^']+)','[^']+',(.+)\)$",
            old_tuple,
            re.DOTALL,
        )

        if not old_parts:
            print(f"错误: 无法解析旧配置记录")
            return False

        config_id = old_parts.group(1)
        create_time = old_parts.group(4)
        remaining_fields = old_parts.group(5)

        # 构建新元组
        new_tuple = f"({config_id},'{data_id}','{group}','{escaped_content}','{md5}','{create_time}','{now}',{remaining_fields})"

        # 替换
        new_sql_content = sql_content.replace(old_tuple, new_tuple)

        # 写回文件
        try:
            with open(self.sql_file, "w", encoding="utf-8") as f:
                f.write(new_sql_content)
            return True
        except Exception as e:
            print(f"错误: 写入 SQL 文件失败: {e}")
            return False

    def sync_single_config(self, data_id: str, group: str = "DEFAULT_GROUP") -> bool:
        """同步单个配置"""
        print(f"同步配置: {data_id} ({group})")

        # 从 Nacos 获取配置
        content = self.get_config_from_nacos(data_id, group)
        if content is None:
            return False

        # 备份 SQL 文件
        self.backup_sql_file()

        # 更新 SQL 文件
        success = self.update_config_in_sql(data_id, group, content)

        if success:
            print(f"✓ 成功同步: {data_id}")
            md5 = self.calculate_md5(content)
            print(f"  MD5: {md5}")
            print(f"  长度: {len(content)} 字符")
        else:
            print(f"✗ 同步失败: {data_id}")

        return success

    def sync_all_configs(self) -> int:
        """同步所有配置"""
        print("获取 Nacos 中所有配置...")
        configs = self.list_configs_from_nacos()

        if not configs:
            print("未找到配置")
            return 0

        print(f"找到 {len(configs)} 个配置\n")

        # 备份一次
        self.backup_sql_file()

        success_count = 0
        for config in configs:
            data_id = config.get("dataId", "")
            group = config.get("group", "DEFAULT_GROUP")

            print(f"[{success_count + 1}/{len(configs)}] 同步: {data_id}")

            content = self.get_config_from_nacos(data_id, group)
            if content and self.update_config_in_sql(data_id, group, content):
                success_count += 1
                print(f"  ✓ 成功")
            else:
                print(f"  ✗ 失败")

        print(f"\n完成: 成功 {success_count}/{len(configs)}")
        return success_count

    def export_all_configs(self, output_file: Optional[str] = None):
        """导出所有配置到新的 SQL 文件"""
        if output_file is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_file = f"db/pig_config_export_{timestamp}.sql"

        print(f"导出配置到: {output_file}")

        configs = self.list_configs_from_nacos()
        if not configs:
            print("未找到配置")
            return

        # 读取 SQL 模板(表结构部分)
        if os.path.exists(self.sql_file):
            with open(self.sql_file, "r", encoding="utf-8") as f:
                template = f.read()

            # 提取表结构部分(INSERT 之前的所有内容)
            insert_pos = template.find("INSERT INTO `config_info`")
            if insert_pos > 0:
                header = template[:insert_pos]
            else:
                header = ""
        else:
            header = ""

        # 构建 INSERT 语句
        insert_values = []
        for i, config in enumerate(configs, 1):
            data_id = config.get("dataId", "")
            group = config.get("group", "DEFAULT_GROUP")

            content = self.get_config_from_nacos(data_id, group)
            if not content:
                continue

            md5 = self.calculate_md5(content)
            escaped_content = self.escape_sql_string(content)

            now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            # 构建元组
            value = f"({i},'{data_id}','{group}','{escaped_content}','{md5}','{now}','{now}','nacos','127.0.0.1','','{self.namespace}','',NULL,NULL,'yaml',NULL,'')"
            insert_values.append(value)

        # 组装 SQL
        insert_statement = "INSERT INTO `config_info` VALUES " + ",".join(
            insert_values
        ) + ";\n"

        # 写入文件
        full_sql = header + insert_statement

        # 添加表尾
        full_sql += """/*!40000 ALTER TABLE `config_info` ENABLE KEYS */;
UNLOCK TABLES;
"""

        with open(output_file, "w", encoding="utf-8") as f:
            f.write(full_sql)

        print(f"✓ 成功导出 {len(insert_values)} 个配置到: {output_file}")


def main():
    parser = argparse.ArgumentParser(
        description="Nacos 配置同步到 pig_config.sql 工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )

    parser.add_argument(
        "--host", default=os.getenv("NACOS_HOST", "127.0.0.1"), help="Nacos 主机地址"
    )
    parser.add_argument(
        "--port",
        type=int,
        default=int(os.getenv("NACOS_PORT", "8848")),
        help="Nacos 端口",
    )
    parser.add_argument(
        "--namespace",
        default=os.getenv("NACOS_NAMESPACE", "public"),
        help="命名空间",
    )
    parser.add_argument(
        "--username",
        default=os.getenv("NACOS_USERNAME", "nacos"),
        help="Nacos 用户名",
    )
    parser.add_argument(
        "--password",
        default=os.getenv("NACOS_PASSWORD", "nacos"),
        help="Nacos 密码",
    )
    parser.add_argument(
        "--sql-file", default="db/pig_config.sql", help="SQL 文件路径"
    )

    # 操作模式
    mode_group = parser.add_mutually_exclusive_group(required=True)
    mode_group.add_argument("--all", action="store_true", help="同步所有配置")
    mode_group.add_argument("--export", action="store_true", help="导出所有配置到新文件")
    mode_group.add_argument("--data-id", help="同步单个配置的 DataId")

    parser.add_argument("--group", default="DEFAULT_GROUP", help="配置分组")
    parser.add_argument("--output", help="导出文件路径(仅用于 --export)")

    args = parser.parse_args()

    # 创建同步工具
    tool = ConfigSyncTool(
        nacos_host=args.host,
        nacos_port=args.port,
        namespace=args.namespace,
        username=args.username,
        password=args.password,
        sql_file=args.sql_file,
    )

    # 执行操作
    if args.all:
        tool.sync_all_configs()
    elif args.export:
        tool.export_all_configs(args.output)
    elif args.data_id:
        success = tool.sync_single_config(args.data_id, args.group)
        sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
