package com.gregguns.utility.turret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.gregguns.utility.turret.TurretGuiModel.Status;
import com.gregguns.utility.turret.TurretGuiModel.Tone;

class TurretGuiModelTest {

    private static final int ALL = TurretGuiModel.flags(false, true, true, true);

    @Test
    void healthyTurretWithGunGuards() {
        assertEquals(Status.GUARDING, TurretGuiModel.status(ALL, true));
        assertEquals(Tone.GOOD, Status.GUARDING.tone());
    }

    @Test
    void missingGunIsAWarning() {
        assertEquals(Status.NO_GUN, TurretGuiModel.status(ALL, false));
        assertEquals(Tone.WARN, Status.NO_GUN.tone());
    }

    @Test
    void statusPriorityIsRepairThenTurretThenRedstoneThenPower() {
        assertEquals(Status.REPAIRING, TurretGuiModel.status(TurretGuiModel.flags(true, false, false, false), false));
        assertEquals(Status.NO_TURRET, TurretGuiModel.status(TurretGuiModel.flags(false, false, false, false), true));
        assertEquals(Status.REDSTONE_OFF, TurretGuiModel.status(TurretGuiModel.flags(false, false, false, true), true));
        assertEquals(Status.NO_POWER, TurretGuiModel.status(TurretGuiModel.flags(false, true, false, true), true));
    }

    @Test
    void everyStatusHasItsOwnLangKey() {
        Set<String> keys = new HashSet<>();
        for (Status s : Status.values()) {
            keys.add(s.langKey());
        }
        assertEquals(Status.values().length, keys.size());
    }

    @Test
    void redstoneKeysFollowTheRealModes() {
        assertEquals("gregguns.turret.gui.redstone.ignore", TurretGuiModel.redstoneKey(RedstoneMode.IGNORE.id()));
        assertEquals("gregguns.turret.gui.redstone.signal", TurretGuiModel.redstoneKey(RedstoneMode.WITH_SIGNAL.id()));
        assertEquals(
            "gregguns.turret.gui.redstone.no_signal",
            TurretGuiModel.redstoneKey(RedstoneMode.WITHOUT_SIGNAL.id()));
        assertEquals("gregguns.turret.gui.redstone.ignore", TurretGuiModel.redstoneKey(99));
    }

    @Test
    void securityKeysCoverEveryLevel() {
        assertEquals("gregguns.turret.gui.security.public", TurretGuiModel.securityKey(0));
        assertEquals("gregguns.turret.gui.security.friends", TurretGuiModel.securityKey(1));
        assertEquals("gregguns.turret.gui.security.owner", TurretGuiModel.securityKey(2));
    }

    @Test
    void lockedControlsNameTheirReason() {
        assertNull(TurretGuiModel.lockedKey(true, true));
        assertEquals("gregguns.turret.gui.locked_owner", TurretGuiModel.lockedKey(false, true));
        assertEquals("gregguns.turret.gui.locked_use", TurretGuiModel.lockedKey(false, false));
    }

    @Test
    void targetDisplayHandlesIdlePlayersAndEntities() {
        assertNull(TurretGuiModel.targetDisplay("", k -> k));
        assertNull(TurretGuiModel.targetDisplay(null, k -> k));
        assertEquals("Alice", TurretGuiModel.targetDisplay("@Alice", k -> "x"));
        assertEquals("Zombie", TurretGuiModel.targetDisplay("entity.Zombie.name", k -> "Zombie"));
    }

    @Test
    void euIsGroupedAndFractionClamped() {
        assertEquals("8,192", TurretGuiModel.eu(8192));
        assertEquals("0", TurretGuiModel.eu(-5));
        assertEquals(0.5F, TurretGuiModel.euFraction(50, 100), 0.0001F);
        assertEquals(1.0F, TurretGuiModel.euFraction(500, 100), 0.0001F);
        assertEquals(0.0F, TurretGuiModel.euFraction(5, 0), 0.0001F);
    }
}
