package com.coolspy3.csmodloader.network;

public enum PacketDirection
{
    /**
     * Indicates that packets are being transferred to the Minecraft client
     */
    CLIENTBOUND,
    /**
     * Indicates that packets are being transferred to the Minecraft server
     */
    SERVERBOUND;
}
