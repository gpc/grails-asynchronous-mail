The Grails Asynchronous Mail plugin
====================================

[![Build](https://github.com/gpc/grails-asynchronous-mail/actions/workflows/build.yml/badge.svg)](https://github.com/gpc/grails-asynchronous-mail/actions/workflows/build.yml)

Description
-----------

Grails Asynchronous Mail is a plugin for sending email messages asynchronously. It persists email messages to the
database with Grails domain classes and sends them by a scheduled Quartz job. Mail is sent on a different thread, with
the `sendAsynchronousMail` (or `sendMail`) method returning instantly, is not waiting for the mail to be actually sent. If
the SMTP server isn't available, or other errors occur, the plugin can be set to retry later.

The plugin depends on the [quartz](https://plugins.grails.org/plugin/grails/quartz) and the [mail](https://plugins.grails.org/plugin/grails/mail) plugins. You also need a persistence provider plugin, [hibernate5](https://plugins.grails.org/plugin/grails/hibernate5) (or the appropriate version of hibernate for previous grails versions) and [mongodb](https://plugins.grails.org/plugin/grails/mongodb) are supported.

Links
-----

* The plugin page: <https://plugins.grails.org/plugin/grails/asynchronous-mail>
* The VCS repository (GitHub): <https://github.com/gpc/grails-asynchronous-mail>
* The issue tracker (GitHub): <https://github.com/gpc/grails-asynchronous-mail/issues>

Installation
------------

To install just add the plugin to the plugins block of `build.gradle`:

For Grails 5.x.x
```groovy
implementation "io.github.gpc:asynchronous-mail:3.1.2"
```

For grails 4.0.x
```groovy
compile "io.github.gpc:asynchronous-mail:3.0.0"
```

For Grails 3.3.x
```groovy
compile "org.grails.plugins:asynchronous-mail:2.0.2"
```

For Grails 3.2.11 and earlier
```groovy
compile "org.grails.plugins:asynchronous-mail:2.0.2-3.2.x"
```

Configuration
-------------

The default plugin configuration is

```groovy
asynchronous.mail.default.attempt.interval=300000l      // Five minutes
asynchronous.mail.default.max.attempts.count=1
asynchronous.mail.send.repeat.interval=60000l           // One minute
asynchronous.mail.expired.collector.repeat.interval=607000l
asynchronous.mail.messages.at.once=100
asynchronous.mail.send.immediately=true
asynchronous.mail.clear.after.sent=false
asynchronous.mail.disable=false
asynchronous.mail.useFlushOnSave=true
asynchronous.mail.persistence.provider='hibernate5'     // Possible values are 'hibernate', 'hibernate4', 'hibernate5', 'mongodb'
asynchronous.mail.newSessionOnImmediateSend=false
asynchronous.mail.taskPoolSize=1
```

If you want to change this options just add options which you want to change to your configuration file `/grails-app/conf/application.groovy` or `/grails-app/conf/application.yml`.

|Option|Default|Description|
|------|-------|-----------|
|`asynchronous.mail.default.attempt.interval`|`300000l`|The default repeat interval in milliseconds between attempts to send an email.|
|`asynchronous.mail.default.max.attempts.count`|`1`|The default max attempts count per message.|
|`asynchronous.mail.send.repeat.interval`|`60000l`|The repeat interval in milliseconds between starts of the `SendJob` which sends email messages.|
|`asynchronous.mail.expired.collector.repeat.interval`|`607000l`|The repeat interval in milliseconds between starts of the `ExpiredCollectorJob` which marks messages as `EXPIRED` if time for sent is expired.|
|`asynchronous.mail.messages.at.once`|`100`|The max count of messages which can be sent per a time.|
|`asynchronous.mail.send.immediately`|`true`|If `true` then the `SendJob` is started immediately after a message was created. Since version 0.1.2.|
|`asynchronous.mail.clear.after.sent`|`false`|If `true` then all messages will be deleted after sent. If `attachments` then attachments of all messages will be deleted.|
|`asynchronous.mail.disable`|`false`|If true then jobs aren't started.|
|`asynchronous.mail.useFlushOnSave`|`true`|By default the plugin flushes all changes to the DB on every step of the sending process for prevent resending but it makes overhead. So you can set this property to `false` and it will have better performance but will not have guarantee of prevention of resending.|
|`asynchronous.mail.persistence.provider`|`hibernate5`|The persistence provider. Possible values are `hibernate`, `hibernate3`, `hibernate4`, `hibernate5`, `mongodb`.|
|`asynchronous.mail.newSessionOnImmediateSend`|`false`|If `true` the new DB session will be created for storing a message into DB. It's needed if you want to send an email in case of error when all changes in DB are rolled back.|
|`asynchronous.mail.taskPoolSize`|`1`|Max count of parallel tasks for sending messages concurrently.|

Configure the [mail](https://plugins.grails.org/plugin/grails/mail) plugin. The Asynchronous Mail plugin uses the [mail](https://plugins.grails.org/plugin/grails/mail) plugin for sending messages to the SMTP server.

Usage
-----

If you already used the [mail](https://plugins.grails.org/plugin/grails/mail) plugin, you have to import class `AsynchronousMailService` to your class.

```groovy
import grails.plugin.asyncmail.AsynchronousMailService
```

Next, inject `asynchronousMailService` or it's alias `asyncMailService` into your class.

```groovy
AsynchronousMailService asynchronousMailService
```

or

```groovy
AsynchronousMailService asyncMailService
```

The `AsynchronousMailService` is a Grails service.

Next, change your sendMail call.

```groovy
asyncMailService.sendMail {
    // Mail parameters
    to 'john.smith@example.com'
    subject 'Test'
    html '<body><u>Test</u></body>'
    attachBytes 'test.txt', 'text/plain', byteBuffer

    // Additional asynchronous parameters (optional)
    beginDate new Date(System.currentTimeMillis()+60000)    // Starts after one minute, default current date
    endDate new Date(System.currentTimeMillis()+3600000)   // Must be sent in one hour, default infinity
    maxAttemptsCount 3   // Max 3 attempts to send, default 1
    attemptInterval 300000    // Minimum five minutes between attempts, default 300000 ms
    delete true    // Marks the message for deleting after sent
    immediate true    // Run the send job after the message was created
    priority 10   // If priority is greater then message will be sent faster
}
```

Also see the sample application at <https://github.com/kefirfromperm/grails-asynchronous-mail-sample> (Grails 3).

The AsynchronousMailController and views
----------------------------------------

You can create a controller and views for viewing and editing email messages in the DB.

```
create-asynchronous-mail-controller com.example.MyMailController
```

Logging
-------

To enable full logging for the plugin just add the following lines to `/grails-app/conf/logback.groovy`.
```groovy
//...
// Enable Asynchronous Mail plugin logging
logger('grails.app.jobs.grails.plugin.asyncmail', TRACE, ['STDOUT'])
logger('grails.app.services.grails.plugin.asyncmail', TRACE, ['STDOUT'])
logger('grails.plugin.asyncmail', TRACE, ['STDOUT'])

// Enable Quartz plugin logging
logger('grails.plugins.quartz', DEBUG, ['STDOUT'])
//...
```

Indexes
-------

A recommendation is to create an index on the `async_mail_mess.status` column. It's result of my heuristic observations. Only DBA have to create indexes anyway.

Issue tracking
--------------

You can report bugs on [GitHub](https://github.com/gpc/grails-asynchronous-mail/issues?state=open).
You also can ask questions in the [Grails Community Slack Channels](https://slack.grails.org/).
Please enable logs and attach them to your issue.

Contribution
------------

If you want to contribute to the plugin just open a pull request to the repository
<https://github.com/gpc/grails-asynchronous-mail>.
