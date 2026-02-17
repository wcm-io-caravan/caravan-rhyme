# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Rhyme** (Reactive Hypermedia) is a Java framework for building and consuming HAL+JSON hypermedia APIs. It is part of the wcm.io Caravan ecosystem. HAL APIs are expressed as annotated Java interfaces that serve as client API (dynamic proxies), server-side structure, and documentation source simultaneously.

## Build Commands

The project uses Maven with a wrapper (`mvnw`). A custom settings file is required for snapshot repositories.

```bash
# Full build (requires JDK 17+ for all modules)
./mvnw clean install -s .maven-settings.xml

# Build a single module (e.g. core)
./mvnw clean install -s .maven-settings.xml -pl core

# Run tests for a single module
./mvnw test -s .maven-settings.xml -pl core

# Run a single test class
./mvnw test -s .maven-settings.xml -pl core -Dtest=HttpHalResourceLoaderTest

# Run a single test method
./mvnw test -s .maven-settings.xml -pl core -Dtest=HttpHalResourceLoaderTest#should_parse_valid_URI

# CI profile (used by GitHub Actions)
./mvnw clean install -s .maven-settings.xml -Pcontinuous-integration

# OSGi integration tests (launches a Sling 14 container)
./mvnw clean verify -s .maven-settings.xml -pl examples/osgi-jaxrs-example-launchpad
```

JDK 11 builds the core modules only. JDK 17+ activates the `java17` profile which adds Spring Boot integration, Spring examples, and coverage tooling.

## Maven Output Handling

Maven builds produce very verbose output. Never pipe Maven output directly into the conversation or let an agent consume it. Always redirect to a log file and inspect only what's needed:

```bash
# Redirect all output to a timestamped log file
./mvnw clean install -s .maven-settings.xml > /private/tmp/claude-501/mvn-build-$(date +%Y%m%d-%H%M%S).log 2>&1

# Check exit code, then inspect only on failure
tail -40 /private/tmp/claude-501/mvn-build-*.log   # last lines for BUILD status
grep -E "ERROR|FAILURE" /private/tmp/claude-501/mvn-build-*.log  # find errors
```

Log file conventions:
- **Directory:** `/private/tmp/claude-501/` (Claude Code's temp directory)
- **Naming:** `mvn-<task>-<YYYYMMDD-HHMMSS>.log` (e.g. `mvn-ci-build-20260217-152300.log`)
- Timestamps in filenames prevent conflicts between parallel or repeated runs

## Module Structure

- **`api-interfaces`** — Annotations (`@HalApiInterface`, `@Related`, `@ResourceState`, `@ResourceProperty`, `@ResourceLink`, `@TemplateVariable`) and base resource interfaces (`LinkableResource`, `EmbeddableResource`). Minimal dependencies.
- **`core`** — Platform-agnostic framework: client proxy generation, HAL response rendering, caching, metadata, error handling. Uses RxJava 3 internally.
- **`integration/osgi-jaxrs`** — OSGi R7 JAX-RS Whiteboard integration with wcm.io Resilient HTTP support.
- **`integration/spring`** — Spring Boot auto-configuration: `SpringRhyme` request-scoped bean, `LinkableResourceMessageConverter`, URL fingerprinting, `WebClient`-based `HalResourceLoader`.
- **`integration/aem`** — AEM integration (work in progress, not released).
- **`testing`** — Test utilities: WireMock-based `AbstractHalResourceLoaderTest`, `MockMvcHalResourceLoaderConfiguration`, `HalCrawler`, Apache HttpClient adapters.
- **`tooling/parent`** — Parent POM with all dependency version management.
- **`tooling/docs-maven-plugin`** — Generates HTML documentation from annotated HAL API interfaces.
- **`examples/`** — Spring Boot, OSGi/JAX-RS, and AWS Lambda example applications.

## Architecture

### Core Design: Interface-Driven HAL

The central pattern: annotated Java interfaces define the HAL API contract. The same interface is used on both client and server sides.

**Client side:** `HalApiClient.getRemoteResource(uri, MyApi.class)` creates a dynamic `java.lang.reflect.Proxy` that fetches HAL+JSON and maps link relations, embedded resources, and state to method return values via annotation-specific handlers in `impl/client/proxy/`.

**Server side:** Controller methods return implementations of `LinkableResource`. The renderer (`impl/renderer/AsyncHalResourceRendererImpl`) introspects `@Related` methods via reflection, invokes them reactively with `Single.zip()`, and assembles the HAL response including embedded resources and links.

### Key Entry Points

- **`Rhyme`** / **`RhymeBuilder`** — Main per-request facade (server-side rendering + client access).
- **`HalApiClient`** / **`HalApiClientBuilder`** — Standalone client for consuming HAL APIs.
- **`HalResourceLoader`** (`@FunctionalInterface`) — Core SPI for HTTP transport: `getHalResource(uri) -> Single<HalResponse>`.
- **`ExceptionStatusAndLoggingStrategy`** — SPI to map custom exceptions to HTTP status codes.
- **`HalApiReturnTypeSupport`** — SPI to extend supported return types beyond the defaults (RxJava 3, Optional, Stream, List).

### Exception Types

- `HalApiClientException` — Upstream request failure (carries HTTP status code).
- `HalApiServerException` — Server-side failure (developer specifies status code).
- `HalApiDeveloperException` — Framework misuse (always 500).

### OSGi Versioning

Public API interfaces use `@ProviderType` and `@ConsumerType` from `org.osgi.annotation.versioning`. The `maven-bundle-plugin` `baseline` goal enforces semantic versioning against the previous release during the build.

## Testing

- **Unit tests** are in each module's `src/test/`. The `core` module has extensive coverage across client proxy, rendering, caching, reflection, and metadata subsystems.
- **Spring integration tests** in `examples/spring-hypermedia` run the same `AbstractCompanyApiIT` test suite three ways: direct Java calls (`InternalConsumerIT`), Spring MockMvc (`MockMvcClientIT`), and real HTTP (`ExternalClientIT`).
- **OSGi integration tests** in `examples/osgi-jaxrs-example-launchpad` launch a full Sling 14 OSGi container via `feature-launcher-maven-plugin` and run failsafe ITs against it.
- Tests use JUnit 5, Mockito, AssertJ, and WireMock.

## Package Conventions

- `io.wcm.caravan.rhyme.api.*` — Public API (stable, versioned).
- `io.wcm.caravan.rhyme.impl.*` — Internal implementation (not exported in OSGi, may change without notice).
- `io.wcm.caravan.rhyme.*.spi.*` — Extension points for framework consumers to implement.
