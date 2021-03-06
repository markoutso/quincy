package com.protocol7.quincy.it;

import static com.protocol7.quincy.utils.Hex.dehex;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.testcontainers.quicgo.QuicGoContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class QuicGoTest {

  @Rule public QuicGoContainer quicGo = new QuicGoContainer();

  private final EventLoopGroup workerGroup = new NioEventLoopGroup();
  private final Bootstrap b = new Bootstrap();
  private InetSocketAddress peer;

  @Before
  public void setUp() {
    peer = quicGo.getAddress();

    b.group(workerGroup);
    b.channel(NioDatagramChannel.class);
    b.remoteAddress(peer);
  }

  @Test
  public void get() throws InterruptedException {

    final BlockingQueue<String> responses = new ArrayBlockingQueue<>(10);

    b.handler(
        new QuicBuilder()
            .withApplicationProtocols("hq-29")
            .withVersion(new Version(dehex("51474fff")))
            .channelInitializer());

    final Channel channel = b.connect().sync().channel();

    final Connection connection =
        Connection.newBootstrap(channel)
            .withStreamHandler(
                (stream, data, finished) -> {
                  responses.add(new String(data, StandardCharsets.US_ASCII));
                })
            .connect()
            .sync()
            .getNow();

    connection.openStream().write("GET /\r\n".getBytes(), true);

    // wait for response
    final String response = responses.poll(10000, TimeUnit.MILLISECONDS);

    assertEquals("404 page not found\n", response);
  }

  @Test
  public void verneg() throws InterruptedException {

    final CountDownLatch responses = new CountDownLatch(1);

    b.handler(
        new QuicBuilder()
            .withApplicationProtocols("hq-29")
            .withVersion(new Version(dehex("deadbeef"))) // invalid version
            .withChannelHandler(
                new ChannelInboundHandlerAdapter() {
                  @Override
                  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                    if (msg instanceof VersionNegotiationPacket) {
                      responses.countDown();
                    }
                  }
                })
            .channelInitializer());

    final Channel channel = b.connect().sync().channel();

    try {
      Connection.newBootstrap(channel).connect().sync().getNow();
      fail("Connection must fail");
    } catch (final RuntimeException e) {
    }

    // wait for verneg
    assertTrue(responses.await(10000, TimeUnit.MILLISECONDS));
  }

  @After
  public void teardown() {
    workerGroup.shutdownGracefully();
  }
}
