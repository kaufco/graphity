package com.kobayami.graphity;

import it.unimi.dsi.fastutil.ints.AbstractIntList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntSpliterator;
import it.unimi.dsi.fastutil.ints.IntSpliterators;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import org.jspecify.annotations.Nullable;

/**
 * A list of {@code int} values in ascending order and without duplicates, as a view of a range
 * of an array.
 * <p>
 * Adjacency lookups such as {@link Graph#outNodes(int)} return it, as do other methods of this
 * library that return nodes or edge indices in ascending order, such as
 * {@link Graphs#reachableFrom(Graph, int)}. It is an {@link IntList} of the fastutil library, so
 * it reads, iterates and streams like any list, without boxing; methods that would change it
 * throw {@link UnsupportedOperationException}. Since its values are sorted and distinct,
 * {@link #indexOf(int)} and {@code contains(int)} search in O(log size()), and {@link #first()}
 * and {@link #last()} are the smallest and the largest value.
 * <p>
 * A view shares its array with the object it comes from instead of copying it; a method that
 * returns a copy, such as {@link #union(SortedIntView)}, says so. Instances are immutable and
 * safe to share between threads, as the arrays behind them never change. A view that
 * {@link #viewOf(int[], int, int)} makes of an array of the caller stays sorted and unchanged only
 * as long as nobody modifies that range of the array.
 * <p>
 * <strong>Memory:</strong> constant; the values stay in the shared array.
 */
public final class SortedIntView extends AbstractIntList implements RandomAccess {

  /** An empty view. */
  public static final SortedIntView EMPTY = new SortedIntView(new int[0], 0, 0);

  // The view is array[fromIndex..toIndex - 1]. The array may be shared with a graph and other
  // views, and is never written.
  private final int[] array;
  private final int fromIndex;
  private final int toIndex;

  // Both take the array as it is, without a copy, and check only the bounds of the range: the
  // caller guarantees that the range is strictly ascending.
  SortedIntView(int[] array) {
    this(array, 0, array.length);
  }

