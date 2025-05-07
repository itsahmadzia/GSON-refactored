package com.google.gson.protobuf;

import com.google.common.base.CaseFormat;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Extension;
import java.util.Set;

public class FieldNameResolver {
  private final CaseFormat protoFormat;
  private final CaseFormat jsonFormat;
  private final Set<Extension<FieldOptions, String>> extensions;
  private final boolean useJsonName;

  public FieldNameResolver(
      CaseFormat protoFormat,
      CaseFormat jsonFormat,
      Set<Extension<FieldOptions, String>> extensions,
      boolean useJsonName) {
    this.protoFormat = protoFormat;
    this.jsonFormat = jsonFormat;
    this.extensions = extensions;
    this.useJsonName = useJsonName;
  }

  public String resolve(FieldDescriptor fd) {
    FieldOptions options = fd.getOptions();
    for (Extension<FieldOptions, String> ext : extensions) {
      if (options.hasExtension(ext)) {
        return options.getExtension(ext);
      }
    }
    if (useJsonName && fd.toProto().hasJsonName()) {
      return fd.getJsonName();
    }
    return protoFormat.to(jsonFormat, fd.getName());
  }

  public CaseFormat getProtoFormat() {
    return protoFormat;
  }
}
