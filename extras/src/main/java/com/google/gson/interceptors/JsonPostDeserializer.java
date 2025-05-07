package com.google.gson.interceptors;

/**
 * Interface for classes that want to modify or inspect an object after it has been deserialized.
 *
 * <p>Implementing classes must either provide a no-argument constructor or be registered via an
 * {@link com.google.gson.InstanceCreator}.
 *
 * @param <T> the type of object to be processed after deserialization
 */
@FunctionalInterface
public interface JsonPostDeserializer<T> {

  /**
   * Called by Gson after {@code object} has been deserialized.
   *
   * @param object the object instance that was deserialized
   */
  void postDeserialize(T object);
}
