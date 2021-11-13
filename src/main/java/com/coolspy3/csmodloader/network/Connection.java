package com.coolspy3.csmodloader.network;

import java.util.function.Supplier;
import com.coolspy3.csmodloader.util.SafelyCloseable;

class Connection implements SafelyCloseable
{

    private SafelyCloseable closeFunction;
    private Supplier<Boolean> statusFunction;

    public Connection(SafelyCloseable closeFunction, Supplier<Boolean> statusFunction)
    {
        this.closeFunction = closeFunction;
        this.statusFunction = statusFunction;
    }

    public void close()
    {
        closeFunction.close();
    }

    public boolean isOpen()
    {
        return statusFunction.get();
    }

    public boolean isClosed()
    {
        return !isOpen();
    }

}
