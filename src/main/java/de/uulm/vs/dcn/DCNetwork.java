package de.uulm.vs.dcn;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a Dining Cryptographer Network.
 * 
 * @author Juri Dispan
 */
public class DCNetwork {
  private static final Logger LOGGER =
      Logger.getLogger(DCNetwork.class.getName());

  static {
    LOGGER.setLevel(Level.OFF);
  }

  private final AtomicReference<State> state =
      new AtomicReference<>(State.CREATED);
  private final List<Cryptographer> cryptographers = new ArrayList<>();
  private final int k;
  private Set<Thread> threads;
  private Semaphore stateTestSem;

  /**
   * Constructs a DCN.
   * 
   * @param k The number of message parts needed to reconstruct a message. This
   *          is needed for assigning message part sharing partners.
   */
  public DCNetwork(int k) {
    this.k = k;
  }

  /**
   * Adds a cryptographer to this network. The new member will automatically
   * establish a seed for generating shared secret bits with every other member
   * of the network.
   * 
   * @param cryptographer The cryptographer to add.
   * @return True if the cryptographer was successfully added, false if the
   *         cryptographer was already a member of the network.
   */
  public boolean addCryptographer(Cryptographer cryptographer) {
    checkState(State.CREATED);
    if (this.cryptographers.contains(cryptographer)) {
      return false;
    }
    this.cryptographers.forEach(
        partner -> Cryptographer.agreeOnSecret(cryptographer, partner));
    this.cryptographers.add(cryptographer);
    cryptographer.setNwState(this.state);
    return true;
  }

  /**
   * Starts the DCN. Members will begin executing the DCN protocol concurrently
   * until {@link DCNetwork#stop} is called on this DCN instance. The DCN also
   * manages the assignment of an appropriate number of sharing partners for
   * each member. Sharing partners will share the message parts they received in
   * order to reconstruct the entire message.
   * 
   * @param stateTestSem The semaphore for synchronising with the members of the
   *                     network.
   * @throws IllegalStateException If {@link DCNetwork#start} has already been
   *                               called on this DCN.
   */
  public void start(Semaphore stateTestSem) {
    checkState(State.CREATED);
    this.stateTestSem = stateTestSem;
    this.state.set(State.RUNNING);
    for (int i = 0; i < cryptographers.size(); i++) {
      for (int j = 1; j < k; j++) {
        cryptographers.get(i).addSharingPartner(
            cryptographers.get((i + j) % cryptographers.size()));
      }
    }

    threads = cryptographers.stream().map(Thread::new).collect(toSet());
    threads.forEach(Thread::start);
    LOGGER.info("DCNetwork started");
  }

  /**
   * Stop this DCN. The network will finish the current round of the DCN
   * protocol and seize any transmission of data thereafter.
   * 
   * @throws IllegalStateException If the DCNetwork is not running.
   */
  public void stop() {
    checkState(State.RUNNING);

    try {
      stateTestSem.acquire(cryptographers.size());
      this.state.set(State.TERMINATED);
      stateTestSem.release(cryptographers.size());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    LOGGER.info("DCNetwork stopping...");

    while (threads.stream().anyMatch(Thread::isAlive)) {
    }

    LOGGER.info("DCNetwork stopped");
  }

  /**
   * Ensure that the DCNetwork is in the desired state, throw an
   * {@link IllegalStateException} otherwise.
   * 
   * @param desired The desired state.
   * @throws IllegalStateException If the DCNetwork is not in the desired state.
   */
  private void checkState(State desired) {
    var curState = state.get();

    if (curState == desired) {
      return;
    }

    switch (curState) {
    case CREATED:
      throw new IllegalStateException("DCN has not been started yet.");
    case RUNNING:
      throw new IllegalStateException("DCN is already running.");
    case TERMINATED:
      throw new IllegalStateException("DCN has already been terminated.");
    }
  }

  /**
   * A state in which a DCN can be at any given moment.
   */
  public enum State {
    /**
     * Immediately after construction, {@link DCNetwork#start} has not yet been
     * called.
     */
    CREATED,
    /**
     * Currently running, {@link DCNetwork#start} has been called once but
     * {@link DCNetwork#stop} has not yet been called.
     */
    RUNNING,
    /**
     * {@link DCNetwork#stop} has been called. The DCN is now terminated and can
     * not be started again.
     */
    TERMINATED;
  }
}
