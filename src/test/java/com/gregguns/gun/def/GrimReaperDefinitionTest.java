package com.gregguns.gun.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.client.render.profile.ClientGunDefinitions;
import com.gregguns.client.render.profile.GrimReaperProfile;
import com.gregguns.client.render.profile.InHandTargets;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.logic.ProjectileKind;
import com.gregguns.gun.logic.ReloadMode;

/** The Grim Reaper against its Techguns sheet and the recipe notes. */
// Spec: docs/techguns-spec/02-guns/sheets-launchers-tools-grenades.md (Grim Reaper);
// docs/gregguns-spec/02-guns/recipes.md
class GrimReaperDefinitionTest {

    @Test
    void carriesTheSheetNumbers() {
        GunStats s = GrimReaperDef.STATS;
        assertEquals(50.0, s.damage());
        assertEquals(4, s.magazineSize());
        assertEquals("ammo_rocket", s.ammoType());
        assertEquals(ReloadMode.INCREMENTAL, s.reloadMode());
        assertEquals(40, s.reloadTicks());
        assertEquals(6, s.fireRateTicks());
        assertEquals(ProjectileKind.ROCKET, s.projectileKind());
        assertEquals(1.75, s.blastRadius());
        assertEquals(0.01, s.projectileGravity());
        assertEquals(1, s.ammoPerShot());
    }

    @Test
    void sharesTheRocketLaunchersRound() {
        assertEquals(RocketLauncherDef.STATS.ammoType(), GrimReaperDef.STATS.ammoType());
        assertEquals(RocketLauncherDef.STATS.blastRadius(), GrimReaperDef.STATS.blastRadius());
    }

    @Test
    void isLuvAndRegistered() {
        assertEquals(Tier.LUV, GrimReaperDef.DEFINITION.tier());
        assertSame(GrimReaperDef.DEFINITION, GunDefinitions.byName("grim_reaper"));
    }

    @Test
    void upgradeRowConsumesTheRocketLauncherAtLuv() {
        assertEquals(
            "GR-97",
            GrimReaperDef.DEFINITION.assembly()
                .id());
        assertEquals(
            Tier.LUV,
            GrimReaperDef.DEFINITION.assembly()
                .tier());
        assertEquals(
            RocketLauncherDef.NAME,
            GrimReaperDef.DEFINITION.assembly()
                .inputs()
                .get(0)
                .itemName());
        assertTrue(
            GrimReaperDef.DEFINITION.assembly()
                .inputs()
                .get(0)
                .anyDamage());
    }

    @Test
    void hasAProfileAndAnInHandRow() {
        assertSame(GrimReaperProfile.DEFINITION, ClientGunDefinitions.byName("grim_reaper"));
        assertTrue(InHandTargets.of("grim_reaper") != null, "the section 7 row is on the table");
    }
}
