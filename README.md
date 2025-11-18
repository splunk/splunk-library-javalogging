# Splunk Logging for Java

#### DAST fork
- This repository was forked from https://github.com/splunk/splunk-library-javalogging
- The purpose of the fork was to make it possible to configure the location of the Java TrustStore so that DAST users could set up integration for a Splunk server using a self-signed certificate

#### Version 1.11.8

Splunk logging for Java enables you to log events to HTTP Event Collector or to a TCP input on a Splunk Enterprise instance within your Java applications. You can use three major Java logging frameworks: [Logback](http://logback.qos.ch), [Log4j 2](http://logging.apache.org/log4j/2.x/), and [java.util.logging](https://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html). Splunk logging for Java is also enabled for [Simple Logging Facade for Java (SLF4J)](http://www.slf4j.org).

Splunk logging for Java provides:

* Appender classes that package events into the proper format for the input type you're using (HTTP Event Collector or TCP).

* Handler classes that export the logging events.

* An optional error handler to catch failures for HTTP Event Collector events.

* Example configuration files for all three frameworks that show how to configure the frameworks to write to HTTP Event Collector or TCP ports.

* Support for batching events (sent to HTTP Event Collector only).</li>

### Requirements

Here's what you need to get going with Splunk logging for Java.

#### Splunk

If you haven't already installed Splunk, download it
[here](http://www.splunk.com/download). For more about installing and running
Splunk and system requirements, see [Installing & Running Splunk](http://dev.splunk.com/view/SP-CAAADRV). Splunk logging for Java is tested with Splunk Enterprise 8.0 and 8.2.0.

#### Java

You'll need Java version 8 or higher, from [OpenJDK](https://openjdk.java.net) or [Oracle](https://www.oracle.com/technetwork/java).

#### Logging frameworks

If you're using the Log4j 2, Simple Logging Facade for Java (SLF4J), or Logback logging frameworks in conjunction with Splunk logging for Java there are additional compatibility requirements. For more about logging framework requirements, see [Enable logging to HEC](https://dev.splunk.com/enterprise/docs/devtools/java/logging-java/howtouseloggingjava/enableloghttpjava/) and [Enable logging to TCP inputs](https://dev.splunk.com/enterprise/docs/devtools/java/logging-java/howtouseloggingjava/enablelogtcpjava). These frameworks require:
* Log4j version 2.17.2
* SLF4J version 1.7.36
* Logback version 1.2.11

## Documentation and resources

* For more information about installing and using Splunk logging for Java, see
  [Overview of Splunk logging for Java](http://dev.splunk.com/goto/sdk-slj).

* For reference documentation, see the
  [Splunk logging for Java API reference](https://docs.splunk.com/DocumentationStatic/JavaLogging/1.8.0/index.html).

* For all things developer with Splunk, see the
  [Splunk Developer Portal](http://dev.splunk.com).

* For more about about Splunk in general, see
  [Splunk>Docs](http://docs.splunk.com/Documentation/Splunk).

## Dependency Management

The `splunk-library-javalogging` artifact can be accessed via Splunk's managed Maven repoitory.

### Apache Maven

First define the repository as follows

```xml
...
  <repositories>
    <repository>
        <id>splunk-artifactory</id>
        <name>Splunk Releases</name>
        <url>https://splunk.jfrog.io/splunk/ext-releases-local</url>
    </repository>
  </repositories>
...
```

... then reference the dependency as follows

```xml
...
  <dependencies>
    <dependency>
      <groupId>com.splunk.logging</groupId>
      <artifactId>splunk-library-javalogging</artifactId>
      <version>${latest.version}</version>
    </dependency>
    ...
  </dependencies>
...
```

The above can be adapted to suit Other dependency management implementations as necessary.

## Custom TrustStore Configuration

### Using Custom Certificates with HTTPS

If your Splunk server uses a self-signed certificate or a certificate signed by a custom CA, you can configure a custom trust store programmatically:

```java
import java.security.KeyStore;
import com.splunk.logging.HttpEventCollectorLogbackAppender;

// Load your custom KeyStore containing trusted certificates
KeyStore trustStore = KeyStore.getInstance("PKCS12");
trustStore.load(new FileInputStream("/path/to/truststore.p12"), "password".toCharArray());

// Configure the appender
HttpEventCollectorLogbackAppender appender = new HttpEventCollectorLogbackAppender();
appender.setUrl("https://your-splunk-server:8088");
appender.setToken("your-token-here");
appender.setTrustStore(trustStore);  // Set custom trust store
appender.start();
```

**Note:** This feature requires programmatic configuration and cannot be set via XML/properties files. It is intended for applications that manage their own certificate stores.

**Important:** If you set `disableCertificateValidation(true)`, it will override the custom TrustStore configuration. The `disableCertificateValidation` setting takes precedence.

### Custom Hostname Verification

If your Splunk server's certificate doesn't have the correct hostname in its Subject Alternative Names (SANs), you can provide a custom hostname verifier:

```java
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import com.splunk.logging.HttpEventCollectorLogbackAppender;

// Configure the appender with custom hostname verification
HttpEventCollectorLogbackAppender appender = new HttpEventCollectorLogbackAppender();
appender.setUrl("https://your-splunk-server:8088");
appender.setToken("your-token-here");
appender.setTrustStore(trustStore);  // Optional: set custom trust store

// Set custom hostname verifier
appender.setHostnameVerifier(new HostnameVerifier() {
    @Override
    public boolean verify(String hostname, SSLSession session) {
        // Implement your custom hostname verification logic
        // For example, accept specific hostnames or verify against certificate
        return true;  // or your custom logic
    }
});
appender.start();
```

**Note:** Custom hostname verification is independent of the TrustStore configuration. You can use it with the default JVM trust store or with a custom TrustStore. However, if `disableCertificateValidation` is set to true, the custom hostname verifier will be ignored.

### PortSwigger Fork-Specific Feature

This fork adds the ability to configure a custom trust store for SSL/TLS connections. This was implemented to support Burp Suite DAST's certificate management system, allowing users to add custom certificates via the UI without modifying the JVM's cacerts file.

For more details, see [ENT-10043](https://portswigger.atlassian.net/browse/ENT-10043).

## License

Splunk logging for Java is licensed under the Apache License 2.0.

See the [LICENSE file](/license.md) for details.

## Contributions

[Get the Splunk Java Logging Framework from GitHub](https://github.com/splunk/splunk-library-javalogging)
and clone the resources to your computer. For example, use the following
command:

    git clone https://github.com/splunk/splunk-library-javalogging.git

## Support

The Splunk logging for Java is community-supported.

1. You can find help through our community on [Splunk Answers](https://community.splunk.com/t5/tag/logging-library-java/tg-p) (use the "logging-library-java" tag to identify your questions).
2. File issues on [GitHub](https://github.com/splunk/splunk-library-javalogging/issues).
