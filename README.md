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

If you want to use UDP to send events to Splunk , then Log4j and Logback  already have Syslog Appenders
And of course you can still use any File appenders and have the file monitored by a Splunk Universal Forwarder.

## Resilience

The HTTP REST and Raw TCP handler/appenders have autonomous socket reconnection logic in case of connection failures.
There is also internal event queuing that is loosely modelled off Splunk's outputs.conf for Universal Forwarders.
You can set these propertys :
* maxQueueSize : defaults to 500KB , format [<integer>|<integer>[KB|MB|GB]]
* dropEventsOnQueueFull : defaults to false , format [ true | false]

And you can use a parallel File appender if you absolutely need disk persistence.

## Data Cloning

If you want "data cloning" functionality, then you can leverage the logging configuration and have (n) different appender
definitions for your various target Indexers.

## Load Balancing

A wrapper appender for Log4j and Logback is in the works.The design pattern is modelled off the Log4j "AsyncAppender" wrapper.
So you will be able to define a "LoadBalancedAppender" and specify nested child appenders to Load Balance across.
This opens up some interesting possibilitys such as load balancing across appenders that use the same or perhaps a 
mixture of different transports , REST / Raw TCP / Syslog.

## Thread Safety

Log4j and Logback are thread safe.

## License

The Splunk Java Logging Framework is licensed under the Creative Commons 3.0 License. 
Details can be found in the file LICENSE.

## Quick Start

1.	Untar releases/splunklogging-0.2.0.tar.gz
2.	All the required jar files are in the lib directory..
3.	Assume you know how to setup your classpath to use your preferred logging framework implementation.
4.	There is a simple code example here https://github.com/damiendallimore/SplunkJavaLogging/blob/master/src/com/dtdsoftware/splunk/logging/examples/Example.java
5.	There are sample logging config files in the config directory for the 3 logging frameworks

## Splunk

If you haven't already installed Splunk, download it here: 
http://www.splunk.com/download. For more about installing and running Splunk 
and system requirements, see Installing & Running Splunk 
(http://dev.splunk.com/view/SP-CAAADRV).

## Contribute

Get the Splunk Java Logging Framework from GitHub (https://github.com/) and clone the 
resources to your computer. For example, use the following command: 

>  git clone https://github.com/damiendallimore/SplunkJavaLogging.git

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
<td>damien@dtdsoftware.com</td>
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













