package com.coolspy3.csmodloader.util;

/**
 * An version of {@link AutoCloseable} which does not throw an exception
 */
public interface SafelyCloseable extends AutoCloseable
{

    /**
     * Creates a new SafelyClosable whose close function calls the close function of the given
     * AutoCloseable within a call to
     * {@link Utils#safe(com.coolspy3.csmodloader.interfaces.ExceptionRunnable)}.
     *
     * @param closable The AutoCloseable to run
     *
     * @return The new SafelyClosable object
     */
    public static SafelyCloseable of(AutoCloseable closable)
    {
        return () -> Utils.safe(closable::close);
    }

    @Override
    public void close();
}
