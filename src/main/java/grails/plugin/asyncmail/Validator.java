package grails.plugin.asyncmail;

import org.apache.commons.validator.routines.EmailValidator;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * Validator for mailbox
 *
 * @author Vitalii Samolovskikh aka Kefir
 */
public class Validator {
    /**
     * Checks if string is valid email address.
     *
     * @param value email address to validate
     * @return <code>true</code> is email address is valid, <code>false</code> otherwise.
     */
    public static boolean isMailbox(String value) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(value);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result || EmailValidator.getInstance(true, true).isValid(value);
    }
}
