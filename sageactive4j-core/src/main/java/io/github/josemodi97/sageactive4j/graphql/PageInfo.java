package io.github.josemodi97.sageactive4j.graphql;

import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.util.Map;

/** Relay-style cursor information for a {@link Connection}. Immutable. */
public final class PageInfo {

    private final boolean hasNextPage;
    private final boolean hasPreviousPage;
    private final String endCursor;
    private final String startCursor;

    public PageInfo(boolean hasNextPage, boolean hasPreviousPage, String endCursor, String startCursor) {
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
        this.endCursor = endCursor;
        this.startCursor = startCursor;
    }

    static PageInfo fromJson(Map<String, Object> json) {
        if (json == null) {
            return new PageInfo(false, false, null, null);
        }
        return new PageInfo(
                Boolean.TRUE.equals(JsonReader.getBoolean(json, "hasNextPage")),
                Boolean.TRUE.equals(JsonReader.getBoolean(json, "hasPreviousPage")),
                JsonReader.getString(json, "endCursor"),
                JsonReader.getString(json, "startCursor"));
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public boolean hasPreviousPage() {
        return hasPreviousPage;
    }

    /** Pass to {@link ListOptions#after(String)} to fetch the next page; {@code null} if unknown. */
    public String getEndCursor() {
        return endCursor;
    }

    public String getStartCursor() {
        return startCursor;
    }

    @Override
    public String toString() {
        return "PageInfo{hasNextPage=" + hasNextPage + ", endCursor=" + endCursor + '}';
    }
}
