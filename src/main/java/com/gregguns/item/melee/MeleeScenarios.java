package com.gregguns.item.melee;

import java.util.UUID;

import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import com.gregguns.combat.DamageModel;
import com.gregguns.combat.DamageType;
import com.gregguns.combat.GunDamageSource;
import com.gregguns.core.testing.Scenario;
import com.gregguns.core.testing.ScenarioContext;
import com.gregguns.core.testing.ScenarioRegistry;
import com.gregguns.core.testing.ScenarioResult;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The {@code melee.*} in-game scenarios: a fake player hits a zombie with the knife and the crowbar through the real
 * vanilla attack path and the health lost must equal the damage model's figure for the tool's raw damage. Dev
 * environment only. Server side.
 */
// Spec: docs/techguns-spec/06-items/special-items.md (Combat Knife, Crowbar); docs/testing/README.md
public final class MeleeScenarios {

    private static final float EPS = 0.05F;
    private static final String NAME = "[GregGunsMeleeTest]";

    private MeleeScenarios() {}

    /**
     * Registers the scenarios.
     *
     * @param registry the registry to add to
     */
    public static void registerAll(ScenarioRegistry registry) {
        registry.register(new Strike("melee.knife", "combat_knife", MeleeMath.HAND_DAMAGE + MeleeMath.knifeBonus()));
        registry.register(new Strike("melee.crowbar", "crowbar", MeleeMath.HAND_DAMAGE + MeleeMath.crowbarBonus()));
    }

    /** Remembers the source of the first hurt event it sees. */
    public static final class SourceSpy {

        /** The last source seen. */
        public volatile DamageSource source;

        /**
         * @param event the hurt event
         */
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onHurt(LivingHurtEvent event) {
            source = event.source;
        }
    }

    private static final class Strike implements Scenario {

        private final String name;
        private final String itemName;
        private final float rawDamage;

        Strike(String name, String itemName, float rawDamage) {
            this.name = name;
            this.itemName = itemName;
            this.rawDamage = rawDamage;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "A fake player hits a zombie with the " + itemName
                + " through the vanilla attack path: it loses the damage model's figure for "
                + rawDamage
                + " PHYSICAL damage, the source is a GregGuns PHYSICAL source, and the tool loses 1 or 2 durability.";
        }

        @Override
        public ScenarioResult run(ScenarioContext ctx) {
            ScenarioResult.Builder out = ScenarioResult.builder(name);
            Item item = cpw.mods.fml.common.registry.GameRegistry.findItem("gregguns", itemName);
            if (item == null) {
                out.check(false, itemName + " is not registered");
                return out.build();
            }
            for (int dx = -1; dx <= 2; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    ctx.setBlock(dx, -1, dz, Blocks.stone, 0);
                    for (int dy = 0; dy <= 3; dy++) {
                        ctx.setBlock(dx, dy, dz, Blocks.air, 0);
                    }
                }
            }
            FakePlayer attacker = new FakePlayer(
                (WorldServer) ctx.world(),
                new GameProfile(UUID.nameUUIDFromBytes(NAME.getBytes()), NAME));
            attacker.setLocationAndAngles(ctx.originX() + 0.5, ctx.originY(), ctx.originZ() + 0.5, -90.0F, 0.0F);
            ItemStack stack = new ItemStack(item);
            attacker.inventory.setInventorySlotContents(0, stack);
            attacker.inventory.currentItem = 0;
            // A real player's tick applies the held item's attribute modifiers; a fake player is not ticked.
            attacker.getAttributeMap()
                .applyAttributeModifiers(stack.getAttributeModifiers());

            EntityZombie target = new EntityZombie(ctx.world());
            target.setLocationAndAngles(ctx.originX() + 1.2, ctx.originY(), ctx.originZ() + 0.5, 90.0F, 0.0F);
            target.setCurrentItemOrArmor(4, new ItemStack(Blocks.pumpkin));
            ctx.spawn(target);

            SourceSpy spy = new SourceSpy();
            MinecraftForge.EVENT_BUS.register(spy);
            try {
                attacker.attackTargetEntityWithCurrentItem(target);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(spy);
            }
            double armor = target.getTotalArmorValue();
            float expected = (float) DamageModel
                .resolve(DamageType.PHYSICAL, rawDamage, 0.0, share -> share * DamageModel.vanillaArmorFactor(armor));
            float lost = target.getMaxHealth() - target.getHealth();
            out.check(Math.abs(expected - lost) < EPS, "zombie should lose " + expected + " but lost " + lost);
            out.check(
                spy.source instanceof GunDamageSource && ((GunDamageSource) spy.source).hit()
                    .type() == DamageType.PHYSICAL,
                "the hurt source should be a GregGuns PHYSICAL source, was " + spy.source);
            out.check(
                spy.source != null && spy.source.getEntity() == attacker,
                "the source should name the attacker as the killer");
            int wear = stack.getItemDamage();
            out.check(wear >= 1 && wear <= 2, "the tool should lose 1 or 2 durability, lost " + wear);
            if (item instanceof ItemCrowbar) {
                out.check(item.getHarvestLevel(stack, "pickaxe") == 2, "the crowbar should mine at level 2");
                out.check(
                    item.getToolClasses(stack)
                        .contains("pickaxe"),
                    "the crowbar should report the pickaxe tool class");
                out.check(
                    item.canHarvestBlock(Blocks.iron_ore, stack),
                    "the crowbar should be able to harvest iron ore (a stone pickaxe can)");
            } else {
                out.check(
                    item.getMaxDamage() == MeleeMath.STEEL_DURABILITY,
                    "the knife should have 1000 uses, has " + item.getMaxDamage());
            }
            return out.build();
        }
    }
}
