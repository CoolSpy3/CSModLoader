package com.coolspy3.csmodloader.network.packet;

/**
 * Represents a packet which can be transmitted between a Minecraft client and server
 */
public abstract class Packet
{

    /**
     * @return The values required to encode this packet using the default serialization mechanism.
     *         If custom serialization is provided, this method is ignored.
     */
    public abstract Object[] getValues();

    /**
     * A WrapperType for a variable-length integer
     */
    public static final class VarInt extends WrapperType<Integer>
    {}

    /**
     * A WrapperType for a variable-length long
     */
    public static final class VarLong extends WrapperType<Long>
    {}

}
