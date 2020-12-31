package com.protocol7.quincy.protocol;

import com.google.common.base.Preconditions;
import com.protocol7.quincy.Varint;
import com.protocol7.quincy.utils.Bytes;

public class PacketNumber {

  public static long parse(final byte[] b) {
    final byte[] pad = new byte[4 - b.length];
    final byte[] bs = Bytes.concat(pad, b);

    return bs[0] << 24 | (bs[1] & 0xFF) << 16 | (bs[2] & 0xFF) << 8 | (bs[3] & 0xFF);
  }

  public static final long MIN = Varint.MIN;

  public static long validate(final long number) {
    Preconditions.checkArgument(number >= 0);
    Preconditions.checkArgument(number <= Varint.MAX);
    return number;
  }

  public static long next(final long number) {
    return number + 1;
  }

  public static int getLength(final long number) {
    return 4; // TODO
  }

  public static byte[] write(final long number, final int length) {
    final byte[] b = new byte[length];
    for (int j = length; j > 0; j--) {
      b[length - j] = (byte) ((number >> (8 * (j - 1))) & 0xFF);
    }
    return b;
  }

  private PacketNumber() {}
}
