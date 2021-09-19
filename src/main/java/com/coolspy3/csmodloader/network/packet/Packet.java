package com.coolspy3.csmodloader.network.packet;

public abstract class Packet {

    public static final Class<VarInt> VAR_INT = VarInt.class;
    public static final Class<VarLong> VAR_LONG = VarLong.class;

    public final int id;

    public Packet(int id) {
        this.id = id;
    }

    abstract Object[] getValues();

    static final class VarInt extends WrapperType<Integer> {}
    static final class VarLong extends WrapperType<Long> {}

}
