package com.coolspy3.csmodloader.util;

/**
 * An exception used to throw a standard Exception as a RuntimeException
 *
 * @see Utils#wrap(com.coolspy3.csmodloader.interfaces.ExceptionFunction)
 * @see Utils#wrap(com.coolspy3.csmodloader.interfaces.ExceptionRunnable)
 * @see Utils#wrap(com.coolspy3.csmodloader.interfaces.ExceptionSupplier)
 */
public class WrapperException extends RuntimeException
{

    public WrapperException(Throwable cause)
    {
        super(cause);
    }

    public WrapperException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
