package com.gregguns.fx;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

import com.gregguns.network.ClientboundPacket;
import com.gregguns.network.PacketStrings;
import com.gregguns.network.validation.ClientRules;

/**
 * Server to client: spawn the named FX attached to an entity. The entity need not exist on the client yet: the sink
 * keeps the request and retries it for {@value PendingAttachQueue#DEFAULT_WAIT_TICKS} ticks (fixes PFX-01), and
 * {@link FxSender#onEntity} sends only to the players tracking the entity.
 *
 * <p>
 * Wire: name (UTF-8 string, at most {@value PacketSpawnParticle#MAX_NAME} characters), entity id (int).
 *
 * <p>
 * Side: sent by the server through {@link FxSender}, handled by the client.
 */
// Spec: docs/techguns-spec/01-core/networking.md (PacketSpawnParticleOnEntity); GGS-04-09; Fixes PFX-01
public final class PacketSpawnParticleOnEntity extends ClientboundPacket {

    private String name = "";
    private int entityId;

    /** Required by the channel. */
    public PacketSpawnParticleOnEntity() {}

    /**
     * @param name     FX name, 1 to {@value PacketSpawnParticle#MAX_NAME} characters
     * @param entityId the entity to follow
     */
    public PacketSpawnParticleOnEntity(String name, int entityId) {
        this.name = name;
        this.entityId = entityId;
    }

    @Override
    public void encode(PacketBuffer buf) throws IOException {
        PacketStrings.write(buf, name);
        buf.writeInt(entityId);
    }

    @Override
    public void decode(PacketBuffer buf) throws IOException {
        name = PacketStrings.read(buf, PacketSpawnParticle.MAX_NAME);
        entityId = buf.readInt();
    }

    @Override
    protected void validate(ClientRules rules, World world) {
        // No target rule on purpose: the entity may not have arrived yet (PFX-01).
        rules.inRange("name length", name.length(), 1, PacketSpawnParticle.MAX_NAME);
    }

    @Override
    protected void handle(World world, EntityPlayer localPlayer) {
        FxHooks.sink()
            .spawnOnEntity(name, entityId);
    }

    /** @return the FX name, for tests */
    String name() {
        return name;
    }

    /** @return the entity id, for tests */
    int entityId() {
        return entityId;
    }
}
