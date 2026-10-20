package com.gregguns.item.gun;

import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import com.google.common.collect.Multimap;
import com.gregguns.explosion.BlockBreakGuard;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.ToolDrain;
import com.gregguns.gun.logic.ToolStats;
import com.gregguns.gun.server.GunPlayerData;
import com.gregguns.gun.sound.GunSoundPlayer;
import com.gregguns.item.melee.MeleeStrike;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A tool gun: the Chainsaw and the Power Hammers. It is an {@link ItemGun} whose <em>left</em> button is vanilla melee
 * and mining and whose <em>right</em> button is the shot (a plain repeating shot, or a charged one for a gun built
 * with {@code GunStats.Builder.charge}). Everything is paid from the rounds loaded, which stand for the fuel or air of
 * the tank swapped in on reload (the empty tank comes back through the normal reload path).
 *
 * <h2>What the item does</h2>
 * <ul>
 * <li><b>Melee</b> (left click on an entity): {@link MeleeStrike} with the attack attribute at
 * {@link ToolStats#meleeDamage()} while a round is loaded and {@link ToolStats#emptyMeleeDamage()} when empty, as a
 * GregGuns PHYSICAL hit with the tool's armor penetration. One hit spends {@link ToolStats#hitCostMilli()}.</li>
 * <li><b>Mining</b> (left click on a block): the item reports the tool classes, harvest level and dig speed of its
 * {@link ToolStats} while loaded and none of them when empty, so Forge's own per-metadata checks decide what it can
 * harvest. Each block it breaks spends {@link ToolStats#blockCostMilli()}. The vanilla break already posts Forge's
 * {@code BlockEvent.BreakEvent}, so protection mods see it.</li>
 * <li><b>Breaking on the player's behalf</b> (a hammer shockwave, the Chainsaw's cut): {@link #breakBlock} asks
 * {@link BlockBreakGuard} first, spends the cost, then breaks and harvests. Use it for every such break.</li>
 * <li><b>Swing sound</b>: {@code guns.<name>.swing}, at most once per {@link ToolStats#swingSoundGapTicks()} ticks per
 * player, only while a round is loaded.</li>
 * </ul>
 *
 * <p>
 * The fraction of a round already spent by a cheap action is kept in the stack's NBT ({@link #KEY_REMAINDER}) next to
 * the rounds; see {@link ToolDrain}. Server side drains; both sides read. Not creative-draining: a creative player
 * spends nothing.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md section 9; docs/gregguns-spec/02-guns/tool-guns.md
// Deviation: GGS-02-32 (fuel-value scaling of the rounds per tank) stays deferred (plan Q7); a full tank is a fixed
// number of rounds.
public class ItemToolGun extends ItemGun {

    /** Key of the spent fraction of the current round, thousandths (short). */
    public static final String KEY_REMAINDER = "toolrem";

    /** What {@link #breakBlock} did. */
    public enum BreakResult {
        /** The block was removed (and harvested when the tool may). */
        BROKEN,
        /** A protection listener cancelled the break; nothing was spent. */
        DENIED,
        /** No round loaded; nothing was done. */
        EMPTY,
        /** Air, or a block that cannot be broken; nothing was spent. */
        NOT_BREAKABLE
    }

    private final ToolStats tool;

    /**
     * @param definition a definition built with {@code tool(..)}
     * @throws IllegalArgumentException if the definition has no tool data
     */
    public ItemToolGun(GunDefinition definition) {
        super(definition);
        this.tool = definition.tool();
        if (tool == null) {
            throw new IllegalArgumentException(definition.name() + " is not a tool gun");
        }
        // Register the classes the standard Forge way, so the item's own table holds them and a pack mod that
        // retunes a level through setHarvestLevel (IguanaTweaks) is read back by getHarvestLevel below.
        for (String toolClass : tool.toolClasses()) {
            super.setHarvestLevel(toolClass, tool.harvestLevel(toolClass));
        }
    }

    /** @return the melee, mining and drain data */
    public ToolStats tool() {
        return tool;
    }

    /**
     * @param stack a stack of this gun
     * @return true when at least one round is loaded
     */
    public boolean hasFuel(ItemStack stack) {
        return stateOf(stack).loaded() > 0;
    }

    // ----- drain -----------------------------------------------------------------------------------------------

    /**
     * Spends part of a round from the stack (server only; the caller skips this for a creative player).
     *
     * @param stack     a stack of this gun
     * @param costMilli the cost in thousandths of a round
     * @return true when a round was loaded to pay with, false when the gun was empty and nothing changed
     */
    public boolean drain(ItemStack stack, int costMilli) {
        ensureTag(stack, stats());
        NBTTagCompound tag = stack.getTagCompound();
        GunState state = GunNbt.read(tag, stats());
        ToolDrain.Spend spend = ToolDrain.spend(state.loaded(), tag.getShort(KEY_REMAINDER), costMilli);
        if (!spend.paid()) {
            tag.removeTag(KEY_REMAINDER);
            return false;
        }
        if (spend.rounds() > 0) {
            state.use(spend.rounds());
            GunNbt.write(tag, state);
        }
        if (spend.remainderMilli() > 0) {
            tag.setShort(KEY_REMAINDER, (short) spend.remainderMilli());
        } else {
            tag.removeTag(KEY_REMAINDER);
        }
        return true;
    }

    /**
     * @param stack a stack of this gun
     * @return the stored spent fraction of the current round, thousandths
     */
    public int remainderMilli(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null ? 0 : tag.getShort(KEY_REMAINDER);
    }

    private static boolean spends(EntityLivingBase who) {
        return who instanceof EntityPlayer && !who.worldObj.isRemote
            && !((EntityPlayer) who).capabilities.isCreativeMode;
    }

    // ----- breaking blocks for the player ----------------------------------------------------------------------

    /**
     * Breaks one block on the player's behalf, as the hammer shockwave and the Chainsaw's cut do (server only). Asks
     * {@link BlockBreakGuard} (so claims and protection mods can refuse), then spends
     * {@link ToolStats#blockCostMilli()}, removes the block and, when the tool class and level allow, drops its
     * items. Nothing is spent and nothing changes unless the result is {@link BreakResult#BROKEN}.
     *
     * @param player the player the break is for
     * @param stack  the player's tool gun stack (spent from)
     * @param x      block X
     * @param y      block Y
     * @param z      block Z
     * @return what happened
     */
    // Source: the removal sequence is ItemInWorldManager.tryHarvestBlock's, 1.7.10: onBlockHarvested,
    // removedByPlayer, onBlockDestroyedByPlayer, then harvestBlock when the tool may.
    public BreakResult breakBlock(EntityPlayerMP player, ItemStack stack, int x, int y, int z) {
        World world = player.worldObj;
        Block block = world.getBlock(x, y, z);
        boolean creative = player.capabilities.isCreativeMode;
        if (block.isAir(world, x, y, z) || (!creative && block.getBlockHardness(world, x, y, z) < 0.0F)) {
            return BreakResult.NOT_BREAKABLE;
        }
        if (!creative && !hasFuel(stack)) {
            return BreakResult.EMPTY;
        }
        int meta = world.getBlockMetadata(x, y, z);
        if (!BlockBreakGuard.mayBreak(world, x, y, z, block, meta, player)) {
            return BreakResult.DENIED;
        }
        if (!creative) {
            drain(stack, tool.blockCostMilli());
        }
        boolean canHarvest = !creative && ForgeHooks.canToolHarvestBlock(block, meta, stack);
        block.onBlockHarvested(world, x, y, z, meta, player);
        if (block.removedByPlayer(world, player, x, y, z, canHarvest)) {
            block.onBlockDestroyedByPlayer(world, x, y, z, meta);
            if (canHarvest) {
                block.harvestBlock(world, player, x, y, z, meta);
            }
        }
        playHit(world, x, y, z);
        return BreakResult.BROKEN;
    }

    private void playHit(World world, int x, int y, int z) {
        String event = definition().hitEvent();
        if (event != null) {
            // Spec: chainsawHit at the block center, volume 0.65.
            GunSoundPlayer.play(world, x + 0.5, y + 0.5, z + 0.5, event, 0.65f, 1.0f, null);
        }
    }

    // ----- melee, mining and swing hooks -----------------------------------------------------------------------

    /**
     * The swing is allowed (unlike a gun's) and plays the throttled swing sound while a round is loaded.
     */
    @Override
    public boolean onEntitySwing(EntityLivingBase entity, ItemStack stack) {
        String event = definition().swingEvent();
        if (event != null && entity instanceof EntityPlayerMP && !entity.worldObj.isRemote && hasFuel(stack)) {
            GunPlayerData data = GunPlayerData.get((EntityPlayerMP) entity);
            long now = entity.worldObj.getTotalWorldTime();
            if (ToolDrain.swingSoundAllowed(data.lastSwingSoundTick(), now, tool.swingSoundGapTicks())) {
                data.setLastSwingSoundTick(now);
                // Spec: chainsawSwing, volume 2.0.
                GunSoundPlayer.play(
                    entity.worldObj,
                    entity.posX,
                    entity.posY + entity.height * 0.5,
                    entity.posZ,
                    event,
                    2.0f,
                    1.0f,
                    null);
            }
        }
        return false;
    }

    /** Breaking is allowed; Forge's own {@code BreakEvent} already guards a player's break. */
    @Override
    public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
        return false;
    }

    /**
     * A left click on an entity is a melee hit (server). A loaded gun hits at full damage and pierces armor by the
     * tool's penetration; an empty one hits as a bare hand with {@link ToolStats#emptyMeleeDamage()}.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!player.worldObj.isRemote) {
            boolean fuel = hasFuel(stack);
            MeleeStrike.attack(
                stack,
                player,
                entity,
                (float) (tool.meleeDamage(fuel) - tool.meleeDamage()),
                fuel ? tool.meleePenetration() : 0.0);
        }
        return true;
    }

    /** Spends one hit's cost after a melee hit landed on a living target (server). */
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (spends(attacker)) {
            drain(stack, tool.hitCostMilli());
        }
        return true;
    }

    /** Spends one block's cost and plays the hit sound when a block with hardness breaks under the tool (server). */
    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z,
        EntityLivingBase entity) {
        if (spends(entity) && block.getBlockHardness(world, x, y, z) > 0.0F) {
            boolean had = hasFuel(stack);
            drain(stack, tool.blockCostMilli());
            if (had) {
                playHit(world, x, y, z);
            }
        }
        return true;
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        return (float) tool.digSpeed(hasFuel(stack), block.getHarvestTool(meta));
    }

    // Source (probes the level with setHarvestLevel, then reads it back with getHarvestLevel(new ItemStack(item), ..)
    // and logs "Could not set harvestlevel" when the two differ):
    // https://github.com/GTNewHorizons/IguanaTweaksTConstruct/blob/2.7.12/src/main/java/iguanaman/iguanatweakstconstruct/harvestlevels/HarvestLevelTweaks.java#L168-L193
    @Override
    public Set<String> getToolClasses(ItemStack stack) {
        return hasFuel(stack) ? super.getToolClasses(stack) : java.util.Collections.<String>emptySet();
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return hasFuel(stack) ? super.getHarvestLevel(stack, toolClass) : -1;
    }

    /** The full melee damage as the attack attribute; the empty-tank weakening is applied by the hit itself. */
    @Override
    @SuppressWarnings("unchecked")
    public Multimap<String, AttributeModifier> getItemAttributeModifiers() {
        Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers();
        map.put(
            SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
            new AttributeModifier(field_111210_e, "Tool modifier", tool.meleeDamage(), 0));
        return map;
    }

    // ----- tooltip ---------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("gregguns.gun.tooltip.melee", tool.meleeDamage(), tool.emptyMeleeDamage()));
        for (String toolClass : tool.toolClasses()) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.gun.tooltip.mining",
                    toolClass,
                    tool.harvestLevel(toolClass),
                    tool.digSpeed()));
        }
    }
}
