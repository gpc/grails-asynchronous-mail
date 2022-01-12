package grails.plugin.asyncmail

import grails.config.Config
import grails.plugins.mail.GrailsMailException
import grails.plugins.mail.MailMessageContentRender
import grails.plugins.mail.MailMessageContentRenderer
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamSource
import org.springframework.mail.MailMessage
import org.springframework.mail.MailSender
import org.springframework.util.Assert

import javax.activation.FileTypeMap
import java.util.concurrent.ExecutorService

/**
 * Build new synchronous message
 */
class AsynchronousMailMessageBuilder {

    private AsynchronousMailMessage message
    private boolean immediately = false
    private boolean immediatelySetted = false

    private Locale locale

    final boolean mimeCapable
    final MailMessageContentRenderer mailMessageContentRenderer

    private FileTypeMap fileTypeMap

    final String defaultFrom
    final String defaultTo
    final String overrideAddress

    AsynchronousMailMessageBuilder(boolean mimeCapable, Config config, FileTypeMap fileTypeMap, MailMessageContentRenderer mailMessageContentRenderer = null) {
        this.mimeCapable = mimeCapable
        this.overrideAddress = config.getProperty('overrideAddress')
        this.defaultFrom = overrideAddress ?: config.getProperty('default.from')
        this.defaultTo = overrideAddress ?: config.getProperty('default.to')
        this.fileTypeMap = fileTypeMap
        this.mailMessageContentRenderer = mailMessageContentRenderer
    }

    void init(Config config) {
        message = new AsynchronousMailMessage()
        message.attemptInterval = config.getProperty('asynchronous.mail.default.attempt.interval', Long, 300000l)
        message.maxAttemptsCount = config.getProperty('asynchronous.mail.default.max.attempts.count', Integer, 1)

        def marks = getAsynchronousMailDeletingOptionsFromValue(config.getProperty('asynchronous.mail.clear.after.sent'))
        message.markDelete = marks[0]
        message.markDeleteAttachments = marks[1]
    }

    private static List<Boolean> getAsynchronousMailDeletingOptionsFromValue(value) {
        switch(value){
            case 'attachments':
                return [false,true]
            case true:
                return [true,false]
            case false:
                return [false,false]
            default:
                return [false,false]
        }
    }
    // Specified fields for asynchronous message
    @SuppressWarnings('unused')
    void beginDate(Date begin) {
        Assert.notNull(begin, "Begin date can't be null.")
        message.beginDate = begin
    }

    @SuppressWarnings('unused')
    void endDate(Date end) {
        Assert.notNull(end, "End date can't be null.")
        message.endDate = end
    }

    // Priority
    @SuppressWarnings('unused')
    void priority(int priority) {
        message.priority = priority
    }

    // Attempts
    @SuppressWarnings('unused')
    void maxAttemptsCount(int max) {
        message.maxAttemptsCount = max
    }

    @SuppressWarnings('unused')
    void attemptInterval(long interval) {
        message.attemptInterval = interval
    }

    // Mark message must be sent immediately
    void immediate(boolean value) {
        immediately = value
        immediatelySetted = true
    }

    // Mark message must be deleted after sent
    void delete(Object value) {
        def marks = getAsynchronousMailDeletingOptionsFromValue(value)
        message.markDelete = marks[0]
        message.markDeleteAttachments = marks[1]
    }

    // Multipart field do nothing
    @SuppressWarnings('unused')
    void multipart(boolean multipart) {
        // Nothing
        // Added analogous to mail plugin
    }

    @SuppressWarnings('unused')
    void multipart(int multipartMode) {
        // Nothing
        // Added analogous to mail plugin
    }

    // Added for compatibility with the Mail plugin
    @SuppressWarnings('unused')
    void async(boolean async) {
        // Nothing
        // Added analogous to mail plugin
    }

    // Mail message headers
    @SuppressWarnings('unused')
    void headers(Map headers) {
        Assert.notEmpty(headers, "Headers can't be null.")

        if(!mimeCapable){
            throw new GrailsMailException("You must use a JavaMailSender to customise the headers.")
        }

        Map map = new HashMap()

        headers.each{key, value->
            String keyString = key?.toString()
            String valueString = value?.toString()

            Assert.hasText(keyString, "Header name can't be null or empty.")
            Assert.hasText(valueString, "Value of header ${keyString} can't be null or empty.")

            map.put(keyString, valueString)
        }

        message.headers = map
    }

