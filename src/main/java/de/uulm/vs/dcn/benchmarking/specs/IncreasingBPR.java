package de.uulm.vs.dcn.benchmarking.specs;

import static de.uulm.vs.dcn.Cryptographer.DELAY;

import java.util.ArrayList;
import java.util.List;

import de.uulm.vs.dcn.Cryptographer;

/**
 * 
 * @author Juri Dispan
 *
 */
public class IncreasingBPR extends BenchmarkSpec {
  private int n;
  private int k;
  private int minBPR;
  private int maxBPR;

  public IncreasingBPR(int n, int k, int minBPR, int maxBPR) {
    super();
    this.n = n;
    this.k = k;
    this.minBPR = minBPR;
    this.maxBPR = maxBPR;
  }

  @Override
  public BMResult[] execute() {
    System.out.printf(
        "Running: IncBPR, n=%d, k=%d, bprmin=%d, bprmax=%d, msgLen=%d, delay=%d\n",
        n, k, minBPR, maxBPR, len, DELAY);

    var points = testPts();
    var vals = new BMResult[points.size()];
    var i = 0;
    for (var bpr : points) {
      Cryptographer.BYTES_PER_ROUND = bpr;
      var rs = testWith(n, k, len, 10);
      vals[i] = new BMResult(bpr, rs[0], rs[1]);
      i++;
      // System.out.println(bpr + " " + rs[0] + " " + rs[1]);
    }
    return vals;
  }

  private List<Integer> testPts() {
    var res = new ArrayList<Integer>();
    for (int i = minBPR; i <= maxBPR; i <<= 1) {
      res.add(i);
    }
    return res;
  }

}
