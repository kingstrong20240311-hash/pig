# pig-gym 健身管理模块

## 模块说明

pig-gym 是基于 Pig 平台的健身管理业务模块，遵循标准的微服务架构设计。

## 模块结构

```
pig-gym/
├── pig-gym-api/          # 公共 API 模块，包含实体、DTO、Feign 接口等
└── pig-gym-biz/          # 业务处理模块，包含控制器、服务、数据访问层等
```

## 技术栈

- Spring Boot 3.5.9
- Spring Cloud 2025.0.1
- Spring Cloud Alibaba 2025.0.0.0
- MyBatis Plus 3.5.15
- MySQL 驱动
- Nacos 服务注册与配置
- OAuth2 资源服务器

## 服务端口

默认端口：**5010**

## 运行方式

### 微服务模式

1. 确保 MySQL 和 Redis 已启动
2. 启动 Nacos 注册中心 (pig-register)
3. 启动网关服务 (pig-gateway)
4. 启动认证服务 (pig-auth)
5. 启动本服务：

```bash
cd pig-gym/pig-gym-biz
mvn spring-boot:run
```

### 构建

```bash
# 构建整个模块
mvn clean package -DskipTests -pl pig-gym -am

# 只构建本模块
mvn clean package -DskipTests -pl pig-gym/pig-gym-api,pig-gym/pig-gym-biz -am
```

## API 文档

服务启动后，访问 Swagger UI：
- 直接访问：http://localhost:5010/doc.html
- 通过网关：http://localhost:9999/doc.html

## 配置

主要配置文件：
- `pig-gym-biz/src/main/resources/application.yml`：基础配置
- Nacos 配置中心：`application-dev.yml` 和 `pig-gym-biz-dev.yml`

## 依赖的公共模块

- pig-common-core：核心工具类
- pig-common-security：OAuth2 资源服务器安全配置
- pig-common-log：日志处理
- pig-common-swagger：API 文档
- pig-common-mybatis：MyBatis Plus 扩展
- pig-common-feign：Feign 客户端扩展

## 开发指南

### 标准包结构

```
com.pig4cloud.pig.gym/
├── controller/      # REST 控制器
├── service/         # 业务逻辑接口
│   └── impl/       # 服务实现
├── mapper/          # MyBatis 映射器
├── entity/          # 数据库实体
└── dto/            # 数据传输对象
```

### 代码格式化

提交前必须格式化代码：

```bash
mvn spring-javaformat:apply
```

## 注意事项

1. 所有时间字段使用 `Instant` 类型（新模块标准）
2. 实体类继承 `BaseEntity` 可自动填充审计字段
3. 使用逻辑删除时需包含 `del_flag` 字段
4. 分页查询使用 MyBatis Plus 的分页插件
5. Feign 调用会自动传递 OAuth2 Token
