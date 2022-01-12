package grails.plugin.asyncmail

import org.springframework.beans.factory.annotation.Value

class AsynchronousMailConfigService {

    @Value('${asynchronous.mail.default.attempt.interval}')
    private long defaultAttemptInterval
    long getDefaultAttemptInterval() { defaultAttemptInterval }

    @Value('${asynchronous.mail.default.max.attempts.count}')
    private int defaultMaxAttemptCount
    int getDefaultMaxAttemptCount() { defaultMaxAttemptCount }

    @Value('${asynchronous.mail.send.repeat.interval}')
    private long sendRepeatInterval
    long getSendRepeatInterval() { sendRepeatInterval }

    @Value('${asynchronous.mail.expired.collector.repeat.interval}')
    private long expiredCollectorRepeatInterval
    long getExpiredCollectorRepeatInterval() { expiredCollectorRepeatInterval }

    @Value('${asynchronous.mail.messages.at.once}')
    private int messagesAtOnce
    int getMessagesAtOnce() { messagesAtOnce }

    @Value('${asynchronous.mail.send.immediately}')
    private boolean sendImmediately
    boolean isSendImmediately() { sendImmediately }

    @Value('${asynchronous.mail.clear.after.sent}')
    private boolean clearAfterSent
    boolean isClearAfterSent() { clearAfterSent }

    @Value('${asynchronous.mail.disable}')
    private boolean disable
    boolean isDisable() { disable }

    @Value('${asynchronous.mail.useFlushOnSave}')
    private boolean useFlushOnSave
    boolean isUseFlushOnSave() { useFlushOnSave }

    @Value('${asynchronous.mail.persistence.provider}')
    private String persistenceProvider
    String getPersistenceProvider() { persistenceProvider }

    @Value('${asynchronous.mail.newSessionOnImmediateSend}')
    private boolean newSessionOnImmediateSend
    boolean getNewSessionOnImmediateSend() { newSessionOnImmediateSend }

    @Value('${asynchronous.mail.taskPoolSize}')
    private int taskPoolSize
    int getTaskPoolSize() { taskPoolSize }

    boolean isMongo() { getPersistenceProvider() == 'mongodb' }
}
