package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;

public interface IOFunction<T, U> {

    public U run(T u) throws IOException;

}
