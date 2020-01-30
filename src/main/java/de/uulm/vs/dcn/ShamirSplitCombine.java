package de.uulm.vs.dcn;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.codahale.shamir.Scheme;

/**
 * 
 * @author Juri Dispan
 *
 */
public class ShamirSplitCombine extends SplitCombineStrategy {

  private final Scheme scheme;

  public ShamirSplitCombine(int n, int k) {
    super(n, k);
    scheme = new Scheme(new SecureRandom(), n, k);
  }

  @Override
  public List<MessagePart> split(byte[] msg) {
    var list = new ArrayList<MessagePart>();
    scheme.split(msg).forEach((i, m) -> list.add(new MessagePart(i, m)));
    return list;
  }

  @Override
  public byte[] combine(List<MessagePart> msgs) {
    var map = new HashMap<Integer, byte[]>(msgs.size());
    msgs.forEach(part -> map.put(part.getPoint(), part.getContent()));
    return scheme.join(map);
  }

}
