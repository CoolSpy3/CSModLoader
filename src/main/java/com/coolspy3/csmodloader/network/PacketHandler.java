package com.coolspy3.csmodloader.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.coolspy3.csmodloader.util.Utils;

public class PacketHandler {

    public static void handlePacket(ConnectionHandler handler, int packetId, InputStream packetData) throws IOException {

    }

	public static void handleRawPacket(ConnectionHandler handler, byte[] packetData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(packetData);
        handlePacket(handler, Utils.readVarInt(bais), bais);
	}

}
