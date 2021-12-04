package com.coolspy3.csmodloader.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An implementation of {@link ArrayList} with additional methods to shift the elements within the
 * list.
 */
public class ShiftableList<T> extends ArrayList<T>
{
    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    public ShiftableList()
    {}

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public ShiftableList(int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Constructs a list containing the elements of the specified collection, in the order they are
     * returned by the collection's iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    public ShiftableList(Collection<? extends T> c)
    {
        super(c);
    }

    /**
     * Locates the first instance of the specified element and attempts to swap it with the element
     * in the previous index. If this element is at the start of the list or not present, this
     * function has no effect.
     *
     * @param el The element to shift
     */
    public void shiftUp(T el)
    {
        int idx = indexOf(el);

        if (idx != -1) shiftUp(idx);
    }

    /**
     * Swaps the element in the specified index with the element in the previous index. This method
     * has no effect if the provided index is {@code 0}.
     *
     * @param idx The element to shift
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public void shiftUp(int idx) throws IndexOutOfBoundsException
    {
        if (idx == 0) return;

        rangeCheck(idx);

        add(idx - 1, remove(idx));
    }

    /**
     * Locates the first instance of the specified element and attempts to swap it with the element
     * in the following index. If this element is at the end of the list or not present, this
     * function has no effect.
     *
     * @param el The element to shift
     */
    public void shiftDown(T el)
    {
        int idx = indexOf(el);

        if (idx != -1) shiftDown(el);
    }

    /**
     * Swaps the element in the specified index with the element in the following index. This method
     * has no effect if the provided index is {@code size() - 1}.
     *
     * @param idx The element to shift
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public void shiftDown(int idx) throws IndexOutOfBoundsException
    {
        if (idx == size() - 1) return;

        rangeCheck(idx);

        add(idx + 1, remove(idx));
    }

    /**
     * Checks whether the specified index appears in this list and, if not, throws an
     * IndexOutOfBoundsException
     *
     * @param idx The index to check
     *
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public void rangeCheck(int idx) throws IndexOutOfBoundsException
    {
        if (idx < 0 || idx >= size())
            throw new IndexOutOfBoundsException("Index: " + idx + " Size: " + size());
    }

}
