package com.coolspy3.csmodloader.util;

public interface SafelyCloseable extends AutoCloseable
{
    public static SafelyCloseable of(AutoCloseable closable)
    {
        return () -> Utils.safe(closable::close);
    }

    public void close();
}
