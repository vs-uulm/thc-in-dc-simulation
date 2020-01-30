package de.uulm.vs.dcn;

import static de.uulm.vs.dcn.Cryptographer.CHARSET;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
public class DemoTest {

  @Test
  public void testDcn3() throws InterruptedException {
    var msg = "Hello";
    var barrier = new CyclicBarrier(3);
    var sem = new Semaphore(3);

    var c1 =
        new Cryptographer(1, new DefaultSplitCombineStrategy(3, 1), barrier,
            sem);
    var c2 =
        new Cryptographer(2, new DefaultSplitCombineStrategy(3, 1), barrier,
            sem);
    var c3 =
        new Cryptographer(3, new DefaultSplitCombineStrategy(3, 1), barrier,
            sem);

    DCNetwork network = new DCNetwork(1);
    network.addCryptographer(c1);
    network.addCryptographer(c2);
    network.addCryptographer(c3);

    var results = new StringBuilder[3];
    for (int i = 0; i < 3; i++) {
      results[i] = new StringBuilder();
    }

    c1.setOnMessagePartReceived(
        m -> results[0].append(new String(Util.strip(m), CHARSET)));
    c2.setOnMessagePartReceived(
        m -> results[1].append(new String(Util.strip(m), CHARSET)));
    c3.setOnMessagePartReceived(
        m -> results[2].append(new String(Util.strip(m), CHARSET)));

    network.start(sem);

    c1.enqueueMessage(msg);

    Thread.sleep(1000);

    network.stop();

    assertEquals(msg, results[0].toString());
    assertEquals(msg, results[1].toString());
    assertEquals(msg, results[2].toString());
  }

  @Test
  public void testDcn4() throws InterruptedException {
    runNetwork(4, "Hello");
  }

  @Test
  public void testDcn5() throws InterruptedException {
    runNetwork(5, "Hello");
  }

  @Test
  public void testDcn100() throws InterruptedException {
    runNetwork(100, "Hello");
  }

  @Test
  public void testDcnLongMessage() throws IOException, InterruptedException {
    var message =
        Files.readString(new File("src/test/long_message.txt").toPath(),
            CHARSET);
    runNetwork(3, message);
  }

  @Test
  public void testDcnVeryLongMessage()
      throws IOException, InterruptedException {
    var message =
        Files.readString(new File("src/test/bible.txt").toPath(), CHARSET);
    runNetwork(3, message);
  }

  public static void runNetwork(int n, String message)
      throws InterruptedException {
    var network = new DCNetwork(1);
    var barrier = new CyclicBarrier(n);
    var sem = new Semaphore(n);
    var stringBuilders =
        Collections
            .synchronizedMap(new HashMap<Cryptographer, StringBuilder>(n));
    Cryptographer sender = null;

    for (int i = 0; i < n; i++) {
      var grapher =
          new Cryptographer(i + 1, new DefaultSplitCombineStrategy(n, 1),
              barrier, sem);
      if (sender == null) {
        sender = grapher;
      }
      network.addCryptographer(grapher);
      stringBuilders.put(grapher, new StringBuilder());
      grapher.setOnMessagePartReceived(msg -> {
        var sb = stringBuilders.get(grapher);
        sb.append(new String(Util.stripR(msg), CHARSET));
      });
    }
    network.start(sem);

    sender.enqueueMessage(message);

    do {
      Thread.sleep(100);
    } while (sender.pendingToSend());

    network.stop();

    stringBuilders.forEach(
        (cr, strBuilder) -> { assertEquals(message, strBuilder.toString()); });

  }

}
