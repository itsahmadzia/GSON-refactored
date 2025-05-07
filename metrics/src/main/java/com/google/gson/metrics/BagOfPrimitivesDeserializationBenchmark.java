package com.google.gson.metrics;

import com.google.caliper.Benchmark;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;

public class BagOfPrimitivesDeserializationBenchmark {
  private Gson gson = new Gson();
  private String json = new BagOfPrimitives(10L, 5, true, "foo").getExpectedJson();

  private BagOfPrimitives deserializeWithReflection(JsonReader jr) throws Exception {
    BagOfPrimitives bag = new BagOfPrimitives();
    while (jr.hasNext()) {
      String name = jr.nextName();
      for (Field field : BagOfPrimitives.class.getDeclaredFields()) {
        if (field.getName().equals(name)) {
          field.setAccessible(true);
          Class<?> fieldType = field.getType();
          if (fieldType.equals(long.class)) {
            field.setLong(bag, jr.nextLong());
          } else if (fieldType.equals(int.class)) {
            field.setInt(bag, jr.nextInt());
          } else if (fieldType.equals(boolean.class)) {
            field.setBoolean(bag, jr.nextBoolean());
          } else if (fieldType.equals(String.class)) {
            field.set(bag, jr.nextString());
          } else {
            throw new IOException("Unexpected type for: " + name);
          }
        }
      }
    }
    return bag;
  }

  @Benchmark
  public int timeGson(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; ++i) {
      BagOfPrimitives bag = gson.fromJson(json, BagOfPrimitives.class);
      dummy += bag.getIntValue();
    }
    return dummy;
  }

  @Benchmark
  public int timeManualWithReflection(int reps) throws Exception {
    int dummy = 0;
    for (int i = 0; i < reps; ++i) {
      JsonReader jr = new JsonReader(new StringReader(json));
      jr.beginObject();
      BagOfPrimitives bag = deserializeWithReflection(jr);
      jr.endObject();
      dummy += bag.getIntValue();
    }
    return dummy;
  }
}
