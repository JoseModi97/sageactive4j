package io.github.josemodi97.sageactive4j.graphql;

import java.util.Map;

/**
 * Turns a response's {@code data} map into a caller-defined type. The hook
 * for using your own JSON library without the core depending on it, e.g.
 * with Jackson: {@code data -> objectMapper.convertValue(data.get("salesInvoices"), MyInvoices.class)}.
 */
public interface ResponseMapper<T> {

    T map(Map<String, Object> data);
}
