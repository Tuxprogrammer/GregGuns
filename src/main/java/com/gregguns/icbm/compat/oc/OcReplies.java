package com.gregguns.icbm.compat.oc;

import com.gregguns.icbm.component.Reply;

/** Turns a {@link Reply} into the values a callback returns: {@code true}, or {@code false} and the reason. */
final class OcReplies {

    private OcReplies() {}

    static Object[] of(Reply r) {
        return r.isOk() ? new Object[] { true } : new Object[] { false, r.reason() };
    }
}
