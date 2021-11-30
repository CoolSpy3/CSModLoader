package com.coolspy3.csmodloader.network.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.function.Function;

import com.coolspy3.csmodloader.network.PacketDirection;
import com.coolspy3.csmodloader.util.WrapperException;

/**
 * Encodes and decodes packets from the packet stream
 */
public final class PacketParser
{

    /**
     * A mapping of packet classes to their associated PacketSpecs
     */
    private static final HashMap<Class<? extends Packet>, PacketSpec> specifications =
            new HashMap<>();

    /**
     * A mapping of packet classes to the functions required to create them during default
     * serialization. These will be expected to create a packet using the values provided by
     * {@link Packet#getValues()}
     */
    private static final HashMap<Class<? extends Packet>, Function<Object[], ? extends Packet>> constructors =
            new HashMap<>();

    /**
     * A mapping of packet ids to their associated clientbound packet classes
     */
    private static final HashMap<Integer, Class<? extends Packet>> cbPacketClasses =
            new HashMap<>();

    /**
     * A mapping of clientbound packet classes to their associated packet ids
     */
    private static final HashMap<Class<? extends Packet>, Integer> cbPacketIds = new HashMap<>();

    /**
     * A mapping of packet ids to their associated serverbound packet classes
     */
    private static final HashMap<Integer, Class<? extends Packet>> sbPacketClasses =
            new HashMap<>();

    /**
     * A mapping of serverbound packet classes to their associated packet ids
     */
    private static final HashMap<Class<? extends Packet>, Integer> sbPacketIds = new HashMap<>();

    /**
     * A mapping of object types to their associated parsers
     */
    private static final HashMap<Class<?>, ObjectParser<?>> objectParsers = new HashMap<>();

    /**
     * A mapping of packets to their associated custom serializers if provided
     */
    private static final HashMap<Class<? extends Packet>, PacketSerializer<?>> customSerializers =
            new HashMap<>();

    static
    {
        Parsers.registerDefaults();
    }

    /**
     * Registers a packet specification. The specification is assumed to annotate the provided
     * packet's class.
     *
     * @param <T> The packet type
     * @param packetType The packet's class type
     * @param constructor The function to use to create the specified during default serialization.
     *        This will be expected to create a packet using the values provided by
     *        {@link Packet#getValues()}
     *
     * @throws IllegalArgumentException If the provided packet's class does not provide
     *         an @PacketSpec annotation
     */
    public static <T extends Packet> void addSpecification(Class<T> packetType,
            Function<Object[], T> constructor) throws IllegalArgumentException
    {
        PacketSpec spec = packetType.getAnnotation(PacketSpec.class);

        if (spec == null) throw new IllegalArgumentException(
                "No specification defined for packet type: " + packetType.getCanonicalName());

        specifications.put(packetType, spec);
        constructors.put(packetType, constructor);
    }

    /**
     * Registers a packet specification.
     *
     * @param <T> The packet type
     * @param packetType The packet's class type
     * @param spec The PacketSpec to associate with the specified packet
     * @param constructor The function to use to create the specified during default serialization.
     *        This will be expected to create a packet using the values provided by
     *        {@link Packet#getValues()}
     */
    public static <T extends Packet> void addSpecification(Class<T> packetType, PacketSpec spec,
            Function<Object[], T> constructor)
    {
        specifications.put(packetType, spec);
        constructors.put(packetType, constructor);
    }

    /**
     * Registers the specified ObjectParser to be available during serialization
     *
     * @param parser The parser to register
     */
    public static void addParser(ObjectParser<?> parser)
    {
        objectParsers.put(parser.getType(), parser);
    }

    /**
     * Registers the specified PacketSerializer to handle serialization for its packet type. This
     * bypasses default serialization.
     *
     * @param serializer The serializer to register
     */
    public static void addSerializer(PacketSerializer<?> serializer)
    {
        customSerializers.put(serializer.getType(), serializer);
    }

