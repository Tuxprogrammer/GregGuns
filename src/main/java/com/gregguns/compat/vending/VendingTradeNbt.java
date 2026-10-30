package com.gregguns.compat.vending;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.gregguns.item.coin.CoinDenomination;

/**
 * Writes {@link VendingCatalog} offers in the Vending Machine's trade-database NBT, the same tree its
 * {@code tradeDatabase.json} holds, so the machine reads them with its own loader. One trade group per offer: a fixed
 * UUID, a tab, no cooldown, no trade cap, one trade (coins in the input slots for the item) and, when the gate quest is
 * known, a BetterQuesting requirement. Every GregGuns group's UUID has {@link #ID_HIGH} as its high half, which is how
 * GregGuns finds its own groups in a live database. Both sides; Minecraft NBT types only.
 */
// Spec: docs/gregguns-spec/06-items/coins-and-vending.md (Mechanism)
// Source (group keys id/category/cooldown/maxTrades/trades/requirements; UUID as <prefix>High/Low longs):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/TradeGroup.java#L55-L111
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/util/NBTConverter.java#L46-L75
// Source (trade keys fromItems/toItems; an item is id/Count/Damage/OreDict, id by registry name):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/trade/Trade.java#L68-L97
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/util/BigItemStack.java#L165-L190
// Source (requirement {name: betterquesting, quest: {questIDHigh, questIDLow}}):
// https://github.com/GTNewHorizons/VendingMachine/blob/0.4.101/src/main/java/com/cubefury/vendingmachine/integration/betterquesting/BqCondition.java#L38-L50
public final class VendingTradeNbt {

    /** The high 64 bits of every GregGuns trade group UUID: "GregGuns" in ASCII. */
    public static final long ID_HIGH = 0x4772656747756E73L;

    /** The database's list of trade groups. */
    public static final String GROUPS = "tradeGroups";
    /** The database's format version. */
    public static final String VERSION = "version";

    private static final String GROUP_ID = "id";
    private static final String GROUP_ID_HIGH = "tradeGroupIDHigh";
    private static final String GROUP_ID_LOW = "tradeGroupIDLow";
    private static final String BQ_CONDITION = "betterquesting";

    private VendingTradeNbt() {}

    /**
     * @param offerKey an offer's {@link VendingCatalog.Offer#key()}
     * @return the trade group UUID: {@link #ID_HIGH}, then the low half of the name-based UUID of the key
     */
    public static UUID groupId(String offerKey) {
        UUID hashed = UUID.nameUUIDFromBytes(("gregguns:vending:" + offerKey).getBytes(StandardCharsets.UTF_8));
        return new UUID(ID_HIGH, hashed.getLeastSignificantBits());
    }

    /**
     * Builds a database tree holding only the given offers, for a merge into the live database.
     *
     * @param offers  the offers to write
     * @param version the live database's version, copied so the merge leaves it unchanged
     * @param coinId  the coin's registry id ({@code gregguns:coin})
     * @param gates   quest key to BetterQuesting quest ID, for the gate quests that exist; an offer whose gate quest
     *                is missing gets no requirement and is open to everyone
     * @return the tree
     */
    public static NBTTagCompound database(List<VendingCatalog.Offer> offers, int version, String coinId,
        Map<String, UUID> gates) {
        NBTTagCompound db = new NBTTagCompound();
        db.setInteger(VERSION, version);
        NBTTagList groups = new NBTTagList();
        for (VendingCatalog.Offer offer : offers) {
            groups.appendTag(group(offer, coinId, gates.get(offer.questKey())));
        }
        db.setTag(GROUPS, groups);
        return db;
    }

    /**
     * @param offer  the offer
     * @param coinId the coin's registry id
     * @param gate   the gate quest's ID, or {@code null} for an open trade
     * @return one trade group
     */
    static NBTTagCompound group(VendingCatalog.Offer offer, String coinId, UUID gate) {
        NBTTagCompound group = new NBTTagCompound();
        UUID id = groupId(offer.key());
        NBTTagCompound idTag = new NBTTagCompound();
        idTag.setLong(GROUP_ID_HIGH, id.getMostSignificantBits());
        idTag.setLong(GROUP_ID_LOW, id.getLeastSignificantBits());
        group.setTag(GROUP_ID, idTag);
        group.setString(
            "category",
            offer.category()
                .key());
        group.setInteger("cooldown", -1);
        group.setInteger("maxTrades", -1);

        NBTTagCompound trade = new NBTTagCompound();
        NBTTagList from = new NBTTagList();
        CoinDenomination.Stack price = offer.price();
        from.appendTag(
            stack(
                coinId,
                price.denomination()
                    .meta(),
                price.count()));
        trade.setTag("fromItems", from);
        NBTTagList to = new NBTTagList();
        to.appendTag(stack("gregguns:" + offer.item(), offer.meta(), offer.count()));
        trade.setTag("toItems", to);
        NBTTagList trades = new NBTTagList();
        trades.appendTag(trade);
        group.setTag("trades", trades);

        NBTTagList requirements = new NBTTagList();
        if (gate != null) {
            NBTTagCompound condition = new NBTTagCompound();
            condition.setString("name", BQ_CONDITION);
            NBTTagCompound quest = new NBTTagCompound();
            quest.setLong("questIDHigh", gate.getMostSignificantBits());
            quest.setLong("questIDLow", gate.getLeastSignificantBits());
            condition.setTag("quest", quest);
            requirements.appendTag(condition);
        }
        group.setTag("requirements", requirements);
        return group;
    }

    /**
     * @param id    a registry id
     * @param meta  the item damage
     * @param count how many
     * @return an item in the Vending Machine's form: registry name, int count, short damage, no ore dictionary
     */
    static NBTTagCompound stack(String id, int meta, int count) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", id);
        tag.setInteger("Count", count);
        tag.setShort("Damage", (short) meta);
        tag.setString("OreDict", "");
        return tag;
    }

    /**
     * @param group a trade group tree
     * @return true if it is a GregGuns group (its UUID's high half is {@link #ID_HIGH})
     */
    public static boolean isOurs(NBTTagCompound group) {
        return group.hasKey(GROUP_ID, Constants.NBT.TAG_COMPOUND) && group.getCompoundTag(GROUP_ID)
            .getLong(GROUP_ID_HIGH) == ID_HIGH;
    }

    /**
     * Drops every GregGuns group from a database tree in place.
     *
     * @param database a database tree (the live database's {@code writeToNBT})
     * @return how many groups were dropped
     */
    public static int removeOurs(NBTTagCompound database) {
        NBTTagList groups = database.getTagList(GROUPS, Constants.NBT.TAG_COMPOUND);
        NBTTagList kept = new NBTTagList();
        int removed = 0;
        for (int i = 0; i < groups.tagCount(); i++) {
            NBTTagCompound group = groups.getCompoundTagAt(i);
            if (isOurs(group)) {
                removed++;
            } else {
                kept.appendTag(group);
            }
        }
        database.setTag(GROUPS, kept);
        return removed;
    }
}
