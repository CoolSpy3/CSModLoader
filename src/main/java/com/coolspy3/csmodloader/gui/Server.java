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
    /**
     * The local port on which to host the server
     */
    public int localPort = 25565;
    /**
     * Whether the server should be run on startup
     */
    public boolean autoStart;

    public Server(String id, String name, String ip, int localPort, boolean autoStart)
    {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.localPort = localPort;
        this.autoStart = autoStart;
    }

}
