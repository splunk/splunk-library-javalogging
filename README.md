# Splunk Java Logging Framework

The purpose of this project is to create a logging framework to allow developers to as seamlessly as possible
integrate Splunk best practice logging semantics into their code.
There are also custom handler/appender implementations for the 3 most prevalent Java logging frameworks in play.

1.	LogBack
2.	Log4j
3.	java.util logging

This framework contains :

*   Implementation of Splunk CIM(Common Information Model) and best practice logging semantics
*   java.util.logging handler for logging to Splunk REST endpoints
*   java.util.logging handler for logging to Splunk Raw TCP Server Socket
*   Log4j appender for logging to Splunk REST endpoints
*   Log4j appender for logging to Splunk Raw TCP Server Socket
*   Logback appender for logging to Splunk REST endpoints
*   Logback appender for logging to Splunk Raw TCP Server Socket
*   Example logging configuration files
*   Javadocs

If you want to use UDP to send events to Splunk , then Log4j and Logback  already have Syslog Appenders.
And of course you can still use any File appenders and have the file monitored by a Splunk Universal Forwarder.

## Splunk Universal Forwarder vs Splunk Java Logging

I always advocate the best practice of using a Splunk Universal Forwarder(UF) monitoring local files wherever possible.
Not only do you get the features inherent in the UF, but you get the added resiliency of the persistence of files.
However, there are going to be situations where, for whatever reason(technical or bureaucratic), that a UF can not
be deployed.In this case, Splunk Java Logging can be used to forward events to Splunk.
Furthermore, in either scenario, you can still utilize the SplunkLogEvent class to construct your log events in best practice 
semantic format.

## Resilience

The HTTP REST and Raw TCP handler/appenders have autonomous socket reconnection logic in case of connection failures.
There is also internal event queuing that is loosely modelled off Splunk's outputs.conf for Universal Forwarders.
You can set these propertys :
* maxQueueSize : defaults to 500KB , format [integer|integer[KB|MB|GB]]
* dropEventsOnQueueFull : defaults to false , format [ true | false]

And you can use a parallel File appender if you absolutely need disk persistence.

## Data Cloning

If you want "data cloning" functionality, then you can leverage the logging configuration and have (n) different appender
definitions for your various target Indexers.

## Load Balancing

Still in the think tank.

## Thread Safety

Log4j and Logback are thread safe.

## License

The Splunk Java Logging Framework is licensed under the Apache License 2.0.

Details can be found in the file LICENSE.

## Quick Start

1.	Untar releases/splunklogging-1.0.tar.gz
2.	All the required jar files are in the lib directory..
3.	Assume you know how to setup your classpath to use your preferred logging framework implementation.
4.	There is a simple code example here https://github.com/splunk/splunk-library-javalogging/blob/master/src/com/splunk/logging/examples/Example.java
5.	There are sample logging config files in the config directory for the 3 logging frameworks

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

Splunk Common Information Model

* http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel

Splunk Best Practice Logging Semantics

* http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6

Introduction to the Splunk product and some of its capabilities

* http://docs.splunk.com/Documentation/Splunk/latest/User/SplunkOverview

## Contact

This project was initiated by Damien Dallimore
<table>

<tr>
<td><em>Email</em></td>
<td>ddallimore@splunk.com</td>
</tr>

<tr>
<td><em>Twitter</em>
<td>@damiendallimore</td>
</tr>

<tr>
<td><em>Splunkbase.com</em>
<td>damiend</td>
</tr>

</table>













