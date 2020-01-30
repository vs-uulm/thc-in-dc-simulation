package de.uulm.vs.dcn;

import java.util.List;

/**
 * 
 * @author Juri Dispan
 *
 */
public abstract class SplitCombineStrategy {
  protected final int n;
  protected final int k;

  /**
   * 
   * @param n The number of produced parts.
   * @param k The minimum amount of message parts required to reconstruct the
   *          original message.
   */
  public SplitCombineStrategy(int n, int k) {
    this.n = n;
    this.k = k;
  }

  /**
   * Split the message into n parts. Every k of these parts can be combined to
   * retrieve the original message, whereas every k-1 parts can not.
   * 
   * @param msg The message to split up.
   * @return A collection containing the split up messages.
   */
  public abstract List<MessagePart> split(byte[] msg);

  /**
   * Combines the specified message parts into one message. If {@code msgs}
   * contains k or more messages, a meaningful message is returned, otherwiese
   * the result is unspecified.
   * 
   * @param msgs The message parts to combine.
   * @return
   */
  public abstract byte[] combine(List<MessagePart> msgs);
}