    /**
     * Retrieves the packet id used to transmit the provided packet class in the given direction
     *
     * @param direction The direction in which the packet will be transmitted
     * @param packetClass The packet class to check
     *
     * @return The packet id used to transmit the provided packet class in the given direction
     *
     * @throws NullPointerException If no packet id exists to transmit the packet in the given
     *         direction
     */
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
                throw new NullPointerException();
        }
    }

    /**
     * Retrieves the packet class to use to deserialize the provided packet id
     *
     * @param direction The direction the packet was traveling
     * @param packetId The id of the packet
     *
     * @return The packet class to use to deserialize the provided packet id
     */
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

    /**
     * Retrieves the PacketSpec associated with the provided packet
     *
     * @param packet The packet to check
     *
     * @return The PacketSpec associated with the provided packet
     *
     * @throws IllegalArgumentException If no PacketSpec is associated with the provided packet
     */
    public static PacketSpec getPacketSpecification(Packet packet) throws IllegalArgumentException
    {
        return getPacketSpecification(packet.getClass());
    }

    /**
     * Retrieves the PacketSpec associated with the provided packet class
     *
     * @param packetClass The packet class to check
     *
     * @return The PacketSpec associated with the provided packet class
     *
     * @throws IllegalArgumentException If no PacketSpec is associated with the provided packet
     *         class
     */
    public static PacketSpec getPacketSpecification(Class<? extends Packet> packetClass)
            throws IllegalArgumentException
    {
        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        return specifications.get(packetClass);
    }

    /**
     * Retrieves the ObjectParser used to serialize the provided type.
     *
     * @param <T> The type of objects serialized by the parser
     * @param type The class type of objects serialized by the parser
     *
     * @return The ObjectParser used to serialize the provided type.
     *
     * @throws ClassCastException If the provided type is a {@link WrapperType} and the resulting
     *         parser is a wrapping parser
     *
     * @see #addParser(ObjectParser)
     * @see #getObjectParser(Class)
     * @see #getWrappedParser(Class)
     */
    @SuppressWarnings("unchecked")
    public static <T> ObjectParser<T> getParser(Class<T> type) throws ClassCastException
    {
        return (ObjectParser<T>) objectParsers.get(type);
    }

    /**
     * Retrieves the ObjectParser used to serialize the provided type.
     *
     * @param type The class type serialized by the parser
     * @return The ObjectParser used to serialize the provided type.
     *
     * @see #addParser(ObjectParser)
     * @see #getParser(Class)
     * @see #getWrappedParser(Class)
     */
    public static ObjectParser<?> getObjectParser(Class<?> type)
    {
        return objectParsers.get(type);
    }

    /**
     * Retrieves the ObjectParser used to serialize the provided wrapper type.
     *
     * @param <T> The original type
     * @param <U> The wrapper type
     * @param type The wrapper type class
     *
     * @return The object parser used to serialize the provided wrapper type.
     *
     * @see #addParser(ObjectParser)
     * @see #getParser(Class)
     * @see #getObjectParser(Class)
     */
    @SuppressWarnings("unchecked")
    public static <T, U extends WrapperType<T>> ObjectParser<T> getWrappedParser(Class<U> type)
    {
        return (ObjectParser<T>) objectParsers.get(type);
    }

    /**
     * Uses a registered ObjectParser and calls
     * {@link ObjectParser#mapping(ObjectParser, Function, Function, Class)}
     *
     * @param <T> The type of the original parser
     * @param <U> The new type to be parsed
     * @param baseType The class type original parser
     * @param encMapper A function converting from the new type to the type of the original parser
     * @param decMapper A function converting from the type of the original parser to the new type
     * @param type The new type
     *
     * @return The new parser
     *
     * @throws IllegalArgumentException If no parser is registered for the provided {@code baseType}
     *
     * @see #addParser(ObjectParser)
     */
    public static <T, U> ObjectParser<U> mappingParser(Class<T> baseType, Function<U, T> encMapper,
            Function<T, U> decMapper, Class<U> type) throws IllegalArgumentException
    {
        ObjectParser<T> parser = getParser(baseType);

        if (parser == null) throw new IllegalArgumentException("Unknown Type: " + type.getName());

        return ObjectParser.mapping(parser, encMapper, decMapper, type);
    }

    /**
     * Uses a registered wrapping ObjectParser and calls
     * {@link ObjectParser#mapping(ObjectParser, Function, Function, Class)}
     *
     * @param <T> The type wrapped by the original parser
     * @param <U> The wrapped type
     * @param <V> The new type to be parsed
     * @param wrappingType The wrapped type class
     * @param encMapper A function converting from the new type to the type of the original parser
     * @param decMapper A function converting from the type of the original parser to the new type
     * @param type The new type
     *
     * @return The new parser
     *
     * @throws IllegalArgumentException If no parser is registered for the provided
     *         {@code wrappingType}
     *
     * @see #addParser(ObjectParser)
     */
    public static <T, U, V extends WrapperType<T>> ObjectParser<U> mappingWrappingParser(
            Class<V> wrappingType, Function<U, T> encMapper, Function<T, U> decMapper,
            Class<U> type) throws IllegalArgumentException
    {
        return ObjectParser.mapping(getWrappedParser(wrappingType), encMapper, decMapper, type);
    }

    /**
     * Uses a registered ObjectParser and calls
     * {@link ObjectParser#mapping(ObjectParser, Function, Function, Class)} and then wraps the
     * resulting parser
     *
     * @param <T> The type of the original parser
     * @param <U> The new type to be parsed
     * @param <V> The new wrapping type
     * @param baseType The class type original parser
     * @param encMapper A function converting from the new type to the type of the original parser
     * @param decMapper A function converting from the type of the original parser to the new type
     * @param wrappedType The new wrapped type
     * @param type The new type
     *
     * @return The new parser
     *
     * @throws IllegalArgumentException If no parser is registered for the provided {@code baseType}
     *
     * @see #addParser(ObjectParser)
     */
    public static <T, U, V extends WrapperType<U>> ObjectParser<U> wrappingMappingParser(
            Class<T> baseType, Function<U, T> encMapper, Function<T, U> decMapper,
            Class<U> wrappedType, Class<V> type) throws IllegalArgumentException
    {
        return ObjectParser.wrapping(mappingParser(baseType, encMapper, decMapper, wrappedType),
                type);
    }

    /**
     * Attempts to read the specified packet type
     *
     * @param <T> The packet type
     * @param packetClass The packet class
     * @param is The stream from which to read
     *
     * @return The read packet
     *
     * @throws IllegalArgumentException If default serialization is used and the packet's
     *         specification or one or more ObjectParsers cannot be found
     * @throws IOException If an I/O error occurs
     *
     * @see #addParser(ObjectParser)
     * @see #addSerializer(PacketSerializer)
     * @see #addSpecification(Class, Function)
     * @see #addSpecification(Class, PacketSpec, Function)
     * @see #registerPacket(Class, Function, int, int...)
     * @see #registerPacket(Class, PacketSerializer, int, int...)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Packet> T read(Class<T> packetClass, InputStream is)
            throws IllegalArgumentException, IOException
    {
        // If a custom serializer is registered, use that
        if (customSerializers.containsKey(packetClass))
            return (T) customSerializers.get(packetClass).read(is);

        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        PacketSpec spec = getPacketSpecification(packetClass);

        Class<?>[] types = spec.types();
        Object[] values = new Object[types.length];

        for (int i = 0; i < types.length; i++)
        {
            try
            {
                values[i] = readAnyObject(types[i], is);
            }
            catch (Exception e)
            {
                throw new WrapperException("Error occurred reading packet: " + packetClass.getName()
                        + " while reading arg (" + i + "): " + types[i].getName(), e);
            }
        }

        return (T) constructors.get(packetClass).apply(values);
    }

    /**
     * Attempts to read an object from an InputStream
     *
     * @param <T> The object type to read
     * @param type The object class type to read
     * @param is The InputStream from which to read
     *
     * @return The read object
     *
     * @throws ClassCastException If the parser associated with the specified object class is a
     *         wrapping parser
     * @throws IllegalArgumentException If no parser is registered which can
     * @throws IOException If an I/O error occurs
     *
     * @see #addParser(ObjectParser)
     */
    @SuppressWarnings("unchecked")
    public static <T> T readObject(Class<T> type, InputStream is)
            throws ClassCastException, IllegalArgumentException, IOException
    {
        ObjectParser<?> parser = getObjectParser(type);

        if (parser == null) throw new IllegalArgumentException("Unknown Type: " + type.getName());

        return (T) objectParsers.get(type).decode(is);
    }

    /**
     * Attempts to read an object from an InputStream
     *
     * @param <T> The object type to read
     * @param type The object class type to read
     * @param is The InputStream from which to read
     *
     * @return The read object
     *
     * @throws IllegalArgumentException If no parser is registered which can
     * @throws IOException If an I/O error occurs
     *
     * @see #addParser(ObjectParser)
     */
    public static <T> Object readAnyObject(Class<T> type, InputStream is)
            throws IllegalArgumentException, IOException
    {
        ObjectParser<?> parser = getObjectParser(type);

        if (parser == null) throw new IllegalArgumentException("Unknown Type: " + type.getName());

        return objectParsers.get(type).decode(is);
    }

    /**
     * Attempts to read a wrapped object from an InputStream
     *
     * @param <T> The object type to read
     * @param <U> The object wrapper type to read
     * @param type The object wrapper type class to read
     * @param is The InputStream from which to read
     *
     * @return The read object
     *
     * @throws IllegalArgumentException If no parser is registered which can
     * @throws IOException If an I/O error occurs
     *
     * @see #addParser(ObjectParser)
     */
    @SuppressWarnings("unchecked")
    public static <T, U extends WrapperType<T>> T readWrappedObject(Class<U> type, InputStream is)
            throws IllegalArgumentException, IOException
    {
        ObjectParser<?> parser = getObjectParser(type);

        if (parser == null) throw new IllegalArgumentException("Unknown Type: " + type.getName());

        return (T) objectParsers.get(type).decode(is);
    }

    /**
     * Registers a packet type by calling {@link #addSpecification(Class, Function)} and
     * {@link #registerPacketClass(Class, int, int...)}
     *
     * @param <T> The type of packet to register
     * @param packetType The packet class type to register
     * @param constructor The function to use to create the specified during default serialization.
     *        This will be expected to create a packet using the values provided by
     *        {@link Packet#getValues()}
     * @param packetId The packet id to use when sending or receiving the packet
     * @param additionalIds Additional ids which may be used to receive the packet
     *
     * @throws IllegalArgumentException If the provided packet class does have provide a
     *         {@link PacketSpec} annotation
     *
     * @see #registerPacket(Class, PacketSerializer, int, int...)
     */
    public static <T extends Packet> void registerPacket(Class<T> packetType,
            Function<Object[], T> constructor, int packetId, int... additionalIds)
            throws IllegalArgumentException
    {
        addSpecification(packetType, constructor);
        registerPacketClass(packetType, packetId, additionalIds);
    }

    /**
     * Registers a packet type by calling {@link #addSpecification(Class, Function)},
     * {@link #addSerializer(PacketSerializer)}, and
     * {@link #registerPacketClass(Class, int, int...)}
     *
     * @param <T> The type of packet to register
     * @param packetType The packet class type to register
     * @param serializer The serializer to register for the specified packet
     * @param packetId The packet id to use when sending or receiving the packet
     * @param additionalIds Additional ids which may be used to receive the packet
     *
     * @throws IllegalArgumentException If the provided packet class does have provide a
     *         {@link PacketSpec} annotation
     *
     * @see #registerPacket(Class, Function, int, int...)
     */
    public static <T extends Packet> void registerPacket(Class<T> packetType,
            PacketSerializer<T> serializer, int packetId, int... additionalIds)
            throws IllegalArgumentException
    {
        addSpecification(packetType, args -> null);
        addSerializer(serializer);
        registerPacketClass(packetType, packetId, additionalIds);
    }

    /**
     * Registers the ids to use to send and receive the specified packet
     *
     * @param packetType The packet class type to register
     * @param packetId The packet id to use when sending or receiving the packet
     * @param additionalIds Additional ids which may be used to receive the packet
     *
     * @throws IllegalArgumentException If the provided packet class does have provide a
     *         {@link PacketSpec} annotation
     *
     * @see #registerPacketClass(PacketDirection, Class, int, int...)
     */
    public static void registerPacketClass(Class<? extends Packet> packetType, int packetId,
            int... additionalIds) throws IllegalArgumentException
    {
        PacketSpec spec = packetType.getAnnotation(PacketSpec.class);

        if (spec == null) throw new IllegalArgumentException(
                "No specification defined for packet type: " + packetType.getCanonicalName());

        registerPacketClass(spec.direction(), packetType, packetId, additionalIds);
    }

    /**
     * Registers the ids to use to send and receive the specified packet
     *
     * @param direction The direction in which the packet will be transmitted
     * @param packetType The packet class type to register
     * @param packetId The packet id to use when sending or receiving the packet
     * @param additionalIds Additional ids which may be used to receive the packet
     *
     * @see #registerPacketClass(Class, int, int...)
     */
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

    /**
     * Writes the specified packet to the given OutputStream
     *
     * @param <T> The packet type
     * @param packet The packet
     * @param os The stream to which to write
     *
     * @throws IllegalArgumentException If default serialization is used and the packet's
     *         specification or one or more ObjectParsers cannot be found
     * @throws IOException If an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    public static <T extends Packet> void write(T packet, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        write(packet, (Class<T>) packet.getClass(), os);
    }

    /**
     * Writes the specified packet to the given OutputStream
     *
     * @param <T> The packet type
     * @param packet The packet
     * @param packetClass The packet class
     * @param os The stream to which to write
     *
     * @throws IllegalArgumentException If default serialization is used and the packet's
     *         specification or one or more ObjectParsers cannot be found
     * @throws IOException If an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    public static <T extends Packet> void write(T packet, Class<T> packetClass, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        if (customSerializers.containsKey(packetClass))
        {
            ((PacketSerializer<T>) customSerializers.get(packetClass)).write(packet, os);

            return;
        }

        if (!specifications.containsKey(packetClass))
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());

        PacketSpec spec = specifications.get(packetClass);

        Class<?>[] types = spec.types();
        Object[] values = packet.getValues();

        for (int i = 0; i < types.length; i++)
            writeObject(types[i], values[i], os);
    }

    /**
     * Writes an object to an OutputStream
     *
     * @param type The object class type to write
     * @param obj The object to write
     * @param os The OutputStream to which to write
     *
     * @throws IllegalArgumentException If no parser is registered which can
     * @throws IOException If an I/O error occurs
     *
     * @see #addParser(ObjectParser)
     */
    public static void writeObject(Class<?> type, Object obj, OutputStream os)
            throws IllegalArgumentException, IOException
    {
        if (!objectParsers.containsKey(type))
            throw new IllegalArgumentException("Unknown Type: " + type.getName());

        objectParsers.get(type).encodeObject(obj, os);
    }

    private PacketParser()
    {}

}
