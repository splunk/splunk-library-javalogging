# Splunk Logging for Java

#### Version 1.6.2

Splunk logging for Java enables you to log events to HTTP Event Collector or to a TCP input on a Splunk Enterprise instance within your Java applications. You can use three major Java logging frameworks: [Logback](http://logback.qos.ch), [Log4j 2](http://logging.apache.org/log4j/2.x/), and [java.util.logging](https://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html). Splunk logging for Java is also enabled for [Simple Logging Facade for Java (SLF4J)](http://www.slf4j.org).

Splunk logging for Java provides:

* Appender classes that package events into the proper format for the input type you're using (HTTP Event Collector or TCP).

* Handler classes that export the logging events.

* An optional error handler to catch failures for HTTP Event Collector events.

* Example configuration files for all three frameworks that show how to configure the frameworks to write to HTTP Event Collector or TCP ports.

* Support for batching events (sent to HTTP Event Collector only).</li>

## Documentation and resources

* For more information about installing and using Splunk logging for Java, see: 
  [Overview of Splunk logging for Java](http://dev.splunk.com/goto/sdk-slj).

* For API reference documentation: 
  [Splunk logging for Java Reference](https://docs.splunk.com/DocumentationStatic/JavaLogging/1.6.2/index.html).

* For all things developer with Splunk: 
  [Splunk Developer Portal](http://dev.splunk.com).

* For more about about Splunk in general, see: 
  [Splunk>Docs](http://docs.splunk.com/Documentation/Splunk).

## License

Splunk logging for Java is licensed under the Apache License 2.0.

See the LICENSE file for details.

## Contributions

[Get the Splunk Java Logging Framework from GitHub](https://github.com/splunk/splunk-library-javalogging) 
and clone the resources to your computer. For example, use the following 
command: 

    git clone https://github.com/splunk/splunk-library-javalogging.git
    
To make a code contribution, see the [Open Source](http://dev.splunk.com/view/opensource/SP-CAAAEDM) page for more information.

## Support

The Splunk logging for Java is community-supported.

1. You can find help through our community on [Splunk Answers](http://answers.splunk.com/) (use the "logging-library-java" tag to identify your questions).
2. File issues on [GitHub](https://github.com/splunk/splunk-library-javalogging/issues).

## Contact us

You can reach the Dev Platform team at [devinfo@splunk.com](mailto:devinfo@splunk.com).
