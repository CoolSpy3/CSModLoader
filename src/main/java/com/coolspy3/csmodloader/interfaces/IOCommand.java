package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;

/**
 * Represents an implementation of {@link Runnable} which handles an I/O operation
 */
@FunctionalInterface
public interface IOCommand
{

    /**
     * Runs this function
     *
     * @throws IOException if an I/O exception occurs.
     */
    public void run() throws IOException;

}
