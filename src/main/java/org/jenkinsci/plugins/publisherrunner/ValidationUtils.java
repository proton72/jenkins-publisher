package org.jenkinsci.plugins.publisherrunner;

import hudson.util.FormValidation;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Created by vdenisov on 02/04/2017.
 */
public class ValidationUtils {

    private ValidationUtils() {
    }

    public static FormValidation validateString(String toValidate)
            throws IOException, ServletException {
        if (toValidate.length() == 0)
            return FormValidation.error(Messages.ValueIsAbsent());
        if (toValidate.length() < 4)
            return FormValidation.warning(Messages.ValueIsTooShort());
        return FormValidation.ok();
    }
}
