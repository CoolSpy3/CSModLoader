package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;
import java.util.function.Function;

/**
 * Represents an implementation of {@link Function} which handles an I/O operation
 *
 * @param <T> the type of the input to the function
 * @param <U> the type of the result of the function
 */
public interface IOFunction<T, U>
{

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     *
     * @throws IOException if an I/O exception occurs.
     */
    public U run(T t) throws IOException;

}
