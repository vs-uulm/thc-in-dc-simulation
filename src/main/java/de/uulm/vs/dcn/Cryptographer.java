package de.uulm.vs.dcn;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import de.uulm.vs.dcn.DCNetwork.State;

/**
 * 
 * @author Juri Dispan
 *
 */
public class Cryptographer implements Runnable {
  /**
   * Number of bytes transmitted each round
   */
  public static int BYTES_PER_ROUND = 32;
  public static final Charset CHARSET = Charset.forName("US-ASCII");

  private static final Consumer<byte[]> DEFAULT_MSG_PART_HANDLER = __ -> {};
  private static final Logger LOGGER =
      Logger.getLogger(Cryptographer.class.getName());

  static {
    LOGGER.setLevel(Level.OFF);
    var handler = new ConsoleHandler();
    handler.setFormatter(new SimpleFormatter());
    handler.setLevel(Level.OFF);
    LOGGER.addHandler(handler);
  }

  /**
   * The number of this instance.
   */
  private final int number;

  /**
   * The RNGs shared with each other cryptographer, in order to generate shared
   * secrets without having to communicate.
   */
  private final Map<Cryptographer, Random> generators = new HashMap<>();

  /**
   * The messages others have sent to us in the current round. Must be reset
   * after each round.
   */
  private final Map<Cryptographer, byte[]> currentMessages = new HashMap<>();

  /**
   * The next bytes that we want to send.
   */
  private final Queue<Byte> queuedMessages = new ConcurrentLinkedQueue<>();

  /**
   * The strategy we use to split messages and combine them again. This enables
   * threshold cyrptography.
   */
  private final SplitCombineStrategy splitCombineStrategy;

  /**
   * Used to synchronise the cryptographers after each phase of the protocol.
   */
  private final CyclicBarrier barrier;

  /**
   * Semaphore used to synchronise access to the critical part of the
   * application, where {@link Cryptographer#nwState} is read. This is because
   * the network must not change the state of the network, when some
   * cryptographers have already read it and some don't.
   */
  private final Semaphore stateTestSem;

  public static int DELAY = 0;

  private final Collection<Cryptographer> msgSharingPartners = new HashSet<>();
  private final byte[] lastMessagePart = new byte[BYTES_PER_ROUND];

  /**
   * Gets called after each round, when we received
   * {@link Cryptographer#BYTES_PER_ROUND} new bytes.
   */
  private Consumer<byte[]> onMessagePartReceived = DEFAULT_MSG_PART_HANDLER;

  /**
   * The state of the network. This must be managed by a {@link DCNetwork}. When
   * the state becomes {@link State#TERMINATED} we jump out of
   * {@link Cryptographer#run()} and terminate.
   */
  private AtomicReference<State> nwState;

  public Cryptographer(int number, SplitCombineStrategy splitCombineStrategy,
      CyclicBarrier barrier, Semaphore stateTestSem) {
    requireNonNull(splitCombineStrategy, "splitCombineStrategy can't be null");
    requireNonNull(barrier, "barrier can't be null");
    requireNonNull(stateTestSem, "stateTestSem can't be null");

    this.number = number;
    this.splitCombineStrategy = splitCombineStrategy;
    this.barrier = barrier;
    this.stateTestSem = stateTestSem;

    this.currentMessages.put(this, new byte[BYTES_PER_ROUND]);
    this.msgSharingPartners.add(this);
  }

  void setNwState(AtomicReference<State> nwState) {
    this.nwState = nwState;
  }

  public int getNumber() {
    return number;
  }

  public boolean pendingToSend() {
    return !queuedMessages.isEmpty();
  }

  public void setOnMessagePartReceived(Consumer<byte[]> consumer) {
    requireNonNull(consumer, "onMessagePartReceived can't be null");
    this.onMessagePartReceived = consumer;
  }

  public void executeProtocolRound1() throws InterruptedException {
    // we acquire the next message to send from queuedMessages.
    // if there are no or less than BYTES_PER_ROUND bytes to send,
    // we leave the remaining bytes as 0s.
    final var originalMessage = new byte[BYTES_PER_ROUND];

    for (var i = 0; i < BYTES_PER_ROUND && pendingToSend(); i++) {
      originalMessage[i] = queuedMessages.poll();
    }

    // We split the message, this enables threshold cryptography.
    final var msgParts = splitCombineStrategy.split(originalMessage);

    // we move the message parts from msgParts into toSend for easier handling
    final var toSend = new HashMap<Cryptographer, byte[]>();
    toSend.put(this, msgParts.get(number - 1).getContent());
    generators.keySet().forEach(
        rec -> toSend.put(rec, msgParts.get(rec.getNumber() - 1).getContent()));

    // we acquire the next shared secret with each of the other cryptographers
    // from the RNG and save it into sharedSecrets.
    final var sharedSecrets = new HashMap<Cryptographer, byte[]>();
    for (var entry : generators.entrySet()) {
      var gen = entry.getValue();
      var cryptographer = entry.getKey();
      var sharedBytes = new byte[BYTES_PER_ROUND];

      gen.nextBytes(sharedBytes);
      sharedSecrets.put(cryptographer, sharedBytes);
    }

    // we xor all the secrets with the message we intend to send.
    // because we send n different messages, we have to to this for each
    // recipient.
    sharedSecrets.values().forEach(secret -> {
      toSend.values().forEach(msg -> {
        for (var i = 0; i < BYTES_PER_ROUND; i++) {
          msg[i] ^= secret[i];
        }
      });
    });

    // Announce the correct message to each cryptographer,
    // including one's self
    addMessagePart(this, toSend.get(this));
    generators.keySet().forEach(cr -> cr.addMessagePart(this, toSend.get(cr)));
    // generators.keySet().forEach(cr -> System.out
    // .println(number + " -> " + cr.number + ": " +
    // Arrays.toString(toSend.get(cr))));

    // sync point
    try {
      barrier.await();
    } catch (BrokenBarrierException e) {
      LOGGER.severe(e.getMessage());
      e.printStackTrace();
    }
  }

