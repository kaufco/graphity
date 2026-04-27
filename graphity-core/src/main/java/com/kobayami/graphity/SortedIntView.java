package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.AbstractIntList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntSpliterator;
import it.unimi.dsi.fastutil.ints.IntSpliterators;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import org.jspecify.annotations.Nullable;

/**
 * A read-only, zero-copy view over a sorted range of a primitive {@code int[]} array.
 * <p>
 * Exposes a window {@code [fromIndex, toIndex)} into a backing array.
 * The backing region must be sorted ascending with no duplicates (set semantics).
 * The view is immutable and thread-safe as long as the backing array is not modified.
 * <p>
 * Indexed access is O(1); search and membership tests are O(log n) via binary search.
 * Sub-range slicing and equality checks operate without copying data.
 * <p>
 * Implements {@link IntList} for API compatibility but does not support mutation.
 */
public final class SortedIntView extends AbstractIntList implements IntList, RandomAccess, Cloneable, java.io.Serializable {

  /** Canonical empty view. Immutable and safely shared. */
  public static final SortedIntView EMPTY = new SortedIntView(new int[0], 0, 0);

  /** Backing array shared across views over the same source. Read-only contract. */
  private final int[] array;

  /** Inclusive start of this view's window into {@link #array}. */
  private final int fromIndex;

  /** Exclusive end of this view's window into {@link #array}. */
  private final int toIndex;

  /**
   * Creates a view over the entire given array.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param array the backing array (must be sorted ascending, no duplicates)
   */
  SortedIntView(int[] array) {
    this(array, 0, array.length);
  }

  /**
   * Creates a view over the range {@code [fromIndex, toIndex)} of the given array.
   * The caller must ensure the range is sorted ascending with no duplicates.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param array     the backing array
   * @param fromIndex start index (inclusive)
   * @param toIndex   end index (exclusive)
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  SortedIntView(int[] array, int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, array.length);
    this.array = array;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
  }

  /**
   * Returns the element at the given index within this view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param index index within this view in {@code 0..size()-1}
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public int getInt(int index) {
    Objects.checkIndex(index, size());
    return array[fromIndex + index];
  }

  /**
   * Returns the number of elements in this view.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int size() {
    return toIndex - fromIndex;
  }

  /**
   * Returns the first (smallest) element in this view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the smallest element
   * @throws NoSuchElementException if the view is empty
   */
  public int first() {
    if (fromIndex == toIndex) throw new NoSuchElementException();
    return array[fromIndex];
  }

  /**
   * Returns the last (largest) element in this view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return the largest element
   * @throws NoSuchElementException if the view is empty
   */
  public int last() {
    if (fromIndex == toIndex) throw new NoSuchElementException();
    return array[toIndex - 1];
  }

  /**
   * Returns the index of {@code value} within this view, or {@code -1} if not found.
   * <p>
   * <strong>Time:</strong> O(log n) via binary search.
   *
   * @param value the value to search for
   * @return zero-based index, or {@code -1}
   */
  @Override
  public int indexOf(int value) {
    var index = Arrays.binarySearch(array, fromIndex, toIndex, value);
    return index >= 0? index - fromIndex : -1;
  }

  /**
   * Identical to {@link #indexOf(int)} - no duplicates exist.
   * <p>
   * <strong>Time:</strong> O(log n) via binary search.
   */
  @Override
  public int lastIndexOf(int value) {
    return indexOf(value);
  }

  /**
   * Returns the absolute index of {@code value} in {@code array[fromIndex..toIndex)},
   * or {@code -1} if not found.
   * <p>
   * <strong>Time:</strong> O(log n) via binary search.
   */
  static int indexOfInRange(int[] array, int fromIndex, int toIndex, int value) {
    var index = Arrays.binarySearch(array, fromIndex, toIndex, value);
    return index >= 0? index: -1;
  }

  /**
   * Returns {@code true} if {@code array[fromIndex..toIndex)} contains {@code value}.
   * <p>
   * <strong>Time:</strong> O(log n) via binary search.
   */
  static boolean containsInRange(int[] array, int fromIndex, int toIndex, int value) {
    return Arrays.binarySearch(array, fromIndex, toIndex, value) >= 0;
  }

