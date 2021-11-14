package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.coolspy3.csmodloader.network.PacketDirection;

public final class PacketParser
{

    private static final HashMap<Class<? extends Packet>, PacketSpec<?>> specifications =
            new HashMap<>();

    private static final HashMap<Integer, Class<? extends Packet>> cbPacketClasses =
            new HashMap<>();
    private static final HashMap<Class<? extends Packet>, Integer> cbPacketIds = new HashMap<>();

    private static final HashMap<Integer, Class<? extends Packet>> sbPacketClasses =
            new HashMap<>();
    private static final HashMap<Class<? extends Packet>, Integer> sbPacketIds = new HashMap<>();

    private static final HashMap<Class<?>, ObjectParser<?>> objectParsers = new HashMap<>();

    static
    {
        Parsers.registerDefaults();
    }

    public static void addSpecification(PacketSpec<?> spec)
    {
        specifications.put(spec.getType(), spec);
    }

    public static void addParser(ObjectParser<?> parser)
    {
        objectParsers.put(parser.getType(), parser);
    }

    public static int getClassId(PacketDirection direction, Class<? extends Packet> packetClass)
            throws NullPointerException
    {
        switch (direction)
        {
            case CLIENTBOUND:
                return cbPacketIds.get(packetClass);

            case SERVERBOUND:
                return sbPacketIds.get(packetClass);

            default:
                return 0;
        }
    }

    public static Class<? extends Packet> getPacketClass(PacketDirection direction, int packetId)
    {
        switch (direction)
        {
            case CLIENTBOUND:
                return cbPacketClasses.get(packetId);

            case SERVERBOUND:
                return sbPacketClasses.get(packetId);

            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> PacketSpec<T> getPacketSpecification(T packet)
            throws IllegalArgumentException
    {
        return getPacketSpecification((Class<T>) packet.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> PacketSpec<T> getPacketSpecification(Class<T> packetClass)
            throws IllegalArgumentException
    {
        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        return (PacketSpec<T>) specifications.get(packetClass);
    }

    public static <T extends Packet> T read(Class<T> packetClass, InputStream is)
            throws IllegalArgumentException, IOException
    {
        return read(getPacketSpecification(packetClass), is);
    }

    public static <T extends Packet> T read(PacketSpec<T> spec, InputStream is)
            throws IllegalArgumentException, IOException
    {
        if (!specifications.containsKey(spec.getType())) addSpecification(spec);

        if (spec.customSerialization()) return spec.read(is);

        Class<?>[] types = spec.types();
        Object[] values = new Object[types.length];

        for (int i = 0; i < types.length; i++)
        {
            Class<?> type = types[i];

            if (!objectParsers.containsKey(type))
                throw new IllegalArgumentException("Unknown Type: " + type.getName());

            values[i] = objectParsers.get(type).decode(is);
        }

        return spec.create(values);
    }

    public static void registerPacketClass(PacketDirection direction,
            Class<? extends Packet> packetClass, int packetId, int... additionalIds)
    {
        switch (direction)
        {
            case CLIENTBOUND:
                cbPacketClasses.put(packetId, packetClass);
                cbPacketIds.put(packetClass, packetId);

                for (int id : additionalIds)
                    cbPacketClasses.put(id, packetClass);

                break;

            case SERVERBOUND:
                sbPacketClasses.put(packetId, packetClass);
                sbPacketIds.put(packetClass, packetId);

                for (int id : additionalIds)
                    sbPacketClasses.put(id, packetClass);

                break;

            default:
                break;
        }
    }

    public static <T extends Packet> void write(T packet, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        write(packet, getPacketSpecification(packet), os);
    }

    public static <T extends Packet> void write(T packet, Class<T> packetClass, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        write(packet, getPacketSpecification(packetClass), os);
    }

    public static <T extends Packet> void write(T packet, PacketSpec<T> spec, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        if (!specifications.containsKey(spec.getType())) addSpecification(spec);

        if (spec.customSerialization())
        {
            spec.write(packet, os);

            return;
        }

        Class<?>[] types = spec.types();
        Object[] values = packet.getValues();

        for (int i = 0; i < types.length; i++)
        {
            Class<?> type = types[i];

            if (!objectParsers.containsKey(type))
                throw new IllegalArgumentException("Unknown Type: " + type.getName());

            objectParsers.get(type).encodeObject(values[i], os);
        }
    }

    private PacketParser()
    {}

}