  public void executeProtocolRound2()
      throws InterruptedException, BrokenBarrierException {
    for (int i = 0; i < lastMessagePart.length; i++) {
      lastMessagePart[i] = 0;
    }

    // Artificial delay to simulate networking
    try {
      Thread.sleep(DELAY);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    currentMessages.values().forEach(msg -> {
      for (var i = 0; i < BYTES_PER_ROUND; i++) {
        lastMessagePart[i] ^= msg[i];
        msg[i] = 0;
      }
    });

    LOGGER.fine(number + ": Received message part");
    LOGGER.finer(() -> Arrays.toString(lastMessagePart));
    barrier.await();
    reassembleMessage();
  }

  /**
   * Queue a message to send. Message transmission begins as soon as previously
   * enqueued messages have been sent.
   * 
   * @param msg The message to send.
   */
  public void enqueueMessage(String msg) {
    enqueueMessage(msg.getBytes(CHARSET));
  }

  /**
   * Queue a message to send. Message transmission begins as soon as previously
   * enqueued messages have been sent.
   * 
   * @param msg The message to send.
   */
  public void enqueueMessage(byte[] msg) {
    for (var b : msg) {
      queuedMessages.add(b);
    }
    LOGGER.info("Queued message to send with length " + msg.length + " bytes.");
    LOGGER.finest(() -> new String(msg));

  }

  /**
   * Join message parts according to this cryptographer's
   * {@link SplitCombineStrategy}.
   * 
   * @param msgs The message parts to join.
   * @return The joined message.
   */
  public byte[] joinMessage(List<MessagePart> msgs) {
    return splitCombineStrategy.combine(msgs);
  }

  @Override
  public void run() {
    LOGGER.fine(number + ": run() called");
    while (true) {
      try {
        LOGGER.fine(number + ": Starting round");
        executeProtocolRound1();
        LOGGER.fine(number + ": Finished phase 1");
        executeProtocolRound2();
        LOGGER.fine(number + ": Finished phase 2");

        stateTestSem.acquire();
        if (this.nwState.get() == State.TERMINATED) {
          stateTestSem.release();
          break;
        }
        barrier.await();
        stateTestSem.release();

      } catch (InterruptedException e) {
        LOGGER.severe("Interrupted while amidst the protocol");
      } catch (BrokenBarrierException e) {
        e.printStackTrace();
      }
    }
  }

  private void reassembleMessage() {
    var msgsParts =
        msgSharingPartners.stream().map(Cryptographer::getCurrentPart)
            .collect(Collectors.toList());

    var msg = joinMessage(msgsParts);
    if (number == 1) {
      // System.out.println(msgsParts);
    }
    onMessagePartReceived.accept(msg);
  }

  private MessagePart getCurrentPart() {
    return new MessagePart(number, lastMessagePart.clone());
  }

  private void addMessagePart(Cryptographer partner, byte[] part) {
    // LOGGER.fine(name + ": received message part");
    if (!Arrays.equals(part, new byte[BYTES_PER_ROUND])) {
      LOGGER.finer(number + ": " + Arrays.toString(part));
    }

    assert part.length == BYTES_PER_ROUND;
    // synchronized (this.currentMessage) {
    var memToWrite = currentMessages.get(partner);
    for (var i = 0; i < BYTES_PER_ROUND; i++) {
      memToWrite[i] ^= part[i];
    }
    // }
  }

  public void addSharingPartner(Cryptographer partner) {
    requireNonNull(partner, "Partner can't be null");
    if (this.msgSharingPartners.contains(partner)) {
      throw new IllegalArgumentException("Partner already set.");
    }
    this.msgSharingPartners.add(partner);
  }

  private void addGenerator(Cryptographer partner, Random rand) {
    requireNonNull(partner, "Partner can't be null");
    requireNonNull(rand, "Random can't be null");
    assert partner != this;

    generators.put(partner, rand);
    currentMessages.put(partner, new byte[BYTES_PER_ROUND]);
  }

  private long randomLong() {
    return new Random().nextLong();
  }

  /**
   * Causes the two specified cryptographers to establish a random but common
   * seed for a pseudo random number generator, which can be used to generate
   * shared secrets on the fly.
   */
  static void agreeOnSecret(Cryptographer c1, Cryptographer c2) {
    var c1rand = c1.randomLong();
    var c2rand = c2.randomLong();

    var sharedGen1 = new Random(c1rand * c2rand);
    var sharedGen2 = new Random(c1rand * c2rand);

    c1.addGenerator(c2, sharedGen1);
    c2.addGenerator(c1, sharedGen2);

  }

  @Override
  public int hashCode() {
    return number;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

}
