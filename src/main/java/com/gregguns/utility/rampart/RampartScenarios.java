package com.gregguns.utility.rampart;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gregguns.GregGuns;
import com.gregguns.api.Tier;
import com.gregguns.block.GregGunsCasings;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioArea;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.item.gun.GunItems;
import com.gregguns.machine.GregGunsMTEIDs;
import com.gregguns.utility.security.SecurityLevel;
import com.gregguns.utility.turret.entity.TurretEntity;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

/**
 * In-game scenarios for the Rampart and the Gun Mount Hatch ({@code utility.rampart_*}): registration facts and
 * recipes, the wall forming at 2, 6 and 10 mounts, a mounted gun that kills a zombie on the shared ammo feed, and a
 * broken hatch that kills its turret. The machines are real GT blocks ticking in the world. Dev environment only,
 * registered from {@code RampartRegistration}. Server side.
 */
// Spec: docs/gregguns-spec/08-machines-production/multiblocks.md (Rampart, GGS-08-74 to GGS-08-81),
// docs/gregguns-spec/09-machines-utility/recipes.md (GGS-09-35 to GGS-09-37)
public final class RampartScenarios {

    private static final int AREA_RADIUS = ScenarioArea.RADIUS;
    private static final String OWNER_NAME = "gregguns-rampart-owner";

    /** Ticks to let GT finish initializing freshly placed tile entities before they are used. */
    private static final int WARMUP_TICKS = 45;

    private RampartScenarios() {}

    /**
     * @param registry where to add the scenarios
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new RegisteredScenario());
        registry.register(new AssemblesScenario());
        registry.register(new MountFiresScenario());
        registry.register(new BreakHatchScenario());
    }

    // --- helpers ----------------------------------------------------------------------------------------------

    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    private static FakePlayer player(WorldServer world, String name) {
        return new FakePlayer(world, new GameProfile(uuid(name), name));
    }

    private static IGregTechTileEntity place(ScenarioContext ctx, int dx, int dy, int dz, int id,
        ForgeDirection facing) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[id];
        ctx.setBlock(dx, dy, dz, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType());
        TileEntity tile = ctx.world()
            .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
        if (!(tile instanceof IGregTechTileEntity)) {
            throw new IllegalStateException("no GT tile entity at offset " + dx + "," + dy + "," + dz);
        }
        IGregTechTileEntity base = (IGregTechTileEntity) tile;
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(facing);
        base.setOwnerName(OWNER_NAME);
        base.setOwnerUuid(uuid(OWNER_NAME));
        return base;
    }

    private static IGregTechTileEntity hatch(ScenarioContext ctx, int dx, int dy, int dz, ItemList item) {
        return place(
            ctx,
            dx,
            dy,
            dz,
            item.get(1)
                .getItemDamage(),
            ForgeDirection.NORTH);
    }

    private static void empty(IGregTechTileEntity... tiles) {
        for (IGregTechTileEntity tile : tiles) {
            if (tile != null) {
                for (int i = 0; i < tile.getSizeInventory(); i++) {
                    tile.setInventorySlotContents(i, null);
                }
            }
        }
    }

    private static int countOf(IGregTechTileEntity tile, Item item) {
        int total = 0;
        for (int i = 0; i < tile.getSizeInventory(); i++) {
            ItemStack s = tile.getStackInSlot(i);
            if (s != null && s.getItem() == item) {
                total += s.stackSize;
            }
        }
        return total;
    }

    private static void noStray(ScenarioContext ctx, ScenarioResult.Builder out) {
        int stray = 0;
        for (Object o : new ArrayList<>(ctx.world().loadedEntityList)) {
            if (!(o instanceof EntityItem)) {
                continue;
            }
            EntityItem e = (EntityItem) o;
            if (Math.abs(e.posX - ctx.originX()) <= AREA_RADIUS && Math.abs(e.posZ - ctx.originZ()) <= AREA_RADIUS
                && e.posY >= ctx.originY() - 1
                && e.posY < ctx.originY() + ScenarioArea.CLEAR_HEIGHT) {
                stray++;
                e.setDead();
            }
        }
        out.checkEquals("stray item entities in the arena", 0, stray);
    }

    private static boolean hasCraftingRecipe(ItemStack result) {
        for (Object o : net.minecraft.item.crafting.CraftingManager.getInstance()
            .getRecipeList()) {
            ItemStack made = ((net.minecraft.item.crafting.IRecipe) o).getRecipeOutput();
            if (made != null && made.getItem() == result.getItem() && made.getItemDamage() == result.getItemDamage()) {
                return true;
            }
        }
        return false;
    }

    private static GTRecipe findAssembler(ItemStack result) {
        for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            if (recipe.mOutputs != null && recipe.mOutputs.length > 0
                && recipe.mOutputs[0] != null
                && recipe.mOutputs[0].getItem() == result.getItem()
                && recipe.mOutputs[0].getItemDamage() == result.getItemDamage()) {
                return recipe;
            }
        }
        return null;
    }

    /** A Rampart at the origin: controller, hologram-built wall, hatches. */
    private static final class Wall {

