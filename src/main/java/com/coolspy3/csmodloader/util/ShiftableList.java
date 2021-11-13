package com.coolspy3.csmodloader.util;

import java.util.ArrayList;
import java.util.Collection;

public class ShiftableList<T> extends ArrayList<T>
{

    public ShiftableList()
    {}

    public ShiftableList(int initialCapacity)
    {
        super(initialCapacity);
    }

    public ShiftableList(Collection<? extends T> c)
    {
        super(c);
    }

    public void shiftUp(T el)
    {
        shiftUp(indexOf(el));
    }

    public void shiftUp(int idx)
    {
        if (idx < 1) return;

        add(idx - 1, remove(idx));
    }

    public void shiftDown(T el)
    {
        shiftDown(indexOf(el));
    }

    public void shiftDown(int idx)
    {
        if (idx >= size()) return;

        add(idx + 1, remove(idx));
    }

}
