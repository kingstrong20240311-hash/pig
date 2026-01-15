# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pig is an enterprise-grade RBAC rapid development platform based on Spring Cloud, Spring Boot, and OAuth2. It supports both microservice and monolithic architectures.

**Key Technologies:**
- Spring Boot 3.5.7
- Spring Cloud 2025.0.0
- Spring Cloud Alibaba 2025.0.0.0
- Spring Authorization Server 1.5.2
- MyBatis Plus 3.5.14
- Java 17

## Architecture Modes

The platform supports **two deployment modes**:

### 1. Microservices Mode (default)
Individual services run independently with Nacos for service discovery:
- **pig-register**: Nacos Server (port 8848)
- **pig-gateway**: Spring Cloud Gateway (port 9999)
- **pig-auth**: Authorization service (port 3000)
- **pig-upms-biz**: User permission management (port 4000)
- **pig-monitor**: Service monitoring (port 5001)
- **pig-codegen**: Code generator (port 5002)
- **pig-quartz**: Scheduled task management (port 5007)

### 2. Monolithic Mode
The **pig-boot** module combines all services into a single deployable artifact (port 9999).

To build with monolithic mode: `mvn clean install -Pboot`

## Common Commands

### Build & Test
```bash
# Build entire project (microservices mode)
mvn clean install

# Build with monolithic module
mvn clean install -Pboot

# Format code (required before commits)
mvn spring-javaformat:apply

# Run tests
mvn test

# Skip tests during build
mvn clean install -DskipTests
```

### Running Services Locally

For microservices mode, start in this order:
1. MySQL and Redis (see Database Setup)
2. `pig-register` (Nacos)
3. `pig-gateway`
4. `pig-auth`
5. `pig-upms-biz`
6. Optional: `pig-monitor`, `pig-codegen`, `pig-quartz`

For monolithic mode:
1. MySQL and Redis
2. `pig-boot` only

Run individual service:
```bash
cd pig-auth
mvn spring-boot:run
```

### Docker Commands
```bash
# Start all services with Docker Compose
docker compose up

# Build and run specific service
cd pig-gateway
mvn clean package
docker build -t pig-gateway .
docker run -p 9999:9999 pig-gateway

# Build Docker image with Maven plugin
mvn clean package docker:build
```

### Database Setup
```bash
# Initial database schema
# Import db/pig.sql into MySQL database named 'pig'
# Import db/pig_config.sql for Nacos configuration

# Update connection in pig-boot/src/main/resources/application-dev.yml
# or individual service application.yml files
```

## Module Structure

### Core Modules

- **pig-common**: Shared utilities and components
  - `pig-common-bom`: Dependency management
  - `pig-common-core`: Core utilities and base classes
  - `pig-common-security`: OAuth2 resource server configuration, security utilities
  - `pig-common-mybatis`: MyBatis Plus extensions, pagination, auto-fill handlers
  - `pig-common-datasource`: Dynamic datasource support
  - `pig-common-feign`: Feign client extensions with OAuth2 token relay
  - `pig-common-log`: Logging infrastructure
  - `pig-common-oss`: File upload/storage utilities
  - `pig-common-swagger`: OpenAPI documentation configuration
  - `pig-common-websocket`: WebSocket support
  - `pig-common-xss`: XSS protection filters
  - `pig-common-seata`: Distributed transaction support
  - `pig-common-excel`: Excel import/export utilities

- **pig-auth**: OAuth2 authorization server implementing Spring Authorization Server
  - Handles multiple OAuth2 grant types
  - Token generation and validation
  - User authentication endpoints

- **pig-upms**: User Permission Management System
  - `pig-upms-api`: Public API interfaces and DTOs
  - `pig-upms-biz`: Business logic for users, roles, permissions, departments, menus

- **pig-gateway**: API Gateway
  - Routes requests to microservices
  - Rate limiting, authentication filters
  - Request/response logging

- **pig-visual**: Operational tools
  - `pig-monitor`: Spring Boot Admin dashboard
  - `pig-codegen`: Database-driven code generator
  - `pig-quartz`: Scheduled task management

### Package Structure

Standard package structure in business modules:
```
com.pig4cloud.pig.{module}/
├── controller/    # REST controllers
├── service/       # Business logic interfaces
│   └── impl/     # Service implementations
├── mapper/        # MyBatis mappers
├── entity/        # Database entities
└── dto/          # Data transfer objects
```

## Development Guidelines

### Code Formatting
**CRITICAL**: All code must follow Spring Java Format conventions. The build will fail if code is not properly formatted.

Install the IntelliJ IDEA plugin: [spring-javaformat-intellij-idea-plugin](https://repo1.maven.org/maven2/io/spring/javaformat/spring-javaformat-intellij-idea-plugin/)

Or format before committing:
```bash
mvn spring-javaformat:apply
```

### Security Configuration

The project uses Spring Authorization Server for OAuth2. Key concepts:

- **Resource Server**: Most services are resource servers validating JWT tokens
- **Authorization Server**: `pig-auth` issues tokens
- **Token Relay**: Feign clients automatically propagate OAuth2 context via `pig-common-security`

Security utilities in `SecurityUtils` provide:
- Current user information extraction
- Role/permission checking
- OAuth2 context access

### Database Conventions

Using MyBatis Plus with conventions in `pig-common-mybatis`:

- **Auto-fill fields**: `create_time`, `update_time`, `create_by`, `update_by` automatically populated
- **Logical delete**: Use `del_flag` field (0=normal, 1=deleted)
- **Pagination**: Built-in via `PigPaginationInnerInterceptor`
- **Base entity**: Extend `BaseEntity` for common fields

Example:
```java
public class SysUser extends BaseEntity {
    // Inherits: createTime, updateTime, createBy, updateBy, delFlag
}
```

### Configuration Management

- Profile-specific configs: `application-{profile}.yml`
- Default profile: `dev`
- Nacos integration: Configs can be externalized to Nacos Server
- Encryption: Sensitive values encrypted with Jasypt (prefix: `ENC(...)`)

### Service Communication

In microservices mode:
- Use `@FeignClient` for inter-service calls (see `pig-common-feign`)
- Feign clients automatically include OAuth2 tokens
- Circuit breakers available via Spring Cloud Circuit Breaker

### API Documentation

Access Swagger UI at:
- Monolithic: `http://localhost:9999/doc.html` (Knife4j)
- Gateway: `http://localhost:9999/doc.html` (aggregates all services)

Annotations are processed automatically via `pig-common-swagger`.

## Testing

Test structure mirrors main source:
```
src/test/java/
└── com.pig4cloud.pig.{module}/
    ├── controller/    # Controller integration tests
    └── service/       # Service unit tests
```

Run tests for specific module:
```bash
cd pig-upms/pig-upms-biz
mvn test
```

## Common Troubleshooting

**Build fails with formatting errors**: Run `mvn spring-javaformat:apply`

**Service can't connect to Nacos**: Check `pig-register` is running and `nacos.server-addr` in config

**Database connection errors**: Verify MySQL is running and credentials in `application-dev.yml` match

**OAuth2 token validation fails**: Ensure `pig-auth` is running and accessible by resource servers

## Additional Resources

Official documentation: [wiki.pig4cloud.com](https://wiki.pig4cloud.com)

## constraint
Use Instant for all time fields in new modules/new tables; keep existing LocalDateTime fields unchanged to avoid breaking legacy serialization and DB mappings.
