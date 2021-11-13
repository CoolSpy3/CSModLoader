package com.coolspy3.csmodloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.SwingUtilities;
import com.coolspy3.csmodloader.gui.Server;
import com.coolspy3.csmodloader.gui.TextAreaFrame;
import com.coolspy3.csmodloader.util.ShiftableList;
import com.google.gson.Gson;

public class Config
{

    static void init()
    {
        cfgFile = GameArgs.get().gameDir.toPath().resolve("csmodloader.cfg.json").toFile();
    }

    public ShiftableList<String> serverList = new ShiftableList<>();
    public HashMap<String, Server> servers = new HashMap<>();

    public static void safeSave()
    {
        try
        {
            save();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);

            SwingUtilities.invokeLater(
                    () -> new TextAreaFrame("Error occured while saving configuration!", e));
        }
    }

    // Base Config Code

    private static File cfgFile = null;
    private static Config INSTANCE = new Config();
    private static final Gson gson = new Gson();

    public static Config getInstance()
    {
        return INSTANCE;
    }

    public static void load() throws IOException, IllegalStateException
    {
        if (cfgFile == null) throw new IllegalStateException("Config has not been initialized");

        if (!cfgFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(cfgFile)))
        {
            String data = "", line;

            while ((line = reader.readLine()) != null)
            {
                data += line;
                data += "\n";
            }

            data = data.substring(0, data.length() - 1);
            INSTANCE = new Gson().fromJson(data, Config.class);
        }
    }

    public static void save() throws IOException, IllegalStateException
    {
        if (cfgFile == null) throw new IllegalStateException("Config has not been initialized");

        cfgFile.createNewFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cfgFile)))
        {
            writer.write(gson.toJson(getInstance()));
        }
    }
}
