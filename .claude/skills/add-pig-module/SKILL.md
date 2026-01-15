---
name: add-pig-module
description: Add a new pig module (api + biz) in this repository, following existing pom structure, package layout, and configuration conventions.
---

# Add Pig Module

Use this skill when the user asks to add a new module under this repo’s multi-module Maven setup.

## Workflow

1) Confirm module name
- Use `pig-{name}` for the aggregate module.
- Create submodules: `pig-{name}-api` and `pig-{name}-biz`.
- Confirm base package: `com.pig4cloud.pig.{name}`.

2) Add Maven modules
- Add `pig-{name}` under `<modules>` in `pom.xml`.
- Create `pig-{name}/pom.xml` (packaging `pom`) listing submodules.
- Create `pig-{name}/pig-{name}-api/pom.xml` and `pig-{name}/pig-{name}-biz/pom.xml`.
- In `pig-{name}-biz`, depend on `pig-{name}-api` and required `pig-common-*` artifacts.

3) Create standard package layout (biz)
- `controller/`, `service/`, `service/impl/`, `mapper/`, `entity/`, `dto/`.
- Add `Pig{Name}Application` under `com.pig4cloud.pig.{name}` with `@SpringBootApplication`.

4) Add basic config
- `pig-{name}-biz/src/main/resources/application.yml` with server port, `spring.application.name`, and datasource/redis placeholders.
- If this service is exposed via gateway, add route config in gateway module as needed.

5) Optional integration
- If `pig-boot` monolith should include this module, add dependency in the `pig-boot` pom and update any boot configs.

## References

- Root modules are defined in `pom.xml`.
- Use `pig-vault` and `pig-upms` as structure examples.
- Keep formatting compatible with Spring Java Format.
