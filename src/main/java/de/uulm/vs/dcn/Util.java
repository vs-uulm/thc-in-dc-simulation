package de.uulm.vs.dcn;

import java.util.ArrayList;
import java.util.List;

/**
 * Various static utility functions.
 * 
 * @author Juri Dispan
 *
 */
public class Util {

  /**
   * Returns a copy of the specified array with tailing 0-valued bytes removed.
   * 
   * @param array The array to strip of tailing 0-bytes.
   * @return A copy of the specified array with tailing 0-valued bytes removed.
   */
  public static byte[] stripR(byte[] array) {
    var endIndex = getEndIndex(array);
    var newA = new byte[endIndex];
    System.arraycopy(array, 0, newA, 0, endIndex);
    return newA;
  }

  public static byte[] strip(byte[] array) {
    var endIndex = getEndIndex(array);
    var startIndex = getStartIndex(array);
    if (endIndex <= startIndex) {
      return new byte[0];
    }
    var newA = new byte[endIndex - startIndex];
    System.arraycopy(array, startIndex, newA, 0, endIndex - startIndex);
    return newA;
  }

  private static int getStartIndex(byte[] array) {
    var nullindex = array.length;
    for (var i = 0; i < array.length; i++) {
      if (array[i] != 0) {
        nullindex = i;
        break;
      }
    }
    return nullindex;
  }

  private static int getEndIndex(byte[] array) {
    var nullindex = 0;
    for (var i = array.length - 1; i >= 0; i--) {
      if (array[i] != 0) {
        nullindex = i + 1;
        break;
      }
    }
    return nullindex;
  }

  /**
   * Concatenates two arrays.
   * 
   * @param a The first array.
   * @param b The second array.
   * @return The concatenation of both arrays, with the contents of a coming
   *         before the contents of b.
   */
  public static byte[] concat(byte[] a, byte[] b) {
    var con = new byte[a.length + b.length];
    System.arraycopy(a, 0, con, 0, a.length);
    System.arraycopy(b, 0, con, a.length, b.length);
    return con;
  }

  public static List<Integer> testPts(int min, int max, int step) {
    List<Integer> pts = new ArrayList<>();
    for (int i = min; i <= max; i += step) {
      pts.add(i);
    }
    return pts;
  }
}
