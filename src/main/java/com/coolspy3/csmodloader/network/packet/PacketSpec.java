package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.coolspy3.csmodloader.network.PacketDirection;

public interface PacketSpec<T extends Packet>
{

    public T create(Object[] args);

    public Class<?>[] types();

    public Class<T> getType();

    public PacketDirection getDirection();

    public default boolean customSerialization()
    {
        return false;
    }

    public default T read(InputStream is) throws IOException
    {
        return PacketParser.read(this, is);
    }

    public default void write(T packet, OutputStream os) throws IOException
    {}

}
