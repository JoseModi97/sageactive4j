package io.github.josemodi97.sageactive4j.graphql;

import io.github.josemodi97.sageactive4j.internal.JsonReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One page of a Relay-style GraphQL connection: its nodes, the total count
 * (when selected), and cursor information. Iterating it iterates this page
 * only - use the domain clients' {@code all...(...)} methods, or
 * {@link Pages}, to walk every page.
 */
public final class Connection<T> implements Iterable<T> {

    private final List<T> nodes;
    private final Long totalCount;
    private final PageInfo pageInfo;

    public Connection(List<T> nodes, Long totalCount, PageInfo pageInfo) {
        this.nodes = nodes == null
                ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(nodes));
        this.totalCount = totalCount;
        this.pageInfo = pageInfo == null ? new PageInfo(false, false, null, null) : pageInfo;
    }

    /**
     * Parses a connection object, accepting both the {@code nodes { ... }}
     * and the {@code edges { node { ... } }} selection styles.
     */
    @SuppressWarnings("unchecked")
    public static <T> Connection<T> fromJson(Map<String, Object> json, Function<Map<String, Object>, T> mapper) {
        if (json == null) {
            return new Connection<T>(null, null, null);
        }
        List<T> nodes = new ArrayList<T>();
        Object rawNodes = json.get("nodes");
        if (rawNodes instanceof List) {
            for (Object node : (List<Object>) rawNodes) {
                if (node instanceof Map) {
                    nodes.add(mapper.apply((Map<String, Object>) node));
                }
            }
        } else {
            for (Object edge : JsonReader.getList(json, "edges")) {
                Object node = edge instanceof Map ? ((Map<String, Object>) edge).get("node") : null;
                if (node instanceof Map) {
                    nodes.add(mapper.apply((Map<String, Object>) node));
                }
            }
        }
        return new Connection<T>(nodes, JsonReader.getLong(json, "totalCount"),
                PageInfo.fromJson(JsonReader.getMap(json, "pageInfo")));
    }

    /** This page's items; never {@code null}. */
    public List<T> getNodes() {
        return nodes;
    }

    /** Total matching records across all pages, or {@code null} if not selected. */
    public Long getTotalCount() {
        return totalCount;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public boolean hasNextPage() {
        return pageInfo.hasNextPage();
    }

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return nodes.iterator();
    }

    @Override
    public String toString() {
        return "Connection{size=" + nodes.size() + ", totalCount=" + totalCount + ", " + pageInfo + '}';
    }
}
