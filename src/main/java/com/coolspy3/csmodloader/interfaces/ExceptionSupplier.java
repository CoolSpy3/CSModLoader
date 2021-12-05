package com.coolspy3.csmodloader.interfaces;

import java.util.function.Supplier;

/**
 * Represents an implementation of {@link Supplier} which throws an optional Exception
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface ExceptionSupplier<T>
{
    /**
     * Gets a result.
     *
     * @return a result
     *
     * @throws Exception if an Exception occurs
     */
    public T get() throws Exception;
}
