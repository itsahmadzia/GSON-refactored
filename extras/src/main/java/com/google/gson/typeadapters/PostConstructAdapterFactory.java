package com.google.gson.typeadapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import javax.annotation.PostConstruct;

/** TypeAdapterFactory that invokes methods annotated with @PostConstruct after deserialization. */
public class PostConstructAdapterFactory implements TypeAdapterFactory {

  @Override
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    Method postConstructMethod = findPostConstructMethod(type.getRawType());
    if (postConstructMethod == null) {
      return null;
    }

    TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
    return new PostConstructAdapter<>(delegate, postConstructMethod);
  }

  private Method findPostConstructMethod(Class<?> clazz) {
    while (clazz != Object.class && clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.isAnnotationPresent(PostConstruct.class)) {
          validatePostConstructMethod(method);
          method.setAccessible(true);
          return method;
        }
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  private void validatePostConstructMethod(Method method) {
    if (method.getParameterCount() != 0) {
      throw new IllegalArgumentException(
          "@PostConstruct method must have no parameters: " + method);
    }
    if (!void.class.equals(method.getReturnType())) {
      throw new IllegalArgumentException("@PostConstruct method must return void: " + method);
    }
  }

  static final class PostConstructAdapter<T> extends TypeAdapter<T> {
    private final TypeAdapter<T> delegate;
    private final Method postConstructMethod;

    public PostConstructAdapter(TypeAdapter<T> delegate, Method postConstructMethod) {
      this.delegate = Objects.requireNonNull(delegate);
      this.postConstructMethod = Objects.requireNonNull(postConstructMethod);
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      delegate.write(out, value);
    }

    @Override
    public T read(JsonReader in) throws IOException {
      T instance = delegate.read(in);
      if (instance != null) {
        try {
          postConstructMethod.invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new IOException("Error invoking @PostConstruct method: " + postConstructMethod, e);
        }
      }
      return instance;
    }
  }
}