  /**
   * Returns {@code this} - the view is immutable and needs no copy.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public SortedIntView clone() {
    return this;
  }

  /**
   * Returns a copy of this view's contents as a new {@code int[]}.
   * <p>
   * <strong>Time:</strong> O(n).<br>
   * <strong>Memory:</strong> 4 x n bytes.
   */
  @Override
  public int[] toIntArray() {
    if (fromIndex == toIndex) return IntArrays.EMPTY_ARRAY;
    return Arrays.copyOfRange(array, fromIndex, toIndex);
  }

  /**
   * Copies this view's contents into {@code dest}, or allocates a new array if
   * {@code dest} is {@code null} or too small.
   * <p>
   * <strong>Time:</strong> O(n).
   */
  @Override
  public int[] toArray(@Nullable int[] dest) {
    var size = size();
    if (dest == null || dest.length < size) {
      return toIntArray();
    }
    System.arraycopy(this.array, fromIndex, dest, 0, size);
    return dest;
  }

  /**
   * Copies {@code length} elements starting at {@code srcOffset} into {@code dest}.
   * <p>
   * <strong>Time:</strong> O(length).
   *
   * @throws IndexOutOfBoundsException if the source or destination range is invalid
   */
  @Override
  public void getElements(int srcOffset, int[] dest, int destOffset, int length) {
    Objects.checkFromIndexSize(srcOffset, length, size());
    Objects.checkFromIndexSize(destOffset, length, dest.length);
    System.arraycopy(this.array, fromIndex + srcOffset, dest, destOffset, length);
  }

  /**
   * Applies {@code action} to each element in this view, in order.
   * <p>
   * <strong>Time:</strong> O(n).
   */
  @Override
  public void forEach(java.util.function.IntConsumer action) {
    for (int i = fromIndex; i < toIndex; i++) {
      action.accept(array[i]);
    }
  }

  /**
   * Applies {@code action} to each element in {@code array[fromIndex..toIndex)}.
   * <p>
   * <strong>Time:</strong> O(toIndex - fromIndex).
   */
  static void forEachInRange(int[] array, int fromIndex, int toIndex, IntConsumer action) {
    for (int i = fromIndex; i < toIndex; ++i) {
      action.accept(array[i]);
    }
  }

  /**
   * Returns a list iterator starting at the given position, backed directly by the array.
   * No per-element bounds checking.
   * <p>
   * <strong>Time:</strong> O(1) to create; O(1) per advance.
   *
   * @param index starting position in {@code 0..size()}
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public IntListIterator listIterator(int index) {
    Objects.checkIndex(index, size() + 1);
    return new IntListIterator() {
      int pos = fromIndex + index;

      @Override public boolean hasNext() { return pos < toIndex; }
      @Override public int nextInt() {
        if (pos >= toIndex) throw new NoSuchElementException();
        return array[pos++];
      }
      @Override public boolean hasPrevious() { return pos > fromIndex; }
      @Override public int previousInt() {
        if (pos <= fromIndex) throw new NoSuchElementException();
        return array[--pos];
      }
      @Override public int nextIndex() { return pos - fromIndex; }
      @Override public int previousIndex() { return pos - fromIndex - 1; }
    };
  }

  /**
   * Returns a spliterator over this view's elements with ORDERED, SIZED, SUBSIZED,
   * DISTINCT, IMMUTABLE and NONNULL characteristics. Supports efficient parallel splitting.
   * <p>
   * The elements are in fact sorted ascending, but the {@link Spliterator#SORTED} flag is
   * intentionally not advertised because fastutil's {@code IntSpliterator.getComparator()}
   * default throws {@link IllegalStateException}, which would break {@link java.util.stream.IntStream}
   * construction via {@link java.util.stream.StreamSupport#intStream}.
   * <p>
   * <strong>Time:</strong> O(1) to create.
   */
  @Override
  public IntSpliterator spliterator() {
    return IntSpliterators.wrap(array, fromIndex, toIndex - fromIndex,
        Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED
            | Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.NONNULL);
  }

