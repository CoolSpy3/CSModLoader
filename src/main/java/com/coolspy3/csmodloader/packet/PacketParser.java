package com.coolspy3.csmodloader.packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public final class PacketParser {

    private static final HashMap<Class<? extends Packet>, PacketSpec<?>> specifications = new HashMap<>();
    private static final HashMap<Class<?>, ObjectParser<?>> objectParsers = new HashMap<>();

    static {
        Parsers.registerDefaults();
    }

    public static <T extends Packet> void addSpecification(PacketSpec<T> spec) {
        specifications.put(spec.getType(), spec);
    }

    public static <T> void addParser(ObjectParser<T> parser) {
        objectParsers.put(parser.getType(), parser);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> T read(Class<T> packetClass, InputStream is) throws IllegalArgumentException, IOException {
        if(!specifications.containsKey(packetClass)) {
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());
        }
        return read((PacketSpec<T>)specifications.get(packetClass), is);
    }

    public static <T extends Packet> T read(PacketSpec<T> spec, InputStream is) throws IllegalArgumentException, IOException {
        if(!specifications.containsKey(spec.getType())) {
            addSpecification(spec);
        }
        if(spec.customSerialization()) {
            return spec.read(is);
        }
        Class<?>[] types = spec.types();
        Object[] values = new Object[types.length];
        for(int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if(!objectParsers.containsKey(type)) {
                throw new IllegalArgumentException("Unknown Type: " + type.getName());
            }
            values[i] = objectParsers.get(type).decode(is);
        }
        return spec.create(values);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> void write(T packet, OutputStream os) throws IllegalArgumentException, IOException {
        write(packet, (Class<T>)packet.getClass(), os);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> void write(T packet, Class<T> packetClass, OutputStream os) throws IllegalArgumentException, IOException {
        if(!specifications.containsKey(packetClass)) {
            throw new IllegalArgumentException("Unknown Specification: " + packetClass.getName());
        }
        write((PacketSpec<T>)specifications.get(packetClass), os);
    }

    public static <T extends Packet> void write(PacketSpec<T> spec, OutputStream os) throws IllegalArgumentException, IOException {
        if(!specifications.containsKey(spec.getType())) {
            addSpecification(spec);
        }
        if(spec.customSerialization()) {
            spec.write(os);
            return;
        }
        Class<?>[] types = spec.types();
        Object[] values = new Object[types.length];
        for(int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if(!objectParsers.containsKey(type)) {
                throw new IllegalArgumentException("Unknown Type: " + type.getName());
            }
            objectParsers.get(type).encodeObject(values[i], os);
        }
    }

    private PacketParser() {}

}
