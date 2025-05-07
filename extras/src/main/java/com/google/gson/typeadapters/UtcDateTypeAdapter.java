package com.google.gson.typeadapters;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class UtcDateTypeAdapter extends TypeAdapter<Date> {
  private static final String PRIMARY_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static final String FALLBACK_FORMAT_PATTERN = "MMM d, yyyy hh:mm:ss a";

  private static final ThreadLocal<DateFormat> primaryFormat =
      ThreadLocal.withInitial(
          () -> {
            SimpleDateFormat sdf = new SimpleDateFormat(PRIMARY_FORMAT_PATTERN, Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
          });

  private static final ThreadLocal<DateFormat> fallbackFormat =
      ThreadLocal.withInitial(
          () -> {
            SimpleDateFormat sdf = new SimpleDateFormat(FALLBACK_FORMAT_PATTERN, Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
          });

  @Override
  public synchronized void write(JsonWriter out, Date value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    out.value(primaryFormat.get().format(value));
  }

  @Override
  public synchronized Date read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    }

    String dateStr = in.nextString();
    try {
      return primaryFormat.get().parse(dateStr);
    } catch (ParseException primaryEx) {
      try {
        return fallbackFormat.get().parse(dateStr);
      } catch (ParseException fallbackEx) {
        throw new JsonParseException("Failed to parse date: " + dateStr, fallbackEx);
      }
    }
  }
}
