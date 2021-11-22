package com.coolspy3.csmodloader.network.packet;

public abstract class Packet
{
    public abstract Object[] getValues();

    public static final class VarInt extends WrapperType<Integer>
    {}
    public static final class VarLong extends WrapperType<Long>
    {}

}
