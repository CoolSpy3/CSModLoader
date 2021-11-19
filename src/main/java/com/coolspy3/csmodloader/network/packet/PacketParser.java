package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.function.Function;

import com.coolspy3.csmodloader.network.PacketDirection;

public final class PacketParser
{

    private static final HashMap<Class<? extends Packet>, PacketSpec> specifications =
            new HashMap<>();
    private static final HashMap<Class<? extends Packet>, Function<Object[], ? extends Packet>> constructors =
            new HashMap<>();

    private static final HashMap<Integer, Class<? extends Packet>> cbPacketClasses =
            new HashMap<>();
    private static final HashMap<Class<? extends Packet>, Integer> cbPacketIds = new HashMap<>();

    private static final HashMap<Integer, Class<? extends Packet>> sbPacketClasses =
            new HashMap<>();
    private static final HashMap<Class<? extends Packet>, Integer> sbPacketIds = new HashMap<>();

    private static final HashMap<Class<?>, ObjectParser<?>> objectParsers = new HashMap<>();

    private static final HashMap<Class<? extends Packet>, PacketSerializer<?>> customSerializers =
            new HashMap<>();

    static
    {
        Parsers.registerDefaults();
    }

    public static <T extends Packet> void addSpecification(Class<T> packetType, PacketSpec spec,
            Function<Object[], T> constructor)
    {
        specifications.put(packetType, spec);
        constructors.put(packetType, constructor);
    }

    public static void addParser(ObjectParser<?> parser)
    {
        objectParsers.put(parser.getType(), parser);
    }

    public static void addSerializer(PacketSerializer<?> serializer)
    {
        customSerializers.put(serializer.getType(), serializer);
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

    public static PacketSpec getPacketSpecification(Packet packet) throws IllegalArgumentException
    {
        return getPacketSpecification(packet.getClass());
    }

    public static PacketSpec getPacketSpecification(Class<? extends Packet> packetClass)
            throws IllegalArgumentException
    {
        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        return specifications.get(packetClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> T read(Class<T> packetClass, InputStream is)
            throws IllegalArgumentException, IOException
    {
        if (customSerializers.containsKey(packetClass))
            return (T) customSerializers.get(packetClass).read(is);

        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        PacketSpec spec = getPacketSpecification(packetClass);

        Class<?>[] types = spec.argTypes();
        Object[] values = new Object[types.length];

        for (int i = 0; i < types.length; i++)
        {
            Class<?> type = types[i];

            if (!objectParsers.containsKey(type))
                throw new IllegalArgumentException("Unknown Type: " + type.getName());

            values[i] = objectParsers.get(type).decode(is);
        }

        return (T) constructors.get(packetClass).apply(values);
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

    @SuppressWarnings("unchecked")
    public static <T extends Packet> void write(T packet, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        write(packet, (Class<T>) packet.getClass(), os);
    }

    public static <T extends Packet> void write(T packet, Class<T> packetClass, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        if (customSerializers.containsKey(packetClass))
        {
            customSerializers.get(packetClass).write(packet, os);

            return;
        }

        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        PacketSpec spec = specifications.get(packetClass);

        Class<?>[] types = spec.argTypes();
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
