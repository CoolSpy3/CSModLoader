package com.coolspy3.csmodloader.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import com.coolspy3.csmodloader.Config;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.util.Utils;

public class ServerInstance
{

    private static String accessToken;
    private static KeyPair rsaKey;

    private static ServerInstance instance;

    private String serverId;
    private String host;
    private int port;

    private ServerSocket server;
    private ArrayList<Connection> connections;

    private ServerInstance(String serverId, String ip)
    {
        this.serverId = serverId;

        String[] parts = ip.split(":");

        if (parts.length > 2) throw new IllegalArgumentException("Invalid ip: " + ip);

        host = parts[0];
        port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

        connections = new ArrayList<>();
    }

    private void startInNewThread() throws IOException
    {
        server = new ServerSocket(25565);
        new Thread(() -> {
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

                    e.printStackTrace(System.err);

                    SwingUtilities.invokeLater(() -> new TextAreaFrame(e));
                }
        }).start();
    }

    private void shutdown()
    {
        Utils.safe(server::close);
        connections.forEach(Connection::close);
    }

    public static void init(String accessToken, KeyPair rsaKey)
    {
        if (ServerInstance.accessToken != null) return;

        ServerInstance.accessToken = accessToken;
        ServerInstance.rsaKey = rsaKey;
    }

    public static boolean isRunning()
    {
        return instance != null;
    }

    public static String getRunningServerId()
    {
        return isRunning() ? instance.serverId : null;
    }

    public static boolean start(String id)
    {
        if (instance != null) return false;

        try
        {
            instance = new ServerInstance(id, Config.getInstance().servers.get(id).ip);
            instance.startInNewThread();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);

            SwingUtilities.invokeLater(() -> new TextAreaFrame("Error starting server!", e));

            return false;
        }

        return true;
    }

    public static void stop()
    {
        if (instance != null) instance.shutdown();
    }

}
