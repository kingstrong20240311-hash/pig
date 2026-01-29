# /mysql — Execute MySQL tasks via docker exec (local dev)

You are Cursor Agent with terminal access.
The user describes a task that should be executed via MySQL.
You MUST infer the target database, generate SQL, execute it immediately using docker exec, then interpret the result.

This command is for LOCAL DEVELOPMENT ONLY.
Database corruption is acceptable and recoverable.

---

## Database routing rules (MANDATORY)

Select database based on task semantics:

- order / matching / trade / outbox / market → pig_order
- vault / balance / freeze / asset → pig_vault
- nacos / config / configuration → pig_config
- otherwise → pig

You MUST explicitly state which database you selected and why.

---

## MySQL execution template (USE EXACTLY)

All SQL MUST be executed using this template:

docker exec pig-e2e-mysql \
  mysql -uroot -proot \
  --default-character-set=utf8mb4 \
  --raw --batch \
  -D {{DATABASE}} \
  -e "/* SQL HERE */"

Notes:
- MySQL container: pig-e2e-mysql
- MySQL version: 8.0
- No permission limits (DDL / DML allowed)
- Do NOT ask for confirmation
- If output is too large, automatically re-run with LIMIT / aggregation

---

## Workflow (STRICT)

1) Restate the task in one sentence.
2) Decide target database using routing rules.
3) Generate SQL (multiple statements allowed).
4) Execute immediately using docker exec template above.
5) Summarize and analyze the result.
6) If MySQL returns an error, include the exact error output and retry if trivial.

---

## Output format (STRICT)

### Task
...

### Database selected
- {{DATABASE}}
- Reason: ...

### SQL
```sql
...
