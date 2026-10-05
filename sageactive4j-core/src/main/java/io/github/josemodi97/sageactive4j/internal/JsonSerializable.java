package io.github.josemodi97.sageactive4j.internal;

/**
 * Implemented by input types that know their own GraphQL/JSON shape.
 * {@link #toJsonValue()} returns anything {@link JsonWriter} accepts -
 * typically a {@code Map} - which is then serialized in its place.
 * Not part of the public API.
 */
public interface JsonSerializable {

    Object toJsonValue();
}
