package com.coolspy3.csmodloader.interfaces;

/**
 * Represents an implementation of {@link Runnable} which throws an optional Exception
 */
@FunctionalInterface
public interface ExceptionRunnable
{
    /**
     * Runs this function
     *
     * @throws Exception if an Exception occurs
     */
    public void run() throws Exception;
}
