package com.google.gson.typeadapters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;

@SuppressWarnings("JavaUtilDate")
public final class UtcDateTypeAdapterTest {
  private final Gson gson =
      new GsonBuilder().registerTypeAdapter(Date.class, new UtcDateTypeAdapter()).create();

  @Test
  public void testLocalTimeZone() {
    Date expected = new Date();
    String json = gson.toJson(expected);
    Date actual = gson.fromJson(json, Date.class);
    assertThat(actual.getTime()).isEqualTo(expected.getTime());
  }

  @Test
  public void testDifferentTimeZones() {
    for (String timeZone : TimeZone.getAvailableIDs()) {
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
      Date expected = cal.getTime();
      String json = gson.toJson(expected);
      Date actual = gson.fromJson(json, Date.class);
      assertThat(actual.getTime()).isEqualTo(expected.getTime());
    }
  }

  @Test
  public void testUtcDatesOnJdkBefore1_7() {
    Gson gson =
        new GsonBuilder().registerTypeAdapter(Date.class, new UtcDateTypeAdapter()).create();
    Date date = gson.fromJson("\"2014-12-05T04:00:00.000Z\"", Date.class);
    assertThat(date.getTime()).isEqualTo(1417752000000L);
  }

  @Test
  public void testUtcWithJdk7Default() {
    Date expected = new Date();
    SimpleDateFormat iso8601Format =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    iso8601Format.setTimeZone(TimeZone.getTimeZone("UTC"));
    String expectedJson = "\"" + iso8601Format.format(expected) + "\"";
    String actualJson = gson.toJson(expected);
    assertThat(actualJson).isEqualTo(expectedJson);
    Date actual = gson.fromJson(expectedJson, Date.class);
    assertThat(actual.getTime()).isEqualTo(expected.getTime());
  }

  @Test
  public void testNullDateSerialization() {
    String json = gson.toJson(null, Date.class);
    assertThat(json).isEqualTo("null");
  }

  @Test
  public void testWellFormedParseException() {
    var e =
        assertThrows(
            JsonParseException.class, () -> gson.fromJson("\"2017-06-20T14\"", Date.class));
    assertThat(e).hasMessageThat().isEqualTo("Failed to parse date: 2017-06-20T14");
  }
}
