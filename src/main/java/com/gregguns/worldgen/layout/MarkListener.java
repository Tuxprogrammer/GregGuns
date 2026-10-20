package com.gregguns.worldgen.layout;

/** Receives the layout marks (chest, spawner, flag points) that fall inside the window, in world coordinates. */
public interface MarkListener {

    MarkListener NONE = (tag, x, y, z) -> {};

    void mark(String tag, int x, int y, int z);
}