  /**
   * Returns a zero-copy sub-view covering {@code [fromIndex, toIndex)} within this view.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param fromIndex start index within this view (inclusive)
   * @param toIndex   end index within this view (exclusive)
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  @Override
  public SortedIntView subList(int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, size());
    if (fromIndex == 0 && toIndex == size()) return this;
    if (fromIndex == toIndex) return EMPTY;
    return new SortedIntView(array, this.fromIndex + fromIndex, this.fromIndex + toIndex);
  }

  /**
   * Creates a zero-copy view over {@code array[fromIndex..toIndex)}.
   * Returns {@link #EMPTY} if the range is empty.
   * The caller must ensure the range is sorted ascending with no duplicates.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param array     the backing array
   * @param fromIndex start index (inclusive)
   * @param toIndex   end index (exclusive)
   * @return a view over the range
   * @throws IndexOutOfBoundsException if the range is invalid
   */
  public static SortedIntView viewOf(int[] array, int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, array.length);
    if (fromIndex == toIndex) return EMPTY;
    return new SortedIntView(array, fromIndex, toIndex);
  }

  /**
   * Tests equality with another {@code SortedIntView} via direct array comparison.
   * <p>
   * <strong>Time:</strong> O(n).
   *
   * @param other view to compare against, may be {@code null}
   * @return {@code true} if both views have the same size and elements
   */
  public boolean equals(@Nullable SortedIntView other) {
    if (other == this) return true;
    if (other == null) return false;
    if (size() != other.size()) return false;
    return Arrays.equals(array, fromIndex, toIndex, other.array, other.fromIndex, other.toIndex);
  }

  /**
   * Tests equality with an arbitrary {@link IntList}, element by element.
   * <p>
   * <strong>Time:</strong> O(n).
   *
   * @param other list to compare against, may be {@code null}
   * @return {@code true} if both have the same size and elements in order
   */
  public boolean equals(@Nullable IntList other) {
    if (other == this) return true;
    if (other == null) return false;
    var size = size();
    if (size != other.size()) return false;
    for (int i = 0; i < size; i++) {
      if (array[fromIndex + i] != other.getInt(i)) return false;
    }
    return true;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return switch (other) {
      case null -> false;
      case SortedIntView view -> equals(view);
      case IntList intList -> equals(intList);
      default -> super.equals(other);
    };
  }

  /**
   * Computes the hash code using direct array access, consistent with the {@link IntList} contract.
   * <p>
   * <strong>Time:</strong> O(n).
   */
  @Override
  public int hashCode() {
    int h = 1;
    for (int i = fromIndex; i < toIndex; i++) {
      h = 31 * h + array[i];
    }
    return h;
  }

  /**
   * Returns the sorted, duplicate-free union of this view and {@code other}.
   * <p>
   * <strong>Time:</strong> O(n + m).<br>
   * <strong>Memory:</strong> up to 4 x (n + m) bytes.
   *
   * @param other the other sorted view
   * @return a new view containing every distinct value from both inputs in sorted order
   */
  public SortedIntView union(SortedIntView other) {
    if (other.isEmpty()) {
      return this;
    }
    if (this.isEmpty()) {
      return other;
    }
    var out = union(this.array, fromIndex, toIndex, other.array, other.fromIndex, other.toIndex);
    return new SortedIntView(out, 0, out.length);
  }

  /**
   * Computes the sorted union of two sorted, duplicate-free ranges.
   * Each distinct value appears exactly once in the result.
   * <p>
   * <strong>Time:</strong> O(n + m) where n = aTo - aFrom, m = bTo - bFrom.<br>
   * <strong>Memory:</strong> up to 4 x (n + m) bytes.
   */
  static int[] union(int[] a, int aFrom, int aTo, int[] b, int bFrom, int bTo) {
    var out = new int[aTo - aFrom + bTo - bFrom];
    var outPos = 0;

    var aPos = aFrom;
    var bPos = bFrom;

    while (aPos < aTo && bPos < bTo) {
      int aValue  = a[aPos];
      int bValue = b[bPos];

      int outValue;
      if (aValue < bValue) {
        outValue = aValue;
        aPos++;
      } else if (bValue < aValue) {
        outValue = bValue;
        bPos++;
      } else {
        // If value is contained in both `a` and `b`, keep it only once in the output set
        outValue = aValue;
        aPos++;
        bPos++;
      }
      out[outPos++] = outValue;
    }

    if (aPos < aTo) {
      outPos += copyTail(out, outPos, a, aPos, aTo);
    } else if (bPos < bTo) {
      outPos += copyTail(out, outPos, b, bPos, bTo);
    }

    // Trim output to actual size in case `a` and `b` contain shared values.
    return outPos == out.length? out : Arrays.copyOf(out, outPos);
  }

  private static int copyTail(int[] dest, int destFrom, int[] src, int srcFrom, int srcTo) {
    var length = srcTo - srcFrom;
    System.arraycopy(src, srcFrom, dest, destFrom, length);
    return length;
  }
}
