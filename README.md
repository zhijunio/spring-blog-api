# Spring Blog REST API

A REST API for a blog application using SpringBoot.

[![CI](https://github.com/zhijunio/spring-blog-api/actions/workflows/ci.yml/badge.svg)](https://github.com/zhijunio/spring-blog-api/actions/workflows/ci.yml)
![Java 25](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![Spring Boot 4.x](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

## Documentation
* [Project Overview](docs/project.md)
* [REST API Docs](docs/rest-apis.md)
* [Best Practices](docs/best-practices.md)

## Tech Stack
* Language: Java 25
* Framework: Spring Boot 4.x
* Web/API: Spring Web MVC
* Security: Spring Security + OAuth2 Resource Server + JWT (RSA keys)
* Validation: Jakarta Bean Validation
* Persistence: Spring Data JPA + Hibernate
* Caching: Spring Cache + Spring Data Redis (transaction-aware Redis cache)
* Messaging: Apache Kafka + Spring Kafka (post-publication email notifications)
* Database: PostgreSQL
* Migrations: Flyway
* Mapping: MapStruct
* Modular architecture/events: Spring Modulith
* Diagnostics/developer UI: BootUI with MCP support
* Observability: Spring Boot Actuator, Micrometer Prometheus, OpenTelemetry, Grafana LGTM
* Email: Spring Mail (JavaMail) + Console email adapter
* API docs: SpringDoc OpenAPI / Swagger UI
* Build: Maven Wrapper, Docker Compose, Spring Boot Buildpacks
* Testing: JUnit 5, Spring Boot test starters, Testcontainers, ArchUnit & Taikai, Spring Modulith tests
* Quality: JaCoCo coverage checks, Spotless, Palantir Java Format

## Prerequisites
* JDK 25
* Docker and Docker Compose
* Your favourite IDE (Recommended: [IntelliJ IDEA](https://www.jetbrains.com/idea/))

Install JDK using [SDKMAN](https://sdkman.io/)

```shell
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk install java 25-tem
$ sdk install maven
```

## How to?

```shell
# Run tests
$ ./mvnw test

# Run application using Maven
$ ./mvnw spring-boot:run

# Start PostgreSQL, Redis, Kafka, Mailpit, and Grafana LGTM
# Compose reads .env automatically
$ docker compose up -d

# Run application with the "local" profile (enables Swagger UI)
$ ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

All REST endpoints are exposed under the `/api` base path.

* REST API: http://localhost:8080/api (e.g. http://localhost:8080/api/posts)
* Swagger UI: http://localhost:8080/swagger-ui/index.html

> **Note**: Swagger UI is disabled by default (`springdoc.swagger-ui.enabled=false`).
> It is only available when running with the `local` profile,
> which enables it via `application-local.yml`.

## Observability

The project uses `grafana/otel-lgtm` for local observability. It bundles Grafana, Loki, Tempo, Prometheus, and an OpenTelemetry Collector.

Start the infrastructure:

```shell
$ docker compose up -d
```

Open the following endpoints:

* Grafana: http://localhost:3000
* Prometheus metrics: http://localhost:8080/actuator/prometheus
* OTLP HTTP receiver: http://localhost:4318

The application exports metrics, traces, and logs through OTLP. When the application runs on the host, it uses `http://localhost:4318` by default. When the application runs inside `docker/compose.yml`, the container endpoint is configured as `http://otel-lgtm:4318`.

`task start` uses `docker/compose.yml`, which starts the application together with PostgreSQL, Redis, Kafka, Mailpit,
and Grafana LGTM. The Docker profile uses service names for internal connections; only `DB_PASSWORD` is supplied from
the environment.

## Environment Variables

Sensitive configuration may be supplied through the process environment:

* `DB_PASSWORD`: PostgreSQL password; defaults to `postgres` for the demo

Docker Compose reads the same variables from the shell or an untracked local `.env` file; no `env_file` declaration is required. To override the demo default, copy `.env.example` to `.env` and change the password. Never commit `.env` or real credentials.

## Generating certs

```shell
# create rsa key pair
openssl genrsa -out keypair.pem 2048

# extract public key
openssl rsa -in keypair.pem -pubout -out public.pem

# create private key in PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out private.pem
```

## Using [Taskfile](https://taskfile.dev/) utility
Task is a task runner that we can use to run any arbitrary commands in an easier way.

```shell
# Run tests
$ task test

# Build docker image
$ task build_image

# Run application in docker container
$ task start
$ task stop
$ task restart
```