        /** Height of the controller above the origin; the floor is two lower. */
        static final int CY = 3;
        final ScenarioContext ctx;
        final int mounts;
        IGregTechTileEntity controller;
        MTERampart mte;
        final int dir;
        IGregTechTileEntity inputBus;
        IGregTechTileEntity outputBus;
        IGregTechTileEntity energy;
        IGregTechTileEntity maintenance;
        final List<IGregTechTileEntity> mountHatches = new ArrayList<>();

        Wall(ScenarioContext ctx, int mounts) {
            this.ctx = ctx;
            this.mounts = mounts;
            controller = place(ctx, 0, CY, 0, GregGunsMTEIDs.RAMPART, ForgeDirection.NORTH);
            mte = (MTERampart) controller.getMetaTileEntity();
            mte.construct(new ItemStack(Items.stick, mounts), false);
            // The wall runs to one side of the controller; the first bay's solid sill row tells which.
            Block probe = ctx.world()
                .getBlock(ctx.originX() + RampartShape.hatchDistance(0), ctx.originY() + CY, ctx.originZ());
            dir = probe == GregGunsCasings.INSTANCE ? 1 : -1;
        }

        int hatchX(int mount) {
            return dir * RampartShape.hatchDistance(mount);
        }

        int hatchY() {
            return CY + RampartShape.hatchRise();
        }

        void addHatches() {
            inputBus = hatch(ctx, -1, CY, 0, ItemList.Hatch_Input_Bus_HV);
            maintenance = hatch(ctx, 1, CY, 0, ItemList.Hatch_Maintenance);
            energy = hatch(ctx, 0, CY - 1, 0, ItemList.Hatch_Energy_HV);
            outputBus = hatch(ctx, 0, CY - 1, 1, ItemList.Hatch_Output_Bus_HV);
            for (int i = 0; i < mounts; i++) {
                placeMountHatch(i);
            }
        }

        IGregTechTileEntity placeMountHatch(int mount) {
            IGregTechTileEntity h = place(
                ctx,
                hatchX(mount),
                hatchY(),
                0,
                GregGunsMTEIDs.GUN_MOUNT_HATCH_MV,
                ForgeDirection.NORTH);
            while (mountHatches.size() <= mount) {
                mountHatches.add(null);
            }
            mountHatches.set(mount, h);
            return h;
        }

        private IGregTechTileEntity tileAt(int dx, int dy, int dz, IGregTechTileEntity fallback) {
            if (fallback == null) {
                return null;
            }
            TileEntity tile = ctx.world()
                .getTileEntity(ctx.originX() + dx, ctx.originY() + dy, ctx.originZ() + dz);
            return tile instanceof IGregTechTileEntity ? (IGregTechTileEntity) tile : fallback;
        }

        /**
         * Re-fetches every tile entity from the world. GT may swap a tile entity's meta tile during its first ticks, so
         * references taken at placement can go stale under server lag. Call it after the warm-up.
         */
        void refresh() {
            controller = tileAt(0, CY, 0, controller);
            mte = (MTERampart) controller.getMetaTileEntity();
            inputBus = tileAt(-1, CY, 0, inputBus);
            maintenance = tileAt(1, CY, 0, maintenance);
            energy = tileAt(0, CY - 1, 0, energy);
            outputBus = tileAt(0, CY - 1, 1, outputBus);
            for (int i = 0; i < mountHatches.size(); i++) {
                mountHatches.set(i, tileAt(hatchX(i), hatchY(), 0, mountHatches.get(i)));
            }
        }

        boolean form() {
            mte.checkStructure(true, controller);
            return mte.mMachine;
        }

