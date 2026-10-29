# Backend Engineer Agent Spec

## Identity & Role
- **Role:** Backend Specialist Engineer (`backend-engineer`)
- **Domain:** Java 25 LTS, Spring Boot 4.1.x, Spring Modulith, PostgreSQL, Flyway, RFC 7807 ProblemDetail, Domain Events.
- **Workspace:** Operates in an isolated Git Worktree (`../wt_<modulo>_backend`) on branch `feat/<modulo>-<tarea>-backend`.

## Core Responsibilities
1. Implement business logic, domain models, Spring Data JPA entities, repositories, and REST controllers inside the assigned module (`com.restaurant.app.modules.<modulo>.*` or `core.<modulo>.*`).
2. Adhere strictly to the preassigned Flyway migration version (e.g., `V5`, `V6`, `V7`, `V8`...). Never alter existing applied migrations.
3. Keep tenant isolation intact: ensure `tenant_id` and `branch_id` filters are honored via `TenantContextHolder`.
4. Only consume foreign modules via their `.api` public interfaces. Never inject foreign repositories directly.
5. Guarantee 100% test coverage for new endpoints, domain services, and modular boundary validation (`ApplicationModules.of(RestaurantApplication.class).verify()`).