    // Field "to"
    void to(CharSequence recipient) {
        Assert.notNull(recipient, "Field to can't be null.")
        to([recipient])
    }

    void to(Object[] recipients) {
        Assert.notNull(recipients, "Field to can't be null.")
        to(recipients.collect { it.toString() })
    }

    void to(List<? extends CharSequence> recipients) {
        message.to = validateAndConvertAddrList('to', recipients)
    }

    private static List<String> validateAndConvertAddrList(String fieldName, List<? extends CharSequence> recipients) {
        Assert.notNull(recipients, "Field $fieldName can't be null.")
        Assert.notEmpty(recipients, "Field $fieldName can't be empty.")

        List<String> list = new ArrayList<String>(recipients.size())
        recipients.each {CharSequence seq ->
            String addr = seq.toString()
            assertEmail(addr, fieldName)
            list.add(addr)
        }
        return list
    }

    private static assertEmail(String addr, String fieldName) {
        Assert.notNull(addr, "Value of $fieldName can't be null.")
        Assert.hasText(addr, "Value of $fieldName can't be blank.")
        if (!Validator.isMailbox(addr)) {
            throw new GrailsMailException("Value of $fieldName must be email address.")
        }
    }

    // Field "bcc"
    @SuppressWarnings('unused')
    void bcc(CharSequence val) {
        Assert.notNull(val, "Field bcc can't be null.")
        bcc([val])
    }

    @SuppressWarnings('unused')
    void bcc(Object[] recipients) {
        Assert.notNull(recipients, "Field bcc can't be null.")
        bcc(recipients.collect { it.toString() })
    }

    void bcc(List<? extends CharSequence> recipients) {
        message.bcc = validateAndConvertAddrList('bcc', recipients)
    }

    // Field "cc"
    @SuppressWarnings('unused')
    void cc(CharSequence val) {
        Assert.notNull(val, "Field cc can't be null.")
        cc([val])
    }

    @SuppressWarnings('unused')
    void cc(Object[] recipients) {
        Assert.notNull(recipients, "Field cc can't be null.")
        cc(recipients.collect { it.toString() })
    }

    void cc(List<? extends CharSequence> recipients) {
        message.cc = validateAndConvertAddrList('cc', recipients)
    }

    // Field "replyTo"
    @SuppressWarnings('unused')
    void replyTo(CharSequence val) {
        def addr = val?.toString()
        assertEmail(addr, 'replyTo')
        message.replyTo = addr
    }

    // Field "from"
    void from(CharSequence sender) {
        def addr = sender?.toString()
        assertEmail(addr, 'from')
        message.from = addr
    }

    // Field "envelope from"
    @SuppressWarnings('unused')
    void envelopeFrom(CharSequence envFrom) {
        def addr = envFrom?.toString()
        assertEmail(addr, 'envelopeFrom')
        message.envelopeFrom = envFrom
    }

    // Field "subject"
    void title(CharSequence subject1) {
        subject(subject1)
    }

    void subject(CharSequence subject) {
        String string = subject?.toString()
        Assert.hasText(string, "Field subject can't be null or blank.")
        message.subject = string
    }

    // Body
    void body(CharSequence seq) {
        text(seq)
    }

    void body(Map params) {
        Assert.notEmpty(params, "Body can't be null or empty.")

        def render = doRender(params)

        if (render.html) {
            html(render.out.toString())
        } else {
            text(render.out.toString())
        }
    }

    void text(CharSequence seq) {
        def string = seq?.toString()
        Assert.hasText(string, "Body text can't be null or blank.")

        if(message.text==null || !message.html) {
            message.html = false
            message.text = string
        } else if(message.html){
            message.alternative = string
        }
    }

    void text(Map params) {
        text(doRender(params).out.toString())
    }

