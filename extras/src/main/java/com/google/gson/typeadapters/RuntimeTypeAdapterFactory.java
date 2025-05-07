package com.google.gson.typeadapters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** A factory for creating type adapters that support polymorphism. */
public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
  private final Class<?> baseType;
  private final String typeFieldName;
  private final Map<String, Class<?>> labelToSubtype = new HashMap<>();
  private final Map<Class<?>, String> subtypeToLabel = new HashMap<>();

  private RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName) {
    this.baseType = Objects.requireNonNull(baseType);
    this.typeFieldName = Objects.requireNonNull(typeFieldName);
  }

  public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
    return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName);
  }

  public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType) {
    return new RuntimeTypeAdapterFactory<>(baseType, "type");
  }

  public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> subtype, String label) {
    if (subtype == null || label == null) {
      throw new NullPointerException("Subtype or label is null");
    }
    if (subtypeToLabel.containsKey(subtype) || labelToSubtype.containsKey(label)) {
      throw new IllegalArgumentException("Subtype or label already registered: " + label);
    }
    labelToSubtype.put(label, subtype);
    subtypeToLabel.put(subtype, label);
    return this;
  }

  public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> subtype) {
    return registerSubtype(subtype, subtype.getSimpleName());
  }

  @Override
  public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> typeToken) {
    if (!baseType.isAssignableFrom(typeToken.getRawType())) {
      return null;
    }

    Map<String, TypeAdapter<?>> labelToAdapter = new HashMap<>();
    Map<Class<?>, TypeAdapter<?>> subtypeToAdapter = new HashMap<>();

    for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
      TypeAdapter<?> adapter = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
      labelToAdapter.put(entry.getKey(), adapter);
      subtypeToAdapter.put(entry.getValue(), adapter);
    }

    return new TypeAdapter<R>() {
      @Override
      public void write(JsonWriter out, R value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }

        Class<?> srcType = value.getClass();
        String label = subtypeToLabel.get(srcType);
        @SuppressWarnings("unchecked")
        TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToAdapter.get(srcType);

        if (delegate == null) {
          throw new JsonParseException(
              "Cannot serialize unregistered subtype: " + srcType.getName());
        }

        JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();
        jsonObject.addProperty(typeFieldName, label);
        Streams.write(jsonObject, out);
      }

      @Override
      public R read(JsonReader in) throws IOException {
        JsonElement jsonElement = Streams.parse(in);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement labelElement = jsonObject.remove(typeFieldName);

        if (labelElement == null) {
          throw new JsonParseException("Missing type field: " + typeFieldName);
        }

        String label = labelElement.getAsString();
        @SuppressWarnings("unchecked")
        TypeAdapter<R> delegate = (TypeAdapter<R>) labelToAdapter.get(label);

        if (delegate == null) {
          throw new JsonParseException("Unregistered subtype label: " + label);
        }

        return delegate.fromJsonTree(jsonObject);
      }
    };
  }
}
