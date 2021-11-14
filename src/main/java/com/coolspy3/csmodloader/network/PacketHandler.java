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

        loadedMods.forEach(Entrypoint::init);
    }

    public void dispatch(Packet p)
    {
        if (subscribers.stream().filter(sub -> sub.invoke(p)).count() > 0)
        {
            ConnectionHandler.getLocal().blockPacket();
        }
    }

    public void handlePacket(PacketDirection direction, int packetId, InputStream packetData)
            throws IOException
    {
        Packet packet = Utils.reporting(() -> {

            Class<? extends Packet> packetClass = PacketParser.getPacketClass(direction, packetId);

            if (packetClass == null) return null;

            return PacketParser.read(packetClass, packetData);

        }, null);

        if (packet == null) return;

        dispatch(packet);
    }

    public void handleRawPacket(PacketDirection direction, byte[] packetData) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(packetData);

        handlePacket(direction, Utils.readVarInt(bais), bais);
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

                if (subscribers.stream().filter(sub -> sub.matches(method)).count() > 0) continue;

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

            if (subscribers.stream().filter(sub -> sub.matches(method)).count() > 0) continue;

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
        if (subscribers.stream().filter(sub -> sub.matches(func)).count() == 0)
            subscribers.add(new SubscriberFunction(func, packet -> {

                func.accept((T) packet);

            }, validTypes));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends Packet> void register(ExceptionFunction<T, Boolean> func,
            Class<? extends T>... validTypes)
    {
        if (subscribers.stream().filter(sub -> sub.matches(func)).count() == 0)
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
            PacketDirection direction = PacketParser.getPacketSpecification(packet).getDirection();

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
            this.func = Utils.reporting(func);
            this.types = Arrays.asList(types);
        }

        public boolean invoke(Packet p)
        {
            return accepts(p.getClass()) ? func.apply(p) : false;
        }

        public boolean accepts(Class<? extends Packet> c)
        {
            return types.stream().filter(type -> type.isAssignableFrom(c)).count() > 0;
        }

        public boolean matches(Object o)
        {
            return o == id;
        }

    }

}
