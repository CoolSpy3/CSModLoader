package com.coolspy3.csmodloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import com.coolspy3.csmodloader.gui.Server;
import com.coolspy3.csmodloader.util.ShiftableList;
import com.coolspy3.csmodloader.util.Utils;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages user-specific configuration settings
 */
public class Config
{

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    /**
     * Initializes this Config and determines the location of the loader's configuration file
     */
    static void init()
    {
        cfgFile = GameArgs.get().gameDir.toPath().resolve("csmodloader.cfg.json").toFile();
    }

    /**
     * A list of server ids in the order in which they should be displayed to the user
     *
     * @see Server#id
     */
    public ShiftableList<String> serverList = new ShiftableList<>();
    /**
     * A mapping of server ids to their corresponding Server objects
     *
     * @see Server#id
     */
    public HashMap<String, Server> servers = new HashMap<>();

    /**
     * A convenience method for invoking {@code Utils.reporting(Config::save)}
     */
    public static void safeSave()
    {
        Utils.reporting(Config::save);
    }

    // Base Config Code

    /**
     * The configuration file
     */
    private static File cfgFile = null;
    /**
     * The global configuration instance
     */
    private static Config INSTANCE = new Config();
    /**
     * A static Gson instance to use for jsonifying data
     */
    private static final Gson gson = new Gson();

    /**
     * @return The global configuration instance
     */
    public static Config getInstance()
    {
        return INSTANCE;
    }

    /**
     * Loads the configuration file into the global configuration instance
     *
     * @throws IOException If an I/O error occurs
     * @throws IllegalStateException If {@link #init()} has not been invoked
     */
    public static void load() throws IOException, IllegalStateException
    {
        if (cfgFile == null) throw new IllegalStateException("Config has not been initialized");

        logger.debug("Loading Config...");

        if (!cfgFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(cfgFile)))
        {
            StringBuilder data = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
            {
                data.append(line).append("\n");
            }

            INSTANCE = gson.fromJson(data.substring(0, data.length() - 1), Config.class);

            INSTANCE.servers.values().stream()
                    .filter(server -> server.localPort < 1 || server.localPort > 65535)
                    .forEach(server -> server.localPort = 25565);
        }
    }

    /**
     * Saves the global configuration instance into the configuration file
     *
     * @throws IOException If an I/O error occurs
     * @throws IllegalStateException If {@link #init()} has not been invoked
     */
    public static void save() throws IOException, IllegalStateException
    {
        if (cfgFile == null) throw new IllegalStateException("Config has not been initialized");

        logger.debug("Saving Config...");

        cfgFile.createNewFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cfgFile)))
        {
            writer.write(gson.toJson(getInstance()));
        }
    }
}
