package com.coolspy3.csmodloader.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

    PacketHandler()
    {
        loadedMods = new ArrayList<>(mods);
        loadedMods.replaceAll(Entrypoint::create);

        loadedMods.forEach(Entrypoint::init);
    }

    public void handlePacket(ConnectionHandler handler, int packetId, InputStream packetData)
            throws IOException
    {
        Packet packet;
        try
        {
            Class<? extends Packet> packetClass = PacketParser.getPacketClass(packetId);

            if (packetClass == null) return;

            packet = PacketParser.read(packetClass, packetData);
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);

            return;
        }
    }

    public void handleRawPacket(ConnectionHandler handler, byte[] packetData) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(packetData);

        handlePacket(handler, Utils.readVarInt(bais), bais);
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

}
