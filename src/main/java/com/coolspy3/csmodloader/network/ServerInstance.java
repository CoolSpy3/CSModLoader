package com.coolspy3.csmodloader.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import com.coolspy3.csmodloader.Config;
import com.coolspy3.csmodloader.gui.Server;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Establishes the "virtual" Minecraft server
 */
public class ServerInstance
{

    private static final Logger logger = LoggerFactory.getLogger(ServerInstance.class);

    /**
     * The player's access token
     */
    private static String accessToken;
    /**
     * The KeyPair to use during initial authentication
     */
    private static KeyPair rsaKey;

    /**
     * The current running instance
     */
    private static ServerInstance instance;

    /**
     * The id of the server to connect to
     *
     * @see Server#id
     */
    private String serverId;
    /**
     * The hostname of the server to which to connect
     */
    private String host;
    /**
     * The port of the server to which to connect
     */
    private int port;

    /**
     * The ServerSocket accepting connections to the server
     */
    private ServerSocket server;
    /**
     * The connections currently connected to the server
     */
    private ArrayList<Connection> connections;

    /**
     * Creates a new ServerInstance
     *
     * @param serverId The id of the server to which to connect
     * @param ip The ip address of the server including an optional port number in the form
     *        "<ip>:<port>"
     */
    private ServerInstance(String serverId, String ip)
    {
        this.serverId = serverId;

        String[] parts = ip.split(":");

        if (parts.length > 2) throw new IllegalArgumentException("Invalid ip: " + ip);

        host = parts[0];
        port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

        connections = new ArrayList<>();
    }

    /**
     * Creates a new ServerSocket on the port 25565 and begins accepting connections to this
     * instance
     *
     * @throws IOException If an I/O error occurs
     */
    private void startInNewThread() throws IOException
    {
        logger.info("Starting Server ({}): {}:{}", serverId, host, port);
        server = new ServerSocket(25565);
        Thread thread = new Thread(() -> {
            while (!server.isClosed())
                try
                {
                    Socket client = server.accept();
                    Socket server = new Socket(host, port);

                    connections.add(
                            ConnectionHandler.start(client, server, host, accessToken, rsaKey));

                    connections.removeIf(Connection::isClosed);
                }
                catch (Exception e)
                {
                    if (server.isClosed()) break;

                    logger.warn("Error Connecting to Client!", e);

                    SwingUtilities.invokeLater(() -> new TextAreaFrame(e));
                }
        });

        thread.setDaemon(true);

        thread.start();
    }

    /**
     * Terminates this ServerInstance by closing the ServerSocket and all open connections
     */
    private void shutdown()
    {
        logger.info("Shutting Down Server ({}): {}:{}", serverId, host, port);
        Utils.safe(server::close);
        connections.forEach(Connection::close);

        instance = null;
    }

    /**
     * Initializes global values for {@link #accessToken} and {@link #rsaKey} so that they do not
     * have to be specified when starting a new ServerInstance
     *
     * @param accessToken The player's access token
     * @param rsaKey The KeyPair to use during initial authentication
     */
    public static synchronized void init(String accessToken, KeyPair rsaKey)
    {
        if (ServerInstance.accessToken != null) return;

        ServerInstance.accessToken = accessToken;
        ServerInstance.rsaKey = rsaKey;
    }

    /**
     * @return Whether a server instance is currently running
     */
    public static boolean isRunning()
    {
        return instance != null;
    }

    /**
     * @return The current id of the running ServerInstance or {@code null} if none is running
     *
     * @see Server#id
     */
    public static String getRunningServerId()
    {
        return isRunning() ? instance.serverId : null;
    }

    /**
     * Starts a new ServerInstance for the given server. If a ServerInstance is already running,
     * this has no effect.
     *
     * @param id The id of the server to start
     *
     * @return Whether the ServerInstance was started successfully
     *
     * @see Server#id
     */
    public static synchronized boolean start(String id)
    {
        if (instance != null) return false;

        try
        {
            instance = new ServerInstance(id, Config.getInstance().servers.get(id).ip);
            instance.startInNewThread();
        }
        catch (Exception e)
        {
            logger.warn("Error Starting Server!", e);

            SwingUtilities.invokeLater(() -> new TextAreaFrame("Error starting server!", e));

            return false;
        }

        return true;
    }

    /**
     * If a ServerInstance is currently running, this method calls its {@link #shutdown()} method.
     * Otherwise, this method has no effect.
     */
    public static synchronized void stop()
    {
        if (instance != null) instance.shutdown();
    }

}
