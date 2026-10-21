package com.gregguns.client.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import com.gregguns.GregGuns;
import com.gregguns.client.projectile.BeamOrigin;
import com.gregguns.client.render.GunRenderers;
import com.gregguns.core.config.ClientConfig;
import com.gregguns.fx.EmitterSim;
import com.gregguns.fx.FxHooks;
import com.gregguns.fx.FxRegistry;
import com.gregguns.fx.FxResources;
import com.gregguns.fx.ParticleBudget;
import com.gregguns.fx.ParticleSpec;
import com.gregguns.fx.ParticleState;
import com.gregguns.fx.ParticleSystemDef;
import com.gregguns.fx.PendingAttachQueue;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The client half of the FX system: owns the live emitters and particles, ticks them once per client tick, and is the
 * installed {@link FxHooks.Sink}, so every FX request (a packet or a client-only call) comes through here on the
 * client thread (GGS-04-09, PFX-04).
 *
 * <ul>
 * <li>Emitters are plain objects in a list, not entities, so they never touch the server world.</li>
 * <li>A global {@link ParticleBudget} caps live particles at {@code client.particleCeiling} (GGS-04-08); bursts shrink
 * as it fills. The count is recounted every tick, so a particle that vanilla dropped from its own list is not
 * leaked.</li>
 * <li>An unknown FX name is logged once and ignored (PFX-02). A request for an entity the client does not know yet
 * waits up to two seconds for it (PFX-01).</li>
 * </ul>
 *
 * <p>
 * Side: client only. Installed by {@code GunsClientModule}.
 */
// Spec: docs/techguns-spec/04-projectiles-fx/particle-system.md (1, 3.1);
// docs/gregguns-spec/04-projectiles-fx/particle-rendering.md
@SideOnly(Side.CLIENT)
public final class FxClient implements FxHooks.Sink {

    /** Most emitters alive at once; further requests are dropped with a warning. */
    static final int MAX_EMITTERS = 1024;
    /** A particle not updated for this many ticks was dropped by someone else and is forgotten. */
    private static final int STALE_TICKS = 2;

    private static final FxClient INSTANCE = new FxClient();

    private static int tick;

    private final Random rng = new Random();
    private final ParticleBudget budget = new ParticleBudget(() -> ClientConfig.particleCeiling);
    private final PendingAttachQueue pending = new PendingAttachQueue();
    private final List<Active> emitters = new ArrayList<>();
    private final List<EntityParticle> particles = new ArrayList<>();
    private final Set<String> warned = new HashSet<>();
    private final Map<String, ResourceLocation> textures = new HashMap<>();
    private final List<ParticleSpec> scratch = new ArrayList<>();
    private final EmitterSim.Owner owner = new EmitterSim.Owner();
    private World lastWorld;
    private boolean warnedFull;

    private static final class Active {

        final EmitterSim sim;
        final int entityId; // -1 for a free emitter

        Active(EmitterSim sim, int entityId) {
            this.sim = sim;
            this.entityId = entityId;
        }
    }

    private FxClient() {}

