# Splunk Logging for Java Changelog

## Version 1.11.5

### Critical Security Update
* Bump Log4J version to latest 2.17.2 @see [CVE-2021-44832 Log4j<2.17.1](https://nvd.nist.gov/vuln/detail/CVE-2021-44832)
* Bump Logback version to latest 1.2.11 @see [CVE-2021-42550 Logback<1.2.8](https://nvd.nist.gov/vuln/detail/CVE-2021-42550)

### Enhancements
* Added StandardErrorCallback class. Register ErrorCallback implementations via logback or log4j xml config. (PR [#215](https://github.com/splunk/splunk-library-javalogging/pull/215))
  * ErrorCallback class used to handle error other than Server errors.

### Minor Changes
* Bump org.slf4j:slf4j-api version to latest [1.7.36](https://github.com/qos-ch/slf4j/releases/tag/v_1.7.36)
* Bump com.squareup.okhttp3:okhttp to latest [4.9.3](https://square.github.io/okhttp/changelogs/changelog_4x/#version-493)
* Bump com.google.code.gson:gson to latest [2.9.0](https://github.com/google/gson/releases/tag/gson-parent-2.9.0)
* Flush HttpClient after flushing appenders. (PR [#207](https://github.com/splunk/splunk-library-javalogging/pull/207))
* Timeout settings modified for OKHttpClient. (PR [#199](https://github.com/splunk/splunk-library-javalogging/pull/199))
* Default behavior of Splunk event header & body are reverted back to v1.7.3. (PR [#198](https://github.com/splunk/splunk-library-javalogging/pull/198))

## Version 1.11.4

### Critical Security Update
* Update Logback to version 1.2.9 per CVE-2021-42550.

## Version 1.11.3

### Critical Security Update
* Upgrade Log4J again v2.17.0 related to CVE-2021-45046 & CVE-2021-44228

## Version 1.11.2

### Critical Security Update
* Upgrading log4J to 2.16 per CVE-2021-45046.

## Version 1.11.1

### Critical Security Update
* Upgrading log4J to 2.15 per CVE-2021-44228. [PR](https://github.com/splunk/splunk-library-javalogging/pull/222)

## Version 1.11.0

### Minor Changes
* Added a parameter to set await termination timeout. [PR](https://github.com/splunk/splunk-library-javalogging/pull/179)

## Version 1.10.0

### Bug Fixes

* Fixed issue causing delayed time when using AsyncAppender (GitHub issue [#186](https://github.com/splunk/splunk-javascript-logging/issues/186))
  * Now the timestamp is being recorded at the time when log event "occurs" instead of the time when log event is being "sent"

### Minor Changes

* Updated the project to use make conventions to spin up local dockerized instances.
* Upgrade version of okhttp to 4.9.1.
* Upgrade version of slf4j to 1.7.30.
* Upgrade version of gson to 2.8.7.
* Upgrade version of junit to 4.13.2.
* Upgrade version of commons to 3.12.

 
## Version 1.9.0

* Resolve an issue with TcpAppender losing events when busy (@avdv)
* Fix an issue with middleware not delegating on completion (@Blackbaud-MikeLueders)
* Add EventHeaderSerializer that allows specifying HEC metadata (@snorwin)
* Allow specification of timeout parameters
* Allow time to be specified by EventBodySerializer (@avpavlov)
* Use an Okhttp client per appender rather than a global client (@snorwin)
* Fix an issue with empty strings in configs (@thomasmey)
* Resolve an issue with sending raw events to HEC (@tburch)
* Allow templated metadata values to be applied to an appender (@brunoalexandresantos)

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
