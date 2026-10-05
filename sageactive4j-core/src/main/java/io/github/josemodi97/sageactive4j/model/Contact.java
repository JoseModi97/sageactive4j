package io.github.josemodi97.sageactive4j.model;

import java.util.List;
import java.util.Map;

/** A contact person of a customer or supplier. */
public final class Contact extends SageObject {

    public Contact(Map<String, Object> json) {
        super(json);
    }

    /** First name. */
    public String getName() { return string("name"); }
    /** Family name. */
    public String getSurname() { return string("surname"); }
    public Boolean getIsDefault() { return bool("isDefault"); }
    public List<Email> getEmails() { return list(Email::new, "emails"); }
    public List<Phone> getPhones() { return list(Phone::new, "phones"); }

    /** An email address of a {@link Contact}. */
    public static final class Email extends SageObject {
        public Email(Map<String, Object> json) {
            super(json);
        }

        public String getEmailAddress() { return string("emailAddress"); }
        /** {@code INVOICES}, {@code PAYMENTS}, {@code OTHERS}, ... */
        public String getUsage() { return string("usage"); }
        public Boolean getIsDefault() { return bool("isDefault"); }
    }

    /** A phone number of a {@link Contact}. */
    public static final class Phone extends SageObject {
        public Phone(Map<String, Object> json) {
            super(json);
        }

        public String getNumber() { return string("number"); }
        /** {@code MOBILE}, {@code LANDLINE}, {@code FAX}, ... */
        public String getType() { return string("type"); }
        public Boolean getIsDefault() { return bool("isDefault"); }
    }
}
