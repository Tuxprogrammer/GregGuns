package com.gregguns.utility.charge;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.gregguns.core.registry.Naming;
import com.gregguns.utility.security.OwnerProfiles;
import com.mojang.authlib.GameProfile;

/**
 * The tile of an Explosive Charge: its settings, owner, orientation and fuse. The server ticks the fuse and, at zero,
 * calls {@link ChargeBlast}; the client only mirrors the state (through the description packet) for the GUI and the
 * armed texture. Both sides.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (NBT, Arming and fuse),
// docs/gregguns-spec/09-machines-utility/explosive-charges.md
public final class TileExplosiveCharge extends TileEntity {

    private static final String K_ORIENTATION = "orientation";
    private static final String K_RADIUS = "blastradius";
    private static final String K_LENGTH = "blastlength";
    private static final String K_FUSE = "fusetime";
    private static final String K_ARMED = "armed";
    private static final String K_OWNER = "ownerId";
    private static final String K_OWNER_NAME = "ownerName";
    private static final String K_TYPE = "chargeType";

    private ChargeParams params = new ChargeParams(ChargeType.TNT);
    private int orientation = 1;
    private UUID ownerId;
    private String ownerName = "";

    /** Required by the tile entity registry. */
    public TileExplosiveCharge() {}

    /**
     * @param type the charge type, taken from the block's metadata when the block is created without an item
     */
    public TileExplosiveCharge(ChargeType type) {
        this.params = new ChargeParams(type);
    }

    /** @return the settings; mutate only through {@link #press} */
    public ChargeParams params() {
        return params;
    }

    /** @return the placed face, 0 to 5 */
    public int orientation() {
        return orientation;
    }

    /** @return the owner UUID; null for a block that was never placed by a player */
    public UUID ownerId() {
        return ownerId;
    }

    /**
     * Set by the item block after placement.
     *
     * @param type        which charge this is
     * @param orientation the clicked face
     * @param owner       the placing player; null leaves the charge ownerless
     */
    public void init(ChargeType type, int orientation, EntityPlayer owner) {
        this.params = new ChargeParams(type);
        this.orientation = ChargeGeometry.normalize(orientation);
        if (owner != null) {
            setOwner(owner.getUniqueID(), owner.getCommandSenderName());
        }
        markDirty();
        sync();
    }

    /**
     * Sets the owner without a player, for an owner who is offline.
     *
     * @param id   the owner UUID
     * @param name the owner name for the stored profile
     */
    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name == null ? "" : name;
        markDirty();
    }

    /**
     * Applies a GUI button. The caller has already checked permission (MUTL-31); this only enforces the rules of the
     * settings.
     *
     * @param button a {@code ChargeParams.BTN_*} id
     */
    public void press(int button) {
        if (worldObj == null || worldObj.isRemote) {
            return;
        }
        boolean wasArmed = params.armed();
        if (params.press(button)) {
            if (!wasArmed && params.armed()) {
                playSound(
                    params.type()
                        .initSound());
            }
            markDirty();
            sync();
        }
    }

    @Override
    public void updateEntity() {
        // Server only: the client copy has nothing to do (detonation is server-side, so the break event is never
        // posted from a client, MUTL-30).
        if (worldObj == null || worldObj.isRemote || !params.armed()) {
            return;
        }
        ChargeFuse.Tick tick = params.tickFuse();
        if (tick == ChargeFuse.Tick.BEEP) {
            playSound(
                params.type()
                    .tickSound());
        } else if (tick == ChargeFuse.Tick.DETONATE) {
            ChargeBlast.detonate(
                (WorldServer) worldObj,
                xCoord,
                yCoord,
                zCoord,
                orientation,
                params,
                ownerId,
                ownerId == null ? null : new GameProfile(ownerId, ownerName.isEmpty() ? "[GregGuns]" : ownerName));
        }
    }

    /** Plays a charge sound event at the block, to nearby players. Server side. */
    void playSound(String event) {
        worldObj.playSoundEffect(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, Naming.soundEvent(event), 4.0F, 1.0F);
    }

    private void sync() {
        if (worldObj != null && !worldObj.isRemote) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /**
     * @param player the player to check
     * @return true when the player is the owner or on the owner's ServerUtilities team (MUTL-31)
     */
    public boolean mayConfigure(EntityPlayer player) {
        return OwnerProfiles.isAuthorized(ownerId, player);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setByte(
            K_TYPE,
            (byte) params.type()
                .meta());
        tag.setByte(K_ORIENTATION, (byte) orientation);
        tag.setShort(K_RADIUS, (short) params.radius());
        tag.setShort(K_LENGTH, (short) params.length());
        tag.setShort(K_FUSE, (short) params.fuse());
        tag.setBoolean(K_ARMED, params.armed());
        if (ownerId != null) {
            tag.setString(K_OWNER, ownerId.toString());
            tag.setString(K_OWNER_NAME, ownerName);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        params = new ChargeParams(ChargeType.fromMeta(tag.getByte(K_TYPE)));
        orientation = ChargeGeometry.normalize(tag.hasKey(K_ORIENTATION) ? tag.getByte(K_ORIENTATION) : 1);
        params.restore(
            tag.hasKey(K_RADIUS) ? tag.getShort(K_RADIUS) : params.radius(),
            tag.hasKey(K_LENGTH) ? tag.getShort(K_LENGTH) : params.length(),
            tag.hasKey(K_FUSE) ? tag.getShort(K_FUSE) : params.fuse(),
            tag.getBoolean(K_ARMED));
        ownerId = null;
        ownerName = tag.getString(K_OWNER_NAME);
        if (tag.hasKey(K_OWNER)) {
            try {
                ownerId = UUID.fromString(tag.getString(K_OWNER));
            } catch (IllegalArgumentException e) {
                // A corrupt owner reads as ownerless rather than failing the chunk load.
                ownerId = null;
            }
        }
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
}
