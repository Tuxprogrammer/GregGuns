package com.gregguns.icbm.warhead;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.gregguns.icbm.explosive.ExplosiveDef;
import com.gregguns.icbm.explosive.Explosives;
import com.gregguns.icbm.explosive.IcbmBlasts;
import com.gregguns.utility.security.OwnerProfiles;
import com.mojang.authlib.GameProfile;

/**
 * The tile of a placed warhead: explosive, fill, owner, how tall it is and its fuse. The server ticks the fuse and, at
 * zero, removes the block and starts the blast through {@link IcbmBlasts}. The client gets the tile through the
 * description packet (sent when the tile loads and once when the fuse is lit), so it knows the fuse is lit and can draw
 * the smoke and sparks; it never counts the fuse down.
 * Both sides.
 */
// Spec: docs/gregguns-icbm-spec/02-explosives/warhead-blocks.md (Use as a demolition charge)
public final class TileWarhead extends TileEntity {

    private static final String K_FUSE = "fuse";
    private static final String K_HEIGHT = "height";
    private static final String K_OWNER = "ownerId";
    private static final String K_OWNER_NAME = "ownerName";

    private WarheadData data = WarheadData.of(WarheadData.DEFAULT_EXPLOSIVE, 1, 1, true);
    private int fuse;
    private int height = 1;
    private UUID ownerId;
    private String ownerName = "";
    private boolean noDrop;
    private boolean removing;
    private boolean initialized;

    /** Required by the tile entity registry. */
    public TileWarhead() {}

    /** @return the warhead's explosive and fill */
    public WarheadData data() {
        return data;
    }

    /** @return the fuse in ticks; 0 when not lit */
    public int fuse() {
        return fuse;
    }

    /** @return true while the fuse runs */
    public boolean armed() {
        return fuse > 0;
    }

    /** @return how many blocks tall this warhead is, 1 for T1 and T2 */
    public int height() {
        return height;
    }

    /** @return the owner's UUID, null for an unowned warhead */
    public UUID ownerId() {
        return ownerId;
    }

    /** @return true while the block is being taken apart, so fillers do not start a second removal */
    boolean removing() {
        return removing;
    }

    void setRemoving() {
        removing = true;
    }

    /** @return true when removing this block must not drop the item (it detonated, or a creative player broke it) */
    boolean noDrop() {
        return noDrop;
    }

    void setNoDrop() {
        noDrop = true;
    }

