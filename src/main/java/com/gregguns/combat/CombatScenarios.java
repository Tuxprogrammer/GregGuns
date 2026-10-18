package com.gregguns.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.hazards.Hazard;
import gregtech.api.hazards.IHazardProtector;

/**
 * The {@code combat.*} in-game scenarios: real sources against spawned mobs (and a fake player), with the health
 * deltas checked against the model's formulas. Dev environment only. Server side.
 */
// Spec: docs/gregguns-spec/02-guns/damage-interop.md (Tests); docs/testing/README.md
final class CombatScenarios {

    private static final float EPS = 0.01F;

    private CombatScenarios() {}

    static void registerAll(ScenarioRegistry registry) {
        registry.register(new Types());
        registry.register(new Penetration());
        registry.register(new PlayerArmor());
        registry.register(new Frames());
        registry.register(new Rejected());
        registry.register(new Listeners());
        registry.register(new Hazmat());
    }

    private static <T extends EntityLivingBase> T spawn(ScenarioContext ctx, T e, int dx) {
        e.setLocationAndAngles(ctx.originX() + 0.5 + dx, ctx.originY(), ctx.originZ() + 0.5, 0, 0);
        return ctx.spawn(e);
    }

    /** Clears frames, restores full health, then hits. Returns the health lost. */
    private static float hitFresh(EntityLivingBase target, DamageSource src, float damage) {
        target.hurtResistantTime = 0;
        target.setHealth(target.getMaxHealth());
        float before = target.getHealth();
        target.attackEntityFrom(src, damage);
        return before - target.getHealth();
    }

    private static boolean near(float expected, float actual) {
        return Math.abs(expected - actual) < EPS;
    }

    private static void armorUp(EntityLivingBase e, ItemStack... piecesHelmetFirst) {
        for (int i = 0; i < piecesHelmetFirst.length; i++) {
            e.setCurrentItemOrArmor(4 - i, piecesHelmetFirst[i]);
        }
    }

    private static void ironSet(EntityLivingBase e) {
        armorUp(
            e,
            new ItemStack(Items.iron_helmet),
            new ItemStack(Items.iron_chestplate),
            new ItemStack(Items.iron_leggings),
            new ItemStack(Items.iron_boots));
    }

    /** Every type through a real source against an unarmored pig: health delta and secondary effect. */
    private static final class Types implements Scenario {

        @Override
        public String name() {
            return "combat.types";
        }

        @Override
        public String description() {
            return "Each damage type hits an unarmored pig: health lost equals the damage; fire/ice/poison/radiation/lightning add their effect.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            for (DamageType type : DamageType.values()) {
                EntityPig pig = spawn(ctx, new EntityPig(ctx.world()), 0);
                float lost = hitFresh(pig, DamageSources.direct(type, null, 0.0), 2.0F);
                out.check(near(2.0F, lost), type + ": expected 2.0 health lost, got " + lost);
                SecondaryEffect effect = SecondaryEffects.forType(type);
                if (effect == null) {
                    out.check(
                        !pig.isBurning() && pig.getActivePotionEffects()
                            .isEmpty(),
                        type + ": expected no secondary effect");
                } else {
                    out.check(hasEffect(pig, effect), type + ": expected secondary effect " + effect.kind());
                }
                ctx.despawn(pig);
            }
            return out.build();
        }

