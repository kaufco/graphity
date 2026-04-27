package com.kobayami.graphity.benchmarks.common;

import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

/**
 * Edge list with DFS edge-type classification.
 */
public final class BenchTypedEdges {

  private final IntList sources;
  private final IntList targets;
  private final ByteList types;

  public BenchTypedEdges(IntList sources, IntList targets, ByteList types) {
    int n = sources.size();
    if (targets.size() != n || types.size() != n) {
      throw new IllegalArgumentException("sources/targets/types size mismatch");
    }
    this.sources = sources;
    this.targets = targets;
    this.types = types;
  }

  public IntList sources() {
    return sources;
  }

  public IntList targets() {
    return targets;
  }

  public ByteList types() {
    return types;
  }

  public int count() {
    return sources.size();
  }

  public Long2ByteOpenHashMap asTypeMap() {
    Long2ByteOpenHashMap out = new Long2ByteOpenHashMap(count());
    for (int i = 0; i < count(); i++) {
      out.put(BenchEdgePairs.encode(sources.getInt(i), targets.getInt(i)), types.getByte(i));
    }
    return out;
  }
}
