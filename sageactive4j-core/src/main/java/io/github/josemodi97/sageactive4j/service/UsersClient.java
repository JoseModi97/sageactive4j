package io.github.josemodi97.sageactive4j.service;

import io.github.josemodi97.sageactive4j.SageActive4jClient;
import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.internal.GraphQLDocuments;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.AccessCheck;
import io.github.josemodi97.sageactive4j.model.User;
import io.github.josemodi97.sageactive4j.model.UserProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** The signed-in user, the organization's users, and permission checks. */
public final class UsersClient extends DomainClient {

    public UsersClient(SageActive4jClient client) {
        super(client);
    }

    /** The signed-in user. Needs no organization - the usual first call to check a connection. */
    public UserProfile getProfile() {
        Map<String, Object> data = global().query(GraphQLDocuments.operation("userProfile", null));
        return new UserProfile(require(data, "userProfile"));
    }

    /** Users of the current organization. */
    public Connection<User> list(ListOptions options) {
        return list(organization("users"), "users", options, null, null, null, User::new);
    }

    /**
     * Whether the signed-in user may run each action (query or mutation
     * names, e.g. {@code createCustomer}, {@code accountingAccounts}).
     */
    public List<AccessCheck> checkAccess(Collection<String> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        Map<String, Object> data = organization("userAccessPolicyCheck").query(GraphQLDocuments.operation(
                "userAccessPolicyCheck", Collections.singletonMap("actions", new ArrayList<String>(actions))));
        List<AccessCheck> checks = new ArrayList<AccessCheck>();
        for (Object item : JsonReader.getList(data, "userAccessPolicyCheck")) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                checks.add(new AccessCheck(map));
            }
        }
        return Collections.unmodifiableList(checks);
    }

    public List<AccessCheck> checkAccess(String... actions) {
        return checkAccess(Arrays.asList(actions));
    }

    /** {@code true} only if Sage Active explicitly allows the action. */
    public boolean isAllowed(String action) {
        for (AccessCheck check : checkAccess(action)) {
            if (action.equals(check.getAction())) {
                return check.isAllowed();
            }
        }
        return false;
    }
}