        static boolean hasEffect(EntityLivingBase e, SecondaryEffect effect) {
            switch (effect.kind()) {
                case IGNITE:
                    return e.isBurning();
                case SLOWNESS:
                    return e.isPotionActive(Potion.moveSlowdown);
                case POISON:
                    return e.isPotionActive(Potion.poison);
                case SHOCK:
                    return e.isPotionActive(Potion.digSlowdown);
                case RADIATION_SICKNESS:
                    return e.isPotionActive(Potion.weakness) && e.isPotionActive(Potion.hunger);
                default:
                    return false;
            }
        }
    }

    /** GUN-34: penetration against a mob in vanilla armor; GUN-37: the shooter survives; GUN-45: all types alike. */
    private static final class Penetration implements Scenario {

        @Override
        public String name() {
            return "combat.penetration";
        }

        @Override
        public String description() {
            return "Mob in full iron armor: 8 damage at 0/15/100% penetration, other blockable types, unresistable, and shooter credit.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityPig target = spawn(ctx, new EntityPig(ctx.world()), 0);
            EntityZombie shooter = spawn(ctx, new EntityZombie(ctx.world()), 3);
            EntityArrow bullet = new EntityArrow(ctx.world());
            ironSet(target);
            out.checkEquals("armor points", 15, target.getTotalArmorValue());

            float pen0 = hitFresh(target, DamageSources.ranged(DamageType.PROJECTILE, bullet, shooter, 0.0), 8.0F);
            out.check(near(3.2F, pen0), "pen 0: expected 3.2, got " + pen0);
            float pen15 = hitFresh(target, DamageSources.ranged(DamageType.PROJECTILE, bullet, shooter, 0.15), 8.0F);
            out.check(near(3.92F, pen15), "pen 0.15: expected 3.92 (GUN-34), got " + pen15);
            float pen100 = hitFresh(target, DamageSources.ranged(DamageType.PROJECTILE, bullet, shooter, 1.0), 8.0F);
            out.check(near(8.0F, pen100), "pen 1.0: expected 8.0, got " + pen100);
            float unres = hitFresh(target, DamageSources.ranged(DamageType.UNRESISTABLE, bullet, shooter, 0.0), 8.0F);
            out.check(near(8.0F, unres), "unresistable: expected 8.0, got " + unres);
            for (DamageType type : DamageType.values()) {
                if (type == DamageType.UNRESISTABLE) {
                    continue;
                }
                float lost = hitFresh(
                    target,
                    DamageSources.ranged(type, bullet, shooter, 0.0)
                        .withoutSecondaryEffects(),
                    8.0F);
                out.check(
                    near(3.2F, lost),
                    type + " at pen 0: expected 3.2 (GUN-45, same armor for every type), got " + lost);
            }

            RangedDamageSource src = DamageSources.ranged(DamageType.PROJECTILE, bullet, shooter, 0.0);
            out.check(src.getEntity() == shooter, "getEntity() should be the shooter (GUN-37)");
            out.check(src.getSourceOfDamage() == bullet, "getSourceOfDamage() should be the projectile");
            out.checkEquals("damageType string", "gregguns.projectile", src.damageType);
            return out.build();
        }
    }

    /**
     * A fake player whose {@code damageEntity} (the part after {@code attackEntityFrom}) can be called directly,
     * because a fresh server player is invulnerable for its first ticks.
     */
    private static final class TestPlayer extends FakePlayer {

        TestPlayer(WorldServer world, GameProfile profile) {
            super(world, profile);
        }

        @Override
        public boolean isEntityInvulnerable() {
            return false;
        }

        float hurt(DamageSource src, float damage) {
            setHealth(getMaxHealth());
            float before = getHealth();
            damageEntity(src, damage);
            return before - getHealth();
        }
    }

    /** The player branch through Forge ApplyArmor: same numbers as the mob, and armor worn exactly once. */
    private static final class PlayerArmor implements Scenario {

        @Override
        public String name() {
            return "combat.player";
        }

        @Override
        public String description() {
            return "Fake player in full iron armor: 8 damage at 15% penetration loses 3.92 and each piece wears once.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            if (!(ctx.world() instanceof WorldServer)) {
                return out.check(false, "needs a server world")
                    .build();
            }
            TestPlayer player = new TestPlayer(
                (WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes(new byte[] { 7 }), "[GregGunsCombat]"));
            try {
                ironSet(player);
                out.checkEquals("armor points", 15, player.getTotalArmorValue());
                float lost = player.hurt(DamageSources.direct(DamageType.PROJECTILE, null, 0.15), 8.0F);
                out.check(near(3.92F, lost), "player pen 0.15: expected 3.92, got " + lost);
                for (int i = 0; i < 4; i++) {
                    ItemStack piece = player.inventory.armorInventory[i];
                    out.check(
                        piece != null && piece.getItemDamage() == 1,
                        "armor slot " + i
                            + " should be worn exactly once, damage="
                            + (piece == null ? "gone" : piece.getItemDamage()));
                }
                float unres = player.hurt(DamageSources.direct(DamageType.UNRESISTABLE, null, 0.0), 8.0F);
                out.check(near(8.0F, unres), "player unresistable: expected 8.0, got " + unres);
            } finally {
                for (int i = 0; i < 4; i++) {
                    player.inventory.armorInventory[i] = null;
                }
            }
            return out.build();
        }
    }

    /** GUN-33: i-frames respected for melee; ignoring sources land every time and leave the frames as they were. */
    private static final class Frames implements Scenario {

        @Override
        public String name() {
            return "combat.iframes";
        }

        @Override
        public String description() {
            return "Two melee hits inside the frames deal 6 + 2, not 6 + 8; ignoring sources land fully and restore the frames.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityPig z = spawn(ctx, new EntityPig(ctx.world()), 0);
            z.hurtResistantTime = 0;
            z.setHealth(10.0F);
            z.attackEntityFrom(DamageSources.direct(DamageType.PHYSICAL, null, 0.0), 6.0F);
            z.attackEntityFrom(DamageSources.direct(DamageType.PHYSICAL, null, 0.0), 8.0F);
            out.check(
                near(8.0F, 10.0F - z.getHealth()),
                "melee 6 then 8 in frames: expected 8 lost (GUN-33), got " + (10.0F - z.getHealth()));
            z.attackEntityFrom(DamageSources.direct(DamageType.PHYSICAL, null, 0.0), 5.0F);
            out.check(near(8.0F, 10.0F - z.getHealth()), "a weaker hit inside the frames should do nothing");

            z.hurtResistantTime = 0;
            z.setHealth(10.0F);
            z.attackEntityFrom(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 4.0F);
            z.attackEntityFrom(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 4.0F);
            out.check(
                near(8.0F, 10.0F - z.getHealth()),
                "two ignoring hits: expected 8 lost, got " + (10.0F - z.getHealth()));
            out.checkEquals("frames after ignoring hits", 0, z.hurtResistantTime);

            z.hurtResistantTime = 15;
            z.setHealth(10.0F);
            z.attackEntityFrom(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 4.0F);
            out.check(
                near(4.0F, 10.0F - z.getHealth()),
                "ignoring hit inside frames: expected full 4, got " + (10.0F - z.getHealth()));
            out.checkEquals("frames restored", 15, z.hurtResistantTime);
            return out.build();
        }
    }

    /**
     * Hits vanilla rejects after the attack event must not leave the target's frames cleared: dead, invulnerable, and
     * fire against Fire Resistance, by raw {@code attackEntityFrom} and by {@link DamageSources#hit}.
     */
    private static final class Rejected implements Scenario {

        @Override
        public String name() {
            return "combat.rejected";
        }

        @Override
        public String description() {
            return "Ignoring-frames hits that vanilla rejects (dead, invulnerable, fire vs Fire Resistance) leave the frames at 15 and the health alone.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());

            EntityPig dead = spawn(ctx, new EntityPig(ctx.world()), 0);
            dead.setHealth(0.0F);
            dead.hurtResistantTime = 15;
            dead.attackEntityFrom(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 4.0F);
            out.checkEquals("dead target: frames", 15, dead.hurtResistantTime);
            ctx.despawn(dead);

            EntityPig inv = spawn(ctx, new EntityPig(ctx.world()), 0);
            NBTTagCompound tag = new NBTTagCompound();
            inv.writeToNBT(tag);
            tag.setBoolean("Invulnerable", true);
            inv.readFromNBT(tag);
            inv.setHealth(10.0F);
            inv.hurtResistantTime = 15;
            inv.attackEntityFrom(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 4.0F);
            out.check(inv.isEntityInvulnerable(), "test setup: the pig should be invulnerable");
            out.checkEquals("invulnerable target: frames", 15, inv.hurtResistantTime);
            out.check(near(10.0F, inv.getHealth()), "invulnerable target lost health: " + inv.getHealth());
            ctx.despawn(inv);

            EntityPig resistant = spawn(ctx, new EntityPig(ctx.world()), 0);
            resistant.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 200, 0));
            resistant.setHealth(10.0F);
            resistant.hurtResistantTime = 15;
            resistant.attackEntityFrom(DamageSources.direct(DamageType.FIRE, null, 0.0), 4.0F);
            out.checkEquals("fire vs Fire Resistance: frames", 15, resistant.hurtResistantTime);
            out.check(near(10.0F, resistant.getHealth()), "Fire Resistance lost health: " + resistant.getHealth());
            resistant.hurtResistantTime = 15;
            boolean taken = DamageSources.hit(resistant, DamageType.FIRE, null, null, 4.0F, 0.0);
            out.check(!taken, "DamageSources.hit should report the rejection");
            out.checkEquals("fire vs Fire Resistance via hit(): frames", 15, resistant.hurtResistantTime);

            // Control: the same hit on a healthy, unprotected pig still lands in full inside the frames.
            resistant.removePotionEffect(Potion.fireResistance.id);
            resistant.hurtResistantTime = 15;
            resistant.setHealth(10.0F);
            resistant.attackEntityFrom(DamageSources.direct(DamageType.PROJECTILE, null, 0.0), 4.0F);
            out.check(
                near(4.0F, 10.0F - resistant.getHealth()),
                "control hit: expected 4 lost, got " + (10.0F - resistant.getHealth()));
            out.checkEquals("control: frames restored", 15, resistant.hurtResistantTime);
            ctx.despawn(resistant);
            return out.build();
        }
    }

    /** GGS-02-18: other mods' listeners see the hit, with the real source and the pre-armor amount. */
    private static final class Listeners implements Scenario {

        /** Records what the other mods would see. */
        public static final class Recorder {

            final List<DamageSource> attacks = new ArrayList<>();
            final List<DamageSource> hurts = new ArrayList<>();
            final List<Float> hurtAmounts = new ArrayList<>();

            @SubscribeEvent(priority = EventPriority.HIGHEST)
            public void onAttack(LivingAttackEvent e) {
                attacks.add(e.source);
            }

            @SubscribeEvent(priority = EventPriority.HIGHEST)
            public void onHurt(LivingHurtEvent e) {
                hurts.add(e.source);
                hurtAmounts.add(e.ammount);
            }
        }

        @Override
        public String name() {
            return "combat.listeners";
        }

        @Override
        public String description() {
            return "A foreign LivingAttackEvent/LivingHurtEvent listener sees the GregGuns source and the pre-armor amount.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            EntityPig target = spawn(ctx, new EntityPig(ctx.world()), 0);
            ironSet(target);
            Recorder rec = new Recorder();
            MinecraftForge.EVENT_BUS.register(rec);
            try {
                RangedDamageSource src = DamageSources.ranged(DamageType.ENERGY, null, null, 0.0);
                float lost = hitFresh(target, src, 10.0F);
                out.check(near(4.0F, lost), "energy 10 vs 15 armor: expected 4.0, got " + lost);
                out.check(
                    rec.attacks.size() == 1 && rec.attacks.get(0) == src,
                    "LivingAttackEvent should carry our source once");
                out.check(
                    rec.hurts.size() == 1 && rec.hurts.get(0) == src,
                    "LivingHurtEvent should carry our source once");
                out.check(
                    rec.hurtAmounts.size() == 1 && near(10.0F, rec.hurtAmounts.get(0)),
                    "LivingHurtEvent amount at HIGHEST should be the raw 10, got " + rec.hurtAmounts);
                out.check(src.isMagicDamage() && !src.isFireDamage(), "energy should be magic, not fire");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(rec);
            }
            return out.build();
        }
    }

    /** A stand-in hazmat armor piece: protects against every hazard; cloth armor values (7 points per set). */
    private static final class HazmatPiece extends ItemArmor implements IHazardProtector {

        HazmatPiece(int slot) {
            super(ArmorMaterial.CLOTH, 0, slot);
        }

        @Override
        public boolean protectsAgainst(ItemStack itemStack, Hazard hazard) {
            return true;
        }
    }

    /** GGS-02-22: full hazmat skips the secondary effect, never the direct damage. */
    private static final class Hazmat implements Scenario {

        @Override
        public String name() {
            return "combat.hazmat";
        }

        @Override
        public String description() {
            return "Fire, ice, poison, radiation and lightning: effect applies to a bare pig, is skipped in full hazmat, damage still lands.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name());
            DamageType[] types = { DamageType.FIRE, DamageType.ICE, DamageType.POISON, DamageType.RADIATION,
                DamageType.LIGHTNING };
            for (DamageType type : types) {
                SecondaryEffect effect = SecondaryEffects.forType(type);
                EntityPig bare = spawn(ctx, new EntityPig(ctx.world()), 0);
                hitFresh(bare, DamageSources.direct(type, null, 0.0), 2.0F);
                out.check(Types.hasEffect(bare, effect), type + ": bare pig should get " + effect.kind());

                EntityPig suited = spawn(ctx, new EntityPig(ctx.world()), 2);
                armorUp(
                    suited,
                    new ItemStack(new HazmatPiece(0)),
                    new ItemStack(new HazmatPiece(1)),
                    new ItemStack(new HazmatPiece(2)),
                    new ItemStack(new HazmatPiece(3)));
                float lost = hitFresh(suited, DamageSources.direct(type, null, 0.0), 2.0F);
                out.check(!Types.hasEffect(suited, effect), type + ": hazmat pig should not get " + effect.kind());
                float expected = 2.0F * (18.0F / 25.0F);
                out.check(
                    near(expected, lost),
                    type + ": hazmat pig damage expected " + expected + " (7 cloth armor points), got " + lost);
                ctx.despawn(bare);
                ctx.despawn(suited);
            }
            return out.build();
        }
    }
}
