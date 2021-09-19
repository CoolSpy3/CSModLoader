package com.coolspy3.csmodloader.interfaces;

import java.io.IOException;

@FunctionalInterface
public interface IOCommand {

    public void run() throws IOException;

}
