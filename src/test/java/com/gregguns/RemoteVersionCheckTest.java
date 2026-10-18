package com.gregguns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RemoteVersionCheckTest {

    private static Map<String, String> remote(String version) {
        Map<String, String> map = new HashMap<>();
        map.put("gregtech", "5.9");
        if (version != null) {
            map.put(GregGuns.MODID, version);
        }
        return map;
    }

    @Test
    void releasedRequiresExactVersion() {
        assertTrue(RemoteVersionCheck.accepts(false, remote("1.0"), "1.0"));
        assertFalse(RemoteVersionCheck.accepts(false, remote("1.1"), "1.0"));
    }

    @Test
    void devAcceptsAnyVersion() {
        assertTrue(RemoteVersionCheck.accepts(true, remote("1.1-dirty"), "1.0"));
    }

    @Test
    void missingModIsRejectedEverywhere() {
        assertFalse(RemoteVersionCheck.accepts(true, remote(null), "1.0"));
        assertFalse(RemoteVersionCheck.accepts(false, remote(null), "1.0"));
        assertFalse(RemoteVersionCheck.accepts(true, Collections.<String, String>emptyMap(), "1.0"));
        assertFalse(RemoteVersionCheck.accepts(true, null, "1.0"));
    }
}
