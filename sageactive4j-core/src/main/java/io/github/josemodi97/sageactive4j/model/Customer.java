package io.github.josemodi97.sageactive4j.model;

import java.util.List;
import java.util.Map;

/** A customer. */
public final class Customer extends ThirdParty {

    public Customer(Map<String, Object> json) {
        super(json);
    }

    /** {@code BUSINESS} or {@code INDIVIDUAL}. */
    public String getCustomerType() { return string("customerType"); }
    public List<Contact> getContacts() { return list(Contact::new, "contacts"); }
}