    /**
     * Sets what the placed item carried.
     *
     * @param casing the casing, to clamp against
     * @param tag    the stack's NBT; may be null
     * @param owner  the placer, may be null
     */
    public void init(WarheadCasing casing, NBTTagCompound tag, EntityPlayer owner) {
        this.data = WarheadStacks.read(casing, tag);
        this.height = casing.heightBlocks();
        if (owner != null) {
            this.ownerId = owner.getUniqueID();
            this.ownerName = owner.getCommandSenderName();
        }
        initialized = true;
        markDirty();
        // A redstone source beside the block lit it in onBlockAdded, before this ran, with the default fill. Light it
        // again now that the real fill is known; a shorter fuse only ever replaces a longer one.
        if (worldObj != null && !worldObj.isRemote
            && worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord)) {
            arm(litFuseTicks(), null);
        }
    }

    /**
     * @return true once {@link #init} ran (or the tile was loaded from NBT); false for a tile just created by a place
     */
    public boolean initialized() {
        return initialized;
    }

    /**
     * Test and filler hook: makes the warhead taller than its casing, to exercise the filler mechanism before a tall
     * casing ships.
     *
     * @param blocks the new height, at least 1
     */
    public void setHeight(int blocks) {
        this.height = Math.max(1, blocks);
        markDirty();
    }

    /** Sets the owner without a player. */
    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name == null ? "" : name;
        markDirty();
    }

    /**
     * The fuse for a warhead lit by redstone or by hand (D51), from its blast radius at its fill.
     *
     * @return ticks
     */
    public int litFuseTicks() {
        com.gregguns.icbm.explosive.ExplosiveDef def = com.gregguns.icbm.explosive.Explosives.get(data.explosive());
        double radius = def == null ? WarheadMath.MIN_RADIUS
            : Math.max(WarheadMath.MIN_RADIUS, def.radius(data.fill()));
        return WarheadFuse.litTicks(radius, com.gregguns.core.config.IcbmConfig.fuseMultiplier);
    }

    /**
     * Lights the fuse. A lit warhead only ever gets a shorter fuse.
     *
     * @param ticks the fuse to set
     * @param actor who lit it, may be null (redstone keeps the placer as owner)
     * @return true when this changed anything
     */
    public boolean arm(int ticks, EntityPlayer actor) {
        if (worldObj == null || worldObj.isRemote) {
            return false;
        }
        int next = WarheadFuse.arm(fuse, ticks);
        if (next == fuse) {
            return false;
        }
        boolean first = fuse <= 0;
        fuse = next;
        if (first) {
            if (actor != null && ownerId == null) {
                setOwner(actor.getUniqueID(), actor.getCommandSenderName());
            }
            worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "game.tnt.primed", 1.0F, 1.0F);
            // Tell the clients once that it is lit, so they draw the smoke and sparks (the fuse itself is not synced).
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
        markDirty();
        return true;
    }

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || fuse <= 0) {
            return;
        }
        WarheadFuse.Step step = WarheadFuse.advance(fuse);
        fuse = step.remaining;
        if (step.action == WarheadFuse.Action.BEEP) {
            worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "random.click", 1.0F, 1.5F);
        } else if (step.action == WarheadFuse.Action.DETONATE) {
            detonate();
        }
    }

    /**
     * Removes the block (with its fillers, without a drop) and starts the blast at its center. Server side.
     *
     * @return true when the blast was accepted by the engine
     */
    public boolean detonate() {
        if (worldObj == null || worldObj.isRemote) {
            return false;
        }
        WorldServer world = (WorldServer) worldObj;
        ExplosiveDef def = Explosives.get(data.explosive());
        int x = xCoord;
        int y = yCoord;
        int z = zCoord;
        int fill = data.fill();
        EntityPlayer actor = OwnerProfiles.playerFor(
            world,
            ownerId,
            ownerId == null ? null : new GameProfile(ownerId, ownerName.isEmpty() ? "[GregGuns]" : ownerName));
        noDrop = true;
        world.setBlockToAir(x, y, z);
        if (def == null) {
            return false;
        }
        return IcbmBlasts.detonate(world, x + 0.5, y + 0.5, z + 0.5, def, fill, actor);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        WarheadStacks.write(tag, data);
        tag.setInteger(K_FUSE, fuse);
        tag.setInteger(K_HEIGHT, height);
        if (ownerId != null) {
            tag.setString(K_OWNER, ownerId.toString());
            tag.setString(K_OWNER_NAME, ownerName);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        String id = tag.getString(WarheadStacks.K_EXPLOSIVE);
        ExplosiveDef def = Explosives.get(id);
        // Keep the stored values as they are on load: the casing is not known here, and the block clamps on place.
        initialized = tag.hasKey(WarheadStacks.K_EXPLOSIVE);
        data = WarheadData.of(id, tag.getInteger(WarheadStacks.K_FILL), def == null ? 1 : def.maxFill(), def != null);
        fuse = tag.getInteger(K_FUSE);
        height = Math.max(1, tag.getInteger(K_HEIGHT));
        ownerId = null;
        ownerName = tag.getString(K_OWNER_NAME);
        if (tag.hasKey(K_OWNER)) {
            try {
                ownerId = UUID.fromString(tag.getString(K_OWNER));
            } catch (IllegalArgumentException e) {
                ownerId = null;
            }
        }
    }
}
