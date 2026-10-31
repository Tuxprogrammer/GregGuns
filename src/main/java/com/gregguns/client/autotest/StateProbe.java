package com.gregguns.client.autotest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.gregguns.icbm.defense.DishSpin;
import com.gregguns.icbm.defense.EntityRadarDish;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/**
 * The dev autotest's {@code cstate <label> <x0> <y0> <z0> <x1> <y1> <z1>} command: logs one line that sums up what this
 * client's world holds in a box, so a two-client session can check that both players see the same state. The line
 * carries a hash of every block and its metadata, the tile entities (with a GregTech machine's id, active flag and
 * facing, or any other tile's client-side NBT), and every entity in the box (dropped items with their stack, living
 * things with their health and worn and held items, a radar dish with its spin state). Positions are floored to whole
 * blocks. Client only, dev only.
 */
@SideOnly(Side.CLIENT)
final class StateProbe {

    /** The largest box the probe walks, in blocks, so a typo can't stall the client. */
    private static final long MAX_VOLUME = 4_000_000L;
    /** At most this many entities are listed; the count is always exact. */
    private static final int MAX_ENTITIES = 120;

    private StateProbe() {}

    /**
     * Logs the state line for the box.
     *
     * @param args the label, then the two corners
     */
    static void log(String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        String label = args[0];
        int[] c = new int[6];
        for (int i = 0; i < 6; i++) {
            c[i] = Integer.parseInt(args[1 + i]);
        }
        String who = mc.thePlayer == null ? "?" : mc.thePlayer.getCommandSenderName();
        ClientAutotest.logLine("cstate " + label + " " + describe(mc.theWorld, c) + " (client " + who + ")");
    }

    /**
     * @param w the world
     * @param c x0, y0, z0, x1, y1, z1 (inclusive, either order)
     * @return the summary: block and tile hashes, then the entity list
     */
    static String describe(World w, int[] c) {
        int x0 = Math.min(c[0], c[3]), x1 = Math.max(c[0], c[3]);
        int y0 = Math.max(0, Math.min(c[1], c[4])), y1 = Math.min(255, Math.max(c[1], c[4]));
        int z0 = Math.min(c[2], c[5]), z1 = Math.max(c[2], c[5]);
        long volume = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        if (volume > MAX_VOLUME) {
            return "box too big: " + volume;
        }
        int unloaded = 0;
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                if (!w.getChunkFromChunkCoords(cx, cz).isChunkLoaded) {
                    unloaded++;
                }
            }
        }
        CRC32 blocks = new CRC32();
        CRC32 tiles = new CRC32();
        int solid = 0;
        int tileCount = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    Block b = w.getBlock(x, y, z);
                    int id = Block.getIdFromBlock(b);
                    int meta = w.getBlockMetadata(x, y, z);
                    blocks.update(id >> 8);
                    blocks.update(id);
                    blocks.update(meta);
                    if (id != 0) {
                        solid++;
                    }
                    if (!b.hasTileEntity(meta)) {
                        continue;
                    }
                    TileEntity te = w.getTileEntity(x, y, z);
                    if (te == null) {
                        continue;
                    }
                    tileCount++;
                    String t = x + ","
                        + y
                        + ","
                        + z
                        + ":"
                        + te.getClass()
                            .getSimpleName()
                        + gtState(te);
                    tiles.update(t.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        List<String> ents = new ArrayList<>();
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1);
        for (Object o : w.getEntitiesWithinAABB(Entity.class, box)) {
            Entity e = (Entity) o;
            if (e == Minecraft.getMinecraft().thePlayer) {
                continue;
            }
            ents.add(entity(e));
        }
        Collections.sort(ents);
        StringBuilder s = new StringBuilder();
        s.append(
            String.format(
                Locale.ROOT,
                "unloaded=%d blocks=%d hash=%08x tiles=%d thash=%08x ents=%d",
                unloaded,
                solid,
                blocks.getValue(),
                tileCount,
                tiles.getValue(),
                ents.size()));
        for (int i = 0; i < ents.size() && i < MAX_ENTITIES; i++) {
            s.append(" [")
                .append(ents.get(i))
                .append(']');
        }
        return s.toString();
    }

    /**
     * A GregTech machine's id, active flag and facing; for any other tile, its client-side NBT (a Carpenter's Blocks
     * garage door keeps its open state there), so a door one client sees open and the other shut differs.
     */
    private static String gtState(TileEntity te) {
        if (!(te instanceof IGregTechTileEntity)) {
            try {
                net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
                te.writeToNBT(tag);
                return " nbt=" + tag;
            } catch (RuntimeException e) {
                return " nbt=?";
            }
        }
        IGregTechTileEntity g = (IGregTechTileEntity) te;
        return " mte=" + g.getMetaTileID() + " active=" + g.isActive() + " facing=" + g.getFrontFacing();
    }

    private static String entity(Entity e) {
        StringBuilder s = new StringBuilder(
            e.getClass()
                .getSimpleName()).append('@')
                    .append((int) Math.floor(e.posX))
                    .append(',')
                    .append((int) Math.floor(e.posY))
                    .append(',')
                    .append((int) Math.floor(e.posZ));
        if (e instanceof EntityItem) {
            s.append(" item=")
                .append(stack(((EntityItem) e).getEntityItem()));
        } else if (e instanceof EntityLivingBase) {
            EntityLivingBase l = (EntityLivingBase) e;
            s.append(" hp=")
                .append((int) Math.ceil(l.getHealth()))
                .append(" eq=");
            for (int slot = 0; slot < 5; slot++) {
                s.append(slot == 0 ? "" : "|")
                    .append(stack(l.getEquipmentInSlot(slot)));
            }
        } else if (e instanceof EntityRadarDish) {
            DishSpin spin = ((EntityRadarDish) e).spin();
            s.append(" spin=")
                .append(spin.spinning())
                .append('/')
                .append(Math.round(spin.baseAngle()))
                .append('/')
                .append(spin.since());
        }
        return s.toString();
    }

    private static String stack(ItemStack st) {
        if (st == null || st.getItem() == null) {
            return "-";
        }
        return Item.itemRegistry.getNameForObject(st.getItem()) + ":" + st.getItemDamage() + "x" + st.stackSize;
    }
}
