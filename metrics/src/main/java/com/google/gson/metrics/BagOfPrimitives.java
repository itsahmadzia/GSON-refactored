package com.google.gson.metrics;

import com.google.common.base.Objects;

public class BagOfPrimitives {
  public static final long DEFAULT_VALUE = 0;
  private long longValue;
  private int intValue;
  private boolean booleanValue;
  private String stringValue;

  public BagOfPrimitives() {
    this(DEFAULT_VALUE, 0, false, "");
  }

  public BagOfPrimitives(long longValue, int intValue, boolean booleanValue, String stringValue) {
    this.longValue = longValue;
    this.intValue = intValue;
    this.booleanValue = booleanValue;
    this.stringValue = stringValue;
  }

  public long getLongValue() {
    return longValue;
  }

  public int getIntValue() {
    return intValue;
  }

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public String getStringValue() {
    return stringValue;
  }

  public String getExpectedJson() {
    return "{"
        + "\"longValue\":"
        + longValue
        + ","
        + "\"intValue\":"
        + intValue
        + ","
        + "\"booleanValue\":"
        + booleanValue
        + ","
        + "\"stringValue\":\""
        + stringValue
        + "\"}";
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(longValue, intValue, booleanValue, stringValue);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BagOfPrimitives)) {
      return false;
    }
    BagOfPrimitives that = (BagOfPrimitives) o;
    return longValue == that.longValue
        && intValue == that.intValue
        && booleanValue == that.booleanValue
        && Objects.equal(stringValue, that.stringValue);
  }

  @Override
  public String toString() {
    return String.format(
        "(longValue=%d,intValue=%d,booleanValue=%b,stringValue=%s)",
        longValue, intValue, booleanValue, stringValue);
  }
}
