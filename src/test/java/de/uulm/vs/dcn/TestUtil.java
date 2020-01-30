package de.uulm.vs.dcn;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * 
 * @author Juri Dispan
 *
 */
public class TestUtil {
  @Test
  public void testStripR1() {
    var arr = new byte[] { 1, 2, 3, 4, 5, 0, 0, 0, 0, 0 };
    var res = Util.stripR(arr);
    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, res);
  }

  @Test
  public void testStripR2() {
    var arr = new byte[] { 1, 2, 3, 4, 5 };
    var res = Util.stripR(arr);
    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, res);
  }

  @Test
  public void testStrip1() {
    var arr = new byte[] { 0, 0, 1, 2, 3, 4, 5, 0, 0, 0, 0, 0 };
    var res = Util.strip(arr);
    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, res);
  }

  @Test
  public void testStrip2() {
    var arr = new byte[] { 1, 2, 3, 4, 5 };
    var res = Util.strip(arr);
    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, res);
  }

  @Test
  public void testConcat1() {
    var a = new byte[] { 1, 2, 3, 4 };
    var b = new byte[] { 5, 6, 7, 8 };

    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, Util.concat(a, b));
  }

  @Test
  public void testConcat2() {
    var a = new byte[] { 1, 2, 3 };
    var b = new byte[] { 5, 6, 7, 8 };

    assertArrayEquals(new byte[] { 1, 2, 3, 5, 6, 7, 8 }, Util.concat(a, b));
  }

  @Test
  public void testConcat3() {
    var a = new byte[] { 1, 2, 3, 4 };
    var b = new byte[] { 5, 6, 7 };

    assertArrayEquals(new byte[] { 1, 2, 3, 4, 5, 6, 7 }, Util.concat(a, b));
  }
}
