package com.coolspy3.csmodloader.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;

import com.coolspy3.csmodloader.Config;
import com.coolspy3.csmodloader.gui.Server;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.util.Utils;

import net.minecraft.client.multiplayer.ServerAddress;
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
     * The current running instances
     */
    private static final ArrayList<ServerInstance> instances = new ArrayList<>();

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
     * The local port on which to host the server
     */
    private int localPort;

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
    private ServerInstance(String serverId, String ip, int localPort)
    {
        this.serverId = serverId;
        this.localPort = localPort;

        ServerAddress addr = ServerAddress.parseString(ip);

        this.host = addr.getHost();
        this.port = addr.getPort();

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
        server = new ServerSocket(localPort);
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

        instances.remove(this);
    }

    /**
     * Gets the id of the running server
     */
    private String getServerId()
    {
        return serverId;
    }

    /**
     * Gets the local port on which this instance is running
     */
    private int getLocalPort()
    {
        return localPort;
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
        return !instances.isEmpty();
    }

    /**
     * @return The current id of any running ServerInstance or {@code null} if none is running
     *
     * @see Server#id
     */
    @Deprecated
    public static String getRunningServerId()
    {
        return isRunning() ? instances.get(0).serverId : null;
    }

    /**
     * Starts a new ServerInstance for the given server. If a ServerInstance is already running on
     * the same local port, this has no effect.
     *
     * @param id The id of the server to start
     *
     * @return Whether the ServerInstance was started successfully
     *
     * @see Server#id
     */
    public static synchronized boolean start(String id)
    {
        Server server = Config.getInstance().servers.get(id);

        if (getInstanceOnPort(server.localPort) != null) return false;

        try
        {
            ServerInstance instance = new ServerInstance(id, server.ip, server.localPort);
            instances.add(instance);
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
     * Calls the {@link #shutdown()} method of any running ServerInstances
     */
    public static synchronized void stop()
    {
        // shutdown modifies instances, so copy the array to avoid a ConcurrentModificationException
        new ArrayList<>(instances).forEach(ServerInstance::shutdown);
    }

    /**
     * Calls the {@link #shutdown()} method of the requested ServerInstance. If the requested id
     * does not correspond to a running instance, this method has no effect.
     *
     * @param id The id of the server to remove
     */
    public static synchronized void stop(String id)
    {
        // shutdown modifies instances, so copy the array to avoid a ConcurrentModificationException
        new ArrayList<>(instances).stream().filter(instance -> instance.getServerId().equals(id))
                .forEach(ServerInstance::shutdown);
    }

    /**
     * Retrieves the ServerInstance running on a given local port
     *
     * @param localPort The local port to check
     *
     * @return The id of the server running on the requested local port or {@code null} if none
     *         exists
     */
    public static synchronized String getInstanceOnPort(int localPort)
    {
        return instances.stream().filter(instance -> instance.getLocalPort() == localPort)
                .map(ServerInstance::getServerId).findAny().orElse(null);
    }

    /**
     * Checks whether a ServerInstance with the specified id is running
     *
     * @param id The id to check
     *
     * @return Whether a ServerInstance with the specified id is running
     */
    public static synchronized boolean isRunning(String id)
    {
        return instances.stream().map(ServerInstance::getServerId).anyMatch(Predicate.isEqual(id));
    }

    /**
     * Stops all running ServerInstances and restarts those whose {@link Server#autoStart} value is
     * {@code true}
     */
    public static void restartServers()
    {
        ServerInstance.stop();
        Config.getInstance().servers.entrySet().stream().filter(entry -> entry.getValue().autoStart)
                .map(Map.Entry::getKey).forEach(ServerInstance::start);
    }

}
