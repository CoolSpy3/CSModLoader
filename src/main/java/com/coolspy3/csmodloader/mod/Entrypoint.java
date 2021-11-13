package com.coolspy3.csmodloader.mod;

public interface Entrypoint
{

    public default Entrypoint create()
    {
        return this;
    }

    public default void init()
    {}

}
