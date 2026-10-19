package com.gregguns.gun.server;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.api.Tier;
import com.gregguns.armor.slots.ElectricGear;
import com.gregguns.combat.DamageType;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.AmmoInventory;
import com.gregguns.gun.logic.ChargeSource;
import com.gregguns.gun.logic.EnergyMath;
import com.gregguns.gun.logic.FireControl;
import com.gregguns.gun.logic.FireMode;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.item.gun.ItemEnergyGun;
import com.gregguns.item.gun.StackCharge;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicBatteryBuffer;

/**
 * {@code gun.energy.item} and {@code gun.energy.charger}: {@link ItemEnergyGun} as a real IC2 and GT electric item.
 * The probe gun is built in the scenario (no gun ships as an energy gun yet) and never registered. Dev environment
 * only.
 */
// Spec: docs/gregguns-spec/02-guns/energy-electric-items.md (GGS-02-09 to GGS-02-16); design 0008
public final class EnergyGunScenarios {

    private static final long EU_PER_SHOT = 1000;
    private static final long CAPACITY = 100_000;
    private static final int WAIT_TICKS = 60;

    private EnergyGunScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry where to add them
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Item());
        registry.register(new Charger());
    }

    private static ItemEnergyGun probe() {
        GunStats stats = GunStats.builder("energy_item_probe")
            .energy(EU_PER_SHOT, CAPACITY)
            .fire(FireMode.AUTO, 2)
            .build();
        return new ItemEnergyGun(
            GunDefinition.of("energy_item_probe", stats)
                .tier(Tier.LV)
                .damageType(DamageType.ENERGY)
                .build());
    }

    private static final class Item implements Scenario {

        @Override
        public String name() {
            return "gun.energy.item";
        }

        @Override
        public String description() {
            return "An LV energy gun item reports IC2 tier 1, a 32 EU/t transfer limit, no energy output and its capacity; it charges, shows the charge on its bar, and a real GunController shot spends exactly its EU per shot from the stack until it clicks dry.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemEnergyGun gun = probe();
            ItemStack stack = new ItemStack(gun);
            out.checkEquals("IC2 tier is the LV voltage index", 1, gun.getTier(stack));
            out.checkEquals("transfer limit is the LV voltage", 32.0, gun.getTransferLimit(stack));
            out.check(!gun.canProvideEnergy(stack), "a gun never gives energy back");
            out.checkEquals("capacity", (double) gun.capacityEu(), gun.getMaxCharge(stack));
            out.checkEquals("a fresh gun is empty", 0L, gun.chargeOf(stack));
            out.checkEquals("empty bar", 1.0, gun.getDurabilityForDisplay(stack));

            double taken = ElectricGear.charge(stack, 3500, Tier.LV);
            out.checkEquals("EU accepted", 3500.0, taken);
            out.checkEquals("stored EU", 3500L, gun.chargeOf(stack));
            out.check(
                Math.abs(gun.getDurabilityForDisplay(stack) - EnergyMath.barFraction(3500, gun.capacityEu())) < 1e-9,
                "the bar shows the charge");

            GunStats stats = gun.stats();
            GunController controller = new GunController(new FireControl(0, 0));
            GunState state = GunState.full(stats);
            AmmoInventory ammo = new AmmoInventory() {

                @Override
                public int count(String ammoType) {
                    return 0;
                }

                @Override
                public int withdraw(String ammoType, int maxItems) {
                    return 0;
                }

                @Override
                public void depositItems(String ammoType, int items) {}

                @Override
                public void depositRounds(String ammoType, int rounds) {}

                @Override
                public ChargeSource charge() {
                    return StackCharge.spending(stack, null);
                }
            };
            int fired = 0;
            int dry = 0;
            controller.triggerPressed();
            for (int i = 0; i < 30; i++) {
                var r = controller.tick(stats, state, ammo, true, false);
                fired += r.shot()
                    .wasFired() ? 1 : 0;
                dry += r.outOfAmmo() ? 1 : 0;
            }
            out.checkEquals("shots from 3500 EU at 1000 each", 3, fired);
            out.checkEquals("EU left after three shots", 500L, gun.chargeOf(stack));
            out.checkEquals("one dry click", 1, dry);
            return out.build();
        }
    }

    private static final class Slot {

        final String label;
        final IGregTechTileEntity tile;
        final ItemStack stack;
        final boolean matching;

        Slot(String label, IGregTechTileEntity tile, ItemStack stack, boolean matching) {
            this.label = label;
            this.tile = tile;
            this.stack = stack;
            this.matching = matching;
        }
    }

    private static IGregTechTileEntity place(ScenarioContext ctx, int dx, int id) {
        Block block = GregTechAPI.sBlockMachines;
        ctx.setBlock(dx, 1, 0, block, GregTechAPI.METATILEENTITIES[id].getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + 1, ctx.originZ());
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity base = (IGregTechTileEntity) tile;
            base.setInitialValuesAsNBT(null, (short) id);
            return base;
        }
        return null;
    }

    private static final class Charger implements Scenario {

        @Override
        public String name() {
            return "gun.energy.charger";
        }

        @Override
        public String description() {
            return "An LV energy gun is accepted and charged by the LV Battery Buffer; the MV buffer refuses it and a forced-in copy stays empty (exact tier, design 0008).";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            ItemEnergyGun gun = probe();
            ItemList[] buffers = { ItemList.Battery_Buffer_1by1_LV, ItemList.Battery_Buffer_1by1_MV };
            String[] labels = { "LV", "MV" };
            java.util.List<Slot> slots = new java.util.ArrayList<>();
            for (int i = 0; i < buffers.length; i++) {
                int id = buffers[i].get(1)
                    .getItemDamage();
                IGregTechTileEntity tile = place(ctx, i * 2, id);
                if (tile == null || !(tile.getMetaTileEntity() instanceof MTEBasicBatteryBuffer)) {
                    return ScenarioResult.fail(name(), "the " + labels[i] + " battery buffer did not place");
                }
                boolean matching = i == 0;
                ItemStack stack = new ItemStack(gun);
                String label = "energy gun in the " + labels[i] + " buffer";
                boolean accepted = ((MTEBasicBatteryBuffer) tile.getMetaTileEntity())
                    .allowPutStack(tile, 0, ForgeDirection.UP, stack);
                out.check(accepted == matching, label + (matching ? " must be accepted" : " must be refused"));
                tile.setInventorySlotContents(0, stack);
                ((BaseMetaTileEntity) tile).setStoredEU(tile.getEUCapacity());
                slots.add(new Slot(label, tile, stack, matching));
            }
            return ScenarioResult.pending(name(), WAIT_TICKS + 5, ticks -> {
                if (ticks < WAIT_TICKS) {
                    return null;
                }
                for (Slot s : slots) {
                    long charge = gun.chargeOf(s.stack);
                    if (s.matching) {
                        out.check(charge > 0, s.label + " should have charged, has " + charge + " EU");
                        out.check(charge <= gun.capacityEu(), s.label + " charged beyond its capacity");
                    } else {
                        out.check(charge == 0, s.label + " must stay empty, has " + charge + " EU");
                    }
                }
                return out.build();
            });
        }
    }
}
