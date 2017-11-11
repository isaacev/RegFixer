package edu.wisc.regfixer.diagnostic;

import java.util.HashMap;
import java.util.Map;

public class Timing {
  private final Map<String, Long> timings;
  private final Map<String, Long> pending;

  public Timing () {
    this.timings = new HashMap<>();
    this.pending = new HashMap<>();
  }

  public void startTiming (String name) {
    this.pending.put(name, System.nanoTime());
  }

  public void stopTimingAndAdd (String name) {
    if (this.timings.containsKey(name) == false) {
      this.timings.put(name, (long)0);
    }

    long duration = System.nanoTime() - this.pending.get(name);
    this.timings.put(name, this.timings.get(name) + duration);
  }

  public long getTiming (String name) {
    if (this.timings.containsKey(name)) {
      return this.timings.get(name);
    }

    return 0;
  }
}
