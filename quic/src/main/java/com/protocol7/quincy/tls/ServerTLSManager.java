package com.protocol7.quincy.tls;

import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import java.security.PrivateKey;
import java.util.List;

public class ServerTLSManager implements InboundHandler {

  private ServerTlsSession tlsSession;

  public ServerTLSManager(
      final ConnectionId connectionId,
      final TransportParameters transportParameters,
      final PrivateKey privateKey,
      final List<byte[]> certificates) {
    this.tlsSession =
        new ServerTlsSession(
            InitialAEAD.create(connectionId.asBytes(), false),
            transportParameters,
            certificates,
            privateKey);
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {

    // TODO check version
    final State state = ctx.getState();
    if (state == State.Started) {
      if (packet instanceof InitialPacket) {
        final InitialPacket initialPacket = (InitialPacket) packet;

        final CryptoFrame cf = (CryptoFrame) initialPacket.getPayload().getFrames().get(0);

        final ServerTlsSession.ServerHelloAndHandshake shah =
            tlsSession.handleClientHello(cf.getCryptoData());

        // sent as initial packet
        // TODO ack?
        ctx.send(new CryptoFrame(0, shah.getServerHello()));

        tlsSession.setHandshakeAead(shah.getHandshakeAEAD());

        // sent as handshake packet
        ctx.send(new CryptoFrame(0, shah.getServerHandshake()));

        tlsSession.setOneRttAead(shah.getOneRttAEAD());

        ctx.setState(State.BeforeReady);
      } else {
        throw new IllegalStateException("Unexpected packet in BeforeInitial: " + packet);
      }
    } else if (state == State.BeforeReady && packet instanceof HandshakePacket) {
      final HandshakePacket fp = (HandshakePacket) packet;
      final CryptoFrame cryptoFrame = (CryptoFrame) fp.getPayload().getFrames().get(0);
      tlsSession.handleClientFinished(cryptoFrame.getCryptoData());

      tlsSession.unsetInitialAead();
      tlsSession.unsetHandshakeAead();

      ctx.setState(State.Ready);
    }

    ctx.next(packet);
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsSession.getAEAD(level);
  }

  public boolean available(final EncryptionLevel level) {
    return tlsSession.available(level);
  }
}
