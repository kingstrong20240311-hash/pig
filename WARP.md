# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Repository type: Multi-module Java 17, Spring Boot 3.5.x, Spring Cloud 2025, Maven build. Supports both microservices and a monolith bootstrap module.

Important references
- Official docs: https://wiki.pig4cloud.com (covers environment setup, service startup, frontend, etc.)
- Code style: spring-javaformat is enforced across modules

Environment prerequisites
- Java 17 (maven-compiler-plugin targets 17)
- Maven (no mvnw wrapper tracked)
- Docker and Docker Compose (for the full microservices stack)

Commands

Build
- Build all modules (default cloud profile is active):
  - mvn -q -T 1C clean package
  - To skip tests: mvn -q -T 1C -DskipTests clean package
- Build monolith profile (adds pig-boot module):
  - mvn -q -Pboot -DskipTests clean package

Format and lint checks (spring-javaformat)
- Auto-format the codebase:
  - mvn spring-javaformat:apply
- Validate formatting without modifying files:
  - mvn -q -DskipTests validate
  - Alternatively: mvn io.spring.javaformat:spring-javaformat-maven-plugin:validate

Tests
- Run all tests across modules:
  - mvn -q test
- Run tests but skip integration/slow tests if you use such conventions via surefire/failsafe (module-specific):
  - mvn -q -DskipITs test
- Run tests in a single module:
  - mvn -q -pl {module-path} -am test
    - Example: mvn -q -pl pig-upms/pig-upms-biz -am test
- Run a single test class (inside a module):
  - mvn -q -pl {module-path} -Dtest={TestClassName} test
  - Example: mvn -q -pl pig-auth -Dtest=PigAuthApplicationTests test
- Run a single test method:
  - mvn -q -pl {module-path} -Dtest={TestClassName}#{methodName} test

Run locally (microservices)
- Start backing services and all app containers (MySQL, Redis, Nacos, gateway, auth, upms, monitor, codegen, quartz):
  - docker compose up --build
- Bring the stack down:
  - docker compose down
- Rebuild and restart one service (example: pig-auth):
  - docker compose build pig-auth && docker compose up -d pig-auth
- Service endpoints (per README and compose):
  - pig-gateway: http://localhost:9999
  - pig-register (Nacos): http://localhost:8848, 9848 (gRPC), 8080 (console)
  - pig-monitor: http://localhost:5001
  - DB: localhost:33306 (root/root via compose defaults)
  - Redis: localhost:36379

Run locally (single service from source)
- Using Spring Boot Maven plugin:
  - mvn -q -pl {module-path} -am -DskipTests spring-boot:run
    - Example (gateway): mvn -q -pl pig-gateway -am -DskipTests spring-boot:run
- Using packaged jar (after build):
  - java -jar {module-path}/target/{artifact}.jar

Run the monolith (pig-boot)
- From source:
  - mvn -q -Pboot -pl pig-boot -am -DskipTests spring-boot:run
- As a jar (after building with -Pboot):
  - java -jar pig-boot/target/pig-boot*.jar

Container images (per-module Dockerfile)
- Each deployable module contains a Dockerfile; images can be built by docker compose or individually, e.g.:
  - docker build -t pig-auth ./pig-auth

High-level architecture

Overview
- This repo is an aggregator Maven project (packaging=pom) that orchestrates multiple Spring Boot microservices and shared libraries. It supports two operating modes:
  1) Microservices: default “cloud” profile activates services that register to Nacos and communicate via Spring Cloud Gateway and OpenFeign.
  2) Monolith: the pig-boot module aggregates dependencies into a single Spring Boot application for simplified local runs.

Key modules (big picture)
- pig-register: Nacos server bootstrap for service discovery and configuration.
- pig-gateway: Spring Cloud Gateway entry point, rate limiting and global error handling.
- pig-auth: Authorization server built on Spring Authorization Server with custom grant flows (password, SMS, etc.) and token customization.
- pig-upms: User/permission management domain split as:
  - pig-upms-api: shared API contracts, DTOs, VOs used by other services
  - pig-upms-biz: business logic, controllers, mappers
- pig-smm: Social/messaging domain split similarly into -api and -biz with gateway integrations and jobs.
- pig-visual: operational services
  - pig-monitor: Spring Boot Admin monitoring
  - pig-codegen: UI-driven code generator
  - pig-quartz: scheduling/daemon service with job management
- pig-common: shared libraries consumed by services
  - pig-common-bom: centralizes dependency versions
  - pig-common-core: common config, constants, exceptions, utils
  - pig-common-datasource: dynamic datasource, data source providers
  - pig-common-mybatis: MyBatis Plus extensions, handlers, pagination plugins
  - pig-common-security: resource server config, auth utilities, aspects, Feign auth interceptors
  - pig-common-feign: Feign auto-config, interceptors, Sentinel integration
  - pig-common-log: logging AOP and eventing utilities
  - pig-common-oss: file and object storage abstraction (local/OSS)
  - pig-common-websocket: WebSocket infrastructure, message distribution (local/redis)
  - pig-common-swagger: OpenAPI/Swagger configuration and metadata
  - pig-common-seata: distributed transaction support
  - pig-common-xss: XSS filtering and Jackson integration
- pig-boot: monolith bootstrap application (includes gateway in single process); convenient for local development.

Cross-cutting concerns
- Security and auth:
  - pig-auth provides token issuance; pig-common-security supplies resource server and Feign interceptors.
- Configuration and discovery:
  - Nacos (pig-register) provides service discovery and centralized configuration. Compose sets NACOS_HOST, REDIS_HOST, MYSQL_HOST for local runs.
- Data access:
  - MyBatis-Plus for ORM; entity definitions typically live in *-api modules; mappers and services in *-biz modules.
- Observability:
  - Spring Boot Admin client is included in all services; pig-monitor hosts the admin UI.
- API contracts:
  - *-api modules expose DTOs/VOs for inter-service reuse, decoupling domain contracts from biz implementations.

Profiles and runtime
- The root pom defines profiles:
  - cloud (active by default): profiles.active=dev, default local development setup, assumes Nacos/Redis/MySQL availability.
  - boot: includes pig-boot for monolith runs; use -Pboot to activate.

Notes distilled from README
- Documentation hub: https://wiki.pig4cloud.com (setup, service startup, frontend)
- Code formatting: run mvn spring-javaformat:apply before submitting changes, or integrate the IntelliJ plugin for auto-formatting.

Troubleshooting quick checks
- Java version mismatch: ensure JAVA_HOME points to a JDK 17 distribution.
- Services not starting locally: confirm docker compose is up and that pig-register (Nacos), pig-mysql, and pig-redis are healthy.
- Module builds: when changing shared libs under pig-common or *-api, rebuild dependents using -pl {changed} -am to compile affected modules.