    void html(CharSequence seq) {
        def string = seq?.toString()
        Assert.hasText(string, "Body can't be null or blank.")

        message.html = true
        if(message.text){
            message.alternative = message.text
        }
        message.text = string
    }

    void html(Map params) {
        html(doRender(params).out.toString())
    }

    protected MailMessageContentRender doRender(Map params) {
        if (mailMessageContentRenderer == null) {
            throw new GrailsMailException("Mail message builder was constructed without a message content renderer so views can't be rendered")
        }

        if (!params.view) {
            throw new GrailsMailException("No view specified.")
        }

        return mailMessageContentRenderer.render(new StringWriter(), params.view as String, params.model as Map, locale, params.plugin as String)
    }

    @SuppressWarnings('unused')
    void locale(String localeStr) {
        Assert.hasText(localeStr, "Locale can't be null or empty.")
        locale(new Locale(localeStr.split('_', 3).toArrayString()))
    }

    void locale(Locale locale) {
        Assert.notNull(locale, "Locale can't be null.")
        this.locale = locale
    }

    // Attachments
    void attachBytes(String name, String mimeType, byte[] content) {
        Assert.hasText(name, "Attachment name can't be blank.")
        Assert.notNull(content, "Attachment content can't be null.")

        if(!mimeCapable){
            throw new GrailsMailException("You must use a JavaMailSender to add attachment.")
        }

        message.addToAttachments(
                new AsynchronousMailAttachment(
                        attachmentName: name, mimeType: mimeType, content: content
                )
        )
    }

    @SuppressWarnings('unused')
    void attach(String fileName, String contentType, byte[] bytes) {
        attachBytes(fileName, contentType, bytes)
    }

    @SuppressWarnings('unused')
    void attach(File file) {
        attach(file.name, file)
    }

    void attach(String fileName, File file) {
        attach(fileName, fileTypeMap.getContentType(file), file)
    }

    void attach(String fileName, String contentType, File file) {
        if (!file.exists()) {
            throw new FileNotFoundException("Can't use $file as an attachment as it does not exist.")
        }

        attach(fileName, contentType, new FileSystemResource(file))
    }

    void attach(String fileName, String contentType, InputStreamSource source) {
        InputStream stream = source.inputStream
        try {
            attachBytes(fileName, contentType, stream.bytes)
        } finally {
            stream.close()
        }
    }

    void inline(String name, String mimeType, byte[] content) {
        Assert.hasText(name, "Inline id can't be blank.")
        Assert.notNull(content, "Inline content can't be null.")

        if(!mimeCapable){
            throw new GrailsMailException("You must use a JavaMailSender to add inlines.")
        }

        message.addToAttachments(
                new AsynchronousMailAttachment(
                        attachmentName: name, mimeType: mimeType, content: content, inline: true
                )
        )
    }

    @SuppressWarnings('unused')
    void inline(File file) {
        inline(file.name, file)
    }

    void inline(String fileName, File file) {
        inline(fileName, fileTypeMap.getContentType(file), file)
    }

    void inline(String contentId, String contentType, File file) {
        if (!file.exists()) {
            throw new FileNotFoundException("Can't use $file as an attachment as it does not exist.")
        }

        inline(contentId, contentType, new FileSystemResource(file))
    }

    void inline(String contentId, String contentType, InputStreamSource source) {
        InputStream stream = source.inputStream
        try {
            inline(contentId, contentType, stream.bytes)
        } finally {
            stream.close()
        }
    }

    @SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
    MailMessage finishMessage() {
        throw new UnsupportedOperationException("You are using the Grails Asynchronous Mail plug-in which doesn't support some methods.")
    }

    @SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
    MailMessage sendMessage(ExecutorService executorService) {
        throw new UnsupportedOperationException("You are using the Grails Asynchronous Mail plug-in which doesn't support some methods.")
    }

    @SuppressWarnings(['unused', 'GrMethodMayBeStatic'])
    MailSender getMailSender(){
        throw new UnsupportedOperationException("You are using the Grails Asynchronous Mail plug-in which doesn't support some methods.")
    }

    AsynchronousMailMessage getMessage() { message }
    boolean getImmediately() { immediately }
    boolean getImmediatelySetted() { immediatelySetted }
}
