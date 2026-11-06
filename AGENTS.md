# AGENTS.md

## Style
- Run `./gradlew check` (or `./gradlew build`) before claiming a task is done. Fix what fails.
- No magic numbers or strings. Constants or enums. Spec-derived values (HTTP codes, date patterns) get named constants too.
- Enums over booleans for parameters.
- Braces always, even on one-line ifs. Guard clauses over nesting.
- Do not modify code unrelated to the task. Minimize the diff.
- Keep fields and methods private. Ask before widening visibility.
- Short comments explaining what and why, not how.

## Java and Spring Boot
- Constructor injection only. Never field @Autowired.
- Keep @Transactional on service methods, never on private methods.
- Never swallow exceptions or catch-and-return-null. Either handle, rethrow, or let it propagate to the advice layer.
- Services expose domain operations. Controllers never touch repositories directly; route through the service layer.
- DTOs at the boundary. Entities do not leave the service layer.
- No println. Use the SLF4J logger that is already there.

## API contract (OpenAPI)
- `docs/openapi.yaml` is the published contract. It is generated from code by springdoc, so never hand-edit it.
- `OpenApiSpecTest` (part of `./gradlew check`) fails when the code no longer matches `docs/openapi.yaml`.
- A drift failure means the API contract changed. Do not regenerate just to make the build pass.
  - Intended change (new endpoint, field, status, validation rule): regenerate with
    `./gradlew test --tests '*OpenApiSpecTest' -PupdateOpenApiSpec=true`, review the diff and commit the spec with the code.
  - Unintended change (e.g. a renamed record component or a changed constraint): fix the code instead.
- In your summary, name any breaking change (removed or renamed field, new required field, tighter validation, changed status code).
- Document new endpoints and responses with `@Operation`/`@ApiResponse`, declaring every status the advice layer can return.

## End-to-end tests
- Every major use case (a new endpoint or business rule, such as create, get or the account limit) gets an e2e test in the `e2e` test package (see `AccountApiE2eTest`).
- E2e tests are black-box: real HTTP on a random port (`RestTestClient`), with Postgres and Redis from `TestcontainersConfiguration`.
  Never import application classes or touch the database; create all state through the API, using unique data per test.
- Assert the client-visible contract: status, headers (`Content-Type`, `Location`), the full body and the problem-detail error shape.
  Hard-code spec values (paths, limits) instead of reading production constants or config, so a contract change fails the test.
- Keep e2e tests to the main path and the key error for each use case. Field validation, edge cases and concurrency belong in the web-slice or MockMvc integration tests.

## Process
- Bug fix: write the failing JUnit test first, watch it fail (`./gradlew test`), then fix, then watch it pass.
- Commit messages: imperative subject under 50 chars, body explains what and why, never how.
- When unsure between two designs, stop and present both with trade-offs instead of picking silently.
