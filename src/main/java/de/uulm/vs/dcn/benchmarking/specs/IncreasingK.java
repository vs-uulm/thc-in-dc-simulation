package de.uulm.vs.dcn.benchmarking.specs;

import static de.uulm.vs.dcn.Cryptographer.BYTES_PER_ROUND;
import static de.uulm.vs.dcn.Cryptographer.DELAY;

import de.uulm.vs.dcn.Util;

/**
 * 
 * @author Juri Dispan
 *
 */
public class IncreasingK extends BenchmarkSpec {
  private final int n;
  private int minK;
  private int maxK;
  private int stepK;

  public IncreasingK(int n, int minK, int maxK, int stepK) {
    this.n = n;
    this.minK = minK;
    this.maxK = maxK;
    this.stepK = stepK;
  }

  @Override
  public BMResult[] execute() {
    System.out.printf(
        "Running: IncK, n=%d, bpr=%d, kmin=%d, kmax=%d, kstep=%d, msgLen=%d, delay=%d\n",
        n, BYTES_PER_ROUND, minK, maxK, stepK, len, DELAY);
    var points = Util.testPts(minK, maxK, stepK);
    var vals = new BMResult[points.size()];
    var i = 0;
    for (var k : points) {
      var rs = testWith(n, k, len, 20);
      vals[i] = new BMResult(k, rs[0], rs[1]);
      i++;
    }
    return vals;
  }

}
