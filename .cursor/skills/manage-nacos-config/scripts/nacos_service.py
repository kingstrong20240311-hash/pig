#!/usr/bin/env python3
"""
Nacos 服务管理工具
通过 Nacos Open API 管理服务注册与发现
"""

import argparse
import json
import os
import sys
from typing import Dict, List, Optional

try:
    import requests
except ImportError:
    print("错误: 缺少 requests 库")
    print("请运行: pip install requests")
    sys.exit(1)


class NacosServiceClient:
    """Nacos 服务客户端"""

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
        self.base_url = f"http://{host}:{port}/nacos/v1/ns"
        self.auth = (username, password) if username and password else None

    def list_services(self, page_no: int = 1, page_size: int = 100, group: str = "DEFAULT_GROUP") -> Dict:
        """列出所有服务"""
        url = f"{self.base_url}/service/list"
        params = {
            "pageNo": page_no,
            "pageSize": page_size,
            "groupName": group,
            "namespaceId": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.get(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"错误: 无法连接到 Nacos 服务器: {e}")
            sys.exit(1)

    def get_service_detail(self, service_name: str, group: str = "DEFAULT_GROUP") -> Optional[Dict]:
        """获取服务详情"""
        url = f"{self.base_url}/service"
        params = {
            "serviceName": service_name,
            "groupName": group,
            "namespaceId": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.get(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            print(f"错误: 获取服务详情失败: {e}")
            return None

    def list_instances(
        self, service_name: str, group: str = "DEFAULT_GROUP", healthy_only: bool = False
    ) -> List[Dict]:
        """列出服务实例"""
        url = f"{self.base_url}/instance/list"
        params = {
            "serviceName": service_name,
            "groupName": group,
            "namespaceId": self.namespace if self.namespace != "public" else "",
            "healthyOnly": str(healthy_only).lower(),
        }

        try:
            response = requests.get(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            result = response.json()
            return result.get("hosts", [])
        except requests.exceptions.RequestException as e:
            print(f"错误: 获取服务实例失败: {e}")
            return []

    def update_instance_health(
        self,
        service_name: str,
        ip: str,
        port: int,
        healthy: bool,
        group: str = "DEFAULT_GROUP",
    ) -> bool:
        """更新实例健康状态"""
        url = f"{self.base_url}/instance"
        data = {
            "serviceName": service_name,
            "groupName": group,
            "ip": ip,
            "port": port,
            "healthy": str(healthy).lower(),
            "namespaceId": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.put(url, data=data, auth=self.auth, timeout=10)
            response.raise_for_status()
            result = response.text
            return result.lower() == "ok"
        except requests.exceptions.RequestException as e:
            print(f"错误: 更新实例健康状态失败: {e}")
            return False

    def deregister_instance(
        self, service_name: str, ip: str, port: int, group: str = "DEFAULT_GROUP"
    ) -> bool:
        """下线服务实例"""
        url = f"{self.base_url}/instance"
        params = {
            "serviceName": service_name,
            "groupName": group,
            "ip": ip,
            "port": port,
            "namespaceId": self.namespace if self.namespace != "public" else "",
        }

        try:
            response = requests.delete(url, params=params, auth=self.auth, timeout=10)
            response.raise_for_status()
            result = response.text
            return result.lower() == "ok"
        except requests.exceptions.RequestException as e:
            print(f"错误: 下线实例失败: {e}")
            return False


def cmd_list_services(client: NacosServiceClient, args):
    """列出所有服务"""
    result = client.list_services(page_no=args.page, page_size=args.size, group=args.group)

    services = result.get("doms", [])
    count = result.get("count", 0)

    print(f"总服务数: {count}")
    print(f"当前页: {args.page}\n")

    if services:
        for service_name in services:
            print(f"服务名称: {service_name}")
            
            # 获取实例信息
            if args.with_instances:
                instances = client.list_instances(service_name, args.group)
                if instances:
                    print(f"  实例数: {len(instances)}")
                    healthy_count = sum(1 for inst in instances if inst.get("healthy", False))
                    print(f"  健康实例: {healthy_count}/{len(instances)}")
                else:
                    print(f"  实例数: 0")
            
            print("-" * 60)
    else:
        print("未找到服务")


def cmd_get_service(client: NacosServiceClient, args):
    """获取服务详情"""
    print(f"服务名称: {args.service_name}")
    print(f"分组: {args.group}")
    print(f"命名空间: {client.namespace}")
    print("-" * 60)

    # 获取服务详情
    detail = client.get_service_detail(args.service_name, args.group)
    if detail:
        print(f"\n服务详情:")
        print(f"  保护阈值: {detail.get('protectThreshold', 0)}")
        print(f"  元数据: {json.dumps(detail.get('metadata', {}), ensure_ascii=False, indent=2)}")
        print(f"  选择器: {detail.get('selector', {})}")

    # 获取实例列表
    instances = client.list_instances(args.service_name, args.group)

    if instances:
        print(f"\n实例列表 (共 {len(instances)} 个):")
        for idx, instance in enumerate(instances, 1):
            ip = instance.get("ip", "")
            port = instance.get("port", "")
            healthy = instance.get("healthy", False)
            enabled = instance.get("enabled", True)
            weight = instance.get("weight", 1.0)
            metadata = instance.get("metadata", {})

            status = "✓ 健康" if healthy else "✗ 不健康"
            enable_status = "启用" if enabled else "禁用"

            print(f"\n  [{idx}] {ip}:{port}")
            print(f"      状态: {status}")
            print(f"      启用: {enable_status}")
            print(f"      权重: {weight}")
            if metadata:
                print(f"      元数据: {json.dumps(metadata, ensure_ascii=False)}")
    else:
        print("\n未找到实例")


def cmd_list_instances(client: NacosServiceClient, args):
    """列出服务实例"""
    instances = client.list_instances(
        args.service_name, args.group, healthy_only=args.healthy_only
    )

    if instances:
        print(f"服务: {args.service_name}")
        print(f"实例数: {len(instances)}\n")

        for idx, instance in enumerate(instances, 1):
            ip = instance.get("ip", "")
            port = instance.get("port", "")
            healthy = instance.get("healthy", False)
            enabled = instance.get("enabled", True)
            weight = instance.get("weight", 1.0)

            status_icon = "✓" if healthy else "✗"
            status_text = "健康" if healthy else "不健康"

            print(f"[{idx}] {ip}:{port}")
            print(f"    状态: {status_icon} {status_text}")
            print(f"    启用: {'是' if enabled else '否'}")
            print(f"    权重: {weight}")
            print("-" * 40)
    else:
        print("未找到实例")


def cmd_update_health(client: NacosServiceClient, args):
    """更新实例健康状态"""
    success = client.update_instance_health(
        service_name=args.service_name,
        ip=args.ip,
        port=args.port,
        healthy=args.healthy,
        group=args.group,
    )

    if success:
        status = "健康" if args.healthy else "不健康"
        print(f"✓ 成功更新实例状态为: {status}")
        print(f"  服务: {args.service_name}")
        print(f"  实例: {args.ip}:{args.port}")
    else:
        print(f"✗ 更新实例状态失败")
        sys.exit(1)


def cmd_deregister(client: NacosServiceClient, args):
    """下线服务实例"""
    if not args.force:
        confirm = input(
            f"确定要下线实例 {args.ip}:{args.port} (服务: {args.service_name})? (yes/no): "
        )
        if confirm.lower() != "yes":
            print("已取消")
            return

    success = client.deregister_instance(
        service_name=args.service_name,
        ip=args.ip,
        port=args.port,
        group=args.group,
    )

    if success:
        print(f"✓ 成功下线实例")
        print(f"  服务: {args.service_name}")
        print(f"  实例: {args.ip}:{args.port}")
    else:
        print(f"✗ 下线实例失败")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description="Nacos 服务管理工具",
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
    parser.add_argument(
        "--group", default="DEFAULT_GROUP", help="服务分组"
    )

    subparsers = parser.add_subparsers(dest="command", help="子命令")

    # list 命令 - 列出所有服务
    parser_list = subparsers.add_parser("list", help="列出所有服务")
    parser_list.add_argument("--page", type=int, default=1, help="页码")
    parser_list.add_argument("--size", type=int, default=100, help="每页大小")
    parser_list.add_argument("--with-instances", action="store_true", help="显示实例统计信息")

    # get 命令 - 获取服务详情
    parser_get = subparsers.add_parser("get", help="获取服务详情")
    parser_get.add_argument("--service-name", required=True, help="服务名称")

    # instances 命令 - 列出服务实例
    parser_instances = subparsers.add_parser("instances", help="列出服务实例")
    parser_instances.add_argument("--service-name", required=True, help="服务名称")
    parser_instances.add_argument(
        "--healthy-only", action="store_true", help="只显示健康实例"
    )

    # health 命令 - 更新实例健康状态
    parser_health = subparsers.add_parser("health", help="更新实例健康状态")
    parser_health.add_argument("--service-name", required=True, help="服务名称")
    parser_health.add_argument("--ip", required=True, help="实例 IP")
    parser_health.add_argument("--port", type=int, required=True, help="实例端口")
    parser_health.add_argument(
        "--healthy",
        type=lambda x: x.lower() == "true",
        required=True,
        help="健康状态 (true/false)",
    )

    # deregister 命令 - 下线服务实例
    parser_dereg = subparsers.add_parser("deregister", help="下线服务实例")
    parser_dereg.add_argument("--service-name", required=True, help="服务名称")
    parser_dereg.add_argument("--ip", required=True, help="实例 IP")
    parser_dereg.add_argument("--port", type=int, required=True, help="实例端口")
    parser_dereg.add_argument("--force", action="store_true", help="强制下线不确认")

    args = parser.parse_args()

    if not args.command:
        parser.print_help()
        sys.exit(1)

    # 创建客户端
    client = NacosServiceClient(
        host=args.host,
        port=args.port,
        namespace=args.namespace,
        username=args.username,
        password=args.password,
    )

    # 执行命令
    if args.command == "list":
        cmd_list_services(client, args)
    elif args.command == "get":
        cmd_get_service(client, args)
    elif args.command == "instances":
        cmd_list_instances(client, args)
    elif args.command == "health":
        cmd_update_health(client, args)
    elif args.command == "deregister":
        cmd_deregister(client, args)


if __name__ == "__main__":
    main()
