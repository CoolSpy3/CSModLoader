package com.coolspy3.csmodloader.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.coolspy3.csmodloader.interfaces.ExceptionConsumer;
import com.coolspy3.csmodloader.interfaces.ExceptionFunction;
import com.coolspy3.csmodloader.mod.Entrypoint;
import com.coolspy3.csmodloader.network.packet.Packet;
import com.coolspy3.csmodloader.network.packet.PacketParser;
import com.coolspy3.csmodloader.util.Utils;

public class PacketHandler
{
    private static final InheritableThreadLocal<PacketHandler> localHandler =
            new InheritableThreadLocal<>();

    private static ArrayList<Entrypoint> mods = new ArrayList<>();
    private final ArrayList<Entrypoint> loadedMods;

    private final ArrayList<SubscriberFunction> subscribers = new ArrayList<>();

    PacketHandler()
    {
        loadedMods = new ArrayList<>(mods);
        loadedMods.replaceAll(Entrypoint::create);

        loadedMods.forEach(entrypoint -> Utils.reporting(() -> entrypoint.init(this)));
    }

    public boolean dispatch(Packet p)
    {
        // Explicitly call filter to ensure that all subscribers are invoked
        return subscribers.stream().filter(sub -> sub.invoke(p)).count() > 0;
    }

    public boolean handlePacket(PacketDirection direction, int packetId, InputStream packetData)
            throws IOException
    {
        Packet packet = Utils.reporting(() -> {

            Class<? extends Packet> packetClass = PacketParser.getPacketClass(direction, packetId);

            if (packetClass == null) return null;

            // If there are no subscribers which care about the Packet, there's no need to parse it
            if (subscribers.stream().noneMatch(sub -> sub.accepts(packetClass)))
                return null;

            return PacketParser.read(packetClass, packetData);

        }, null);

        if (packet == null) return false;

        return dispatch(packet);
    }

    public boolean handleRawPacket(PacketDirection direction, byte[] packetData) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(packetData);

        return handlePacket(direction, Utils.readVarInt(bais), bais);
    }

    public void register(Object o)
    {
        if (o instanceof Class)
        {
            Class<?> cls = (Class<?>) o;

            for (Method method : cls.getMethods())
            {

                int mod = method.getModifiers();

                if (!Modifier.isPublic(mod) || !Modifier.isStatic(mod)) return;

                if (subscribers.stream().anyMatch(sub -> sub.matches(method))) continue;

                Class<? extends Packet>[] validTypes = validateMethod(method);

                if (validTypes == null) continue;

                Class<?> returnType = method.getReturnType();

                if (returnType == Boolean.class || returnType == Boolean.TYPE)
                {
                    subscribers.add(new SubscriberFunction(method,
                            packet -> (Boolean) method.invoke(null, packet), validTypes));
                }
                else
                {

                    subscribers.add(new SubscriberFunction(method, packet -> {
                        method.invoke(null, packet);
                    }, validTypes));
                }
            }

            return;
        }

        Class<?> cls = o.getClass();

        for (Method method : cls.getMethods())
        {

            int mod = method.getModifiers();

            if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) return;

            if (subscribers.stream().anyMatch(sub -> sub.matches(method))) continue;

            Class<? extends Packet>[] validTypes = validateMethod(method);

            if (validTypes == null) continue;

            Class<?> returnType = method.getReturnType();

            if (returnType == Boolean.class || returnType == Boolean.TYPE)
            {
                subscribers.add(new SubscriberFunction(method,
                        packet -> (Boolean) method.invoke(o, packet), validTypes));
            }
            else
            {

                subscribers.add(new SubscriberFunction(method, packet -> {

                    method.invoke(o, packet);
                }, validTypes));
            }
        }
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends Packet> void register(ExceptionConsumer<T> func,
            Class<? extends T>... validTypes)
    {
        if (subscribers.stream().noneMatch(sub -> sub.matches(func)))
            subscribers.add(new SubscriberFunction(func, packet -> {

                func.accept((T) packet);

            }, validTypes));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends Packet> void register(ExceptionFunction<T, Boolean> func,
            Class<? extends T>... validTypes)
    {
        if (subscribers.stream().noneMatch(sub -> sub.matches(func)))
            subscribers.add(new SubscriberFunction(func, packet -> {

                return func.apply((T) packet);

            }, validTypes));
    }

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

    void linkToCurrentThread()
    {
        localHandler.set(this);
    }

    public static PacketHandler getLocal()
    {
        return localHandler.get();
    }

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
                Class<? extends Packet>[] types)
        {
            this(id, packet -> {

                func.accept(packet);

                return false;

            }, types);
        }

        public SubscriberFunction(Object id, ExceptionFunction<Packet, Boolean> func,
                Class<? extends Packet>[] types)
        {
            this.id = id;
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
            return o == id;
        }

    }

}
