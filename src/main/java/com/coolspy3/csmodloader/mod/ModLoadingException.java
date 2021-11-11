package com.coolspy3.csmodloader.mod;

public class ModLoadingException extends Exception {

    public ModLoadingException(String mod, Throwable cause) {
        super("Error loading mod: " + mod + "!", cause);
    }

}
