# Splunk Logging for Java Changelog

## Version 1.8.0

* Update JSON serialization- message property should not be encoded as a string
* Changed underlying HTTP client to OkHttp. This change should decrease memory
  usage and increase performance.
* Updated Gradle build plugins to latest versions

## Version 1.7.3

* Update Log4j dependency version to 2.10.0 [#114](https://github.com/splunk/splunk-library-javalogging/pull/114).

## Version 1.7.2

* Closing httpclient properly on logback shutdown [#112](https://github.com/splunk/splunk-library-javalogging/pull/112).

## Version 1.7.1

* Change HttpEventCollectorLogbackAppender stop method to call this.sender.close()  [#93](https://github.com/splunk/splunk-library-javalogging/pull/93).
* Rename 'Http' plugin name for HttpEventCollectorLog4jAppender to 'SplunkHttp' [#92](https://github.com/splunk/splunk-library-javalogging/pull/92).

## Version 1.7.0

* Effectively the equivalent to Version 1.6.2

## Version 1.6.2

*  Add support to allow users to define their own event body serializer for HTTP event adapter: Simply create a class implementing `com.splunk.logging.EventBodySerializer`,
and add the full class name as a property (`eventBodySerializer`) to the adapter.
Default will be a JSON event body containing message, severity, and other properties. [#86](https://github.com/splunk/splunk-library-javalogging/pull/86).

## Version 1.6.1

* TcpAppender performance improvement, prevents 100% CPU usage [#85](https://github.com/splunk/splunk-library-javalogging/pull/85).

## Version 1.6.0
* Changed messagedMimeType metadata property to messageFormat
* Fixes unit tests, and performance issues
* Fixes issues with log4j
* Fixes Cookie Expiry date issue [#74](https://github.com/splunk/splunk-library-javalogging/pull/74)
* Added Raw Endpoint support to HEC [#75](https://github.com/splunk/splunk-library-javalogging/pull/75)

## Version 1.5.3
* Add support for Logback access [#54](https://github.com/splunk/splunk-library-javalogging/issues/54)
* Make more parameters optional for Log4j appender [#47](https://github.com/splunk/splunk-library-javalogging/issues/47)

## Version 1.5.2

* Add support for setting the `host` metadata field, GitHub issue [#24](https://github.com/splunk/splunk-library-javalogging/issues/24).
* Send additional logger attributes (logger name, thread name, context/property maps) to the event, GitHub pull request [#29](https://github.com/splunk/splunk-library-javalogging/pull/29).
* Add support for exception reporting as its own field.
* Add support for marker including as its own field.

## Version 1.5.1

* Fix issues with Logback 1.1.x, see GitHub issue [#21](https://github.com/splunk/splunk-library-javalogging/issues/21).

## Version 1.5.0

* Add support for HTTP Event Collector.

## Version 1.0.1

* Add fix for hanging logback thread in `TcpAppender`.

## Version 1.0.0

Initial Splunk Logging for Java release.
