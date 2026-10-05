package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** The signed-in user. */
public final class UserProfile extends SageObject {

    public UserProfile(Map<String, Object> json) {
        super(json);
    }

    public String getFullName() { return string("fullName"); }
    public String getFirstName() { return string("firstName"); }
    public String getLastName() { return string("lastName"); }
    public String getAuthenticationEmail() { return string("authenticationEmail"); }
    /** e.g. {@code fr-FR}; usable as the language for localized error messages. */
    public String getApplicationLanguageCode() { return string("applicationLanguageCode"); }
}
