package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface PacketSerializer<T extends Packet>
{

    public Class<T> getType();

    public T read(InputStream is) throws IOException;

    public void write(T packet, OutputStream os) throws IOException;

}