    /** Installs the spawner and the tick handler. Call once from the client module's init. */
    public static void init() {
        FxHooks.install(INSTANCE);
        BlastFxClient.install();
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    /** @return the client tick count, used to spot stale particles */
    static int tickCount() {
        return tick;
    }

    /** @return the particle budget, for the debug counters and the stress scenario */
    public static ParticleBudget budget() {
        return INSTANCE.budget;
    }

    /** @return emitters alive now */
    public static int emitterCount() {
        return INSTANCE.emitters.size();
    }

    @Override
    public void spawnAt(String name, double x, double y, double z, double mx, double my, double mz) {
        double[] at = ownShotPosition(x, y, z);
        if (at == BeamOrigin.SKIP) {
            return;
        }
        if (at != null) {
            x = at[0];
            y = at[1];
            z = at[2];
        }
        for (ParticleSystemDef def : resolve(name)) {
            add(new Active(EmitterSim.free(def, rng, x, y, z, mx, my, mz), -1));
        }
    }

    @Override
    public void spawnOnEntity(String name, int entityId) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return;
        }
        if (world.getEntityByID(entityId) == null) {
            pending.offer(name, entityId, PendingAttachQueue.DEFAULT_WAIT_TICKS);
            return;
        }
        attachNow(name, entityId);
    }

    private void attachNow(String name, int entityId) {
        for (ParticleSystemDef def : resolve(name)) {
            add(new Active(EmitterSim.attached(def, rng), entityId));
        }
    }

    private List<ParticleSystemDef> resolve(String name) {
        FxRegistry registry = FxResources.shared();
        List<ParticleSystemDef> defs = registry.resolve(name);
        if (defs.isEmpty() && warned.add(name.toLowerCase(Locale.ROOT))) {
            GregGuns.LOG.warn("FX '{}' is not defined; ignoring it", name);
        }
        return defs;
    }

    private void add(Active a) {
        if (emitters.size() >= MAX_EMITTERS) {
            if (!warnedFull) {
                warnedFull = true;
                GregGuns.LOG.warn("FX emitter limit ({}) reached; dropping new effects", MAX_EMITTERS);
            }
            return;
        }
        emitters.add(a);
    }

    /**
     * Ticks emitters and particles. Runs at the end of each client tick.
     *
     * @param event the tick event
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world != lastWorld) {
            reset();
            lastWorld = world;
        }
        if (world == null || mc.effectRenderer == null || paused(mc)) {
            return;
        }
        tick++;
        pending.tick(id -> world.getEntityByID(id) != null, this::attachNow);
        Iterator<Active> it = emitters.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            scratch.clear();
            if (a.entityId < 0) {
                a.sim.tickFree(budget, scratch);
            } else {
                Entity e = world.getEntityByID(a.entityId);
                fill(owner, e);
                if (!owner.dead && e != null && relocateOwner(e, owner)) {
                    continue; // no muzzle known: skip this emitter for the shooter only
                }
                a.sim.tickAttached(owner, budget, scratch);
            }
            for (ParticleSpec spec : scratch) {
                spawnParticle(mc, world, a.sim.def(), spec);
            }
            if (a.sim.isDead()) {
                it.remove();
            }
        }
        prune();
    }

    /**
     * Own-shot rule: the server spawns shot FX at the shooter's eye, which is inside the local camera. In first person
     * they move to the muzzle (BeamOrigin math, as RenderBeam does); other viewers keep the true origin.
     *
     * @return null to keep the position, {@link BeamOrigin#SKIP} to drop it, else the muzzle position
     */
    private static double[] ownShotPosition(double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.thePlayer;
        if (p == null || mc.gameSettings.thirdPersonView != 0 || mc.renderViewEntity != p) {
            return null;
        }
        double[] cam = { p.posX, p.posY, p.posZ };
        double[] pos = { x, y, z };
        if (!BeamOrigin.isOwnShot(pos, cam)) {
            return null;
        }
        double[] eye = GunRenderers.firstPersonMuzzle(p.getHeldItem());
        double[] off = eye == null ? null : BeamOrigin.eyeToWorld(eye, p.rotationYaw, p.rotationPitch);
        return BeamOrigin.relocate(pos, cam, true, off);
    }

    /** Moves an attached owner at the eye to the muzzle; @return true if the emitter must be skipped this tick. */
    private static boolean relocateOwner(Entity e, EmitterSim.Owner o) {
        double[] at = ownShotPosition(e.posX, e.posY, e.posZ);
        if (at == null) {
            return false;
        }
        if (at == BeamOrigin.SKIP) {
            return true;
        }
        o.x = at[0];
        o.y = at[1];
        o.z = at[2];
        return false;
    }

    private static boolean paused(Minecraft mc) {
        return mc.isSingleplayer() && mc.currentScreen != null && mc.currentScreen.doesGuiPauseGame();
    }

    private void spawnParticle(Minecraft mc, World world, ParticleSystemDef def, ParticleSpec spec) {
        ResourceLocation texture = textures.computeIfAbsent(def.texture(), FxClient::textureOf);
        EntityParticle p = new EntityParticle(world, new ParticleState(def, spec), texture, tick);
        particles.add(p);
        mc.effectRenderer.addEffect(p);
    }

    private static ResourceLocation textureOf(String texture) {
        return texture.indexOf(':') >= 0 ? new ResourceLocation(texture)
            : new ResourceLocation(GregGuns.MODID, "textures/fx/" + texture);
    }

    private static void fill(EmitterSim.Owner o, Entity e) {
        if (e == null || e.isDead) {
            o.dead = true;
            return;
        }
        o.dead = false;
        o.x = e.posX;
        o.y = e.boundingBox != null ? e.boundingBox.minY : e.posY;
        o.z = e.posZ;
        o.yaw = e.rotationYaw;
        o.pitch = e.rotationPitch;
        double mx = e.motionX;
        double my = e.motionY;
        double mz = e.motionZ;
        if (mx == 0 && my == 0 && mz == 0) {
            // The client may not receive motion for fast projectiles; the position change says where it went.
            mx = e.posX - e.lastTickPosX;
            my = e.posY - e.lastTickPosY;
            mz = e.posZ - e.lastTickPosZ;
        }
        o.mx = mx;
        o.my = my;
        o.mz = mz;
    }

    private void prune() {
        Iterator<EntityParticle> it = particles.iterator();
        while (it.hasNext()) {
            EntityParticle p = it.next();
            if (p.isDead) {
                it.remove();
            } else if (tick - p.lastUpdateTick > STALE_TICKS) {
                p.setDead();
                it.remove();
            }
        }
        budget.resync(particles.size());
    }

    private void reset() {
        for (EntityParticle p : particles) {
            p.setDead();
        }
        particles.clear();
        emitters.clear();
        pending.clear();
        budget.resync(0);
        warnedFull = false;
    }
}
