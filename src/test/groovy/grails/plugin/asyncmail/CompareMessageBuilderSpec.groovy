package grails.plugin.asyncmail

import grails.plugins.mail.MailMessageBuilder
import spock.lang.Specification

/**
 * @author Vitalii Samolovskikh aka Kefir, Puneet Behl
 */
class CompareMessageBuilderSpec extends Specification {

    void "testing builder methods"() {
        setup:
        def ammbMethods  = AsynchronousMailMessageBuilder.metaClass.methods
        def mbMethods = MailMessageBuilder.metaClass.methods

        // Remove these methods as they are not part of MailMessageBuilder api and has found to be missing from
        // AsynchronousMailMessageBuilder with some versions of groovy which will make the test fail
        mbMethods.removeAll { ['getProperty', 'setProperty', 'invokeMethod', 'access$0', 'pfaccess$0', 'pfaccess$1'].contains(it.name) }

        expect:
        mbMethods.every { MetaMethod mbm ->
            mbm.isPublic() && ammbMethods.find {it.isPublic() && it.name == mbm.name && it.returnType == mbm.returnType && it.signature == mbm.signature }
        }
    }
}
