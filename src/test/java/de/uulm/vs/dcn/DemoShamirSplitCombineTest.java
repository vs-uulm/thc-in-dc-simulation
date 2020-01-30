package de.uulm.vs.dcn;

import static de.uulm.vs.dcn.Cryptographer.CHARSET;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import org.junit.Test;

/**
 * 
 * @author Juri Dispan
 *
 */
public class DemoShamirSplitCombineTest {

  @Test
  public void testDcnShamir33() throws InterruptedException {
    runNetwork(3, 3, "Hello");
  }

  @Test
  public void testDcnShamir32() throws InterruptedException {
    runNetwork(3, 2, "Hello");
  }

  @Test
  public void testDcnShamir44() throws InterruptedException {
    runNetwork(4, 4, "Hello");
  }

  @Test
  public void testDcnShamir42() throws InterruptedException {
    runNetwork(4, 2, "Hello");
  }

  @Test
  public void testDcnShamir103() throws InterruptedException {
    runNetwork(10, 3, "Hello");
  }

  @Test
  public void testDcnLongMessage() throws IOException, InterruptedException {
    var message =
        Files.readString(new File("src/test/long_message.txt").toPath(),
            CHARSET);
    runNetwork(3, 2, message);
  }

  public static void runNetwork(int n, int k, String message)
      throws InterruptedException {
    // System.out.println(n + " " + k + " " + message);
    var network = new DCNetwork(k);
    var barrier = new CyclicBarrier(n);
    var sem = new Semaphore(n);
    var strat = new ShamirSplitCombine(n, k);
    var rcvs =
        Collections.synchronizedMap(new HashMap<Cryptographer, byte[]>());
    var members = new ArrayList<Cryptographer>(n);

    for (int i = 0; i < n; i++) {
      var grapher = new Cryptographer(i + 1, strat, barrier, sem);
      network.addCryptographer(grapher);
      grapher.setOnMessagePartReceived(
          msg -> rcvs.merge(grapher, msg, Util::concat));
      members.add(grapher);
    }
    network.start(sem);

    members.get(0).enqueueMessage(message);

    while (members.get(0).pendingToSend()) {
      Thread.sleep(100);
    }

    network.stop();

    members.forEach(member -> {
      var rec = new String(Util.strip(rcvs.get(member)), CHARSET);
      assertEquals(message, rec);
    });

  }

}
