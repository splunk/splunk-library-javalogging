# Splunk Logging for Java

#### Version 1.5.2

This project provides utilities to easily log data using Splunk's recommended 
best practices to any supported logger, using any of the three major Java 
logging frameworks (Logback, Log4J 2, and java.util.logging), to the [HTTP Event Collector] (http://dev.splunk.com/view/event-collector/SP-CAAAE6M) and to Splunk TCP 
inputs.

In particular, it provides:

* Appenders for HTTP Event Collector for pushing from Logback, Log4J 2 and java.util.logging.
* The `SplunkCimLogEvent` class, which encapsulates the CIM (Common 
  Information Model) in Splunk Enterprise and best practices for semantic 
  logging.
* A TCP appender for Logback, which is the only one of the three frameworks 
  listed above that doesn't provide native support for writing to TCP ports.
* Example configuration files for all three frameworks, showing how to 
  configure them to write to Splunk TCP ports.

## Advice

### Splunk Universal Forwarder vs Splunk TCP Inputs

If you can, it is better to log to files and monitor them with a Splunk 
Universal Forwarder. This provides you with the features of the Universal 
Forwarder, and added robustness from having persistent files. However, there 
are situations where using a Universal Forwarder is not a possibility. In 
these cases, writing directly to a TCP input is a reasonable approach.

In either scenario, we recommend using the `SplunkCimLogEvent` class 
provided by this library to construct your log events according to Splunk's 
recommended best practices.

### Resilience

All of the TCP appenders we show config files for (SocketHandler for 
java.util.logging, SocketAppender for Log4J 2, and the TCPAppender provided 
with this library for Logback) will attempt to reconnect in case of dropped 
connections.

### Data Cloning

You can use [data cloning](http://docs.splunk.com/Splexicon:Datacloning) by 
providing multiple instances of your TCP handler in your logging 
configuration, each instance pointing to different indexers.

### Load Balancing

Rather than trying to reinvent 
[load balancing](http://docs.splunk.com/Splexicon:Loadbalancing) across your 
indexers in your log configuration, set up a Splunk Universal Forwarder with a 
TCP input. Have all your logging sources write to that TCP input, and use the 
Universal Forwarder's load balancing features to distribute the data from 
there to a set of indexers.

### Thread Safety

Log4j and Logback are thread-safe.

### Sending events to HTTP Event Collector

HTTP Event Collector requires Splunk 6.3+. Splunk Java library supports sending
events through `java.util.logging`, `log4j` and `logback` standard loggers. 
In order to use HTTP Event Collector it has to be enabled on the server and an 
application token should be created.

Splunk Logging for Java includes several examples of configuration files in 
`src/test/resources` folder.  For instance `java.util.logging` configuration looks like:

```
handlers=com.splunk.logging.HttpEventCollectorLoggingHandler
com.splunk.logging.HttpEventCollectorLoggingHandler.url=https://splunk-server:8088
com.splunk.logging.HttpEventCollectorLoggingHandler.token=<token-guid>
```

Sending events is simple:

```java
Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
LOGGER.info("hello world");
```

For more information, see http://dev.splunk.com/view/SP-CAAAE2K.

# License

Splunk logging for Java is licensed under the Apache License 2.0.

Details can be found in the LICENSE file.

## Using Splunk Logging for Java

To use the Splunk Logging for Java library, you will need to add it and the 
logging library you have chosen to use to your project, open a TCP input on a 
Splunk instance to write your log events to, configure your logging system, 
and then use the `SplunkCimLogEvent` class to generate well formed log 
entries.

1. Add the Splunk Logging for Java library to your project. If you are using 
   Maven, add the following to your dependencies section:

    ```xml
    <dependency>
        <groupId>com.splunk.logging</groupId>
        <artifactId>splunk-library-javalogging</artifactId>
        <version>1.5.2</version>
    </dependency>
    ```

    You might also want to add the following repository to your repositories section:

    ```
    <repository>
        <id>splunk</id>
        <name>splunk-releases</name>
        <url>http://splunk.artifactoryonline.com/splunk/ext-releases-local</url>
    </repository>
    ```

   If you are using Ant, download the corresponding JAR file from 
   [http://dev.splunk.com/goto/sdk-slj](http://dev.splunk.com/goto/sdk-slj).

2. Add the logging framework you plan to use. The three big ones in use today 
   are *Logback*, *Log4J 2.x*, and *java.util.logging* (which comes with your 
   JDK). If you are using Maven, add the corresponding dependencies below to 
   your `pom.xml`:

    * Logback:

    ```xml
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.5</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.0.13</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-core</artifactId>
        <version>1.0.13</version>
    </dependency>
    ```
    * Log4J 2.x:

    ```xml
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.0-beta9</version>
    </dependency>
    ```
    
    * java.util.logging ships with the JDK.

3. Create a TCP input in Splunk that you will write to. To learn how, see
   [Get data from TCP and UDP ports](http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports).

4. Configure your logging system. Here are simple example configurations for 
   each of the three systems. The `log4j2.xml` and `logback.xml` files 
   should be put somewhere in the classpath of your program. 
   jdklogging.properties should be specified to your program by passing the 
   following to the Java executable:

   ```
   -Djava.util.logging.config.file=/path/to/jdklogging.properties
   ```

   * Logback (to be put in `logback.xml` on the classpath)

    ```xml
    <configuration>
        <!--
        You should send data to Splunk using TCP inputs. You can find the 
        documentation on how to open TCP inputs on Splunk at http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports.

        Logback does not ship with a usable appender for TCP sockets (its 
        SocketAppender serializes Java objects for deserialization by a 
        server elsewhere). Instead, use the TcpAppender provided with this 
        library.

        This example assumes that you have Splunk running on your local 
        machine (127.0.0.1) with a TCP input configured on port 15000. 
        Note that TCP inputs are *not* the same as Splunk's management 
        port.

        You can control the format of what is logged by changing the 
        encoder (see http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout 
        for details), but the pattern below produces a simple timestamp, 
        followed by the full message and a newline, like the following:

            2012-04-26 14:54:38,461 [%thread] %level text of my event
        -->
        
        <appender name="socket" class="com.splunk.logging.TcpAppender">
            <RemoteHost>127.0.0.1</RemoteHost>
            <Port>15000</Port>
            <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>%date{ISO8601} [%thread] %level: %msg%n</pattern>
            </layout>
        </appender>

        <logger name="splunk.logger" additivity="false" level="INFO">
            <appender-ref ref="socket"/>
        </logger>

        <root level="INFO">
            <appender-ref ref="socket"/>
        </root>
    </configuration>
    ```

   * Log4j 2.x (to be put in `log4j2.xml` on the classpath)

    ```xml
    <Configuration status="info" name="example" packages="">
        <!-- Define an appender that writes to a TCP socket. We use Log4J's 
        SocketAppender, which is documented at https://logging.apache.org/log4j/2.x/manual/appenders.html#SocketAppender.

        You can find the documentation on how to open TCP inputs on Splunk 
        at http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports. 
        Note that TCP inputs are *not* the same as Splunk's management port.
        -->
        
        <Appenders>
            <Socket name="socket" host="127.0.0.1" port="15000">
            <PatternLayout pattern="%p: %m%n" charset="UTF-8"/>
            </Socket>
        </Appenders>
        <!-- Define a logger named 'splunk.logger' which writes to the socket appender we defined above. -->
        <Loggers>
            <Root level="INFO">
            </Root>
            <Logger name="splunk.logger" level="info">
            <AppenderRef ref="socket"/>
            </Logger>
        </Loggers>
    </Configuration>
    ```

   * java.util.logging

    ```
    # We will write to a Splunk TCP input using java.util.logging's 
    # SocketHandler. This line sets it to be the default handler for 
    # all loggers.
    handlers = java.util.logging.SocketHandler
    config =

    # Set the default logging level for the root logger
    .level = INFO

    # Implicitly create a logger called 'splunk.logger', set its 
    # level to INFO, and make it log using the SocketHandler.
    splunk.logger.level = INFO
    splunk.logger.handlers = java.util.logging.SocketHandler

    # Configure the SocketHandler to write to TCP port localhost:15000. 
    # Note that TCP inputs are *not* the same as Splunk's management 
    # port. You can find the documentation about how to open TCP 
    # inputs in Splunk at http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports.
    #
    # You can find the documentation on using a SocketHandler at http://docs.oracle.com/javase/7/docs/api/java/util/logging/SocketHandler.html.

    java.util.logging.SocketHandler.level = INFO
    java.util.logging.SocketHandler.host = localhost
    java.util.logging.SocketHandler.port = 15000

    # With Java 7, you can set the format of SimpleFormatter. On Java 6, 
    # you cannot and you will probably want to write a custom formatter 
    # for your system. The syntax of the format string is given at 
    # http://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax 
    # and http://docs.oracle.com/javase/7/docs/api/java/util/logging/SimpleFormatter.html
    # for logging specific behavior.
    java.util.logging.SocketHandler.formatter = SimpleFormatter
    java.util.logging.SimpleFormatter.format = "%1$F %1$r %4$s: %6$s%n"
    ```

5. Import `com.splunk.logging.SplunkCimLogEvent` and use it to create 
   events. This example code uses Logback as the logger, but the part 
   relevant to `SplunkCimLogEvent` will be unchanged for other frameworks:

   ```java
    logger.info(new SplunkCimLogEvent("Event name", "event-id") {{
        // You can add an arbitrary key=value pair with addField.
        addField("name", "value");

        // If you are logging exceptions, use addThrowable, which
        // does nice formatting. If ex is an exception you have caught
        // you would log it with
        addThrowableWithStacktrace(ex);

        // SplunkCimLogEvent provides lots of convenience methods for
        // fields defined by Splunk's Common Information Model. See
        // the SplunkCimLogEvent JavaDoc for a complete list.
        setAuthAction("deny");
    }});
   ```

## Splunk

If you haven't already installed Splunk, download it here: 
[http://www.splunk.com/download](http://www.splunk.com/download). 
For more about installing and running Splunk and system requirements, 
see [Installing & Running Splunk](http://dev.splunk.com/view/SP-CAAADRV).

## Contribute

[Get the Splunk Java Logging Framework from GitHub](https://github.com/splunk/splunk-library-javalogging) 
and clone the resources to your computer. For example, use the following 
command: 

    git clone https://github.com/splunk/splunk-library-javalogging.git

## Resources

Documentation for this library

* [http://dev.splunk.com/goto/sdk-slj](http://dev.splunk.com/goto/sdk-slj)

Splunk Common Information Model

* [http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel](http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel)

Splunk Best Practice Logging Semantics

* [http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6](http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6)

Introduction to the Splunk product and some of its capabilities

* [http://docs.splunk.com/Documentation/Splunk/latest/User/SplunkOverview](http://docs.splunk.com/Documentation/Splunk/latest/User/SplunkOverview)

## Contact

You can reach the Dev Platform team at [devinfo@splunk.com](mailto:devinfo@splunk.com).
<# Splunk Logging for Java

This project provides utilities to easily log data using Splunk's recommended 
best practices to any supported logger, using any of the three major Java 
logging frameworks (Logback, Log4J 2, and java.util.logging), and to Splunk TCP 
inputs.

In particular, it provides:

* The `SplunkCimLogEvent` class, which encapsulates the CIM (Common 
  Information Model) in Splunk Enterprise and best practices for semantic 
  logging.
* A TCP appender for Logback, which is the only one of the three frameworks 
  listed above that doesn't provide native support for writing to TCP ports.
* Example configuration files for all three frameworks, showing how to 
  configure them to write to Splunk TCP ports.

## Advice

### Splunk Universal Forwarder vs Splunk TCP Inputs

If you can, it is better to log to files and monitor them with a Splunk 
Universal Forwarder. This provides you with the features of the Universal 
Forwarder, and added robustness from having persistent files. However, there 
are situations where using a Universal Forwarder is not a possibility. In 
these cases, writing directly to a TCP input is a reasonable approach.

In either scenario, we recommend using the `SplunkCimLogEvent` class 
provided by this library to construct your log events according to Splunk's 
recommended best practices.

### Resilience

All of the TCP appenders we show config files for (SocketHandler for 
java.util.logging, SocketAppender for Log4J 2, and the TCPAppender provided 
with this library for Logback) will attempt to reconnect in case of dropped 
connections.

### Data Cloning

You can use [data cloning](http://docs.splunk.com/Splexicon:Datacloning) by 
providing multiple instances of your TCP handler in your logging 
configuration, each instance pointing to different indexers.

### Load Balancing

Rather than trying to reinvent 
[load balancing](http://docs.splunk.com/Splexicon:Loadbalancing) across your 
indexers in your log configuration, set up a Splunk Universal Forwarder with a 
TCP input. Have all your logging sources write to that TCP input, and use the 
Universal Forwarder's load balancing features to distribute the data from 
there to a set of indexers.

### Thread Safety

Log4j and Logback are thread-safe.

### Sending events to HTTP Event Collector

HTTP Event Collector requires Splunk 6.3+. Splunk Java library supports sending
events through `java.util.logging`, `log4j` and `logback` standard loggers. 
In order to use HTTP Event Collector it has to be enabled on the server and an 
application token should be created.

Splunk Logging for Java includes several examples of configuration files in 
`src/test/resources` folder.  For instance `java.util.logging` configuration looks like:

```
handlers=com.splunk.logging.HttpEventCollectorLoggingHandler
com.splunk.logging.HttpEventCollectorLoggingHandler.url=https://splunk-server:8088
com.splunk.logging.HttpEventCollectorLoggingHandler.token=<token-guid>
```

Sending events is simple:

```java
Logger LOGGER = java.util.logging.Logger.getLogger("splunk.java.util");
LOGGER.info("hello world");
```

For more information, see http://dev.splunk.com/view/SP-CAAAE2K.

# License

The Splunk Logging for Java is licensed under the Apache License 2.0.

Details can be found in the LICENSE file.

## Using Splunk Logging for Java

To use the Splunk Logging for Java library, you will need to add it and the 
logging library you have chosen to use to your project, open a TCP input on a 
Splunk instance to write your log events to, configure your logging system, 
and then use the `SplunkCimLogEvent` class to generate well formed log 
entries.

1. Add the Splunk Logging for Java library to your project. If you are using 
   Maven, add the following to your dependencies section:

    ```xml
    <dependency>
        <groupId>com.splunk.dev</groupId>
        <artifactId>splunk-library-javalogging</artifactId>
        <version>1.5.2</version>
    </dependency>
    ```

   If you are using Ant, download the corresponding JAR file from 
   [http://dev.splunk.com/goto/sdk-slj](http://dev.splunk.com/goto/sdk-slj).</li>

2. Add the logging framework you plan to use. The three big ones in use today 
   are *Logback*, *Log4J 2.x*, and *java.util.logging* (which comes with your 
   JDK). If you are using Maven, add the corresponding dependencies below to 
   your `pom.xml`:

    * Logback:

    ```xml
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.5</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.0.13</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-core</artifactId>
        <version>1.0.13</version>
    </dependency>
    ```
    * Log4J 2.x:

    ```xml
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.0-beta9</version>
    </dependency>
    ```
    
    * java.util.logging ships with the JDK.

3. Create a TCP input in Splunk that you will write to. To learn how, see
   [Get data from TCP and UDP ports](http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports).

4. Configure your logging system. Here are simple example configurations for 
   each of the three systems. The `log4j2.xml` and `logback.xml` files 
   should be put somewhere in the classpath of your program. 
   jdklogging.properties should be specified to your program by passing the 
   following to the Java executable:

   ```
   -Djava.util.logging.config.file=/path/to/jdklogging.properties
   ```

   * Logback (to be put in `logback.xml` on the classpath)

    ```xml
    <configuration>
        <!--
        You should send data to Splunk using TCP inputs. You can find the 
        documentation on how to open TCP inputs on Splunk at http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports.

        Logback does not ship with a usable appender for TCP sockets (its 
        SocketAppender serializes Java objects for deserialization by a 
        server elsewhere). Instead, use the TcpAppender provided with this 
        library.

        This example assumes that you have Splunk running on your local 
        machine (127.0.0.1) with a TCP input configured on port 15000. 
        Note that TCP inputs are *not* the same as Splunk's management 
        port.

        You can control the format of what is logged by changing the 
        encoder (see http://logback.qos.ch/manual/layouts.html#ClassicPatternLayout 
        for details), but the pattern below produces a simple timestamp, 
        followed by the full message and a newline, like the following:

            2012-04-26 14:54:38,461 [%thread] %level text of my event
        -->
        
        <appender name="socket" class="com.splunk.logging.TcpAppender">
            <RemoteHost>127.0.0.1</RemoteHost>
            <Port>15000</Port>
            <layout class="ch.qos.logback.classic.PatternLayout">
            <pattern>%date{ISO8601} [%thread] %level: %msg%n</pattern>
            </layout>
        </appender>

        <logger name="splunk.logger" additivity="false" level="INFO">
            <appender-ref ref="socket"/>
        </logger>

        <root level="INFO">
            <appender-ref ref="socket"/>
        </root>
    </configuration>
    ```

   * Log4j 2.x (to be put in `log4j2.xml` on the classpath)

    ```xml
    <Configuration status="info" name="example" packages="">
        <!-- Define an appender that writes to a TCP socket. We use Log4J's 
        SocketAppender, which is documented at https://logging.apache.org/log4j/2.x/manual/appenders.html#SocketAppender.

        You can find the documentation on how to open TCP inputs on Splunk 
        at http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports. 
        Note that TCP inputs are *not* the same as Splunk's management port.
        -->
        
        <Appenders>
            <Socket name="socket" host="127.0.0.1" port="15000">
            <PatternLayout pattern="%p: %m%n" charset="UTF-8"/>
            </Socket>
        </Appenders>
        <!-- Define a logger named 'splunk.logger' which writes to the socket appender we defined above. -->
        <Loggers>
            <Root level="INFO">
            </Root>
            <Logger name="splunk.logger" level="info">
            <AppenderRef ref="socket"/>
            </Logger>
        </Loggers>
    </Configuration>
    ```

   * java.util.logging

    ```
    # We will write to a Splunk TCP input using java.util.logging's 
    # SocketHandler. This line sets it to be the default handler for 
    # all loggers.
    handlers = java.util.logging.SocketHandler
    config =

    # Set the default logging level for the root logger
    .level = INFO

    # Implicitly create a logger called 'splunk.logger', set its 
    # level to INFO, and make it log using the SocketHandler.
    splunk.logger.level = INFO
    splunk.logger.handlers = java.util.logging.SocketHandler

    # Configure the SocketHandler to write to TCP port localhost:15000. 
    # Note that TCP inputs are *not* the same as Splunk's management 
    # port. You can find the documentation about how to open TCP 
    # inputs in Splunk at http://docs.splunk.com/Documentation/Splunk/latest/Data/Monitornetworkports.
    #
    # You can find the documentation on using a SocketHandler at http://docs.oracle.com/javase/7/docs/api/java/util/logging/SocketHandler.html.

    java.util.logging.SocketHandler.level = INFO
    java.util.logging.SocketHandler.host = localhost
    java.util.logging.SocketHandler.port = 15000

    # With Java 7, you can set the format of SimpleFormatter. On Java 6, 
    # you cannot and you will probably want to write a custom formatter 
    # for your system. The syntax of the format string is given at 
    # http://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html#syntax 
    # and http://docs.oracle.com/javase/7/docs/api/java/util/logging/SimpleFormatter.html
    # for logging specific behavior.
    java.util.logging.SocketHandler.formatter = SimpleFormatter
    java.util.logging.SimpleFormatter.format = "%1$F %1$r %4$s: %6$s%n"
    ```

5. Import `com.splunk.logging.SplunkCimLogEvent` and use it to create 
   events. This example code uses Logback as the logger, but the part 
   relevant to `SplunkCimLogEvent` will be unchanged for other frameworks:

   ```java
    logger.info(new SplunkCimLogEvent("Event name", "event-id") {{
        // You can add an arbitrary key=value pair with addField.
        addField("name", "value");

        // If you are logging exceptions, use addThrowable, which
        // does nice formatting. If ex is an exception you have caught
        // you would log it with
        addThrowableWithStacktrace(ex);

        // SplunkCimLogEvent provides lots of convenience methods for
        // fields defined by Splunk's Common Information Model. See
        // the SplunkCimLogEvent JavaDoc for a complete list.
        setAuthAction("deny");
    }});
   ```

## Splunk

If you haven't already installed Splunk, download it here: 
[http://www.splunk.com/download](http://www.splunk.com/download). 
For more about installing and running Splunk and system requirements, 
see [Installing & Running Splunk](http://dev.splunk.com/view/SP-CAAADRV).

## Contribute

[Get the Splunk Java Logging Framework from GitHub](https://github.com/splunk/splunk-library-javalogging) 
and clone the resources to your computer. For example, use the following 
command: 

    git clone https://github.com/splunk/splunk-library-javalogging.git

## Resources

Documentation for this library

* [http://dev.splunk.com/goto/sdk-slj](http://dev.splunk.com/goto/sdk-slj)

Splunk Common Information Model

* [http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel](http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel)

Splunk Best Practice Logging Semantics

* [http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6](http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6)

Introduction to the Splunk product and some of its capabilities

* [http://docs.splunk.com/Documentation/Splunk/latest/User/SplunkOverview](http://docs.splunk.com/Documentation/Splunk/latest/User/SplunkOverview)

## Contact

You can reach the Dev Platform team at [devinfo@splunk.com](mailto:devinfo@splunk.com).
