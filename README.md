# Splunk Logging for Java

This project provides utilities to easily log data using Splunk's recommended best practices to any
supported logger, using any of the three major Java logging frameworks (Logback, Log4J 2, and
java.uti.logging), and to Splunk TCP inputs.

In particular, it provides

* A class SplunkCimLogEvent which encapsulates Splunk's CIM (Common Information Model) and best
  practices for semantic logging.
* A TCP appender for Logback, which is the only one of the three frameworks above that doesn't provide
  native support for writing to TCP ports.
* Example configuration files for all three frameworks showing how to configure them to write
  to Splunk TCP ports.

## Advice

### Splunk Universal Forwarder vs Splunk TCP Inputs

If you can, it is better to log to files and monitor the files with a Splunk Universal Forwarder. This provides you
with the features of the Universal Forwarder, and added robustness from having persistent files. However, there
are situations where a Universal Forwarder is not a possibility. In these cases, writing directly to a TCP input
is a reasonable approach.

In either scenario we recommend using the SplunkCimLogEvent class provided by this library to construct your
log events according to Splunk's recommended best practices.

### Resilience

All of the TCP appenders we show config files for (SocketHandler for java.util.logging, SocketAppender for log4j 2,
and the TCPAppender provided with this library for Logback) will attempt to reconnect in case of dropped connections.

### Data Cloning

You can have "data cloning" by providing multiple instances of your TCP handler in your logging configuration, each
instance pointing to different indexers.

### Load Balancing

Rather than trying to reinvent load balancing across your indexers in your log configuration, set up a Splunk
Universal Forwarder with a TCP input. Have all your logging sources write to that TCP input, and use the
Universal Forwarder's load balancing features to distribute the data from there to a set of indexers.

### Thread Safety

Log4j and Logback are thread safe.

## License

The Splunk Java Logging Framework is licensed under the Apache License 2.0.

Details can be found in the file LICENSE.

## Using Splunk Logging for Java

### With Logback

[TODO: Write this]

### With Log4J

[TODO: Write this]

### With java.util.logging

[TODO: Write this]

## Splunk

If you haven't already installed Splunk, download it here: 
http://www.splunk.com/download. For more about installing and running Splunk 
and system requirements, see Installing & Running Splunk 
(http://dev.splunk.com/view/SP-CAAADRV).

## Contribute

Get the Splunk Java Logging Framework from GitHub (https://github.com/) and clone the 
resources to your computer. For example, use the following command: 

>  git clone https://github.com/splunk/splunk-library-javalogging.git

## Resources

Documentation for this library

* http://dev.splunk.com/goto/sdk-slj

Splunk Common Information Model

* http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel

Splunk Best Practice Logging Semantics

* http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6

Introduction to the Splunk product and some of its capabilities

* http://docs.splunk.com/Documentation/Splunk/latest/User/SplunkOverview

## Contact

You can reach the Dev Platform team at [devinfo@splunk.com](mailto:devinfo@splunk.com).











