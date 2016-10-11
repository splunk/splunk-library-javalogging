# Splunk Logging for Java Changelog

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
