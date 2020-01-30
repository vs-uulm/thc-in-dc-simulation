package de.uulm.vs.dcn;

import java.util.Arrays;

/**
 * 
 * @author Juri Dispan
 *
 */
public class MessagePart {
  private final int point;
  private final byte[] content;

  public MessagePart(int point, byte[] content) {
    super();
    this.point = point;
    this.content = content;
  }

  public int getPoint() {
    return point;
  }

  public byte[] getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "(" + point + ", " + Arrays.toString(content) + ")\n";
  }
}
