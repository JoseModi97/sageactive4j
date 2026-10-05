package io.github.josemodi97.sageactive4j.model;

import java.util.Map;

/** Whether the signed-in user may run one action (a query or mutation name). */
public final class AccessCheck extends SageObject {

    public AccessCheck(Map<String, Object> json) {
        super(json);
    }

    public String getAction() { return string("action"); }

    public boolean isAllowed() { return Boolean.TRUE.equals(bool("isAllowed")); }
}
