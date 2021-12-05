package com.coolspy3.csmodloader.network;

import java.util.function.Supplier;

import com.coolspy3.csmodloader.util.SafelyCloseable;

/**
 * Represents the general contract for a connection to a resource
 */
class Connection implements SafelyCloseable
{

    private SafelyCloseable closeFunction;
    private Supplier<Boolean> statusFunction;

    /**
     * Creates a new connection
     *
     * @param closeFunction A function which can be called to close the connection
     * @param statusFunction A function which can be used to determine whether the connection is
     *        still open
     */
    public Connection(SafelyCloseable closeFunction, Supplier<Boolean> statusFunction)
    {
        this.closeFunction = closeFunction;
        this.statusFunction = statusFunction;
    }

    /**
     * Closes this Connection
     */
    public void close()
    {
        closeFunction.close();
    }

    /**
     * @return Whether this connection is still open
     */
    public boolean isOpen()
    {
        return statusFunction.get();
    }

    /**
     * @return Whether this connection is closed
     */
    public boolean isClosed()
    {
        return !isOpen();
    }

}
