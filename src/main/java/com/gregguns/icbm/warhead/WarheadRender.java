package com.gregguns.icbm.warhead;

/**
 * The render id of the warhead block renderer. The client sets it before any chunk is built; on a server it stays at
 * {@code -1}, which no code there reads. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Model and rendering)
public final class WarheadRender {

    /** The ISBRH id, or -1 before the client registered it. */
    public static volatile int id = -1;

    private WarheadRender() {}
}
