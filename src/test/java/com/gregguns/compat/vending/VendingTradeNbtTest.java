package com.gregguns.compat.vending;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.junit.jupiter.api.Test;

import com.gregguns.api.Tier;
import com.gregguns.item.coin.CoinDenomination;

class VendingTradeNbtTest {

    private static final VendingCatalog.Offer PISTOL = new VendingCatalog.Offer(
        "item:pistol_9mm",
        "pistol_9mm",
        0,
        1,
        Tier.LV,
        VendingCatalog.Category.MISC,
        60);
    private static final VendingCatalog.Offer EGG = new VendingCatalog.Offer(
        "egg:CyberDemon",
        "npc_egg",
        67,
        1,
        Tier.EV,
        VendingCatalog.Category.MISC,
        2000);

    @Test
    void aGroupHasTheMachinesShape() {
        UUID gate = QuestGate.questId("gg.lv.intro");
        NBTTagCompound g = VendingTradeNbt.group(PISTOL, "gregguns:coin", gate);
        NBTTagCompound id = g.getCompoundTag("id");
        assertEquals(Constants.NBT.TAG_LONG, id.func_150299_b("tradeGroupIDHigh"));
        assertEquals(VendingTradeNbt.ID_HIGH, id.getLong("tradeGroupIDHigh"));
        assertEquals(
            VendingTradeNbt.groupId("item:pistol_9mm"),
            new UUID(id.getLong("tradeGroupIDHigh"), id.getLong("tradeGroupIDLow")));
        assertEquals("misc", g.getString("category"));
        assertEquals(-1, g.getInteger("cooldown"));
        assertEquals(-1, g.getInteger("maxTrades"));

        NBTTagList trades = g.getTagList("trades", Constants.NBT.TAG_COMPOUND);
        assertEquals(1, trades.tagCount());
        NBTTagCompound trade = trades.getCompoundTagAt(0);
        assertFalse(trade.hasKey("fromCurrency"), "GregGuns trades take coins as items, not wallet currency");
        NBTTagCompound coin = trade.getTagList("fromItems", Constants.NBT.TAG_COMPOUND)
            .getCompoundTagAt(0);
        assertEquals("gregguns:coin", coin.getString("id"));
        assertEquals(Constants.NBT.TAG_SHORT, coin.func_150299_b("Damage"), "Damage is a short, as ItemStack reads it");
        assertEquals(CoinDenomination.TEN.meta(), coin.getShort("Damage"));
        assertEquals(Constants.NBT.TAG_INT, coin.func_150299_b("Count"));
        assertEquals(6, coin.getInteger("Count"));
        assertEquals("", coin.getString("OreDict"));
        NBTTagCompound goods = trade.getTagList("toItems", Constants.NBT.TAG_COMPOUND)
            .getCompoundTagAt(0);
        assertEquals("gregguns:pistol_9mm", goods.getString("id"));
        assertEquals(1, goods.getInteger("Count"));

        NBTTagList reqs = g.getTagList("requirements", Constants.NBT.TAG_COMPOUND);
        assertEquals(1, reqs.tagCount());
        NBTTagCompound req = reqs.getCompoundTagAt(0);
        assertEquals("betterquesting", req.getString("name"));
        NBTTagCompound q = req.getCompoundTag("quest");
        assertEquals(gate, new UUID(q.getLong("questIDHigh"), q.getLong("questIDLow")));
    }

    @Test
    void anUngatedGroupIsOpen() {
        NBTTagCompound g = VendingTradeNbt.group(EGG, "gregguns:coin", null);
        assertEquals(
            0,
            g.getTagList("requirements", Constants.NBT.TAG_COMPOUND)
                .tagCount());
        NBTTagCompound egg = g.getTagList("trades", Constants.NBT.TAG_COMPOUND)
            .getCompoundTagAt(0)
            .getTagList("toItems", Constants.NBT.TAG_COMPOUND)
            .getCompoundTagAt(0);
        assertEquals("gregguns:npc_egg", egg.getString("id"));
        assertEquals(67, egg.getShort("Damage"));
    }

    @Test
    void theDatabaseCopiesTheVersionAndGatesOnlyKnownQuests() {
        Map<String, UUID> gates = new HashMap<>();
        gates.put("gg.lv.intro", QuestGate.questId("gg.lv.intro"));
        NBTTagCompound db = VendingTradeNbt.database(Arrays.asList(PISTOL, EGG), 7, "gregguns:coin", gates);
        assertEquals(7, db.getInteger(VendingTradeNbt.VERSION));
        NBTTagList groups = db.getTagList(VendingTradeNbt.GROUPS, Constants.NBT.TAG_COMPOUND);
        assertEquals(2, groups.tagCount());
        assertEquals(
            1,
            groups.getCompoundTagAt(0)
                .getTagList("requirements", Constants.NBT.TAG_COMPOUND)
                .tagCount());
        assertEquals(
            0,
            groups.getCompoundTagAt(1)
                .getTagList("requirements", Constants.NBT.TAG_COMPOUND)
                .tagCount(),
            "the EV intro is not in the database, so the egg is open");
    }

    @Test
    void removeOursKeepsThePacksGroups() {
        NBTTagCompound db = VendingTradeNbt
            .database(Arrays.asList(PISTOL, EGG), 1, "gregguns:coin", Collections.<String, UUID>emptyMap());
        NBTTagCompound pack = new NBTTagCompound();
        NBTTagCompound packId = new NBTTagCompound();
        packId.setLong("tradeGroupIDHigh", -9178793868474562574L);
        packId.setLong("tradeGroupIDLow", -8321622777038284087L);
        pack.setTag("id", packId);
        pack.setString("category", "raw");
        NBTTagList groups = db.getTagList(VendingTradeNbt.GROUPS, Constants.NBT.TAG_COMPOUND);
        groups.appendTag(pack);
        db.setTag(VendingTradeNbt.GROUPS, groups);

        assertTrue(VendingTradeNbt.isOurs(groups.getCompoundTagAt(0)));
        assertFalse(VendingTradeNbt.isOurs(pack));
        assertFalse(VendingTradeNbt.isOurs(new NBTTagCompound()), "a group without an id is not ours");
        assertEquals(2, VendingTradeNbt.removeOurs(db));
        NBTTagList left = db.getTagList(VendingTradeNbt.GROUPS, Constants.NBT.TAG_COMPOUND);
        assertEquals(1, left.tagCount());
        assertEquals(
            "raw",
            left.getCompoundTagAt(0)
                .getString("category"));
        assertEquals(0, VendingTradeNbt.removeOurs(db));
    }

    @Test
    void theWholeCatalogMaps() {
        List<VendingCatalog.Offer> all = VendingCatalog.build();
        NBTTagCompound db = VendingTradeNbt.database(all, 1, "gregguns:coin", Collections.<String, UUID>emptyMap());
        NBTBase list = db.getTag(VendingTradeNbt.GROUPS);
        assertEquals(all.size(), ((NBTTagList) list).tagCount());
    }
}
