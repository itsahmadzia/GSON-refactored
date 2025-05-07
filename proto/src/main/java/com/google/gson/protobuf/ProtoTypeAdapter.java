package com.google.gson.protobuf;

import static java.util.Objects.requireNonNull;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.protobuf.DescriptorProtos.EnumValueOptions;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Extension;
import com.google.protobuf.Message;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProtoTypeAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {

  public enum EnumSerialization {
    NAME,
    NUMBER
  }

  public interface EnumValueStrategy {
    Object serialize(EnumValueDescriptor desc);

    EnumValueDescriptor deserialize(EnumDescriptor enumType, JsonElement element);
  }

  public static class NameStrategy implements EnumValueStrategy {
    private final EnumValueResolver enumResolver;

    public NameStrategy(EnumValueResolver enumResolver) {
      this.enumResolver = enumResolver;
    }

    @Override
    public Object serialize(EnumValueDescriptor desc) {
      return enumResolver.getCustValue(desc);
    }

    @Override
    public EnumValueDescriptor deserialize(EnumDescriptor enumType, JsonElement element) {
      return Utils.findByName(enumType, element.getAsString(), enumResolver);
    }
  }

  public static class NumberStrategy implements EnumValueStrategy {
    @Override
    public Object serialize(EnumValueDescriptor desc) {
      return Integer.valueOf(desc.getNumber());
    }

    @Override
    public EnumValueDescriptor deserialize(EnumDescriptor enumType, JsonElement element) {
      return Utils.findByNumber(enumType, element.getAsInt());
    }
  }

  public static class Builder {
    private final Set<Extension<FieldOptions, String>> serializedNameExtensions = new HashSet<>();
    private final Set<Extension<EnumValueOptions, String>> serializedEnumValueExtensions =
        new HashSet<>();
    private EnumSerialization enumSerialization;
    private CaseFormat protoFormat;
    private CaseFormat jsonFormat;
    private boolean shouldUseJsonNameFieldOption = false;

    private Builder(EnumSerialization enumSerialization, CaseFormat from, CaseFormat to) {
      setEnumSerialization(enumSerialization);
      setFieldNameSerializationFormat(from, to);
    }

    public Builder setEnumSerialization(EnumSerialization e) {
      this.enumSerialization = requireNonNull(e);
      return this;
    }

    public Builder setFieldNameSerializationFormat(CaseFormat from, CaseFormat to) {
      this.protoFormat = from;
      this.jsonFormat = to;
      return this;
    }

    public Builder addSerializedNameExtension(Extension<FieldOptions, String> ext) {
      serializedNameExtensions.add(requireNonNull(ext));
      return this;
    }

    public Builder addSerializedEnumValueExtension(Extension<EnumValueOptions, String> ext) {
      serializedEnumValueExtensions.add(requireNonNull(ext));
      return this;
    }

    public Builder setShouldUseJsonNameFieldOption(boolean flag) {
      this.shouldUseJsonNameFieldOption = flag;
      return this;
    }

    public ProtoTypeAdapter build() {
      EnumValueResolver enumResolver = new EnumValueResolver(serializedEnumValueExtensions);
      EnumValueStrategy strategy =
          (enumSerialization == EnumSerialization.NAME)
              ? new NameStrategy(enumResolver)
              : new NumberStrategy();
      FieldNameResolver nameResolver =
          new FieldNameResolver(
              protoFormat, jsonFormat, serializedNameExtensions, shouldUseJsonNameFieldOption);
      return new ProtoTypeAdapter(strategy, nameResolver);
    }
  }

  public static Builder newBuilder() {
    return new Builder(EnumSerialization.NAME, CaseFormat.LOWER_UNDERSCORE, CaseFormat.LOWER_CAMEL);
  }

  private final EnumValueStrategy enumStrategy;
  private final FieldNameResolver nameResolver;

  private ProtoTypeAdapter(EnumValueStrategy enumStrategy, FieldNameResolver nameResolver) {
    this.enumStrategy = enumStrategy;
    this.nameResolver = nameResolver;
  }

  @Override
  public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject ret = new JsonObject();
    for (Map.Entry<FieldDescriptor, Object> entry : src.getAllFields().entrySet()) {
      String name = nameResolver.resolve(entry.getKey());
      if (entry.getKey().getType() == FieldDescriptor.Type.ENUM) {
        Utils.serializeEnumField(ret, name, entry.getValue(), context, enumStrategy);
      } else {
        ret.add(name, context.serialize(entry.getValue()));
      }
    }
    return ret;
  }

  @Override
  public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    try {
      JsonObject jsonObject = json.getAsJsonObject();
      @SuppressWarnings("unchecked")
      Class<? extends Message> protoClass = (Class<? extends Message>) typeOfT;
      Utils.validateMessageClass(protoClass);

      Message.Builder builder = Utils.newBuilderInstance(protoClass);
      Message defaultInstance = Utils.getDefaultInstance(protoClass);
      Descriptor descriptor = Utils.getDescriptor(protoClass);

      for (FieldDescriptor field : descriptor.getFields()) {
        Utils.deserializeField(
            jsonObject,
            field,
            protoClass,
            defaultInstance,
            builder,
            context,
            nameResolver,
            enumStrategy);
      }
      return builder.build();
    } catch (Exception e) {
      throw new JsonParseException("Error while parsing proto", e);
    }
  }
}