        void fixMaintenance() {
            mte.mWrench = true;
            mte.mScrewdriver = true;
            mte.mSoftMallet = true;
            mte.mHardHammer = true;
            mte.mSolderingTool = true;
            mte.mCrowbar = true;
        }

        void power() {
            // No cable network feeds the hatch, so keep its buffer full.
            energy.increaseStoredEnergyUnits(Tier.HV.voltage() * 20, true);
        }

        MTEGunMountHatch hatchMte(int mount) {
            return (MTEGunMountHatch) mountHatches.get(mount)
                .getMetaTileEntity();
        }

        void teardown() {
            empty(inputBus, outputBus);
            for (IGregTechTileEntity h : mountHatches) {
                empty(h);
            }
            int reach = RampartShape.totalLength(mounts) + 1;
            for (int x = -reach; x <= reach; x++) {
                for (int y = CY - 2; y <= CY + 6; y++) {
                    for (int z = 0; z < RampartShape.DEPTH; z++) {
                        ctx.setBlock(x, y, z, Blocks.air, 0);
                    }
                }
            }
        }
    }

    // --- scenarios --------------------------------------------------------------------------------------------

    /** Registration facts and recipes. */
    private static final class RegisteredScenario implements Scenario {

        @Override
        public String name() {
            return "utility.rampart_registered";
        }

