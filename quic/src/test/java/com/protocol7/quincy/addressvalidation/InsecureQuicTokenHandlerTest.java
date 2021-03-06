/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.protocol7.quincy.addressvalidation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.quincy.protocol.ConnectionId;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.Test;

public class InsecureQuicTokenHandlerTest {

  @Test
  public void testMaxTokenLength() {
    assertEquals(
        InsecureQuicTokenHandler.MAX_TOKEN_LEN, InsecureQuicTokenHandler.INSTANCE.maxTokenLength());
  }

  @Test
  public void testTokenProcessingIpv4() throws UnknownHostException {
    testTokenProcessing(true);
  }

  @Test
  public void testTokenProcessingIpv6() throws UnknownHostException {
    testTokenProcessing(false);
  }

  private static void testTokenProcessing(final boolean ipv4) throws UnknownHostException {
    final InetSocketAddress validAddress;
    final InetSocketAddress invalidAddress;
    if (ipv4) {
      validAddress =
          new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 10, 10, 1}), 9999);
      invalidAddress =
          new InetSocketAddress(InetAddress.getByAddress(new byte[] {10, 10, 10, 10}), 9999);
    } else {
      validAddress =
          new InetSocketAddress(
              InetAddress.getByAddress(
                  new byte[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 1}),
              9999);
      invalidAddress =
          new InetSocketAddress(
              InetAddress.getByAddress(
                  new byte[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10}),
              9999);
    }

    final byte[] token =
        InsecureQuicTokenHandler.INSTANCE.writeToken(ConnectionId.random(), validAddress);
    assertThat(token.length, lessThanOrEqualTo(InsecureQuicTokenHandler.INSTANCE.maxTokenLength()));

    assertTrue(InsecureQuicTokenHandler.INSTANCE.validateToken(token, validAddress).isPresent());

    // Use another address and check that the validate fails.
    assertFalse(InsecureQuicTokenHandler.INSTANCE.validateToken(token, invalidAddress).isPresent());
  }
}
