package com.coolspy3.csmodloader.interfaces;

import java.util.function.Function;

/**
 * Represents an implementation of {@link Function} which throws an optional Exception
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface ExceptionFunction<T, U>
{
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     *
     * @throws Exception if an Exception occurs
     */
    public U apply(T t) throws Exception;
}
