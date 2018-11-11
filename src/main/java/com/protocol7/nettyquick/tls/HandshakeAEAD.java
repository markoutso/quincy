package com.protocol7.nettyquick.tls;

public class HandshakeAEAD {


    public static AEAD create(byte[] handshakeSecret, byte[] helloHash, boolean quic, boolean isClient) {
        String labelPrefix;
        if (quic) {
            labelPrefix = AEADUtil.QUIC_LABEL_PREFIX;
        } else {
            labelPrefix = AEADUtil.TLS_13_LABEL_PREFIX;
        }

        // client_handshake_traffic_secret = hkdf-Expand-Label(
        //    key = handshake_secret,
        //    label = "c hs traffic",
        //    context = hello_hash,
        //    len = 32)
        byte[] clientHandshakeTrafficSecret = AEADUtil.expandLabel(handshakeSecret, "tls13 ","c hs traffic", helloHash, 32);

        //server_handshake_traffic_secret = hkdf-Expand-Label(
        //    key = handshake_secret,
        //    label = "s hs traffic",
        //    context = hello_hash,
        //    len = 32)
        byte[] serverHandshakeTrafficSecret = AEADUtil.expandLabel(handshakeSecret, "tls13 ","s hs traffic", helloHash, 32);

        // client_handshake_key = hkdf-Expand-Label(
        //    key = client_handshake_traffic_secret,
        //    label = "key",
        //    context = "",
        //    len = 16)
        byte[] clientHandshakeKey = AEADUtil.expandLabel(clientHandshakeTrafficSecret, labelPrefix, "key", new byte[0], 16);

        //server_handshake_key = hkdf-Expand-Label(
        //    key = server_handshake_traffic_secret,
        //    label = "key",
        //    context = "",
        //    len = 16)
        byte[] serverHandshakeKey = AEADUtil.expandLabel(serverHandshakeTrafficSecret, labelPrefix,"key", new byte[0], 16);


        //client_handshake_iv = hkdf-Expand-Label(
        //    key = client_handshake_traffic_secret,
        //    label = "iv",
        //    context = "",
        //    len = 12)
        byte[] clientHandshakeIV = AEADUtil.expandLabel(clientHandshakeTrafficSecret, labelPrefix,"iv", new byte[0], 12);


        //server_handshake_iv = hkdf-Expand-Label(
        //    key = server_handshake_traffic_secret,
        //    label = "iv",
        //    context = "",
        //    len = 12)
        byte[] serverHandshakeIV = AEADUtil.expandLabel(serverHandshakeTrafficSecret, labelPrefix,"iv", new byte[0], 12);

        if (isClient) {
            return new AEAD(clientHandshakeKey, serverHandshakeKey, clientHandshakeIV, serverHandshakeIV);
        } else {
            return new AEAD(serverHandshakeKey, clientHandshakeKey, serverHandshakeIV, clientHandshakeIV);
        }
    }

}