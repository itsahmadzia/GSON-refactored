package com.google.gson.functional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.junit.Before;
import org.junit.Test;

/** Functional tests for enums with Proguard. */
public class EnumWithObfuscatedTest {
  private Gson gson;

  @Before
  public void setUp() {
    gson = new Gson();
  }

  public enum Gender {
    @SerializedName("MAIL")
    MALE,

    @SerializedName("FEMAIL")
    FEMALE
  }

  @Test
  public void testEnumClassWithObfuscated() {
    // Only run this check if explicitly requested (e.g., during an obfuscated test run)
    if (Boolean.getBoolean("gson.test.expectObfuscated")) {
      for (Gender enumConstant : Gender.class.getEnumConstants()) {
        assertThrows(
            "Enum is not obfuscated",
            NoSuchFieldException.class,
            () -> Gender.class.getField(enumConstant.name()));
      }
    }

    // These assertions always run
    assertThat(gson.fromJson("\"MAIL\"", Gender.class)).isEqualTo(Gender.MALE);
    assertThat(gson.toJson(Gender.MALE, Gender.class)).isEqualTo("\"MAIL\"");
  }
}
