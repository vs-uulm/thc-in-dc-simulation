package de.uulm.vs.dcn;

import java.util.ArrayList;
import java.util.List;

/**
 * A (n, 1) secret sharing scheme. Splitting a message works by copying it.
 * Combining message parts works by choosing one of the provided parts.
 * 
 * @author Juri Dispan
 *
 */
public class DefaultSplitCombineStrategy extends SplitCombineStrategy {

  public DefaultSplitCombineStrategy(int n, int k) {
    super(n, k);
  }

  @Override
  public List<MessagePart> split(byte[] msg) {
    var list = new ArrayList<MessagePart>();
    for (var i = 0; i < n; i++) {
      list.add(new MessagePart(i + 1, msg.clone()));
    }
    return list;
  }

  @Override
  public byte[] combine(List<MessagePart> msgs) {
    return msgs.stream().map(MessagePart::getContent).findFirst()
        .orElse(new byte[0]);
  }

}