  SortedIntView(int[] array, int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, array.length);
    this.array = array;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
  }

  /**
   * Returns the value at position {@code index}.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param index position in {@code 0..size()-1}
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public int getInt(int index) {
    Objects.checkIndex(index, size());
    return array[fromIndex + index];
  }

  /**
   * Returns the number of values.
   * <p>
   * <strong>Time:</strong> O(1).
   */
  @Override
  public int size() {
    return toIndex - fromIndex;
  }

  /**
   * Returns the first value, which is the smallest.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return smallest value
   * @throws NoSuchElementException if the view is empty
   */
  public int first() {
    if (fromIndex == toIndex) throw new NoSuchElementException();
    return array[fromIndex];
  }

  /**
   * Returns the last value, which is the largest.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @return largest value
   * @throws NoSuchElementException if the view is empty
   */
  public int last() {
    if (fromIndex == toIndex) throw new NoSuchElementException();
    return array[toIndex - 1];
  }

  /**
   * Returns the position of {@code value}, or {@code -1} if the view does not contain it.
   * <p>
   * <strong>Time:</strong> O(log size()).
   *
   * @param value the value to search for
   * @return position in {@code 0..size()-1}, or {@code -1}
   */
  @Override
  public int indexOf(int value) {
    int index = Arrays.binarySearch(array, fromIndex, toIndex, value);
    return index >= 0 ? index - fromIndex : -1;
  }

  /**
   * Returns the position of {@code value}, or {@code -1} if the view does not contain it.
   * Equivalent to {@link #indexOf(int)}, since no value occurs twice.
   * <p>
   * <strong>Time:</strong> O(log size()).
   */
  @Override
  public int lastIndexOf(int value) {
    return indexOf(value);
  }

  // indexOf and contains on a range of an array, without a view object, for the adjacency lookups
  // of the graph classes. indexOfInRange returns the position in the array, not in the range: in
  // the out-adjacency of a graph, that is the edge index.
  static int indexOfInRange(int[] array, int fromIndex, int toIndex, int value) {
    int index = Arrays.binarySearch(array, fromIndex, toIndex, value);
    return index >= 0 ? index : -1;
  }

  static boolean containsInRange(int[] array, int fromIndex, int toIndex, int value) {
    return Arrays.binarySearch(array, fromIndex, toIndex, value) >= 0;
  }

  /**
   * Returns the values as a new array.
   * <p>
   * <strong>Time:</strong> O(size()).<br>
   * <strong>Memory:</strong> 4 x size() bytes for the result.
   */
  @Override
  public int[] toIntArray() {
    if (fromIndex == toIndex) return IntArrays.EMPTY_ARRAY;
    return Arrays.copyOfRange(array, fromIndex, toIndex);
  }

  /**
   * Returns the values in {@code dest} from position {@code 0} on, or in a new array if
   * {@code dest} is {@code null} or too short.
   * <p>
   * <strong>Time:</strong> O(size()).<br>
   * <strong>Memory:</strong> 4 x size() bytes if it returns a new array, otherwise none.
   *
   * @param dest the array to fill, or {@code null} for a new one
   * @return {@code dest}, or a new array
   */
  @Override
  public int[] toArray(int @Nullable [] dest) {
    int size = size();
    if (dest == null || dest.length < size) {
      return toIntArray();
    }
    System.arraycopy(this.array, fromIndex, dest, 0, size);
    return dest;
  }

  /**
   * Copies {@code length} values, from position {@code srcOffset} on, into {@code dest} at
   * {@code destOffset}.
   * <p>
   * <strong>Time:</strong> O(length).
   *
   * @throws IndexOutOfBoundsException if the range to copy is not within this view or not within
   *                                   {@code dest}
   */
  @Override
  public void getElements(int srcOffset, int[] dest, int destOffset, int length) {
    Objects.checkFromIndexSize(srcOffset, length, size());
    Objects.checkFromIndexSize(destOffset, length, dest.length);
    System.arraycopy(this.array, fromIndex + srcOffset, dest, destOffset, length);
  }

  /**
   * Calls {@code action} for every value, in ascending order.
   * <p>
   * <strong>Time:</strong> O(size()).
   */
  @Override
  public void forEach(IntConsumer action) {
    for (int i = fromIndex; i < toIndex; i++) {
      action.accept(array[i]);
    }
  }

  /**
   * Returns a list iterator over the values, starting at position {@code index}.
   * <p>
   * <strong>Time:</strong> O(1) to create the iterator.
   *
   * @param index position in {@code 0..size()}
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
   * Returns a spliterator over the values, which reports them as sorted, distinct and immutable.
   * <p>
   * <strong>Time:</strong> O(1) to create the spliterator.
   */
  @Override
  public IntSpliterator spliterator() {
    return IntSpliterators.wrapPreSorted(
        array, fromIndex, size(), Spliterator.DISTINCT | Spliterator.IMMUTABLE, null);
  }

  /**
   * Returns the values at the positions {@code fromIndex..toIndex-1} as a view of the same array,
   * not a copy.
   * <p>
   * <strong>Time:</strong> O(1).
   *
   * @param fromIndex first position
   * @param toIndex   end position, exclusive
   * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater
   *                                   than {@code size()}, or {@code fromIndex > toIndex}
   */
  @Override
  public SortedIntView subList(int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, size());
    if (fromIndex == 0 && toIndex == size()) return this;
    if (fromIndex == toIndex) return EMPTY;
    return new SortedIntView(array, this.fromIndex + fromIndex, this.fromIndex + toIndex);
  }

  /**
   * Returns a view of {@code array[fromIndex..toIndex-1]}, which must be strictly ascending.
   * <p>
   * The range is checked, not copied: the view shares {@code array}, and stays sorted and
   * unchanged only as long as nobody modifies that range of it.
   * <p>
   * <strong>Time:</strong> O(toIndex - fromIndex).
   *
   * @param array     the array to view
   * @param fromIndex first position of the range
   * @param toIndex   end position of the range, exclusive
   * @return a view of the range
   * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater
   *                                   than {@code array.length}, or {@code fromIndex > toIndex}
   * @throws IllegalArgumentException  if the range is not strictly ascending
   */
  public static SortedIntView viewOf(int[] array, int fromIndex, int toIndex) {
    Objects.checkFromToIndex(fromIndex, toIndex, array.length);
    if (fromIndex == toIndex) return EMPTY;
    for (int i = fromIndex + 1; i < toIndex; i++) {
      if (array[i] <= array[i - 1]) {
        throw new IllegalArgumentException("array[" + fromIndex + ".." + toIndex
            + ") must be strictly ascending, but " + array[i] + " follows " + array[i - 1]
            + " at index " + i);
      }
    }
    return new SortedIntView(array, fromIndex, toIndex);
  }

  /**
   * Returns whether {@code other} is a list with the same values in the same order, as the
   * {@link java.util.List} contract defines it.
   * <p>
   * <strong>Time:</strong> O(size()).
   */
  @Override
  public boolean equals(@Nullable Object other) {
    if (other == this) return true;
    if (other instanceof SortedIntView view) return equalsView(view);
    if (other instanceof IntList list) return equalsIntList(list);
    return super.equals(other);
  }

  private boolean equalsView(SortedIntView other) {
    return Arrays.equals(array, fromIndex, toIndex, other.array, other.fromIndex, other.toIndex);
  }

  private boolean equalsIntList(IntList other) {
    int size = size();
    if (size != other.size()) return false;
    for (int i = 0; i < size; i++) {
      if (array[fromIndex + i] != other.getInt(i)) return false;
    }
    return true;
  }

  /**
   * Returns a hash code over the values, as the {@link java.util.List} contract defines it.
   * <p>
   * <strong>Time:</strong> O(size()).
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
   * Returns the values of this view and {@code other} together, in ascending order and without
   * duplicates.
   * <p>
   * The result is a copy, merged on every call, unless one of the two views is empty: then it is
   * the other one.
   * <p>
   * <strong>Time:</strong> O(size() + other.size()).<br>
   * <strong>Memory:</strong> up to 4 x (size() + other.size()) bytes for the result; while it
   * runs, up to as much again if the two views share values.
   *
   * @param other the view to unite with this one
   * @return the union
   * @throws IllegalArgumentException if the union has more than {@code Integer.MAX_VALUE - 8}
   *                                  values, the most an array can hold
   */
  public SortedIntView union(SortedIntView other) {
    if (other.isEmpty()) {
      return this;
    }
    if (this.isEmpty()) {
      return other;
    }
    return new SortedIntView(
        unionInRange(array, fromIndex, toIndex, other.array, other.fromIndex, other.toIndex));
  }

  // The merge behind union and BiAdjacentGraph.neighbors, on ranges of arrays, so that neither
  // needs view objects. Throws IllegalArgumentException if the union does not fit into an array.
  static int[] unionInRange(int[] a, int aFrom, int aTo, int[] b, int bFrom, int bTo) {
    var out = new int[unionCapacity(a, aFrom, aTo, b, bFrom, bTo, ArrayLimits.MAX_ARRAY_LENGTH)];
    int outPos = 0;

    int aPos = aFrom;
    int bPos = bFrom;

    while (aPos < aTo && bPos < bTo) {
      int aValue = a[aPos];
      int bValue = b[bPos];

      int outValue;
      if (aValue < bValue) {
        outValue = aValue;
        aPos++;
      } else if (bValue < aValue) {
        outValue = bValue;
        bPos++;
      } else {
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
    return outPos == out.length ? out : Arrays.copyOf(out, outPos);
  }

  // The length to allocate for the union of two sorted, duplicate-free ranges: the sum of their
  // sizes, or, if that exceeds maxLength, the size of the union itself, counted by a merge that
  // writes nothing - so that two large ranges with many common values still fit. maxLength is
  // the largest array length; tests pass a small one. Throws IllegalArgumentException if the
  // union has more than maxLength values.
  static int unionCapacity(
      int[] a, int aFrom, int aTo, int[] b, int bFrom, int bTo, int maxLength) {
    long sizeSum = (long) (aTo - aFrom) + (bTo - bFrom);
    if (sizeSum <= maxLength) return (int) sizeSum;
    int aPos = aFrom;
    int bPos = bFrom;
    long unionSize = 0;
    while (aPos < aTo && bPos < bTo) {
      int aValue = a[aPos];
      int bValue = b[bPos];
      if (aValue <= bValue) aPos++;
      if (bValue <= aValue) bPos++;
      unionSize++;
    }
    unionSize += (long) (aTo - aPos) + (bTo - bPos);
    if (unionSize > maxLength) {
      throw new IllegalArgumentException("the union has " + unionSize
          + " values, more than the largest array length of " + maxLength);
    }
    return (int) unionSize;
  }

  private static int copyTail(int[] dest, int destFrom, int[] src, int srcFrom, int srcTo) {
    int length = srcTo - srcFrom;
    System.arraycopy(src, srcFrom, dest, destFrom, length);
    return length;
  }
}
