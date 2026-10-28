package com.gregguns.worldgen.spawner;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gregguns.api.Tiered;
import com.gregguns.core.text.AddedBy;
import com.gregguns.core.text.TierText;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The item of a hole spawner block, with the D41 tooltip: the tier, which NPCs it places, and behind Shift the range,
 * the cap and the "broken stays broken" rule. Worldgen places the block; the item exists for creative-mode picking and
 * NEI. Both sides.
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-guard-spawner.md (Tooltip)
public final class ItemBlockHoleSpawner extends ItemBlock {

    /** @param block a {@link BlockHoleSpawner} */
    public ItemBlockHoleSpawner(Block block) {
        super(block);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        boolean shift = net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
        BlockHoleSpawner b = (BlockHoleSpawner) field_150939_a;
        String tierLine = String.format(
            StatCollector.translateToLocal("gregguns.icbm.part.tooltip.tier"),
            TierText.colored(((Tiered) b).getTier()));
        String summary = StatCollector.translateToLocal(
            "gregguns.hole_spawner." + b.kind()
                .blockName() + ".summary");
        List<String> details = Arrays.asList(
            StatCollector.translateToLocalFormatted(
                "gregguns.hole_spawner.details",
                (int) HoleCaps.PLAYER_RANGE,
                HoleCaps.MAX_NEARBY,
                HoleCaps.MAX_PER_ATTEMPT));
        String hint = StatCollector.translateToLocal("gregguns.icbm.part.tooltip.shift");
        tooltip.addAll(HoleTooltipFormat.lines(tierLine, summary, details, hint, shift));
        if (shift) {
            tooltip.add(AddedBy.line());
        }
    }
}
