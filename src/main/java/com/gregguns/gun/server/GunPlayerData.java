package com.gregguns.gun.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

import com.gregguns.gun.logic.ChargeState;
import com.gregguns.gun.logic.GunController;
import com.gregguns.gun.logic.LoopStart;
import com.gregguns.item.gun.ItemGun;

/**
 * The server's per-player gun state: the {@link GunController} (cooldown, trigger, reload progress, zoom) plus what
 * the handler needs to notice a weapon switch. Attached to the player as an extended property, so it lives and dies
 * with the {@code EntityPlayerMP}: a new body after death starts clean, and nothing is kept in a static map.
 *
 * <p>
 * Transient by design. A reload only moves ammo when it completes, so nothing is lost if the player logs out
 * mid-reload, and there is nothing to save.
 *
 * <p>
 * Spec: docs/techguns-spec/01-core/player-state.md (TechgunsExtendedPlayerProperties.fireDelay). Deviation: only the
 * server holds a controller; the client keeps a separate prediction mirror. Server only.
 */
public final class GunPlayerData implements IExtendedEntityProperties {

    /** Extended-property key. */
    public static final String ID = "gregguns.gun";

    private final GunController controller = new GunController();
    private final ChargeState charge = new ChargeState();
    private ItemGun activeGun;
    private int activeSlot = -1;
    private boolean zoomSent;
    private long lastShotTick = LoopStart.NEVER;

    /**
     * Returns the player's data, attaching it on first use.
     *
     * @param player a server-side player
     */
    public static GunPlayerData get(EntityPlayer player) {
        IExtendedEntityProperties existing = player.getExtendedProperties(ID);
        if (existing instanceof GunPlayerData) {
            return (GunPlayerData) existing;
        }
        GunPlayerData fresh = new GunPlayerData();
        player.registerExtendedProperties(ID, fresh);
        return fresh;
    }

    /** @return the player's controller */
    public GunController controller() {
        return controller;
    }

    /** @return the player's hold-to-charge state (I10); idle unless a charge gun is being charged */
    public ChargeState charge() {
        return charge;
    }

    /** @return the gun driven on the last tick, or null if none was held */
    ItemGun activeGun() {
        return activeGun;
    }

    /** @return the hotbar slot of {@link #activeGun()} */
    int activeSlot() {
        return activeSlot;
    }

    void setActive(ItemGun gun, int slot) {
        this.activeGun = gun;
        this.activeSlot = slot;
    }

    /** @return the zoom state last sent to the client */
    boolean zoomSent() {
        return zoomSent;
    }

    void setZoomSent(boolean zoomSent) {
        this.zoomSent = zoomSent;
    }

    /** @return the world tick of the player's last shot, or {@link LoopStart#NEVER} */
    long lastShotTick() {
        return lastShotTick;
    }

    void setLastShotTick(long tick) {
        this.lastShotTick = tick;
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {}

    @Override
    public void loadNBTData(NBTTagCompound compound) {}

    @Override
    public void init(Entity entity, World world) {}
}
