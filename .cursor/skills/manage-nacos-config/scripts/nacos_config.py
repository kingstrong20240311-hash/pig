#!/usr/bin/env python3
"""
Nacos 配置管理工具
通过 Nacos Open API 管理配置中心的配置项
"""

import argparse
import hashlib
import os
import sys
from typing import Dict, List, Optional

try:
    import requests
except ImportError:
    print("错误: 缺少 requests 库")
    print("请运行: pip install requests")
    sys.exit(1)


class NacosConfigClient:
    """Nacos 配置客户端"""

    def __init__(
        self,
        host: str = "127.0.0.1",
        port: int = 8848,
        namespace: str = "public",
        username: str = "nacos",
        password: str = "nacos",
    ):
        self.host = host
        self.port = port
        self.namespace = namespace
        self.username = username
        self.password = password
        self.base_url = f"http://{host}:{port}/nacos/v1/cs"
        self.base_url_v3 = f"http://{host}:{port}/nacos/v3/console/cs"
        self.auth = (username, password) if username and password else None

    def list_configs_from_sql(self, sql_file: str = "db/pig_config.sql") -> List[Dict]:
        """从 pig_config.sql 文件读取配置列表"""
        import re
        
        if not os.path.exists(sql_file):
            print(f"警告: SQL 文件不存在: {sql_file}")
            return []
        
        try:
            with open(sql_file, "r", encoding="utf-8") as f:
                content = f.read()
            
            # 查找 INSERT INTO config_info VALUES 语句
            pattern = r"\((\d+),'([^']+)','([^']+)'"
            matches = re.findall(pattern, content)
            
            configs = []
            for match in matches:
                config_id, data_id, group = match
                configs.append({
                    "id": config_id,
                    "dataId": data_id,
                    "group": group,
                })
            
            return configs
        except Exception as e:
            print(f"错误: 读取 SQL 文件失败: {e}")
            return []

    def get_config(self, data_id: str, group: str = "DEFAULT_GROUP") -> Optional[str]:
        """获取配置内容"""
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
            print(f"错误: 获取配置失败: {e}")
            return None

    def update_config(
        self,
        data_id: str,
        group: str,
        content: str,
        config_type: str = "yaml",
        desc: str = "",
    ) -> bool:
        """更新配置"""
        url = f"{self.base_url}/configs"
        data = {
            "dataId": data_id,
            "group": group,
            "content": content,
            "type": config_type,
            "desc": desc,
            "tenant": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.post(url, data=data, auth=self.auth, timeout=10)
            response.raise_for_status()
            result = response.text
            if result.lower() == "true":
                return True
            else:
                print(f"错误: Nacos 返回 false")
                return False
        except requests.exceptions.RequestException as e:
            print(f"错误: 更新配置失败: {e}")
            return False

    def delete_config(self, data_id: str, group: str = "DEFAULT_GROUP") -> bool:
        """删除配置"""
        url = f"{self.base_url}/configs"
        params = {
            "dataId": data_id,
            "group": group,
            "tenant": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.delete(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            result = response.text
            return result.lower() == "true"
        except requests.exceptions.RequestException as e:
            print(f"错误: 删除配置失败: {e}")
            return False


def cmd_list(client: NacosConfigClient, args):
    """列出所有配置 - 从 SQL 文件读取"""
    print("注意: 从 db/pig_config.sql 读取配置列表\n")
    
    configs = client.list_configs_from_sql(args.sql_file)
    
    if configs:
        total = len(configs)
        print(f"总配置数: {total}\n")
        
        # 分页显示
        start = (args.page - 1) * args.size
        end = start + args.size
        page_configs = configs[start:end]
        
        for config in page_configs:
            data_id = config.get("dataId", "")
            group = config.get("group", "")
            config_id = config.get("id", "")
            
            print(f"ID: {config_id}")
            print(f"DataId: {data_id}")
            print(f"Group: {group}")
            
            # 尝试从 Nacos 获取配置状态
            content = client.get_config(data_id, group)
            if content:
                print(f"状态: ✓ 存在于 Nacos")
                print(f"大小: {len(content)} 字符")
            else:
                print(f"状态: ✗ 不在 Nacos 中")
            
            print("-" * 60)
        
        if total > args.size:
            print(f"\n显示第 {args.page} 页,共 {(total + args.size - 1) // args.size} 页")
    else:
        print("未找到配置")


def cmd_get(client: NacosConfigClient, args):
    """获取配置详情"""
    content = client.get_config(args.data_id, args.group)

    if content:
        print(f"DataId: {args.data_id}")
        print(f"Group: {args.group}")
        print(f"Namespace: {client.namespace}")
        print("-" * 60)
        print(content)
    else:
        print(f"错误: 配置不存在或获取失败")
        sys.exit(1)


def cmd_update(client: NacosConfigClient, args):
    """更新配置"""
    # 获取配置内容
    if args.file:
        if not os.path.exists(args.file):
            print(f"错误: 文件不存在: {args.file}")
            sys.exit(1)

        with open(args.file, "r", encoding="utf-8") as f:
            content = f.read()
    elif args.content:
        content = args.content
    else:
        print("错误: 必须提供 --content 或 --file 参数")
        sys.exit(1)

    # 更新配置
    success = client.update_config(
        data_id=args.data_id,
        group=args.group,
        content=content,
        config_type=args.type,
        desc=args.desc or "",
    )

    if success:
        print(f"✓ 成功更新配置: {args.data_id}")
        print(f"  Group: {args.group}")
        print(f"  Namespace: {client.namespace}")

        # 计算并显示 MD5
        md5 = hashlib.md5(content.encode("utf-8")).hexdigest()
        print(f"  MD5: {md5}")
    else:
        sys.exit(1)


def cmd_delete(client: NacosConfigClient, args):
    """删除配置"""
    if not args.force:
        confirm = input(f"确定要删除配置 '{args.data_id}' 吗? (yes/no): ")
        if confirm.lower() != "yes":
            print("已取消")
            return

    success = client.delete_config(args.data_id, args.group)

    if success:
        print(f"✓ 成功删除配置: {args.data_id}")
    else:
        print(f"✗ 删除配置失败")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description="Nacos 配置管理工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )

    # 全局参数
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

    subparsers = parser.add_subparsers(dest="command", help="子命令")

    # list 命令
    parser_list = subparsers.add_parser("list", help="列出所有配置(从 SQL 文件读取)")
    parser_list.add_argument("--page", type=int, default=1, help="页码")
    parser_list.add_argument("--size", type=int, default=20, help="每页大小")
    parser_list.add_argument(
        "--sql-file", default="db/pig_config.sql", help="SQL 文件路径"
    )

    # get 命令
    parser_get = subparsers.add_parser("get", help="获取配置详情")
    parser_get.add_argument("--data-id", required=True, help="配置 ID")
    parser_get.add_argument("--group", default="DEFAULT_GROUP", help="配置分组")

    # update 命令
    parser_update = subparsers.add_parser("update", help="更新配置")
    parser_update.add_argument("--data-id", required=True, help="配置 ID")
    parser_update.add_argument("--group", default="DEFAULT_GROUP", help="配置分组")
    parser_update.add_argument("--content", help="配置内容")
    parser_update.add_argument("--file", help="配置文件路径")
    parser_update.add_argument("--type", default="yaml", help="配置类型")
    parser_update.add_argument("--desc", help="配置描述")

    # delete 命令
    parser_delete = subparsers.add_parser("delete", help="删除配置")
    parser_delete.add_argument("--data-id", required=True, help="配置 ID")
    parser_delete.add_argument("--group", default="DEFAULT_GROUP", help="配置分组")
    parser_delete.add_argument("--force", action="store_true", help="强制删除不确认")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    # 创建客户端
    client = NacosConfigClient(
        host=args.host,
        port=args.port,
        namespace=args.namespace,
        username=args.username,
        password=args.password,
    )

    # 执行命令
    if args.command == "list":
        cmd_list(client, args)
    elif args.command == "get":
        cmd_get(client, args)
    elif args.command == "update":
        cmd_update(client, args)
    elif args.command == "delete":
        cmd_delete(client, args)


if __name__ == "__main__":
    main()
