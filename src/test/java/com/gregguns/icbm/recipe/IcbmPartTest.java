package com.gregguns.icbm.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class IcbmPartTest {

    @Test
    void namesAreUniqueSnakeCase() {
        Set<String> seen = new HashSet<>();
        for (IcbmPart part : IcbmPart.values()) {
            assertTrue(
                part.registryName()
                    .matches("[a-z][a-z0-9_]*"),
                part.registryName());
            assertTrue(seen.add(part.registryName()));
            assertEquals(part, IcbmPart.byRegistryName(part.registryName()));
        }
        assertEquals(IcbmPart.values().length, seen.size());
        assertEquals(12, seen.size());
        assertTrue(seen.contains("plastic_explosive"));
        assertTrue(seen.contains("napalm_canister"));
    }

    @Test
    void inputsAreAtOrBelowThePartsTierAndFitTheAssembler() {
        for (IcbmPart part : IcbmPart.values()) {
            int items = 1;
            int fluids = 0;
            for (Ing in : part.inputs()) {
                assertTrue(
                    in.tier()
                        .voltageIndex()
                        <= part.tier()
                            .voltageIndex(),
                    part + " takes " + in.id() + " above its tier");
                if (in.isFluid()) {
                    fluids++;
                } else {
                    items++;
                }
            }
            assertTrue(items <= 9 && fluids <= 1, part.toString());
            assertTrue(part.outputCount() >= 1 && part.unitCost() > 0.0);
        }
    }

    @Test
    void napalmIsACanisterPlusGasolineAndNothingElse() {
        IcbmPart napalm = IcbmPart.NAPALM_CANISTER;
        assertEquals(
            2,
            napalm.inputs()
                .size());
        assertEquals(
            Ing.Src.PART,
            napalm.inputs()
                .get(0)
                .src());
        assertEquals(
            "DISPERSAL_CANISTER",
            napalm.inputs()
                .get(0)
                .id());
        assertEquals(
            "Gasoline",
            napalm.inputs()
                .get(1)
                .id());
    }

    @Test
    void plasticExplosiveIsPtfeAndIndustrialTnt() {
        IcbmPart pe = IcbmPart.PLASTIC_EXPLOSIVE;
        assertEquals(
            "plate:Polytetrafluoroethylene",
            pe.inputs()
                .get(0)
                .id());
        assertEquals(
            Ing.Src.INDUSTRIAL_TNT,
            pe.inputs()
                .get(1)
                .src());
    }

    @Test
    void t2PartsAreEvAndT1PartsAtMostHv() {
        assertEquals(com.gregguns.api.Tier.EV, IcbmPart.FUZE_ASSEMBLY_MK2.tier());
        assertEquals(com.gregguns.api.Tier.EV, IcbmPart.FRAGMENTATION_SLEEVE_TITANIUM.tier());
        assertEquals(com.gregguns.api.Tier.HV, IcbmPart.FUZE_ASSEMBLY_MK1.tier());
    }
}
