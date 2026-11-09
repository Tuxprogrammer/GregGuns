package com.gregguns.item.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gregguns.api.GregGunsComponent;
import com.gregguns.api.Tier;
import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A healing consumable: Bandages or the Medkit. Holding right-click below full health uses it: every
 * {@link ConsumableSpec#FEEDBACK_PERIOD} ticks the user hears it and swings the arm, and at the end of the use time it
 * heals at once, adds Regeneration and consumes one item. At full health a right click does nothing but say so.
 *
 * <p>
 * Server-authoritative: the heal, the effect, the sounds and the full-health notice come from the server. The arm
 * swing is the client's, which tells the server to show it to everyone else. Common side.
 */
// Spec: docs/gregguns-spec/06-items/recipes.md (RC-61, RC-62), docs/techguns-spec/06-items/special-items.md (Medkit)
public final class ItemConsumable extends Item implements Tiered, GregGunsComponent {

    /** Lang key of the chat line a right click at full health sends. */
    public static final String FULL_HEALTH_KEY = "gregguns.consumable.full_health";

    /** The world tick of each player's last full-health notice; server thread only. */
    private static final Map<EntityPlayer, Long> LAST_NOTICE = new WeakHashMap<>();

    private final Tier tier;
    private final ConsumableSpec spec;

    /**
     * @param tier the D1 tier
     * @param spec the heal numbers
     */
    public ItemConsumable(Tier tier, ConsumableSpec spec) {
        this.tier = tier;
        this.spec = spec;
        setMaxStackSize(16);
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    /** @return the heal numbers */
    public ConsumableSpec spec() {
        return spec;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (ConsumableSpec.canUse(player.getHealth(), player.getMaxHealth())) {
            player.setItemInUse(stack, getMaxItemUseDuration(stack));
        } else if (!world.isRemote) {
            // A held button re-clicks every 4 ticks, so the notice waits out a cooldown per player.
            long now = world.getTotalWorldTime();
            Long last = LAST_NOTICE.get(player);
            if (last == null || ConsumableSpec.noticeDue(last, now)) {
                LAST_NOTICE.put(player, now);
                player.addChatComponentMessage(new ChatComponentTranslation(FULL_HEALTH_KEY));
            }
        }
        return stack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return spec.useTicks();
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.none;
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityPlayer player, int count) {
        if (!ConsumableSpec.isFeedbackTick(count)) {
            return;
        }
        World world = player.worldObj;
        if (world.isRemote) {
            // The client's swing reaches the server as an animation packet, which shows it to everyone nearby.
            player.swingItem();
        } else {
            world.playSoundAtEntity(player, ConsumableSpec.USE_SOUND, 1.0F, 1.2F);
        }
    }

    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
        }
        if (!world.isRemote) {
            player.heal(spec.healHp());
            player.addPotionEffect(new PotionEffect(Potion.regeneration.id, spec.regenTicks(), spec.regenAmplifier()));
            world.playSoundAtEntity(player, "random.burp", 0.5F, 0.9F + world.rand.nextFloat() * 0.1F);
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        List<String> details = new ArrayList<>();
        details.add(
            StatCollector.translateToLocalFormatted(
                ConsumableTooltipFormat.healKey(spec.healHp()),
                ConsumableTooltipFormat.hearts(spec.healHp())));
        details.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.consumable.tooltip.regen",
                ConsumableTooltipFormat.roman(spec.regenAmplifier() + 1),
                ConsumableTooltipFormat.seconds(spec.regenTicks())));
        details.add(
            StatCollector.translateToLocalFormatted(
                "gregguns.consumable.tooltip.use",
                ConsumableTooltipFormat.seconds(spec.useTicks())));
        tooltip.addAll(
            ConsumableTooltipFormat.lines(
                StatCollector.translateToLocalFormatted("gregguns.consumable.tooltip.tier", TierText.colored(tier)),
                StatCollector.translateToLocal("gregguns.consumable.tooltip.summary"),
                details,
                StatCollector.translateToLocal("gregguns.consumable.tooltip.shift"),
                AddedBy.line(),
                net.minecraft.client.gui.GuiScreen.isShiftKeyDown()));
    }
}
