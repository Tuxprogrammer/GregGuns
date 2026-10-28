package com.gregguns.icbm.defense;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.WorldTargets;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to every client tracking an {@link EntityRadarDish}: whether the dish turns, the angle and the low 32 bits of
 * the game time of the last change, and the model scale. Sent on change and repeated about once a second, so a client
 * that starts tracking late catches up; clients get the angle from {@link DishSpin}. A bad value or a target that is
 * not a dish is dropped (CORE-40).
 *
 * <p>
 * Wire: entity id (int), spinning (byte), angle (float), start (int), scale (float). Append-only list position: see
 * {@code GregGunsNetwork}. Side: sent by the server, handled by the client.
 */
// Spec: docs/gregguns-icbm-spec/05-detection-defense/radar.md (The dish)
public final class PacketRadarDish extends ClientboundPacket {

    private int entityId;
    private boolean spinning;
    private float angle;
    private int start;
    private float scale;

    /** Required by the channel. */
    public PacketRadarDish() {}

    /**
     * @param dish the dish entity, with the controller's state already pushed
     */
    PacketRadarDish(EntityRadarDish dish) {
        this.entityId = dish.getEntityId();
        this.spinning = dish.spin()
            .spinning();
        this.angle = dish.spin()
            .baseAngle();
        this.start = (int) dish.spin()
            .since();
        this.scale = dish.scale();
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        buf.writeInt(entityId);
        buf.writeByte(spinning ? 1 : 0);
        buf.writeFloat(angle);
        buf.writeInt(start);
        buf.writeFloat(scale);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        entityId = buf.readInt();
        spinning = buf.readByte() != 0;
        angle = buf.readFloat();
        start = buf.readInt();
        scale = buf.readFloat();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        rules.finite("angle", angle)
            .finite("scale", scale)
            .inRange("scale", (long) (scale * 100.0F), 25, 800)
            .targetOfType("radar dish", () -> {
                Entity e = WorldTargets.entity(world, entityId);
                return e instanceof EntityRadarDish ? e : null;
            }, Entity.class);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        ((EntityRadarDish) world.getEntityByID(entityId)).applyState(spinning, angle, start, scale);
    }
}
