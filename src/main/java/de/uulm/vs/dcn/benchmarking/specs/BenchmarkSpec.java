package de.uulm.vs.dcn.benchmarking.specs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import de.uulm.vs.dcn.Cryptographer;
import de.uulm.vs.dcn.DCNetwork;
import de.uulm.vs.dcn.DefaultSplitCombineStrategy;
import de.uulm.vs.dcn.SplitCombineStrategy;
import de.uulm.vs.dcn.Util;

/**
 * 
 * @author Juri Dispan
 *
 */
public abstract class BenchmarkSpec {
  protected BiFunction<Integer, Integer, SplitCombineStrategy> scStategySupp;
  protected int len;
  private String outFolder;
  private String name;

  public abstract BMResult[] execute();

  public String getName() {
    return name;
  };

  public void setName(String name) {
    this.name = name;
  }

  public void setLen(int len) {
    this.len = len;
  }

  public void setStrategySupp(
      BiFunction<Integer, Integer, SplitCombineStrategy> scStrategySupp) {
    this.scStategySupp = scStrategySupp;
  }

  public void setOutFolder(String outFolder) {
    this.outFolder = outFolder;
  }

  public void runSpec() {
    // warmup
    for (var i = 0; i < 50; i++) {
      testWith(3, 3, 10, 100);
    }

    var data = execute();
    var outFile = new File(outFolder + '/' + getName() + ".csv");

    var resultStr =
        "x,y\n" + Arrays.stream(data)
            .map(bmr -> bmr.x + "," + bmr.y + "," + bmr.sigma + "\n")
            .reduce(String::concat).orElse("");

    try {
      Files.writeString(outFile.toPath(), resultStr);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  protected <T extends SplitCombineStrategy> double[] testWith(int n, int k,
      int msgLen, int reps) {

    var measurements = new HashSet<Long>(reps);
    var message = genMessage(msgLen);
    var strat = scStategySupp.apply(n, k);
    if (strat instanceof DefaultSplitCombineStrategy) {
      k = 1;
    }

    for (var i = 0; i < reps; i++) {
      measurements.add(runNetwork(n, k, message, strat));
    }

    return meanAndSigma(measurements);
  }

  private double[] meanAndSigma(Set<Long> vals) {
    var throughputs =
        vals.stream().mapToDouble(l -> (len / 1.024) / l).mapToObj(l -> l)
            .collect(Collectors.toSet());
    var mean = throughputs.stream().mapToDouble(l -> l).sum() / vals.size();

    var sigma =
        Math.sqrt(throughputs.stream().mapToDouble(l -> l)
            .map(x_i -> (x_i - mean) * (x_i - mean)).sum() / (vals.size() - 1));
    return new double[] { mean, sigma };
  }

  private String genMessage(int msgLen) {
    var strbd = new StringBuilder(msgLen);
    while (msgLen-- > 0) {
      strbd.append('x');
    }
    return strbd.toString();
  }

  private long runNetwork(int n, int k, String message,
      SplitCombineStrategy scStrategy) {
    var network = new DCNetwork(k);
    var barrier = new CyclicBarrier(n);
    var sem = new Semaphore(n);
    var byteBuffers =
        Collections.synchronizedMap(new HashMap<Cryptographer, byte[]>(n));
    Cryptographer sender = null;

    for (int i = 0; i < n; i++) {
      var grapher = new Cryptographer(i + 1, scStrategy, barrier, sem);
      if (sender == null) {
        sender = grapher;
      }
      network.addCryptographer(grapher);
      byteBuffers.put(grapher, new byte[0]);
      grapher.setOnMessagePartReceived(
          msg -> byteBuffers.merge(grapher, msg, Util::concat));
    }

    var startTime = System.currentTimeMillis();
    network.start(sem);

    sender.enqueueMessage(message);

    do {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } while (sender.pendingToSend());

    network.stop();
    var endTime = System.currentTimeMillis();
    return endTime - startTime;
  }

  static class BMResult {
    int x;
    double y;
    double sigma;

    BMResult(int x, double y, double sigma) {
      this.x = x;
      this.y = y;
      this.sigma = sigma;
    }
  }
}
