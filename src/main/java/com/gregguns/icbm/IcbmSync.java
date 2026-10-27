package com.gregguns.icbm;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.cleanroommc.modularui.value.sync.StringSyncValue;

/**
 * Sync values the ICBM screens share.
 *
 * <p>
 * ModularUI2 drops a client-to-server write unless the value says it allows it ("Sync handler is unable to send
 * packets to server"). Every value the player edits on an ICBM screen goes through {@link #editableText}, so none of
 * them forgets. The setter still runs on the server, where it parses, clamps and checks the player.
 */
public final class IcbmSync {

    private IcbmSync() {}

    /**
     * A text value the client may write.
     *
     * @param getter reads the current value (server side)
     * @param setter takes the typed text and must validate it itself
     * @return the sync value, with client-to-server writes allowed
     */
    public static StringSyncValue editableText(Supplier<String> getter, Consumer<String> setter) {
        return new StringSyncValue(getter, setter).allowC2S();
    }
}
