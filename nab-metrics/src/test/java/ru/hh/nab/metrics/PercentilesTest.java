package ru.hh.nab.metrics;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class PercentilesTest {
  private static final int[] PERCENTILES = {50, 99, 100};

  @Test
  public void zero() {
    Map<Integer, Integer> valueToCount = new HashMap<>();

    Map<Integer, Integer> percentileToValue = Percentiles.computePercentiles(valueToCount, PERCENTILES);

    assertTrue(percentileToValue.isEmpty());
  }

  @Test
  public void one() {
    Map<Integer, Integer> valueToCount = new HashMap<>();
    valueToCount.put(1, 1);

    Map<Integer, Integer> percentileToValue = Percentiles.computePercentiles(valueToCount, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(1, percentileToValue.get(99).intValue());
    assertEquals(1, percentileToValue.get(100).intValue());
  }

  @Test
  public void two() {
    Map<Integer, Integer> valueToCount = new HashMap<>();
    valueToCount.put(1, 2);

    Map<Integer, Integer> percentileToValue = Percentiles.computePercentiles(valueToCount, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(1, percentileToValue.get(99).intValue());
    assertEquals(1, percentileToValue.get(100).intValue());
  }

  @Test
  public void three() {
    Map<Integer, Integer> valueToCount = new HashMap<>();
    valueToCount.put(1, 2);
    valueToCount.put(2, 1);

    Map<Integer, Integer> percentileToValue = Percentiles.computePercentiles(valueToCount, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(2, percentileToValue.get(99).intValue());
    assertEquals(2, percentileToValue.get(100).intValue());
  }

  @Test
  public void hundred() {
    Map<Integer, Integer> valueToCount = new HashMap<>();
    valueToCount.put(1, 98);
    valueToCount.put(2, 1);
    valueToCount.put(3, 1);

    Map<Integer, Integer> percentileToValue = Percentiles.computePercentiles(valueToCount, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(2, percentileToValue.get(99).intValue());
    assertEquals(3, percentileToValue.get(100).intValue());
  }

  @Test
  public void zeroArray() {
    Map<Integer, Long> percentileToValue = Percentiles.computePercentiles(new long[0], PERCENTILES);

    assertTrue(percentileToValue.isEmpty());
  }

  @Test
  public void oneArray() {
    Map<Integer, Long> percentileToValue = Percentiles.computePercentiles(new long[] {1}, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(1, percentileToValue.get(99).intValue());
    assertEquals(1, percentileToValue.get(100).intValue());
  }

  @Test
  public void twoArray() {
    Map<Integer, Long> percentileToValue = Percentiles.computePercentiles(new long[] {1, 1}, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(1, percentileToValue.get(99).intValue());
    assertEquals(1, percentileToValue.get(100).intValue());
  }

  @Test
  public void threeArray() {
    Map<Integer, Long> percentileToValue = Percentiles.computePercentiles(new long[] {1, 2, 1}, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(2, percentileToValue.get(99).intValue());
    assertEquals(2, percentileToValue.get(100).intValue());
  }

  @Test
  public void hundredArray() {
    long [] values = new long[100];
    for (int i = 0; i < 98; i++) {
      values[i] = 1;
    }
    values[98] = 2;
    values[99] = 3;

    Map<Integer, Long> percentileToValue = Percentiles.computePercentiles(values, PERCENTILES);

    assertEquals(3, percentileToValue.size());
    assertEquals(1, percentileToValue.get(50).intValue());
    assertEquals(2, percentileToValue.get(99).intValue());
    assertEquals(3, percentileToValue.get(100).intValue());
  }
}
