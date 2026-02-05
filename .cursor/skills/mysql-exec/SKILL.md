---
name: mysql-exec
description: Execute local MySQL tasks via docker exec using a fixed command template and strict workflow. Use when the user asks to run SQL, query/update database data, or mentions MySQL/SQL/database operations.
---

# MySQL Exec (Local Dev)

## 适用场景

需要通过数据库验证数据是否真实

## 约束

- 仅用于本地开发环境。
- 数据库损坏可接受（可通过重置环境恢复）。
- 不要求用户确认，直接执行并解释结果。

## 数据库路由规则（必须）

按语义选择数据库：

- order / matching / trade / outbox / market → `pig_order`
- vault / balance / freeze / asset → `pig_vault`
- nacos / config / configuration → `pig_config`
- 其他 → `pig`

必须明确说明所选数据库及原因。

## 执行模板（必须原样使用）

所有 SQL 必须使用以下模板执行：

```bash
docker exec pig-e2e-mysql \
  mysql -uroot -proot \
  --default-character-set=utf8mb4 \
  --raw --batch \
  -D {{DATABASE}} \
  -e "/* SQL HERE */"
```

说明：
- MySQL 容器名：`pig-e2e-mysql`
- 版本：8.0
- 不限制 DDL/DML
- 输出过大时，自动使用 `LIMIT`/聚合后重试

## 工作流（严格）

1) 用一句话复述任务。  
2) 根据路由规则选择数据库。  
3) 生成 SQL（允许多语句）。  
4) 立即用模板执行。  
5) 汇总并分析结果。  
6) 若 MySQL 报错，原样给出错误并在可修复时重试。  

## 输出格式（严格）

```
### Task
...

### Database selected
- {{DATABASE}}
- Reason: ...

### SQL
```sql
...
```
```
