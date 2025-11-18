# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **PortSwigger fork** of Splunk's Java logging library (`splunk-library-javalogging`). The fork was created to enable configuration of the Java TrustStore location, allowing DAST users to integrate with Splunk servers using self-signed certificates.

The library provides logging integration for three major Java logging frameworks:
- **Logback** - via `HttpEventCollectorLogbackAppender` and `TcpAppender`
- **Log4j 2** - via `HttpEventCollectorLog4jAppender`
- **java.util.logging** - via `HttpEventCollectorLoggingHandler`

Events can be sent to Splunk via:
- **HTTP Event Collector (HEC)** - JSON or raw format, with batching support
- **TCP** - Direct TCP socket connections

## Build System

This project uses **Maven** with Java 8 compatibility.

### Common Commands

**Build the project:**
```bash
mvn clean install
```

**Run all tests (unit + acceptance):**
```bash
make test
# OR
mvn -P Unittest -B verify
mvn -P AcceptanceTest -B verify
```

**Run only unit tests:**
```bash
mvn -P Unittest -B verify
```

**Run only acceptance tests:**
```bash
mvn -P AcceptanceTest -B verify
```

**Run stress tests:**
```bash
mvn -P StressTest -B verify
```

**Run a specific test class:**
```bash
mvn test -Dtest=HttpEventCollectorUnitTest
```

**Generate JavaDocs:**
```bash
mvn javadoc:jar
```

## Architecture

### Core Components

**`HttpEventCollectorSender`** (`src/main/java/com/splunk/logging/HttpEventCollectorSender.java`)
- Central component for sending events to Splunk HTTP Event Collector
- Handles batching, retries, and HTTP communication via OkHttp3
- Supports two send modes: `Sequential` (ordered) and `Parallel` (async)
- Manages event batching with configurable thresholds:
  - `DefaultBatchInterval`: 10 seconds
  - `DefaultBatchSize`: 10KB
  - `DefaultBatchCount`: 10 events
- Uses `HttpEventCollectorMiddleware` for retry logic and error handling

**Framework-Specific Appenders:**
- `HttpEventCollectorLog4jAppender` - Log4j 2 integration
- `HttpEventCollectorLogbackAppender` - Logback integration
- `HttpEventCollectorLoggingHandler` - java.util.logging integration
- `TcpAppender` - Logback TCP socket appender

**Event Model:**
- `HttpEventCollectorEventInfo` - Represents a single log event with metadata
- `SplunkCimLogEvent` - Common Information Model (CIM) compliant event structure
- `EventBodySerializer` / `EventHeaderSerializer` - Interfaces for custom serialization

**Serialization Package** (`com.splunk.logging.serialization`)
- `HecJsonSerializer` - JSON serialization for HEC format
- `EventInfoTypeAdapter` - Gson type adapter for events
- `PlainTextEventBodySerializer` - Plain text format serialization

**Error Handling:**
- `HttpEventCollectorErrorHandler` - Interface for custom error handling
- `StandardErrorCallback` (in `util` package) - Default error callback implementation
- `HttpEventCollectorMiddleware` - Middleware chain for retry logic
- `HttpEventCollectorResendMiddleware` - Automatic retry on failure

### Key Configuration Options

All appenders support these common configuration parameters:
- `url` - Splunk HEC endpoint
- `token` - Authentication token
- `source`, `sourcetype`, `index`, `host` - Splunk metadata
- `batchInterval`, `batchCount`, `batchSize` - Batching configuration
- `retriesOnError` - Number of retry attempts
- `disableCertificateValidation` - For self-signed certs (PortSwigger addition)
- `sendMode` - "sequential" or "parallel"
- `messageFormat` - "text" or "json"

## Testing Strategy

The project has three test profiles:

1. **Unit Tests** (`-P Unittest`)
   - `HttpEventCollectorUnitTest` - Core sender functionality
   - `SplunkCimLogEventUnitTest` - CIM event model
   - Tests use `HttpEventCollectorUnitTestMiddleware` for mocking

2. **Acceptance Tests** (`-P AcceptanceTest`)
   - `HttpEventCollector_Log4j2Test`
   - `HttpEventCollector_LogbackTest`
   - `HttpEventCollector_JavaLoggingTest`
   - `Log4jFunctionalTest`, `LogbackFunctionalTest`, `JULFunctionalTest`
   - These tests require a running Splunk instance (via Docker Compose)

3. **Stress Tests** (`-P StressTest`)
   - `HttpLoggerStressTest` - Performance and load testing

**Running Splunk for tests:**
```bash
make up        # Start Splunk in Docker
make wait_up   # Wait for Splunk to be ready
make down      # Stop Splunk
```

## Dependencies

**Core Runtime:**
- `com.google.code.gson:gson` (2.9.0) - JSON serialization
- `com.squareup.okhttp3:okhttp` (4.11.0) - HTTP client
- `com.squareup.okio:okio` (3.5.0) - I/O utilities (forced version for CVE mitigation)

**Logging Frameworks (provided scope):**
- Log4j 2.17.2 (CVE-2021-44228, CVE-2021-44832 mitigation)
- Logback 1.2.11 (CVE-2021-42550 mitigation)
- SLF4J 1.7.36

## PortSwigger-Specific Modifications

This fork adds support for **custom TrustStore configuration** to enable SSL/TLS connections to Splunk servers using self-signed certificates or certificates signed by custom CAs.

### Custom TrustStore Feature (v1.11.9+)

**Purpose:** Allow applications to programmatically configure trusted certificates without modifying the JVM's cacerts file.

**Implementation:**
- `HttpEventCollectorSender.setTrustStore(KeyStore)` - Configure custom trust store on the sender
- `HttpEventCollectorLogbackAppender.setTrustStore(KeyStore)` - Configure custom trust store on the appender
- SSL configuration happens in `startHttpClient()` method of `HttpEventCollectorSender`

**Usage:**
```java
KeyStore trustStore = // ... load your custom trust store
HttpEventCollectorLogbackAppender appender = new HttpEventCollectorLogbackAppender();
appender.setUrl("https://splunk-server:8088");
appender.setToken("token");
appender.setTrustStore(trustStore);
appender.start();
```

**Error Handling:**
- SSL configuration failures are logged to `System.err` but don't prevent the appender from starting
- Falls back to JVM default trust store if custom configuration fails
- Follows the library's "graceful degradation" philosophy

**Testing:**
- Unit tests in `HttpEventCollectorTrustStoreTest.java` (if implemented)
- Uses WireMock with self-signed certificate to verify SSL configuration

### Legacy Feature: disableCertificateValidation

The `disableCertificateValidation` flag remains available but is **not recommended for production use**. It completely disables certificate validation, creating a security vulnerability. Use the custom TrustStore feature instead.

## Version Information

Current version: **1.11.9** (TBD)
- Addresses CVE-2023-3635 via okio upgrade
- Version is defined in `pom.xml`

When bumping versions, update:
1. `pom.xml` - `<version>` tag
2. `README.md` - Version header
3. `CHANGELOG.md` - Add new version section