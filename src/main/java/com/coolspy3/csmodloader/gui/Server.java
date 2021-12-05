package com.coolspy3.csmodloader.gui;

/**
 * Represents a server's name, ip, etc.
 */
public class Server
{

    /**
     * A unique identifier associated with this server
     */
    public final String id;
    /**
     * The user-friendly name of this server
     */
    public String name;
    /**
     * This server's ip
     */
    public String ip;

    public Server(String id, String name, String ip)
    {
        this.id = id;
        this.name = name;
        this.ip = ip;
    }

}
