package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** A user of the current organization. */
public final class User extends SageObject {

    public User(Map<String, Object> json) {
        super(json);
    }

    public String getFullName() { return string("fullName"); }
    public String getFirstName() { return string("firstName"); }
    public String getLastName() { return string("lastName"); }
    public String getAuthenticationEmail() { return string("authenticationEmail"); }
    public String getApplicationLanguageCode() { return string("applicationLanguageCode"); }
    public String getAuth0UserId() { return string("auth0UserId"); }
}
