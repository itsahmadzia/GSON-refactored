package com.google.gson.interceptors;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

/**
 * A type adapter factory that implements {@code @Intercept} by wrapping deserialized objects with a
 * post-deserialization interceptor.
 */
public final class InterceptorFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Intercept intercept = type.getRawType().getAnnotation(Intercept.class);
    if (intercept == null) {
      return null;
    }

    TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
    return new InterceptorAdapter<>(delegate, intercept);
  }

  private static class InterceptorAdapter<T> extends TypeAdapter<T> {
    private final TypeAdapter<T> delegate;
    private final JsonPostDeserializer<T> postDeserializer;

    @SuppressWarnings("unchecked")
    public InterceptorAdapter(TypeAdapter<T> delegate, Intercept intercept) {
      this.delegate = delegate;
      try {
        this.postDeserializer =
            (JsonPostDeserializer<T>)
                intercept.postDeserialize().getDeclaredConstructor().newInstance();

      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException(
            "Failed to instantiate post deserializer: " + intercept.postDeserialize().getName(), e);
      }
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      delegate.write(out, value);
    }

    @Override
    public T read(JsonReader in) throws IOException {
      T result = delegate.read(in);
      if (result != null) {
        postDeserializer.postDeserialize(result);
      }
      return result;
    }
  }
}
