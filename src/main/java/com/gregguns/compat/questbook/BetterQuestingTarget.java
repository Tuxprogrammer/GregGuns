package com.gregguns.compat.questbook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.google.gson.JsonObject;
import com.gregguns.GregGuns;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.utils.NBTConverter;
import betterquesting.handlers.SaveLoadHandler;
import betterquesting.network.handlers.NetChapterSync;
import betterquesting.network.handlers.NetQuestSync;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLine;
import betterquesting.questing.QuestLineDatabase;
import betterquesting.questing.QuestLineEntry;
import cpw.mods.fml.common.FMLCommonHandler;

/**
 * The only class that touches BetterQuesting (3.8.89-GTNH). Load it only behind
 * {@code Loader.isModLoaded("betterquesting")}; {@link com.gregguns.compat.BetterQuestingInstall} does. Server side.
 *
 * <p>
 * It does what BetterQuesting's own {@code /bq_admin default load} does, but for our quests only and into the live
 * databases, so no other quest, line or player progress is touched.
 */
// Spec: docs/gregguns-spec/12-questbook/authoring.md (File layout, IDs); design record 0015
public final class BetterQuestingTarget implements QuestbookTarget {

    /** JSON to NBT exactly as the DefaultQuests loader reads a file. */
    // Source:
    // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L360-L361
    private static NBTTagCompound toNbt(JsonObject json) {
        return NBTConverter.JSONtoNBT_Object(json, new NBTTagCompound(), true);
    }

    @Override
    public Set<UUID> questIds() {
        return new HashSet<>(QuestDatabase.INSTANCE.keySet());
    }

    @Override
    public Set<UUID> lineIds() {
        return new HashSet<>(QuestLineDatabase.INSTANCE.keySet());
    }

    @Override
    public List<UUID> lineOrder() {
        List<UUID> ids = new ArrayList<>();
        // Source (the sorted view; the raw order list is protected and need not hold every line):
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestLineDatabase.java#L72-L76
        for (Map.Entry<UUID, IQuestLine> entry : QuestLineDatabase.INSTANCE.getOrderedEntries()) {
            ids.add(entry.getKey());
        }
        return ids;
    }

    @Override
    public void putQuest(UUID id, JsonObject definition) {
        NBTTagCompound tag = toNbt(definition);
        IQuest quest = QuestDatabase.INSTANCE.get(id);
        if (quest == null) {
            // Source:
            // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestDatabase.java#L26-L30
            quest = QuestDatabase.INSTANCE.createNew(id);
            quest.readFromNBT(tag);
            return;
        }
        // Replace the definition inside the existing QuestInstance. Completion data lives in the instance and is kept
        // by readFromNBT; task progress lives in the tasks, which readFromNBT rebuilds, so snapshot it and put it back.
        // Source:
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestInstance.java#L541-L547
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestInstance.java#L605-L640
        NBTTagCompound progress = quest.writeProgressToNBT(new NBTTagCompound(), null);
        quest.readFromNBT(tag);
        quest.readProgressFromNBT(progress, false);
    }

    @Override
    public void putLine(UUID id, JsonObject definition, Map<UUID, JsonObject> positions) {
        // Source:
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L392-L414
        QuestLine line = new QuestLine();
        line.readFromNBT(toNbt(definition));
        for (Map.Entry<UUID, JsonObject> position : positions.entrySet()) {
            line.put(position.getKey(), new QuestLineEntry(toNbt(position.getValue())));
        }
        QuestLineDatabase.INSTANCE.put(id, line);
    }

    @Override
    public void setLineOrder(List<UUID> order) {
        // Source:
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/questing/QuestLineDatabase.java#L52-L58
        for (int i = 0; i < order.size(); i++) {
            QuestLineDatabase.INSTANCE.setOrderIndex(order.get(i), i);
        }
    }

    @Override
    public void save() {
        // Source (saveDatabases writes QuestDatabase.json when dirty, as BetterQuesting's own world save does):
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/handlers/SaveLoadHandler.java#L121-L149
        SaveLoadHandler.INSTANCE.markDirty();
        SaveLoadHandler.INSTANCE.saveDatabases();
    }

    @Override
    public void sync() {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager() == null
            || server.getConfigurationManager().playerEntityList.isEmpty()) {
            // Nobody is connected; BetterQuesting sends the full database to each player on login.
            return;
        }
        // Source (the same three calls /bq_admin default load ends with):
        // https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/betterquesting/commands/admin/QuestCommandDefaults.java#L478-L481
        NetQuestSync.quickSync(null, true, true);
        NetChapterSync.sendSync(null, null);
        GregGuns.LOG.debug("Questbook install synced to connected clients");
    }
}
