package com.gregguns.armor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;

import com.gregguns.armor.logic.ArmorBonus;
import com.gregguns.armor.logic.ArmorSlot;
import com.gregguns.armor.logic.BonusLifecycle;
import com.gregguns.armor.logic.BonusMap;
import com.gregguns.armor.logic.FallMath;
import com.gregguns.armor.logic.NightVisionPolicy;
import com.gregguns.armor.logic.PowerRules;
import com.gregguns.armor.slots.SlotEquipment;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Applies worn-armor bonuses to players and removes them again. Registered on both the Forge and the FML event bus by
 * {@link ArmorModule}.
 *
 * <p>
 * Attribute bonuses (speed, extra health, knockback resistance) are applied on the server only, as non-saved modifiers
 * with fixed UUIDs, and only when the wanted set of bonuses changes ({@link BonusLifecycle}); the client gets them from
 * vanilla's attribute sync. Event bonuses (jump, fall, break speed) are stateless reads of the worn pieces. Night
 * Vision
 * follows {@link NightVisionPolicy}.
 *
 * <p>
 * Removal: bonuses are stripped on logout, death, respawn and dimension change, and implicitly on the next tick when a
 * piece comes off or breaks. No bonus touches step height here (the client module handles it with
 * {@link com.gregguns.armor.logic.StepHeightPolicy}) and none removes a potion it did not grant.
 *
 * <p>
 * Server thread only for state; event handlers also run on the client for the stateless bonuses.
 */
// Spec: docs/techguns-spec/05-armor/set-bonuses.md sections 3 and 5; BUGS.md CORE-14, CORE-15
public final class ArmorEvents {

    // Fixed ids so the modifiers are found again after a reload. Random, generated once for GregGuns.
    private static final UUID SPEED_ID = UUID.fromString("7f4c2a10-3b6e-4d51-9a28-5c0e1d9b6a01");
    private static final UUID HEALTH_ID = UUID.fromString("7f4c2a10-3b6e-4d51-9a28-5c0e1d9b6a02");
    private static final UUID KNOCKBACK_ID = UUID.fromString("7f4c2a10-3b6e-4d51-9a28-5c0e1d9b6a03");

    /** Sound event of the steam armor's jump assist (sounds.json). */
    static final String STEAM_JUMP_SOUND = "gregguns:effects.steamarmor_jump";

    private final BonusLifecycle<UUID> lifecycle = new BonusLifecycle<>();
    private final Set<UUID> nightVisionOwned = new HashSet<>();
    private final SlotEquipment slots = new SlotEquipment();
    /** Last horizontal position per player (x, z), to tell walking from standing for the power drain. */
    private final Map<UUID, double[]> lastPos = new HashMap<>();

