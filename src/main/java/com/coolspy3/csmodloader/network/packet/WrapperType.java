package com.coolspy3.csmodloader.network.packet;

/**
 * Represents a type which is functionally equivalent to an existing class but is refered to
 * differently to allow for a different method of parsing. This is to accommodate types such as
 * variable-length integers and longs which can both be represented as either their primitive or
 * variable-length type.
 *
 * Keep in mind that while nesting wrapper types will not necessarily break any part of the code, it
 * is not recommended as the generic-type methods are not designed to accommodate it. Implementing
 * code should instead create multiple WrapperTypes with the same base type.
 *
 * @param <T> The type wrapped by this WrapperType
 */
public class WrapperType<T>
{}
