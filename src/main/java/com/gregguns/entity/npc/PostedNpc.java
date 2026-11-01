package com.gregguns.entity.npc;

/**
 * An NPC that holds a post: when no fight holds him he walks back to it and wanders only near it. A red Military
 * Base's Dictator Dave is the one (D353). The post is saved with the entity.
 *
 * <p>
 * Usage: {@code ((PostedNpc) dave).setPost(x, y, z, 6)} right after spawning him. Server side.
 */
// Spec: docs/gregguns-spec/11-worldgen/structure-placement.md (Military Base: faction and command post), D353
public interface PostedNpc {

    /**
     * @param x      block X of the post
     * @param y      block Y of the post
     * @param z      block Z of the post
     * @param radius how far he strays when idle, blocks
     */
    void setPost(int x, int y, int z, int radius);

    /** @return the post as {@code {x, y, z, radius}}, or null when he holds none */
    int[] post();
}
