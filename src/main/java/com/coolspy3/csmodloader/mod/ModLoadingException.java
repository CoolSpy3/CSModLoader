package com.coolspy3.csmodloader.mod;

/**
 * Indicates that an error has occurred loading a specific mod
 */
public class ModLoadingException extends Exception
{

    /**
     * @param mod The mod which caused the error
     * @param cause The error
     */
    public ModLoadingException(String mod, Throwable cause)
    {
        super("Error loading mod: " + mod + "!", cause);
    }

}
