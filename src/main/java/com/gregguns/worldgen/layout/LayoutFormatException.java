package com.gregguns.worldgen.layout;

/** A layout file is malformed. The message names the source and, where known, the line (GGS-11-28, WGEN-23). */
public class LayoutFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    public LayoutFormatException(String message) {
        super(message);
    }
}
