package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A class providing custom code to serialize and deserialize a packet
 *
 * @param <T> The type of packet which can be serialized
 */
public interface PacketSerializer<T extends Packet>
{

    /**
     * @return The packet type serialized by this PacketSerializer
     */
    public Class<T> getType();

    /**
     * Deserializes a packet from the provided InputStream
     *
     * @param is The stream from which to read
     *
     * @return The resulting object
     *
     * @throws IOException If an I/O error occurs
     */
    public T read(InputStream is) throws IOException;

    /**
     * Writes a packet to the provided OutputStream
     *
     * @param packet The packet to serialize
     * @param os The OutputStream onto which to write the packet
     *
     * @throws IOException If an I/O error occurs
     */
    public void write(T packet, OutputStream os) throws IOException;

}
