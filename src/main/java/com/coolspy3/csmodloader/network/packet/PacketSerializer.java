package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface PacketSerializer<T extends Packet>
{

    public Class<T> getType();

    public Packet read(InputStream is) throws IOException;

    public void write(Packet packet, OutputStream os) throws IOException;

}
