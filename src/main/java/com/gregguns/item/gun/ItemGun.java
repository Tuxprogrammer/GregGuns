package com.gregguns.item.gun;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.CamoChangeable;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.combat.DamageType;
import com.gregguns.core.registry.Naming;
import com.gregguns.gun.def.GunDefinition;
import com.gregguns.gun.logic.GunNbt;
import com.gregguns.gun.logic.GunState;
import com.gregguns.gun.logic.GunStats;
import com.gregguns.gun.net.PacketGunZoom;
import com.gregguns.network.GregGunsNetwork;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A gun: one item class driven entirely by its {@link GunStats}. It holds no gameplay logic of its own. The server
 * ticks the per-player {@link com.gregguns.gun.logic.GunController} from
 * {@link com.gregguns.gun.server.ServerGunHandler}; this class supplies the stats, the stack NBT rules, the vanilla
 * suppression and the tooltip.
 *
 * <h2>Stack state</h2>
 * Rounds loaded and the camo index live in the stack's NBT through {@link GunNbt} ({@code ammo}, {@code camo}). The
 * server writes them; clients only read what the inventory sync brings back. Item damage is <em>not</em> wear: it is
 * "rounds missing" carried out of a crafting recipe and converted by {@link #onCreated}. The durability bar shows
 * {@code 1 - loaded / magazine}.
 *
 * <h2>Input</h2>
 * Fire is the vanilla attack button, read by the client input handler and sent as an intent. This class cancels the
 * vanilla attack, swing and block break while a gun is held. Right-click toggles zoom on scoped guns and does nothing
 * on others. Reload is a {@code SyncedKeybind}. See {@code docs/gregguns-spec/02-guns/offhand-backhand.md} for how
 * Backhand treats the right-click.
 *
 * <p>
 * Not enchantable and not repairable. Both sides; the tooltip is client only.
 */
// Spec: docs/techguns-spec/02-guns/gun-lifecycle.md sections 1, 2, 4, 9 and 10; docs/design/0009 row 7.
public class ItemGun extends Item implements Tiered, CamoChangeable {

    private final GunDefinition definition;
    private final String contentName;
    private final GunStats stats;
    private final Tier tier;
    private final DamageType damageType;
    private final String fireEvent;
    private final String reloadEvent;
    private final String rechamberEvent;
    private final int camoCount;

    /**
     * @param definition the gun's definition: its name (the registry name and sound prefix), stats, tier, damage type,
     *                   sound events and camo count
     */
    public ItemGun(GunDefinition definition) {
        this.definition = definition;
        this.contentName = definition.name();
        this.stats = definition.stats();
        this.tier = definition.tier();
        this.damageType = definition.damageType();
        this.fireEvent = definition.fireEvent();
        this.reloadEvent = definition.reloadEvent();
        this.rechamberEvent = definition.rechamberEvent();
        this.camoCount = definition.camoCount();
        setMaxStackSize(1);
        setNoRepair();
    }

    /** @return the definition this item was built from */
    public GunDefinition definition() {
        return definition;
    }

    /** @return the gun's stats */
    public GunStats stats() {
        return stats;
    }

    /** @return the registry name without the mod id */
    public String contentName() {
        return contentName;
    }

    /** @return the unprefixed sound event of one shot */
    public String fireEvent() {
        return fireEvent;
    }

    /** @return the unprefixed sound event played with every shot after the fire sound, or null if the gun has none */
    public String rechamberEvent() {
        return rechamberEvent;
    }

    /** @return the unprefixed sound event of a reload */
    public String reloadEvent() {
        return reloadEvent;
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    // ----- camo (the Camo Bench) -------------------------------------------------------------------------------

    @Override
    public int camoCount(ItemStack stack) {
        return camoCount;
    }

    /**
     * @param stack a stack of this gun
     * @return the stored camo index clamped into {@code [0, camoCount)}; 0 for a tag-less stack
     */
    @Override
    public int getCamo(ItemStack stack) {
        int stored = stateOf(stack).camo();
        return stored >= 0 && stored < camoCount ? stored : 0;
    }

    /**
     * Stores a camo index and nothing else: the rounds loaded stay as they are, and a tag-less stack keeps the full
     * magazine it reads as (GUN-01). An out-of-range index stores 0.
     */
    @Override
    public void setCamo(ItemStack stack, int camo) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        GunState state = GunNbt.read(tag, stats);
        state.setCamo(camo >= 0 && camo < camoCount ? camo : 0);
        GunNbt.write(tag, state);
    }

    @Override
    public String camoNameKey(ItemStack stack) {
        return "item.gregguns." + contentName + ".camoname." + getCamo(stack);
    }

    // ----- NBT -----------------------------------------------------------------------------------------------

    /**
     * Gives a tag-less stack its NBT, once. A recipe output with item damage {@code d} becomes a gun with
     * {@code magazine - d} rounds and damage 0, so "damage = clip" yields an empty gun and damage 0 a full one.
     * A stack that already has the {@code ammo} key is left alone.
     *
     * @param stack a stack of a gun
     * @param stats the gun's stats
     */
    public static void ensureTag(ItemStack stack, GunStats stats) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(GunNbt.KEY_AMMO)) {
            return;
        }
        GunState state = GunState.fromCraftingDamage(stats, stack.getItemDamage());
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        } else if (tag.hasKey(GunNbt.KEY_CAMO)) {
            state.setCamo(tag.getByte(GunNbt.KEY_CAMO));
        }
        GunNbt.write(tag, state);
        stack.setItemDamage(0);
    }

    /** Reads a gun stack's state without changing it; a tag-less stack reads as a full gun. */
    public GunState stateOf(ItemStack stack) {
        return GunNbt.read(stack.getTagCompound(), stats);
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        ensureTag(stack, stats);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        ItemStack stack = new ItemStack(item);
        ensureTag(stack, stats);
        list.add(stack);
    }

    // ----- durability bar (rounds, not wear) -------------------------------------------------------------------

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return stateOf(stack).barFraction(stats);
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack stack, ItemStack material) {
        return false;
    }

    // ----- vanilla suppression: fire is the attack button, so the attack must do nothing else ------------------

    @Override
    public boolean onEntitySwing(EntityLivingBase entity, ItemStack stack) {
        return true;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        return true;
    }

    /**
     * Right-click. On a scoped gun the client sends the zoom intent; the server decides. An unscoped gun does
     * nothing (GUN-47: no flag is sent either).
     */
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote && stats.hasScope()) {
            GregGunsNetwork.channel()
                .sendToServer(new PacketGunZoom(world.provider.dimensionId, player.inventory.currentItem));
        }
        return stack;
    }

    // ----- tooltip ---------------------------------------------------------------------------------------------

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        GunState state = stateOf(stack);
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("gregguns.gun.tooltip.ammo", EnumChatFormatting.WHITE + ammoName()));
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("gregguns.gun.tooltip.loaded", state.loaded(), stats.magazineSize()));
        tooltip.add(
            StatCollector.translateToLocalFormatted("gregguns.gun.tooltip.damage", GunTooltipFormat.damage(stats)));
        if (!GuiScreen.isShiftKeyDown()) {
            tooltip.add(StatCollector.translateToLocal("gregguns.gun.tooltip.shift"));
            return;
        }
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.gun.tooltip.damage_type",
                StatCollector.translateToLocal(damageType.nameLangKey())));
        tooltip.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.gun.tooltip.range",
                GunTooltipFormat.dropOffRange(stats),
                GunTooltipFormat.reach(stats)));
        tooltip.add(
            StatCollector.translateToLocalFormatted("gregguns.gun.tooltip.spread", GunTooltipFormat.spread(stats)));
        tooltip.add(StatCollector.translateToLocalFormatted("gregguns.gun.tooltip.clipsize", stats.magazineSize()));
        tooltip.add(
            StatCollector
                .translateToLocalFormatted("gregguns.gun.tooltip.reload_time", GunTooltipFormat.reloadSeconds(stats)));
        if (stats.penetration() > 0) {
            tooltip.add(
                StatCollector.translateToLocalFormatted(
                    "gregguns.gun.tooltip.armor_pen",
                    GunTooltipFormat.penetrationPercent(stats)));
        }
        if (stats.hasScope()) {
            tooltip.add(StatCollector.translateToLocalFormatted("gregguns.gun.tooltip.zoom", stats.zoomFactor()));
        }
        if (stats.silenced()) {
            tooltip.add(StatCollector.translateToLocal("gregguns.gun.tooltip.silenced"));
        }
    }

    @SideOnly(Side.CLIENT)
    private String ammoName() {
        Item ammo = GameRegistry.findItem(Naming.NAMESPACE, stats.ammoType());
        return ammo == null ? stats.ammoType() : new ItemStack(ammo).getDisplayName();
    }

    /**
     * A gun with no 3D profile yet (see {@code ClientGunDefinitions.FLAT_ICON_FALLBACK}) is drawn as its flat icon;
     * holding it like a tool keeps the icon upright in the hand instead of lying flat. Guns with a model draw
     * themselves.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean isFull3D() {
        return com.gregguns.client.render.profile.ClientGunDefinitions.usesFallback(definition.name());
    }
}
