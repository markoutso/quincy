package com.protocol7.quincy.connection;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;

public interface Connection extends FrameSender {

  static ConnectionBootstrap newBootstrap(final Channel channel) {
    return new ConnectionBootstrap(channel);
  }

  Packet sendPacket(Packet p);

  void setRemoteConnectionId(final ConnectionId remoteConnectionId);

  Version getVersion();

  AEAD getAEAD(EncryptionLevel level);

  Future<Void> close(TransportError error, FrameType frameType, String msg);

  Future<Void> close();

  InetSocketAddress getPeerAddress();

  Stream openStream();

  State getState();

  void onPacket(Packet packet);

  void setState(State state);

  void closeByPeer();

  void reset(ConnectionId sourceConnectionId);

  void setToken(byte[] retryToken);

  interface Listener {
    void action();
  }

  void addCloseListener(Listener listener);
}
