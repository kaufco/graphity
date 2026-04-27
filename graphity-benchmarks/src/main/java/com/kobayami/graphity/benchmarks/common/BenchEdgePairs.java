package com.kobayami.graphity.benchmarks.common;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Lightweight edge-pair container for benchmark ports.
 */
public final class BenchEdgePairs {

  private final IntList sources;
  private final IntList targets;

  public BenchEdgePairs(IntList sources, IntList targets) {
    if (sources.size() != targets.size()) {
      throw new IllegalArgumentException("source/target size mismatch");
    }
    this.sources = sources;
    this.targets = targets;
  }

  public static BenchEdgePairs empty() {
    return new BenchEdgePairs(new IntArrayList(0), new IntArrayList(0));
  }

  public IntList sources() {
    return sources;
  }

  public IntList targets() {
    return targets;
  }

  public int count() {
    return sources.size();
  }

  public LongSet asLongSet() {
    LongSet out = new LongOpenHashSet(count());
    for (int i = 0; i < count(); i++) {
      out.add(encode(sources.getInt(i), targets.getInt(i)));
    }
    return out;
  }

  public static long encode(int source, int target) {
    return ((long) source << 32) | (target & 0xFFFF_FFFFL);
  }
}
