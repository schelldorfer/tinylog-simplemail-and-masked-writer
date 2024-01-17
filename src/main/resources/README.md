<!-- this file must be edited in folder src/main/resources -->
<!-- the file in root folder (basedir) will be overwritten on maven install phase -->

# tinylog-simplemail-and-masked-writer

## Overview

This is a set of [custom writer](https://tinylog.org/v2/extending/#custom-writer)
for [tinylog 2](https://tinylog.org/v2/) logging framework.

## Installation

### Maven

```
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>${project.artifactId}</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Gradle

```
compile(group: '${project.groupId}', name: '${project.artifactId}', version: '${project.version}', ext: 'pom')
```

### Build Repository

https://artifactory.e-switch.ch/artifactory/libs-release-public

(add this repository to `repositories` section in your `pom.xml` or `build.gradle`)

## SimpleMailWriter

Email Writer based on Simple Java Mail

Emails are sent with [Simple Java Mail](https://www.simplejavamail.org/) which is based on Jakarata Mail 2 library.

### Configuration

#### Writer name

`simple mail`

#### Mail configuration

set mail properties directly in tinylog configuration writer config

see https://www.simplejavamail.org/configuration.html#section-available-properties for all available properties

#### Message Formatter

[Message Formatter](https://tinylog.org/v2/extending/#custom-logging-api) (`format` property) is supported

#### Send Interval

A send interval can be configured to buffer all log entries which occur withing this interval.

After the interval has passed one combined email is sent.

Use property `sendinterval` in writer config to activate and configure send interval.

see [java.time.Duration#parse(CharSequence)](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence))
for supported values

##### Filter

[Include](#include-filter) and [Exclude](#exclude-filter) filters can be defined to filter Log Entries.

Log Message, Exception Classname (including package name) and Exception Message are used to filter Log Entry.

If property is not set, no filtering is applied.

Multiple strings can be separated by `;`

###### Include Filter

Set property `filter.include` to define a list of include strings.

Log Entry must contain at least ONE string from this list, otherwise it's discarded.

###### Exclude Filter

Set property `filter.exclude` to define a list of exclude strings.

If Log Entry contains at least ONE string from this list, it's discarded.

#### Example

example of `tinylog.properties`:

```
writer_simplemail=simple mail
writer_simplemail.level=error
writer_simplemail.format={date: yyyy-MM-dd HH:mm:ss.SSS} {{level}|min-size=7} [{thread}] {class}.{method}()\t{context: prefix}{message}
writer_simplemail.simplejavamail.javaxmail.debug=false
writer_simplemail.simplejavamail.smtp.host=smtp.mailserver.com
writer_simplemail.simplejavamail.defaults.subject=SimpleMail Writer
writer_simplemail.simplejavamail.defaults.from.address=simplemail@mailserver.com
writer_simplemail.simplejavamail.defaults.to.address=logs@mailserver.com
writer_simplemail.sendinterval=PT5M
writer_simplemail.filter.include=include1; include2
writer_simplemail.filter.exclude=exclude1; exclude2
```

## Masked Writers

Masked Writers allow to mask (replace) some part of the log message.

### Configuration

#### Writer names

`masked console`, `masked file` and  `masked rolling file`

see [MaskedWriterUtil](src/main/java/ch/eswitch/tinylog/writers/MaskedWriterUtil.java) for description, configuration
and usage






