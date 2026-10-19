package com.gregguns.utility.charge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

/**
 * Source-level regressions for the server-only detonation: the bug fixes that no world is needed to state. The block
 * behavior is covered by the {@code utility.charge_*} scenarios.
 */
class ChargeBlastRulesTest {

    private static String source(String name) throws Exception {
        Path p = Paths.get("src/main/java/com/gregguns/utility/charge/" + name + ".java");
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    @Test
    void doesNotReproduceMUTL30_theBreakEventIsPostedOnlyOnTheServerWithARealPlayer() throws Exception {
        String blast = source("ChargeBlast");
        // Detonation takes the actor from OwnerProfiles, which never returns null, and posts through the shared guard.
        assertTrue(blast.contains("OwnerProfiles.playerFor("));
        assertTrue(blast.contains("BlockBreakGuard.mayBreak("));
        // The tile detonates only when the world is not remote, so a client never posts the event.
        String tile = source("TileExplosiveCharge");
        assertTrue(tile.contains("worldObj.isRemote || !params.armed()"));
    }

    @Test
    void doesNotCreateAnExplosion() throws Exception {
        // GGS-09-12: blocks are removed one by one; no Explosion, no entity damage, no fire.
        String blast = source("ChargeBlast");
        assertFalse(blast.contains("new Explosion"));
        assertFalse(blast.contains("createExplosion"));
        assertFalse(blast.contains("newExplosion"));
    }

    @Test
    void dropsAreGatheredBeforeTheBlockIsCleared() throws Exception {
        // GGS-09-13: getDrops at fortune 0 comes before setBlockToAir for the bored block.
        String blast = source("ChargeBlast");
        int getDrops = blast.indexOf(".getDrops(");
        int clear = blast.indexOf("world.setBlockToAir(p[0]");
        assertTrue(getDrops > 0 && clear > getDrops);
        assertTrue(blast.contains("meta, 0)"));
    }

    @Test
    void aCanceledEventStopsItsColumn() throws Exception {
        // GGS-09-11: the cancel branch returns from the column loop.
        String blast = source("ChargeBlast");
        int cancel = blast.indexOf("!BlockBreakGuard.mayBreak");
        int ret = blast.indexOf("return;", cancel);
        int next = blast.indexOf("getDrops", cancel);
        assertTrue(cancel > 0 && ret > cancel && ret < next);
    }

    @Test
    void thePublicApiIsStable() throws Exception {
        Method m = ChargeBlast.class.getMethod(
            "detonate",
            net.minecraft.world.WorldServer.class,
            int.class,
            int.class,
            int.class,
            int.class,
            ChargeParams.class,
            java.util.UUID.class,
            com.mojang.authlib.GameProfile.class);
        assertEquals(ChargeBlast.Result.class, m.getReturnType());
    }
}
