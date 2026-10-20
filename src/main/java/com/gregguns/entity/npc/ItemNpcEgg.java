package com.gregguns.entity.npc;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.GregGuns;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The spawn egg of every NPC: one item, with the mob in the item damage ({@link NpcMob#entityId()}). The name is
 * "Spawn " plus the mob's localized name; the colors come from {@link NpcMob}. Using it on a block puts the mob at the
 * center of the neighboring block; right-clicking liquid does the same at the liquid's column. The egg only spawns what
 * {@link NpcRegistry} has registered: an unregistered subtype does nothing and is not consumed, and the result of one
 * use is never reused by the next (fixes NPC-18, NPC-19, NPC-20). Both sides; spawning is server only.
 */
// Spec: docs/techguns-spec/10-entities-npcs/spawning.md (2. Spawn eggs), mob-catalog.md (Spawn eggs)
public class ItemNpcEgg extends Item {

    /** Texture of the egg body (tinted by the base color). */
    public static final String ICON_BASE = "gregguns:spawn_egg";
    /** Texture of the egg spots (tinted by the spot color). */
    public static final String ICON_SPOTS = "gregguns:spawn_egg_overlay";

    private static final int FENCE_RENDER_TYPE = 11;

    @SideOnly(Side.CLIENT)
    private IIcon spots;

    public ItemNpcEgg() {
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    // --- naming and look ----------------------------------------------------------------------------------------

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String prefix = StatCollector.translateToLocal(getUnlocalizedName() + ".name")
            .trim();
        NpcMob mob = NpcMob.byId(stack.getItemDamage());
        if (mob == null) {
            return prefix;
        }
        return prefix + " " + StatCollector.translateToLocal(mob.langKey());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        super.registerIcons(register);
        spots = register.registerIcon(ICON_SPOTS);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int damage, int pass) {
        return pass > 0 ? spots : itemIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        NpcMob mob = NpcMob.byId(stack.getItemDamage());
        if (mob == null || !mob.hasEgg()) {
            return 0xFFFFFF;
        }
        return pass == 0 ? mob.eggBase() : mob.eggSpot();
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        for (NpcMob mob : NpcRegistry.eggMobs()) {
            list.add(new ItemStack(item, 1, mob.entityId()));
        }
    }

    // --- use ----------------------------------------------------------------------------------------------------

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        Block block = world.getBlock(x, y, z);
        double[] at = EggPlacement.onBlockFace(x, y, z, side, block.getRenderType() == FENCE_RENDER_TYPE);
        return spawnAndConsume(stack, player, world, NpcMob.byId(stack.getItemDamage()), at);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }
        MovingObjectPosition hit = getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return stack;
        }
        int x = hit.blockX;
        int y = hit.blockY;
        int z = hit.blockZ;
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, hit.sideHit, stack)) {
            return stack;
        }
        if (world.getBlock(x, y, z) instanceof BlockLiquid) {
            spawnAndConsume(stack, player, world, NpcMob.byId(stack.getItemDamage()), EggPlacement.atLiquid(x, y, z));
        }
        return stack;
    }

    private static boolean spawnAndConsume(ItemStack stack, EntityPlayer player, World world, NpcMob mob, double[] at) {
        String tag = stack.hasDisplayName() ? stack.getDisplayName() : null;
        Entity spawned = spawn(world, mob, at[0], at[1], at[2], tag);
        if (spawned != null && !player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        return spawned != null;
    }

    /**
     * Creates and spawns one mob. The entity is local to this call: a failed lookup returns null and never hands back
     * an entity from an earlier call (fixes NPC-18). Server side.
     *
     * @param world   the server world
     * @param mob     the kind; must be registered ({@link NpcRegistry#isRegistered})
     * @param x       feet X
     * @param y       feet Y
     * @param z       feet Z
     * @param tagName the name tag to apply, or null
     * @return the spawned entity, or null if the mob is unknown, unregistered or the world refused it
     */
    public static Entity spawn(World world, NpcMob mob, double x, double y, double z, String tagName) {
        if (mob == null || !NpcRegistry.isRegistered(mob)) {
            GregGuns.LOG.warn("Spawn egg for an unregistered NPC kind: {}", mob);
            return null;
        }
        Entity entity = EntityList.createEntityByName(mob.fullName(), world);
        if (entity == null) {
            GregGuns.LOG.warn("Entity {} is not known to EntityList", mob.fullName());
            return null;
        }
        entity.setLocationAndAngles(x, y, z, world.rand.nextFloat() * 360.0F, 0.0F);
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            living.rotationYawHead = living.rotationYaw;
            living.renderYawOffset = living.rotationYaw;
            living.onSpawnWithEgg(null);
            if (tagName != null) {
                living.setCustomNameTag(tagName);
            }
        }
        if (!world.spawnEntityInWorld(entity)) {
            return null;
        }
        if (entity instanceof EntityLiving) {
            ((EntityLiving) entity).playLivingSound();
        }
        return entity;
    }
}
