package com.google.gson.protobuf;

import com.google.common.base.CaseFormat;
import com.google.common.collect.MapMaker;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class Utils {
  private Utils() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  private static final ConcurrentMap<String, ConcurrentMap<Class<?>, Method>> cache =
      new MapMaker().makeMap();

  public static Method getMethod(Class<?> clazz, String name, Class<?>... params)
      throws NoSuchMethodException {
    ConcurrentMap<Class<?>, Method> m = cache.computeIfAbsent(name, k -> new MapMaker().makeMap());
    return m.computeIfAbsent(
        clazz,
        c -> {
          try {
            return c.getMethod(name, params);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public static void validateMessageClass(Class<? extends Message> cls) {
    if (DynamicMessage.class.isAssignableFrom(cls)) {
      throw new IllegalStateException("only generated messages are supported");
    }
  }

  public static Message.Builder newBuilderInstance(Class<? extends Message> cls) throws Exception {
    return (Message.Builder) getMethod(cls, "newBuilder").invoke(null);
  }

  public static Message getDefaultInstance(Class<? extends Message> cls) throws Exception {
    return (Message) getMethod(cls, "getDefaultInstance").invoke(null);
  }

  public static Descriptor getDescriptor(Class<? extends Message> cls) throws Exception {
    return (Descriptor) getMethod(cls, "getDescriptor").invoke(null);
  }

  public static void serializeEnumField(
      JsonObject ret,
      String name,
      Object value,
      JsonSerializationContext context,
      ProtoTypeAdapter.EnumValueStrategy strategy) {
    if (value instanceof Collection) {
      JsonArray array = new JsonArray();
      @SuppressWarnings("unchecked")
      Collection<Descriptors.EnumValueDescriptor> enumValues =
          (Collection<Descriptors.EnumValueDescriptor>) value;
      for (Descriptors.EnumValueDescriptor ed : enumValues) {
        array.add(context.serialize(strategy.serialize(ed)));
      }
      ret.add(name, array);
    } else {
      ret.add(name, context.serialize(strategy.serialize((Descriptors.EnumValueDescriptor) value)));
    }
  }

  public static void deserializeField(
      JsonObject jsonObject,
      Descriptors.FieldDescriptor fd,
      Class<? extends Message> protoClass,
      Message defaultInstance,
      Message.Builder builder,
      JsonDeserializationContext context,
      FieldNameResolver nameResolver,
      ProtoTypeAdapter.EnumValueStrategy strategy)
      throws Exception {

    String jsonField = nameResolver.resolve(fd);
    JsonElement el = jsonObject.get(jsonField);
    if (el == null || el.isJsonNull()) {
      return;
    }

    Object value;
    if (fd.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
      if (el.isJsonArray()) {
        List<Descriptors.EnumValueDescriptor> list = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
          list.add(strategy.deserialize(fd.getEnumType(), e));
        }
        value = list;
      } else {
        value = strategy.deserialize(fd.getEnumType(), el);
      }
    } else if (fd.isRepeated()) {
      String protoName =
          nameResolver.getProtoFormat().to(CaseFormat.LOWER_CAMEL, fd.getName()) + "_";
      Field f = protoClass.getDeclaredField(protoName);
      @SuppressWarnings("unchecked")
      TypeToken<List<?>> token = (TypeToken<List<?>>) TypeToken.of(f.getGenericType());
      Type type = token.getSupertype(List.class).getType();
      value = context.deserialize(el, type);
    } else {
      Object defaultVal = defaultInstance.getField(fd);
      value = context.deserialize(el, defaultVal.getClass());
    }
    builder.setField(fd, value);
  }

  public static Descriptors.EnumValueDescriptor findByName(
      Descriptors.EnumDescriptor desc, String val, EnumValueResolver resolver) {
    for (Descriptors.EnumValueDescriptor ed : desc.getValues()) {
      String name = resolver.getCustValue(ed);
      if (name.equals(val)) {
        return ed;
      }
    }
    throw new IllegalArgumentException("Unrecognized enum name: " + val);
  }

  public static Descriptors.EnumValueDescriptor findByNumber(
      Descriptors.EnumDescriptor desc, int num) {
    Descriptors.EnumValueDescriptor ed = desc.findValueByNumber(num);
    if (ed == null) {
      throw new IllegalArgumentException("Unrecognized enum value: " + num);
    }
    return ed;
  }
}
