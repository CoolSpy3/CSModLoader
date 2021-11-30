package com.coolspy3.csmodloader.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.coolspy3.csmodloader.interfaces.ExceptionConsumer;
import com.coolspy3.csmodloader.interfaces.ExceptionFunction;
import com.coolspy3.csmodloader.mod.Entrypoint;
import com.coolspy3.csmodloader.mod.ModLoader;
import com.coolspy3.csmodloader.network.packet.Packet;
import com.coolspy3.csmodloader.network.packet.PacketParser;
import com.coolspy3.csmodloader.util.Utils;

/**
 * Manages the parsing and dispatching of packets
 */
public class PacketHandler
{
    private static final InheritableThreadLocal<PacketHandler> localHandler =
            new InheritableThreadLocal<>();

    /**
     * The collection of entrypoints returned from {@link ModLoader#loadMods()}.
     */
    private static ArrayList<Entrypoint> mods = new ArrayList<>();
    /**
     * The mods loaded for this PacketHandler instance, determined by calling
     * {@link Entrypoint#create()} on all mods in {@link #mods}.
     */
    private final ArrayList<Entrypoint> loadedMods;

    private final ArrayList<SubscriberFunction> subscribers = new ArrayList<>();

    /**
     * Creates a new PacketHandler and initializes all mods by calling
     * {@link Entrypoint#init(PacketHandler)} on all instances returned by
     * {@link Entrypoint#create()}.
     */
    PacketHandler()
    {
        loadedMods = new ArrayList<>(mods);
        loadedMods.replaceAll(Entrypoint::create);

        loadedMods.forEach(entrypoint -> Utils.reporting(() -> entrypoint.init(this)));
    }

    /**
     * Dispatches the given packet to all subscribed listeners.
     *
     * @param p The packet to dispatch
     *
     * @return Whether any of the subscribers requested that the packet be blocked
     */
    public boolean dispatch(Packet p)
    {
        // Explicitly call filter to ensure that all subscribers are invoked
        return subscribers.stream().filter(sub -> sub.invoke(p)).count() > 0;
    }

    /**
     * Attempts to parse a packet from the provided InputStream
     *
     * @param direction The direction in which the packet is being sent
     * @param packetId The id of the packet to read
     * @param packetData The InputStream from which to read the packet
     *
     * @return Whether any of the subscribers requested that the packet be blocked
     */
    public boolean handlePacket(PacketDirection direction, int packetId, InputStream packetData)
    {
        Packet packet = Utils.reporting(() -> {

            Class<? extends Packet> packetClass = PacketParser.getPacketClass(direction, packetId);

            if (packetClass == null) return null;

            // If there are no subscribers which care about the Packet, there's no need to parse it
            if (subscribers.stream().noneMatch(sub -> sub.accepts(packetClass))) return null;

            return PacketParser.read(packetClass, packetData);

        }, null);

        if (packet == null) return false;

        return dispatch(packet);
    }

    /**
     * Attempts to parse a packet
     *
     * @param direction The direction in which the packet is being sent
     * @param packetData The encoded packet
     *
     * @return Whether any of the subscribers requested that the packet be blocked
     *
     * @throws IOException If an I/O error occurs
     */
    public boolean handleRawPacket(PacketDirection direction, byte[] packetData) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(packetData);