        @Override
        public String description() {
            return "The Rampart and the four Gun Mount Hatches are registered at the right ids and tiers; Bastion Plating has four metas with an Assembler recipe each, the controller a hand recipe and every hatch an Assembler recipe.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (GregGunsCasings.INSTANCE == null) {
                return ScenarioResult.fail(name(), "the casing block is missing (needs the guns module)");
            }
            Object rampart = GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.RAMPART];
            out.check(rampart instanceof MTERampart, "MTE 20120 should be an MTERampart");
            if (rampart instanceof MTERampart) {
                out.check(
                    hasCraftingRecipe(((MTERampart) rampart).getStackForm(1)),
                    "the Rampart controller needs a hand recipe");
            }
            for (RampartRecipeSpec.Row row : RampartRecipeSpec.Row.values()) {
                Object h = GregTechAPI.METATILEENTITIES[row.mteId()];
                out.check(
                    h instanceof MTEGunMountHatch,
                    row.tier() + " Gun Mount Hatch (" + row.mteId() + ") should be a MTEGunMountHatch");
                if (h instanceof MTEGunMountHatch) {
                    out.checkEquals(
                        row.tier() + " hatch tier",
                        row.tier()
                            .voltageIndex(),
                        ((MTEGunMountHatch) h).hatchTierIndex());
                    GTRecipe recipe = findAssembler(((MTEGunMountHatch) h).getStackForm(1));
                    out.check(recipe != null, row.tier() + " hatch needs an Assembler recipe");
                    if (recipe != null) {
                        out.checkEquals(row.tier() + " hatch EU/t", (int) row.recipeEut(), recipe.mEUt);
                    }
                }
            }
            for (BastionPlating plating : BastionPlating.values()) {
                ItemStack block = new ItemStack(GregGunsCasings.INSTANCE, 1, plating.meta());
                GTRecipe recipe = findAssembler(block);
                out.check(recipe != null, plating + " Bastion Plating needs an Assembler recipe");
                if (recipe != null) {
                    out.checkEquals(plating + " plating EU/t", (int) plating.recipeEut(), recipe.mEUt);
                    out.checkEquals(plating + " plating yield", BastionPlating.YIELD, recipe.mOutputs[0].stackSize);
                    out.checkEquals(plating + " plating ticks", BastionPlating.DURATION_TICKS, recipe.mDuration);
                }
            }
            return out.build();
        }
    }

    /** The wall forms at its sizes, and the shape rules hold. */
    private static final class AssemblesScenario implements Scenario {

        @Override
        public String name() {
            return "utility.rampart_assembles";
        }

        @Override
        public String description() {
            return "A Rampart built by the controller's hologram routine forms at 2, 6 and 10 mounts with Bastion Plating all around, reports its mounts and armor, is open in its crenels, fails for a missing mount hatch, a blocked crenel or a missing Energy Hatch, takes its armor from the lowest plating, and shares one security setting with every mount.";
        }

        @Override
        public int areaRadius() {
            return AREA_RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (GregGunsCasings.INSTANCE == null
                || !(GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.RAMPART] instanceof MTERampart)) {
                return ScenarioResult.fail(name(), "the Rampart is not registered (needs GG_MODULES=guns,...,utility)");
            }
            for (int n : new int[] { RampartShape.MIN_MOUNTS, RampartShape.DEFAULT_MOUNTS, RampartShape.MAX_MOUNTS }) {
                Wall w = new Wall(ctx, n);
                w.addHatches();
                boolean formed = w.form();
                out.check(formed, "a " + n + "-mount Rampart with all hatches should form");
                out.checkEquals("mounts of the " + n + "-mount Rampart", n, w.mte.mountCount());
                out.checkEquals("armor of an all-Iron wall", 5, w.mte.armorValue());
                int last = w.dir * (RampartShape.totalLength(n) - 2);
                out.check(
                    ctx.world()
                        .getBlock(ctx.originX() + last, ctx.originY() + Wall.CY, ctx.originZ())
                        == GregGunsCasings.INSTANCE,
                    "the cap should be the last block of the " + n + "-mount wall");
                out.check(
                    ctx.world()
                        .isAirBlock(ctx.originX() + w.hatchX(0), ctx.originY() + Wall.CY + 2, ctx.originZ()),
                    "the crenel above a mount hatch should be open air (non-convex wall)");
                w.teardown();
            }
            out.check(RampartShape.isNonConvex(RampartShape.DEFAULT_MOUNTS), "the wall's shape should be non-convex");

            Wall w = new Wall(ctx, 2);
            w.addHatches();
            out.check(w.form(), "the two-mount Rampart should form");
            // GGS-08-75: a missing mount hatch is a structure error.
            int hx = w.hatchX(1);
            ctx.setBlock(hx, Wall.CY + 1, 0, Blocks.air, 0);
            out.check(!w.form(), "a missing Gun Mount Hatch should break the Rampart");
            out.checkEquals("mounts of a broken Rampart", 0, w.mte.mountCount());
            w.placeMountHatch(1);
            out.check(w.form(), "putting the hatch back should form the Rampart again");
            // The crenel must stay open.
            ctx.setBlock(hx, Wall.CY + 2, 0, Blocks.stone, 0);
            out.check(!w.form(), "a block in a crenel should break the Rampart");
            ctx.setBlock(hx, Wall.CY + 2, 0, Blocks.air, 0);
            out.check(w.form(), "clearing the crenel should form the Rampart again");
            // The Energy Hatch is required.
            ctx.setBlock(0, Wall.CY - 1, 0, GregGunsCasings.INSTANCE, GregGunsCasings.BASTION_IRON);
            out.check(!w.form(), "a Rampart without an Energy Hatch should not form");
            w.energy = hatch(ctx, 0, Wall.CY - 1, 0, ItemList.Hatch_Energy_HV);
            out.check(w.form(), "restoring the Energy Hatch should form the Rampart again");

            // GGS-08-78: the lowest plating meta gives the armor.
            swapPlating(ctx, w, GregGunsCasings.BASTION_IRON, GregGunsCasings.BASTION_STEEL);
            out.check(w.form(), "an all-Steel wall should form");
            out.checkEquals("armor of an all-Steel wall", 10, w.mte.armorValue());
            ctx.setBlock(w.dir * 2, Wall.CY, 0, GregGunsCasings.INSTANCE, GregGunsCasings.BASTION_CARBON);
            out.check(w.form(), "a wall with one Carbon block should form");
            out.checkEquals("armor with one Carbon block", 10, w.mte.armorValue());
            ctx.setBlock(w.dir * 2, Wall.CY, 0, GregGunsCasings.INSTANCE, GregGunsCasings.BASTION_IRON);
            out.check(w.form(), "a wall with one Iron block should form");
            out.checkEquals("armor with one Iron block (the lowest)", 5, w.mte.armorValue());
            out.checkEquals(
                "a mount hatch's armor",
                5,
                w.hatchMte(0)
                    .armorValue());

            // GGS-08-79: one setting on the controller, for every mount.
            WorldServer world = (WorldServer) ctx.world();
            FakePlayer owner = player(world, OWNER_NAME);
            FakePlayer stranger = player(world, "gregguns-rampart-stranger");
            out.check(!w.mte.cycleSecurity(stranger), "a stranger must not change the security level");
            SecurityLevel before = w.mte.security();
            out.check(w.mte.cycleSecurity(owner), "the owner may change the security level");
            out.check(before != w.mte.security(), "the security level should change");
            for (int i = 0; i < 2; i++) {
                out.checkEquals(
                    "security of mount " + i,
                    w.mte.security(),
                    w.hatchMte(i)
                        .security());
            }
            out.check(w.mte.toggleAnimals(owner), "the owner may toggle Target Animals");
            out.check(
                w.hatchMte(0)
                    .targetsAnimals()
                    && w.hatchMte(1)
                        .targetsAnimals(),
                "Target Animals should apply to every mount");
            w.teardown();
            noStray(ctx, out);
            return out.build();
        }

        private static void swapPlating(ScenarioContext ctx, Wall w, int from, int to) {
            int reach = RampartShape.totalLength(w.mounts) + 1;
            for (int x = -reach; x <= reach; x++) {
                for (int y = Wall.CY - 2; y <= Wall.CY + 6; y++) {
                    for (int z = 0; z < RampartShape.DEPTH; z++) {
                        Block b = ctx.world()
                            .getBlock(ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z);
                        if (b == GregGunsCasings.INSTANCE && ctx.world()
                            .getBlockMetadata(ctx.originX() + x, ctx.originY() + y, ctx.originZ() + z) == from) {
                            ctx.setBlock(x, y, z, GregGunsCasings.INSTANCE, to);
                        }
                    }
                }
            }
        }
    }

    /** A mounted gun and shared ammo kill a zombie; the empty magazine goes to the Output Bus. */
    private static final class MountFiresScenario implements Scenario {

        @Override
        public String name() {
            return "utility.rampart_mount_fires";
        }

        @Override
        public String description() {
            return "A two-mount Rampart with a 9mm Pistol in one Gun Mount Hatch and magazines in the Input Bus shoots a zombie dead; the empty magazine lands in the Output Bus, the turret has the plating's armor, and the controller draws 4 EU/t per mount.";
        }

        @Override
        public int areaRadius() {
            return AREA_RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (GregGunsCasings.INSTANCE == null || GunItems.get("pistol_9mm") == null
                || !(GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.RAMPART] instanceof MTERampart)) {
                return ScenarioResult.fail(name(), "needs GG_MODULES=guns,...,utility");
            }
            Wall w = new Wall(ctx, 2);
            w.addHatches();
            Item mag = GameRegistry.findItem(GregGuns.MODID, "mag_9mm_pistol");
            Item emptyMag = GameRegistry.findItem(GregGuns.MODID, "mag_9mm_pistol_empty");
            ItemStack gun = new ItemStack(GunItems.get("pistol_9mm"));
            // One round loaded, so the second shot reloads from the bus and hands back an empty magazine.
            gun.setTagCompound(new NBTTagCompound());
            GunNbt.write(gun.getTagCompound(), new GunState(1));
            // A platform in front of the wall at hatch height, and a zombie on it.
            int px = w.hatchX(0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlock(px + dx, Wall.CY + 1, -9 + dz, Blocks.stone, 0);
                }
            }
            EntityZombie zombie = new EntityZombie(ctx.world());
            zombie.setLocationAndAngles(
                ctx.originX() + px + 0.5,
                ctx.originY() + Wall.CY + 2,
                ctx.originZ() - 9 + 0.5,
                0.0F,
                0.0F);
            zombie.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 4000));
            long[] drawSeen = { 0 };
            boolean[] armed = { false };
            return ScenarioResult.pending(name(), 900 + WARMUP_TICKS, ticks -> {
                // Wait for GT to settle the new tiles, re-fetch them, form the wall, and only then load and spawn.
                if (!armed[0]) {
                    if (ticks < WARMUP_TICKS) {
                        return null;
                    }
                    w.refresh();
                    if (!w.form()) {
                        if (ticks < WARMUP_TICKS + 40) {
                            return null;
                        }
                        w.teardown();
                        return ScenarioResult.fail(name(), "the two-mount Rampart should form");
                    }
                    w.fixMaintenance();
                    w.mountHatches.get(0)
                        .setInventorySlotContents(MTEGunMountHatch.GUN_SLOT, gun);
                    w.inputBus.setInventorySlotContents(0, new ItemStack(mag, 4));
                    ctx.spawn(zombie);
                    armed[0] = true;
                    return null;
                }
                w.power();
                if (w.mte.mountsRunning() && drawSeen[0] == 0) {
                    drawSeen[0] = -w.mte.lEUt;
                }
                if (!zombie.isDead && zombie.getHealth() > 0) {
                    return null;
                }
                ScenarioResult.Builder out = ScenarioResult.builder(name());
                TurretEntity turret = w.hatchMte(0)
                    .linkedTurret();
                out.check(turret != null, "mount 0 should have a turret");
                if (turret != null) {
                    out.checkEquals("the turret's armor value (all-Iron plating)", 5, turret.getTotalArmorValue());
                    out.check(turret.host() == w.hatchMte(0), "the turret's host should be the mount hatch");
                }
                out.check(
                    w.hatchMte(1)
                        .linkedTurret() != null,
                    "mount 1 should have a turret too");
                out.checkEquals("EU/t drawn by two mounts", 8L, drawSeen[0]);
                out.checkEquals("empty magazines on the Output Bus", 1, countOf(w.outputBus, emptyMag));
                out.checkEquals("magazines left on the Input Bus", 3, countOf(w.inputBus, mag));
                ItemStack mounted = w.mountHatches.get(0)
                    .getStackInSlot(MTEGunMountHatch.GUN_SLOT);
                out.check(mounted != null, "the gun should stay in its hatch");
                w.teardown();
                // The zombie's own drops are not stray items; the Output Bus assertion above covers the empties.

                return out.build();
            });
        }
    }

    /** Breaking a hatch kills its turret at once; breaking the structure kills the rest. */
    private static final class BreakHatchScenario implements Scenario {

        @Override
        public String name() {
            return "utility.rampart_break_hatch";
        }

        @Override
        public String description() {
            return "In a powered two-mount Rampart each hatch keeps a turret; breaking one hatch kills only its turret, and the next structure check, which fails, kills the other.";
        }

        @Override
        public int areaRadius() {
            return AREA_RADIUS;
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            if (GregGunsCasings.INSTANCE == null
                || !(GregTechAPI.METATILEENTITIES[GregGunsMTEIDs.RAMPART] instanceof MTERampart)) {
                return ScenarioResult.fail(name(), "needs GG_MODULES=guns,...,utility");
            }
            Wall w = new Wall(ctx, 2);
            w.addHatches();
            TurretEntity[] turrets = new TurretEntity[2];
            int[] stage = { -1 };
            int[] brokenAt = { 0 };
            return ScenarioResult.pending(name(), 600 + WARMUP_TICKS, ticks -> {
                if (stage[0] < 0) {
                    // Wait out GT's warm-up, then re-fetch every tile and form the wall.
                    if (ticks < WARMUP_TICKS) {
                        return null;
                    }
                    w.refresh();
                    if (!w.form()) {
                        if (ticks < WARMUP_TICKS + 40) {
                            return null;
                        }
                        w.teardown();
                        return ScenarioResult.fail(name(), "the two-mount Rampart should form");
                    }
                    w.fixMaintenance();
                    stage[0] = 0;
                    return null;
                }
                w.power();
                switch (stage[0]) {
                    case 0:
                        turrets[0] = w.hatchMte(0)
                            .linkedTurret();
                        turrets[1] = w.hatchMte(1)
                            .linkedTurret();
                        if (turrets[0] == null || turrets[1] == null) {
                            return null;
                        }
                        ctx.setBlock(w.hatchX(0), Wall.CY + 1, 0, Blocks.air, 0);
                        stage[0] = 1;
                        brokenAt[0] = ticks;
                        return null;
                    case 1:
                        // Wait on state: the kill follows the hatch's removal, which a lagging server may take
                        // a few ticks to process.
                        if (!turrets[0].isDead && ticks < brokenAt[0] + 100) {
                            return null;
                        }
                        ScenarioResult.Builder out = ScenarioResult.builder(name());
                        out.check(turrets[0].isDead, "breaking the hatch must kill that mount's turret");
                        out.check(
                            !turrets[1].isDead,
                            "the other mount's turret stays until the structure is rechecked");
                        out.check(!w.form(), "a Rampart without a mount hatch should not form");
                        out.check(turrets[1].isDead, "a failed structure check must kill the remaining turret");
                        out.checkEquals("mounts of a broken Rampart", 0, w.mte.mountCount());
                        w.teardown();
                        noStray(ctx, out);
                        return out.build();
                    default:
                        return null;
                }
            });
        }
    }
}
