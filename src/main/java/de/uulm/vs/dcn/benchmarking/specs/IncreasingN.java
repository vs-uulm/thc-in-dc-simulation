package de.uulm.vs.dcn.benchmarking.specs;

import static de.uulm.vs.dcn.Cryptographer.BYTES_PER_ROUND;
import static de.uulm.vs.dcn.Cryptographer.DELAY;

import de.uulm.vs.dcn.Util;

/**
 * 
 * @author Juri Dispan
 *
 */
public class IncreasingN extends BenchmarkSpec {
  private int minN;
  private int maxN;
  private int stepN;
  private int k;

  public IncreasingN(int minN, int maxN, int stepN, int k) {
    super();
    this.minN = minN;
    this.maxN = maxN;
    this.stepN = stepN;
    this.k = k;
  }

  @Override
  public BMResult[] execute() {
    System.out.printf(
        "Running: IncN, k=%d, bpr=%d, nmin=%d, nmax=%d, nstep=%d, msgLen=%d, delay=%d\n",
        k, BYTES_PER_ROUND, minN, maxN, stepN, len, DELAY);
    var points = Util.testPts(minN, maxN, stepN);
    var vals = new BMResult[points.size()];
    var i = 0;
    for (var n : points) {
      var rs = testWith(n, k, len, 10);
      vals[i] = new BMResult(n, rs[0], rs[1]);
      i++;
    }
    return vals;
  }

}
