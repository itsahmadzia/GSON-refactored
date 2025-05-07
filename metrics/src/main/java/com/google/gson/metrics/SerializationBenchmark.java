package com.google.gson.metrics;

import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.gson.Gson;

public class SerializationBenchmark {
  private Gson gson;
  private BagOfPrimitives bagOfPrimitives;

  @BeforeExperiment
  public void setUp() {
    gson = new Gson();
    bagOfPrimitives = new BagOfPrimitives(10L, 5, true, "foo");
  }

  private String serializeObject(Object obj) {
    return gson.toJson(obj);
  }

  @Benchmark
  public int timeBagOfPrimitives(int reps) {
    int dummy = 0;
    for (int i = 0; i < reps; ++i) {
      String json = serializeObject(bagOfPrimitives);
      dummy += json.length();
    }
    return dummy;
  }
}
