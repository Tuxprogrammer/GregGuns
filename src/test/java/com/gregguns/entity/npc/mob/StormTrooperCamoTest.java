package com.gregguns.entity.npc.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.gregguns.armor.logic.ArmorCatalog;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.SetDef;
import com.gregguns.entity.npc.loot.EquipmentProfile;
import com.gregguns.entity.npc.loot.NpcProfiles;

/** The Stormtrooper wears full T3 HEV in camo 1 (silver), and the HEV camo index picks the matching texture. */
// Spec: docs/techguns-spec/10-entities-npcs/per-mob.md (10. Stormtrooper)
class StormTrooperCamoTest {

    @Test
    void everyArmorPieceIsHevCamoOne() {
        EquipmentProfile eq = NpcProfiles.get(StormTrooper.PROFILE)
            .equipment();
        EquipmentProfile.Gear gear = eq.roll(new Random(1), 1, g -> true, s -> 3);
        assertEquals(
            4,
            gear.armor()
                .size());
        for (EquipmentProfile.ArmorPick pick : gear.armor()) {
            assertEquals("t3_hev", pick.setId());
            assertEquals(1, pick.camo());
        }
    }

    @Test
    void hevCamoIndexMapsToTexture() {
        SetDef hev = ArmorCatalog.find("t3_hev");
        String base = "gregguns:textures/models/armor/";
        assertEquals(base + "t3_hev_1.png", hev.layerTexture(ArmorSlot.CHESTPLATE, 0));
        assertEquals(base + "t3_hev_silver_1.png", hev.layerTexture(ArmorSlot.CHESTPLATE, 1));
        assertEquals(base + "t3_hev_black_1.png", hev.layerTexture(ArmorSlot.CHESTPLATE, 2));
        assertEquals(base + "t3_hev_silver_2.png", hev.layerTexture(ArmorSlot.LEGGINGS, 1));
    }
}