        return handlePacket(direction, Utils.readVarInt(bais), bais);
    }

    /**
     * Attempts to register as many methods as possible from the given object or class.
     *
     * If the object is a class, this method registers all valid public static methods as listeners.
     * Otherwise, this method registers all valid public non-static methods as listeners. If any
     * methods have already been registered, they will not be re-registered.
     *
     * For information on what constitutes a valid method, see the documentation for
     * {@link #validateMethod(Method)}.
     *
     * @param o The object to register
     */
    public void register(Object o)
    {
        Class<?> cls = o instanceof Class ? (Class<?>) o : o.getClass();

        for (Method method : cls.getMethods())
        {

            if (method.getDeclaringClass() != cls) return;

            int mod = method.getModifiers();

            // Iff o is Class, method must be static
            if (!Modifier.isPublic(mod) || (o instanceof Class != Modifier.isStatic(mod))) return;

            if (subscribers.stream().anyMatch(sub -> sub.matches(method))) continue;

            Class<? extends Packet>[] validTypes = validateMethod(method);

            if (validTypes == null) continue;

            Class<?> returnType = method.getReturnType();

            if (returnType == Boolean.class || returnType == Boolean.TYPE)
                subscribers.add(new SubscriberFunction(method,
                        packet -> (Boolean) method.invoke(o, packet), validTypes));

            else
                subscribers.add(new SubscriberFunction(method, packet -> {

                    method.invoke(o, packet);

                }, validTypes));
        }
    }

    /**
     * Registers the provided function to be called whenever a packet is dispatched. If this
     * function has already been registered, this method has no effect.
     *
     * @param <T> The packet type accepted by this function
     * @param func The function to register
     * @param validTypes The packet types which should be fed to this function. At least one type
     *        must be specified or the function will not be invoked.
     *
     * @throws NullPointerException If {@code func} is {@code null}
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends Packet> void register(ExceptionConsumer<T> func,
            Class<? extends T>... validTypes) throws NullPointerException
    {
        if (subscribers.stream().noneMatch(sub -> sub.matches(func)))
            subscribers.add(new SubscriberFunction(func, packet -> {

                func.accept((T) packet);

            }, validTypes));
    }

    /**
     * Registers the provided function to be called whenever a packet is dispatched. If this
     * function has already been registered, this method has no effect.
     *
     * A return value of {@code true} from {@code func} will be interpreted as a request to block
     * the processed packet.
     *
     * @param <T> The packet type accepted by this function
     * @param func The function to register
     * @param validTypes The packet types which should be fed to this function. At least one type
     *        must be specified or the function will not be invoked.
     *
     * @throws NullPointerException If {@code func} is {@code null}
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends Packet> void register(ExceptionFunction<T, Boolean> func,
            Class<? extends T>... validTypes) throws NullPointerException
    {
        if (subscribers.stream().noneMatch(sub -> sub.matches(func)))
            subscribers.add(new SubscriberFunction(func, packet -> {

                return func.apply((T) packet);

            }, validTypes));
    }

    /**
     * Checks if a method can be automatically registered by {@link #register(Object)}. A method is
     * considered valid if it meets all of the following conditions:
     *
     * 1) It is annotated with {@link SubscribeToPacketStream} and
     *
     * 2) It accepts a single argument which is a subclass of packet
     *
     * If the function returns a boolean, it will be used as an indicator of whether the function
     * requests that the packet it was sent be blocked. Otherwise, it will be assumed to be
     * {@code false}.
     *
     * The function will be fed all packets which are a subclass of any of the classes returned by
     * {@link SubscribeToPacketStream#acceptedPacketTypes()} as long as they are accepted by the
     * method. If none of these types are accepted or the array is empty, the method will be fed all
     * packet types which it can accept.
     *
     * @param method The method to check
     *
     * @return An array of packet types which can be handled by this method or {@code null} if the
     *         function cannot be registered
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Packet>[] validateMethod(Method method)
    {
        SubscribeToPacketStream subscribeAnnotation =
                method.getAnnotation(SubscribeToPacketStream.class);

        if (subscribeAnnotation == null) return null;

        Class<?>[] parameterTypes = method.getParameterTypes();

        if (parameterTypes.length != 1) return null;

        Class<?> packetType = parameterTypes[0];

        if (!Packet.class.isAssignableFrom(packetType)) return null;

        Class<? extends Packet>[] validTypes =
                Arrays.stream(subscribeAnnotation.acceptedPacketTypes())
                        .filter(packetType::isAssignableFrom).toArray(Class[]::new);

        return validTypes.length == 0 ? new Class[] {(Class<? extends Packet>) packetType}
                : validTypes;
    }

    /**
     * Attempts to send the given packet using its specification and the local connection handler
     *
     * @param packet The packet to send
     *
     * @return Whether the packet was sent successfully
     *
     * @see PacketParser#getPacketSpecification(Packet)
     * @see PacketParser#getClassId(PacketDirection, Class)
     * @see ConnectionHandler#getLocal()
     */
    public boolean sendPacket(Packet packet)
    {
        return Utils.reporting(() -> {
            PacketDirection direction = PacketParser.getPacketSpecification(packet).direction();

            int packetId = PacketParser.getClassId(direction, packet.getClass());

            ConnectionHandler.getLocal().write(direction, packetId,
                    os -> PacketParser.write(packet, os));

            return true;

        }, false);
    }

    /**
     * Registers this PacketHandler as the default handler to process packets for this thread
     *
     * @see #getLocal()
     */
    void linkToCurrentThread()
    {
        localHandler.set(this);
    }

    /**
     * @return The PacketHandler which is registered as the default handler to process packets for
     *         this thread
     *
     * @see #linkToCurrentThread()
     */
    public static PacketHandler getLocal()
    {
        return localHandler.get();
    }

    /**
     * Sets the mods which will be loaded during the next creation of a PacketListener.
     *
     * Note: This method may only be called once (Usually by the mod loader)
     *
     * @param mods The new mod list.
     */
    public static void setMods(ArrayList<Entrypoint> mods)
    {
        if (PacketHandler.mods.isEmpty()) PacketHandler.mods = mods;
    }

    private static final class SubscriberFunction
    {

        private final Object id;
        private final Function<Packet, Boolean> func;
        private final List<Class<? extends Packet>> types;

        public SubscriberFunction(Object id, ExceptionConsumer<Packet> func,
                Class<? extends Packet>[] types) throws NullPointerException
        {
            this(id, packet -> {

                func.accept(packet);

                return false;

            }, types);
        }

        public SubscriberFunction(Object id, ExceptionFunction<Packet, Boolean> func,
                Class<? extends Packet>[] types) throws NullPointerException
        {
            this.id = Objects.requireNonNull(id);
            this.func = Utils.reporting(func, false);
            this.types = Arrays.asList(types);
        }

        public boolean invoke(Packet p)
        {
            return accepts(p.getClass()) ? func.apply(p) : false;
        }

        public boolean accepts(Class<? extends Packet> c)
        {
            return types.stream().anyMatch(type -> type.isAssignableFrom(c));
        }

        public boolean matches(Object o)
        {
            return id.equals(o);
        }

    }

}
