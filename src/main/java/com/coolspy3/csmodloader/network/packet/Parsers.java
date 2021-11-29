package com.coolspy3.csmodloader.network.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.coolspy3.csmodloader.util.Utils;

/**
 * Contains default ObjectParsers for common types
 */
public final class Parsers
{

    /**
     * @return The ObjectParsers defined by this class
     */
    public static ObjectParser<?>[] defaults()
    {
        return new ObjectParser[] {
                ObjectParser.of(b -> new Byte[] {(byte) (b ? 0x01 : 0x00)}, b -> b[0] == 0x01, 1,
                        Boolean.class),

                ObjectParser.of(b -> new Byte[] {b}, b -> b[0], 1, Byte.class),

                ofNumber(2, ByteBuffer::putShort, ByteBuffer::getShort, Short.class),
                ofNumber(4, ByteBuffer::putInt, ByteBuffer::getInt, Integer.class),
                ofNumber(8, ByteBuffer::putLong, ByteBuffer::getLong, Long.class),
                ofNumber(4, ByteBuffer::putFloat, ByteBuffer::getFloat, Float.class),
                ofNumber(8, ByteBuffer::putDouble, ByteBuffer::getDouble, Double.class),

                ObjectParser.of(Utils::writeString, Utils::readString, String.class),

                ObjectParser.wrapping(
                        ObjectParser.of(Utils::writeVarInt, Utils::readVarInt, Integer.class),
                        Packet.VarInt.class),
                ObjectParser.wrapping(
                        ObjectParser.of(Utils::writeVarLong, Utils::readVarLong, Long.class),
                        Packet.VarLong.class),

                ObjectParser.of(uid -> {
                    ByteBuffer buf = ByteBuffer.allocate(16);
                    buf.putLong(uid.getMostSignificantBits());
                    buf.putLong(uid.getLeastSignificantBits());
                    return Utils.box(buf.array());
                }, B -> {
                    byte[] b = Utils.unbox(B);
                    return new UUID(Utils.fromBytes(Arrays.copyOf(b, 8), ByteBuffer::getLong),
                            Utils.fromBytes(Arrays.copyOfRange(b, 8, 16), ByteBuffer::getLong));
                }, 16, UUID.class), ObjectParser.of((b, os) -> Utils.writeBytes(Utils.unbox(b), os),
                        is -> Utils.box(Utils.readBytes(is)), Byte[].class)};
    }

    /**
     * Calls {@link PacketParser#addParser(ObjectParser)} for all parsers returned by
     * {@link #defaults()}.
     */
    public static void registerDefaults()
    {
        Stream.of(defaults()).forEach(PacketParser::addParser);
    }

    /**
     * Creates an object parser which can parse a fixed number of bytes
     * 
     * @param <T> The type of the parser
     * @param length The number of bytes used to encode each object
     * @param encFunc A function which stores the object into a ByteBuffer
     * @param decFunc A function which reads the object from a ByteBuffer
     * @param type The class type of the parser
     *
     * @return The new parser
     */
    public static <T> ObjectParser<T> ofNumber(int length,
            BiFunction<ByteBuffer, T, ByteBuffer> encFunc, Function<ByteBuffer, T> decFunc,
            Class<T> type)
    {
        return ObjectParser.of(getBytes(length, encFunc), fromBytes(decFunc), length, type);
    }

    /**
     * Creates a function which serializes an object
     *
     * @param <T> The object type to serialize
     * @param length The number of bytes encoded by each object
     * @param convFunc A function which stores the object into a ByteBuffer
     *
     * @return A function which serializes the object type
     */
    public static <T> Function<T, Byte[]> getBytes(int length,
            BiFunction<ByteBuffer, T, ByteBuffer> convFunc)
    {
        return v -> Utils.box(Utils.getBytes(v, length, convFunc));
    }

    /**
     * Creates a function which deserializes an object
     *
     * @param <T> The object type to deserialize
     * @param convFunc A function which reads the object from a ByteBuffer
     *
     * @return A function which deserializes the object type
     */
    public static <T> Function<Byte[], T> fromBytes(Function<ByteBuffer, T> convFunc)
    {
        return b -> Utils.fromBytes(Utils.unbox(b), convFunc);
    }

    private Parsers()
    {}

}
