# Westpac Technical Assessment: Savings Account API

A Spring Boot REST API that opens and retrieves savings bank accounts. Accounts are stored in Postgres,
and GET responses are cached in Redis.

## Tech stack

Java 25 · Spring Boot 4.1 · Gradle (wrapper included) · Postgres 17 · Redis 7 · Flyway · springdoc-openapi · JUnit 5 + Testcontainers

## Prerequisites

| Tool | Version | Why |
|---|---|---|
| JDK | 25 | Build toolchain. Gradle won't download it for you, so it must be installed locally. |
| Docker | with Compose v2 | Runs Postgres and Redis locally. The tests also need it (Testcontainers). |
| Git | any | To clone the repository |

You don't need to install Gradle; use `./gradlew`. The app uses ports `8080`, `5432` and `6379`, so they must be free.

## Getting started
Clone this repo then:
```bash
cd techassesment.westpac

docker compose up -d     # start Postgres and Redis
./gradlew bootRun        # start the API on http://localhost:8080
```

Flyway creates the database schema on startup. To check the app is up, run
`curl localhost:8080/actuator/health`.

Try it:

```bash
curl -i -X POST localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -d '{"customerName": "Jane Citizen", "accountNickname": "Holiday Fund"}'
```

To stop, press `Ctrl+C` to stop the API, then run `docker compose down` (add `-v` to also delete the data).


## API documentation

Endpoints, request and response schemas, validation rules and error responses are defined in the OpenAPI spec:

- **Committed spec:** [`docs/openapi.yaml`](docs/openapi.yaml)
- **Swagger UI**, while the app is running: http://localhost:8080/swagger-ui.html

The spec is generated from the code. `OpenApiSpecTest` fails if the committed copy no longer matches.
After an intentional API change, regenerate it and review the diff:

```bash
./gradlew test --tests '*OpenApiSpecTest' -PupdateOpenApiSpec=true
```

## Testing

```bash
./gradlew check   # all tests; Docker must be running, but the compose stack is not needed
```


## Working with AI agents

Conventions for coding agents are in [`AGENTS.md`](AGENTS.md); `CLAUDE.md` imports it. The Spring skills in
`.agents/` and `.claude/` were installed from [Amplicode/spring-skills](https://github.com/Amplicode/spring-skills).
