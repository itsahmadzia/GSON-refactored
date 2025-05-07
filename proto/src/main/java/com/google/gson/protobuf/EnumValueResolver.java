package com.google.gson.protobuf;

import com.google.protobuf.DescriptorProtos.EnumValueOptions;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Extension;
import java.util.Set;

public class EnumValueResolver {
  private final Set<Extension<EnumValueOptions, String>> extensions;

  public EnumValueResolver(Set<Extension<EnumValueOptions, String>> extensions) {
    this.extensions = extensions;
  }

  public String getCustValue(EnumValueDescriptor desc) {
    for (Extension<EnumValueOptions, String> ext : extensions) {
      if (desc.getOptions().hasExtension(ext)) {
        return desc.getOptions().getExtension(ext);
      }
    }
    return desc.getName();
  }
}