    // --- tick
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Re-evaluates a player's armor once per server tick.
     *
     * @param event the tick event; only the END phase on the server counts
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == Side.SERVER) {
            update(event.player);
        }
    }

    /**
     * Brings one player's applied bonuses in line with what they wear. Public so scenarios can drive a fake player,
     * which the game never ticks. Server side.
     *
     * @param player the player
     */
    public void update(EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return;
        }
        // Powered sets first: drain, refuel and the powered flags, so the bonuses below see this tick's charge.
        double[] last = lastPos.put(player.getUniqueID(), new double[] { player.posX, player.posZ });
        boolean moving = last != null && PowerRules.isMoving(player.posX - last[0], player.posZ - last[1]);
        ArmorPower.tick(player, moving, player.worldObj.getTotalWorldTime());
        BonusMap worn = Armor.bonuses(player);
        double speed = worn.get(ArmorBonus.SPEED);
        if (worn.get(ArmorBonus.SPEED_WATER) > 0.0 && inWaterOrLava(player)) {
            speed += worn.get(ArmorBonus.SPEED_WATER);
        }
        if (worn.get(ArmorBonus.COOLING_SYSTEM) >= 1.0 && player.isBurning()) {
            // Cooling system: puts the wearer out without a charge cost (Techguns rule).
            player.extinguish();
        }
        if (speed > 0.0 && player.isSprinting()) {
            // Techguns rule: the speed bonus doubles while sprinting.
            speed *= 2.0;
        }
        BonusLifecycle.Change change = lifecycle.update(player.getUniqueID(), worn.with(ArmorBonus.SPEED, speed));
        if (change.changed()) {
            writeAttributes(player, change.now());
        }
        // Special-slot gear (goggles, Tactical Mask, masks, back device) ticks here too and may want night vision.
        boolean gear = slots.tick(player);
        updateNightVision(player, worn.get(ArmorBonus.NIGHT_VISION) > 0.0 || gear);
    }

    private void writeAttributes(EntityPlayer player, BonusMap now) {
        setModifier(
            player.getEntityAttribute(SharedMonsterAttributes.movementSpeed),
            SPEED_ID,
            "GregGuns armor speed",
            now.get(ArmorBonus.SPEED),
            2);
        setModifier(
            player.getEntityAttribute(SharedMonsterAttributes.maxHealth),
            HEALTH_ID,
            "GregGuns armor health",
            now.get(ArmorBonus.EXTRA_HEALTH),
            0);
        setModifier(
            player.getEntityAttribute(SharedMonsterAttributes.knockbackResistance),
            KNOCKBACK_ID,
            "GregGuns armor knockback resistance",
            now.get(ArmorBonus.KNOCKBACK_RESISTANCE),
            0);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setModifier(IAttributeInstance attribute, UUID id, String name, double amount, int operation) {
        AttributeModifier old = attribute.getModifier(id);
        if (old != null) {
            attribute.removeModifier(old);
        }
        if (amount != 0.0) {
            // setSaved(false): the modifier never reaches the player's save file, so a crash or a removed mod cannot
            // leave a bonus behind.
            attribute.applyModifier(new AttributeModifier(id, name, amount, operation).setSaved(false));
        }
    }

    /**
     * Night Vision upkeep for any living entity; package-visible so scenarios can drive a mob (a fake player has no
     * network handler).
     */
    void updateNightVision(EntityLivingBase player, boolean wanted) {
        UUID id = player.getUniqueID();
        boolean owned = nightVisionOwned.contains(id);
        if (!wanted && !owned) {
            return;
        }
        PotionEffect existing = player.getActivePotionEffect(Potion.nightVision);
        NightVisionPolicy.Decision d = NightVisionPolicy
            .decide(wanted, owned, existing == null ? -1 : existing.getDuration());
        switch (d.action()) {
            case GRANT:
                player.addPotionEffect(new PotionEffect(Potion.nightVision.id, NightVisionPolicy.REFRESH_TICKS, 0));
                break;
            case REMOVE:
                player.removePotionEffect(Potion.nightVision.id);
                break;
            default:
                break;
        }
        if (d.owned()) {
            nightVisionOwned.add(id);
        } else {
            nightVisionOwned.remove(id);
        }
    }

    // --- removal
    // ------------------------------------------------------------------------------------------------------

    /**
     * Removes every bonus from a player and forgets them. Public for scenarios. Server side.
     *
     * @param player the player
     */
    public void strip(EntityPlayer player) {
        if (player.worldObj.isRemote) {
            return;
        }
        lifecycle.forget(player.getUniqueID());
        lastPos.remove(player.getUniqueID());
        slots.forget(player);
        writeAttributes(player, BonusMap.EMPTY);
        updateNightVision(player, false);
    }

    /** @param event a player logging in; clears anything left from an earlier session of the same entity id */
    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        strip(event.player);
    }

    /** @param event a player logging out; bonuses must not outlive the session */
    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        strip(event.player);
    }

    /** @param event a respawn; the new entity starts clean */
    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        strip(event.player);
    }

    /** @param event a dimension change; the next tick re-applies what the player still wears */
    @SubscribeEvent
    public void onDimensionChange(PlayerChangedDimensionEvent event) {
        strip(event.player);
    }

    /** @param event a death; bonuses end with the player */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            strip((EntityPlayer) event.entityLiving);
        }
    }

    // --- stateless event bonuses
    // --------------------------------------------------------------------------------------

    /**
     * @param event a jump; adds the jump bonus to vertical motion. Powered leggings that give the bonus pay
     *              {@link PowerRules#JUMP_COST} units, and steam leggings play the steam-jump sound
     */
    @SubscribeEvent
    public void onJump(LivingJumpEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            event.entityLiving.motionY += Armor.bonus(event.entityLiving, ArmorBonus.JUMP);
            ItemGregGunsArmor legs = ArmorPower.poweredPiece(event.entityLiving, ArmorSlot.LEGGINGS);
            if (legs != null && legs.piece()
                .poweredBonuses()
                .get(ArmorBonus.JUMP) > 0.0 && !event.entityLiving.worldObj.isRemote) {
                ArmorPower.spend(event.entityLiving, legs.set(), PowerRules.JUMP_COST);
                if (legs.set()
                    .power()
                    .kind() == com.gregguns.armor.logic.PowerSpec.PowerKind.STEAM) {
                    // Spec: set-bonuses.md 3.2 (steam jump sound, volume 0.66, pitch 1).
                    event.entityLiving.worldObj.playSoundAtEntity(event.entityLiving, STEAM_JUMP_SOUND, 0.66F, 1.0F);
                }
            }
        }
    }

    /** @param event a fall; shortens the distance by the boots' reduction and free height */
    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            BonusMap b = Armor.bonuses(event.entityLiving);
            if (b.get(ArmorBonus.FALL_REDUCTION) != 0.0 || b.get(ArmorBonus.FREE_FALL_HEIGHT) != 0.0) {
                ItemGregGunsArmor boots = ArmorPower.poweredPiece(event.entityLiving, ArmorSlot.BOOTS);
                if (boots != null && PowerRules.fallCosts(event.distance) && !event.entityLiving.worldObj.isRemote) {
                    // The boots only pay for a fall long enough to matter (Techguns: more than 3 blocks).
                    ArmorPower.spend(event.entityLiving, boots.set(), PowerRules.FALL_COST);
                }
                event.distance = (float) FallMath
                    .adjust(event.distance, b.get(ArmorBonus.FALL_REDUCTION), b.get(ArmorBonus.FREE_FALL_HEIGHT));
            }
        }
    }

    /**
     * Cooling system (HEV chestplate): fire and lava hits on a wearer with a charged chestplate are canceled and cost
     * {@link PowerRules#COOLING_COST} unit each. Runs early so other mods see no damage.
     *
     * @param event an attack on a living entity
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttack(LivingAttackEvent event) {
        if (!(event.entityLiving instanceof EntityPlayer)) {
            return;
        }
        DamageSource src = event.source;
        if (src != DamageSource.lava && src != DamageSource.inFire && src != DamageSource.onFire) {
            return;
        }
        if (Armor.bonus(event.entityLiving, ArmorBonus.COOLING_SYSTEM) >= 1.0) {
            event.setCanceled(true);
            if (!event.entityLiving.worldObj.isRemote) {
                ItemGregGunsArmor chest = ArmorPower.poweredPiece(event.entityLiving, ArmorSlot.CHESTPLATE);
                if (chest != null) {
                    ArmorPower.spend(event.entityLiving, chest.set(), PowerRules.COOLING_COST);
                }
            }
        }
    }

    private static boolean inWaterOrLava(EntityPlayer player) {
        return player.isInWater() || player.isInsideOfMaterial(net.minecraft.block.material.Material.lava);
    }

    /** @param event a block-break speed query; multiplies the speed by the break bonuses */
    @SubscribeEvent
    public void onBreakSpeed(BreakSpeed event) {
        BonusMap b = Armor.bonuses(event.entityPlayer);
        double factor = 1.0 + b.get(ArmorBonus.BREAK_SPEED);
        if (b.get(ArmorBonus.BREAK_SPEED_WATER) != 0.0 && (event.entityPlayer.isInWater()
            || event.entityPlayer.isInsideOfMaterial(net.minecraft.block.material.Material.lava))) {
            factor *= 1.0 + b.get(ArmorBonus.BREAK_SPEED_WATER);
        }
        event.newSpeed = (float) (event.newSpeed * factor);
    }

    /**
     * @param player a player
     * @return the bonuses the lifecycle believes are applied; for scenarios and tests
     */
    public BonusMap applied(EntityPlayer player) {
        return lifecycle.applied(player.getUniqueID());
    }

    /**
     * @param player a player
     * @return whether armor currently owns the player's Night Vision effect
     */
    public boolean ownsNightVision(EntityLivingBase player) {
        return nightVisionOwned.contains(player.getUniqueID());
    }
}
