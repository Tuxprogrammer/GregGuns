package com.gregguns.icbm.silo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;

import com.gregguns.GregGuns;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.icbm.missile.EntityMissile;
import com.gregguns.icbm.missile.MissileShotsScenario;
import com.gregguns.icbm.missile.MissileTier;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** {@code testing.icbm_i4_silo}: builds a silo with dark concrete and a wall hatch and leaves it standing. Dev only. */
final class I4SiloSetup implements Scenario {

    @Override
    public String name() {
        return "testing.icbm_i4_silo";
    }

    @Override
    public String description() {
        return "Builds a LuV silo with mixed dark and light concrete, a hatch in a wall and a T3 missile on the pad, then leaves it for a client session. Not part of test all.";
    }

    @Override
    public boolean includedInAll() {
        return false;
    }

    @Override
    public int areaRadius() {
        return 14;
    }

    @Override
    public ScenarioResult run(ScenarioContext harness) {
        final ScenarioContext ctx = com.gregguns.icbm.missile.I4Anchor.of(harness, 80, 0);
        if (SiloScenarios.gate() != null) {
            return ScenarioResult.skip(name(), SiloScenarios.gate());
        }
        @SuppressWarnings("unchecked")
        List<EntityPlayerMP> players = new ArrayList<>(
            MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList);
        SiloScenarios.build(ctx, ItemList.Hatch_Energy_LuV);
        final World w = ctx.world();
        final int x = ctx.originX();
        final int y = ctx.originY() + SiloScenarios.CY;
        final int z = ctx.originZ();
        w.setWorldTime(6000L);
        final MissileShotsScenario.Plan plan = new MissileShotsScenario.Plan();
        plan.after(60, () -> {
            net.minecraft.block.Block conc = GregTechAPI.sBlockConcretes;
            // A dark checker over every concrete cell (metas 0 to 7 are dark), mixed with the builder's light.
            int light = 0;
            int dark = 0;
            for (int dy = -3; dy <= 12; dy++) {
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        if (w.getBlock(x + dx, y + dy, z + dz) == conc) {
                            if ((dx + dy + dz) % 2 == 0) {
                                w.setBlock(x + dx, y + dy, z + dz, conc, 6, 2);
                                dark++;
                            } else {
                                light++;
                            }
                        }
                    }
                }
            }
            GregGuns.LOG.info("I4C silo concrete light={} dark={}", light, dark);
            int hatchId = ItemList.Hatch_Input_Bus_HV.get(1)
                .getItemDamage();
            w.setBlock(x - 1, y + 2, z, GregTechAPI.sBlockMachines, hatchId, 3);
            ((IGregTechTileEntity) w.getTileEntity(x - 1, y + 2, z)).setInitialValuesAsNBT(null, (short) hatchId);
        });
        plan.after(40, () -> {
            SiloScenarios.power(ctx);
            EntityMissile m = SiloScenarios.stand(ctx, MissileTier.T3);
            m.configure(0, "nuclear", 4, 3, null);
            m.fill(m.capacityMb(), true);
            GregGuns.LOG.info(
                "I4C silo formed={} origin={},{},{} controllerY={}",
                SiloScenarios.formed(ctx),
                x,
                ctx.originY(),
                z,
                y);
            for (EntityPlayerMP p : players) {
                p.setGameType(WorldSettings.GameType.CREATIVE);
                MissileShotsScenario.cam(p, x + 0.5, y - 1.0 + 1.6, z - 9.0, x + 0.5, y - 2.0, z + 2.5);
            }
        });
        return ScenarioResult.pending(name(), 400, t -> {
            plan.run(t);
            return plan.idle() ? ScenarioResult.builder(name())
                .build() : null;
        });
    }
}
