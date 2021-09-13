package com.coolspy3.csmodloader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;

public class Config {

    public String mcDir = Utils.noFail(McUtils.getMcDirFile()::getCanonicalPath);

    // Base Config Code

    private static final File cfgFile = McUtils.getMcDirPath().resolve("csmodloader.cfg.json").toFile();
    private static Config INSTANCE = new Config();

    public static Config getInstance() {
        return INSTANCE;
    }

    public static void load() throws IOException {
        if(!cfgFile.exists()) {
            return;
        }
        try(BufferedReader reader = new BufferedReader(new FileReader(cfgFile))) {
            String data = "", line;
            while((line = reader.readLine()) != null) {
                data += line;
                data += "\n";
            }
            data = data.substring(0, data.length()-1);
            Gson gson = new Gson();
            INSTANCE = gson.fromJson(data, Config.class);
        }
    }

    public static void save() throws IOException {
        cfgFile.createNewFile();
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(cfgFile))) {
            Gson gson = new Gson();
            writer.write(gson.toJson(getInstance()));
        }
    }
}
