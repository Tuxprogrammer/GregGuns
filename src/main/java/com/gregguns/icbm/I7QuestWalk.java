package com.gregguns.icbm;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import com.google.common.collect.Maps;
import com.gregguns.GregGuns;
import com.gregguns.core.testing.ScenarioPoller;
import com.gregguns.core.testing.ScenarioResult;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestDatabase;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.cache.QuestCache;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import bq_standard.rewards.RewardChoice;
import bq_standard.rewards.RewardItem;
import bq_standard.rewards.RewardQuestCompletion;
import bq_standard.tasks.TaskCrafting;
import bq_standard.tasks.TaskOptionalRetrieval;
import bq_standard.tasks.TaskRetrieval;
import cpw.mods.fml.common.FMLCommonHandler;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * The I7 quest walk behind {@code testing.icbm_i7_quests} (dev only, server side, loaded only with BetterQuesting).
 * For every quest in {@code i7-quests.txt} (lines {@code <key> <uuid>}, prerequisites first) and the first connected
 * player it:
 * <ol>
 * <li>resets the player's progress on the quest and completes any prerequisite the walk has not completed itself (pack
 * tier quests and other chapters), then checks the quest is unlocked;</li>
 * <li>puts the items every retrieval task asks for into the inventory, as the game makes them: the output of a GT,
 * crafting or smelting recipe for that item and damage, picked to carry the task's NBT when the task matches NBT. It
 * fails when recipes make the item but none carries that NBT, so a quest whose NBT no player can produce shows up;</li>
 * <li>lets BetterQuesting's own inventory listener detect them, and fails a quest it doesn't complete (a direct
 * {@code detect} afterwards tells a listener problem from an item mismatch). A crafting task gets the crafting
 * event the game fires;</li>
 * <li>picks a choice, claims, and checks every reward item arrived with its damage and NBT, that no task or reward
 * item is BetterQuesting's missing-item placeholder, that a quest-completion reward completes its quest, and notes a
 * reward whose NBT matches no recipe's output.</li>
 * </ol>
 * Each quest logs one {@code I7Q} line.
 */
// Spec: docs/gregguns-spec/12-questbook/index.md; BetterQuesting 3.8.89-GTNH:
// https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskRetrieval.java#L145-L270
// (detect and onInventoryChange), .../bq_standard/handlers/EventHandler.java#L320-L343 (the inventory check),
// .../betterquesting/questing/QuestInstance.java#L243-L294 (canClaim and claimReward).
final class I7QuestWalk implements ScenarioPoller {

    private static final File LIST = new File("i7-quests.txt");
    private static final String PLACEHOLDER = "betterquesting:placeholder";
    /** Ticks a quest gets for BetterQuesting to see the items before the walk calls detect itself. */
    private static final int AUTO_TICKS = 100;
    /** Ticks the direct detect gets. */
    private static final int DIRECT_TICKS = 80;

    private final String name;
    private final List<String[]> quests;
    private final Set<UUID> walked = new HashSet<>();
    private final ScenarioResult.Builder out;
    private final Map<String, List<ItemStack>> producers = new HashMap<>();
    private int index = -1;
    private int phaseStart;
    private int phase;
    private int passed;

    private I7QuestWalk(String name, List<String[]> quests) {
        this.name = name;
        this.quests = quests;
        this.out = ScenarioResult.builder(name);
    }

    /**
     * @param name the scenario name
     * @return the pending walk, or a failure when the list or the player is missing
     */
    static ScenarioResult start(String name) {
        List<String[]> quests = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(LIST.toPath(), StandardCharsets.UTF_8)) {
                String l = line.trim();
                if (!l.isEmpty() && !l.startsWith("#")) {
                    quests.add(l.split("\\s+"));
                }
            }
        } catch (IOException e) {
            return ScenarioResult.fail(name, "cannot read " + LIST.getAbsolutePath() + ": " + e);
        }
        if (player() == null) {
            return ScenarioResult.fail(name, "needs a connected player");
        }
        I7QuestWalk walk = new I7QuestWalk(name, quests);
        walk.indexProducers();
        return ScenarioResult.pending(name, quests.size() * (AUTO_TICKS + DIRECT_TICKS + 40) + 200, walk);
    }

    private static EntityPlayerMP player() {
        List<?> ps = MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList;
        return ps.isEmpty() ? null : (EntityPlayerMP) ps.get(0);
    }

    private static IQuestDatabase db() {
        return QuestingAPI.getAPI(ApiReference.QUEST_DB);
    }

    @Override
    public ScenarioResult poll(int t) {
        EntityPlayerMP p = player();
        if (p == null) {
            return out.check(false, "the player left")
                .build();
        }
        if (index < 0 || phase == 3) {
            index++;
            if (index >= quests.size()) {
                out.note("quests walked: " + quests.size() + ", passed: " + passed);
                return out.build();
            }
            phase = 0;
            phaseStart = t;
        }
        String key = quests.get(index)[0];
        UUID id = UUID.fromString(quests.get(index)[1]);
        IQuest q = db().get(id);
        UUID pid = QuestingAPI.getQuestingUUID(p);
        if (q == null) {
            fail(key, "not in the quest database");
            phase = 3;
            return null;
        }
        if (phase == 0) {
            setup(p, pid, key, id, q);
            phase = 1;
            phaseStart = t;
            return null;
        }
        if (phase == 1) {
            if (q.isComplete(pid)) {
                claim(p, pid, key, id, q, "auto");
                return null;
            }
            if (t - phaseStart >= AUTO_TICKS) {
                ParticipantInfo info = new ParticipantInfo(p);
                for (DBEntry<ITask> task : q.getTasks()
                    .getEntries()) {
                    task.getValue()
                        .detect(info, Maps.immutableEntry(id, q));
                }
                phase = 2;
                phaseStart = t;
            }
            return null;
        }
        // phase 2: after the direct detect
        if (q.isComplete(pid)) {
            fail(key, "BetterQuesting's inventory check did not complete it; a direct detect did");
            claim(p, pid, key, id, q, "direct");
        } else if (t - phaseStart >= DIRECT_TICKS) {
            StringBuilder s = new StringBuilder("not completed with the items in the inventory; tasks:");
            for (DBEntry<ITask> task : q.getTasks()
                .getEntries()) {
                s.append(' ')
                    .append(
                        task.getValue()
                            .getClass()
                            .getSimpleName())
                    .append('=')
                    .append(
                        task.getValue()
                            .isComplete(pid));
            }
            fail(key, s.toString());
            phase = 3;
        }
        return null;
    }

    // --- steps ---------------------------------------------------------------------------------------------------

    private void setup(EntityPlayerMP p, UUID pid, String key, UUID id, IQuest q) {
        q.resetUser(pid, true);
        List<String> forced = new ArrayList<>();
        for (UUID req : q.getRequirements()) {
            IQuest rq = db().get(req);
            if (rq == null) {
                fail(key, "prerequisite " + req + " is not in the database");
                continue;
            }
            if (!rq.isComplete(pid)) {
                if (walked.contains(req)) {
                    fail(key, "prerequisite " + req + " was walked but is not complete");
                }
                rq.setComplete(pid, System.currentTimeMillis());
                forced.add(req.toString());
            }
        }
        refresh(p, id);
        out.check(q.isUnlocked(pid), key + ": unlocked once its prerequisites are complete");
        p.inventory.clearInventory(null, -1);
        List<String> sources = new ArrayList<>();
        List<ItemStack> crafted = new ArrayList<>();
        for (DBEntry<ITask> entry : q.getTasks()
            .getEntries()) {
            ITask task = entry.getValue();
            if (task instanceof TaskRetrieval) {
                TaskRetrieval r = (TaskRetrieval) task;
                for (BigItemStack big : r.requiredItems) {
                    ItemStack src = source(key, big, r.ignoreNBT, sources);
                    if (src != null) {
                        give(p, key, src, big.stackSize);
                    }
                }
            } else if (task instanceof TaskCrafting) {
                TaskCrafting c = (TaskCrafting) task;
                for (BigItemStack big : c.requiredItems) {
                    ItemStack src = source(key, big, c.ignoreNBT, sources);
                    if (src != null) {
                        ItemStack made = src.copy();
                        made.stackSize = big.stackSize;
                        crafted.add(made);
                    }
                }
            } else {
                fail(
                    key,
                    "task type the walk can't drive: " + task.getClass()
                        .getSimpleName());
            }
        }
        p.inventoryContainer.detectAndSendChanges();
        for (ItemStack made : crafted) {
            FMLCommonHandler.instance()
                .firePlayerCraftingEvent(p, made, new InventoryBasic("i7", false, 9));
        }
        GregGuns.LOG.info("I7Q {} setup forced={} items={}", key, forced.size(), sources);
    }

    private void claim(EntityPlayerMP p, UUID pid, String key, UUID id, IQuest q, String how) {
        walked.add(id);
        for (DBEntry<ITask> entry : q.getTasks()
            .getEntries()) {
            if (entry.getValue() instanceof TaskOptionalRetrieval && !entry.getValue()
                .isComplete(pid)) {
                fail(key, "the optional task is not complete although its items were given");
            }
        }
        p.inventory.clearInventory(null, -1);
        List<ItemStack> expected = new ArrayList<>();
        List<UUID> completes = new ArrayList<>();
        for (DBEntry<IReward> entry : q.getRewards()
            .getEntries()) {
            IReward r = entry.getValue();
            if (r instanceof RewardItem) {
                for (BigItemStack big : ((RewardItem) r).items) {
                    expected.addAll(rewardStacks(key, big));
                }
            } else if (r instanceof RewardChoice) {
                RewardChoice c = (RewardChoice) r;
                for (BigItemStack big : c.choices) {
                    placeholder(key, "choice", big.getBaseStack());
                }
                if (!c.choices.isEmpty()) {
                    int sel = index % c.choices.size();
                    c.setSelection(pid, sel);
                    expected.addAll(rewardStacks(key, c.choices.get(sel)));
                }
            } else if (r instanceof RewardQuestCompletion) {
                // Reset the target first, so the check below sees this reward complete it.
                UUID target = ((RewardQuestCompletion) r).questNum;
                IQuest tq = target == null ? null : db().get(target);
                if (tq != null) {
                    tq.resetUser(pid, true);
                }
                completes.add(target);
            } else {
                out.note(
                    key + ": reward type "
                        + r.getClass()
                            .getSimpleName()
                        + " (not checked)");
            }
        }
        boolean could = q.canClaim(p, false);
        out.check(could, key + ": claimable once complete");
        if (could) {
            q.claimReward(p, false);
        }
        p.inventoryContainer.detectAndSendChanges();
        List<String> missing = new ArrayList<>();
        for (ItemStack want : expected) {
            int have = 0;
            for (ItemStack s : p.inventory.mainInventory) {
                if (s != null && s.getItem() == want.getItem()
                    && s.getItemDamage() == want.getItemDamage()
                    && ItemStack.areItemStackTagsEqual(s, want)) {
                    have += s.stackSize;
                }
            }
            if (have < want.stackSize) {
                missing.add(describe(want) + " (have " + have + ")");
            }
        }
        if (!missing.isEmpty()) {
            fail(key, "rewards not delivered: " + missing);
        }
        for (UUID done : completes) {
            IQuest target = done == null ? null : db().get(done);
            if (target == null || !target.isComplete(pid)) {
                missing.add("completion of " + done);
                fail(key, "the quest-completion reward did not complete " + done);
            }
        }
        out.check(q.hasClaimed(pid), key + ": marked claimed");
        boolean ok = missing.isEmpty() && could && q.hasClaimed(pid);
        if (ok && "auto".equals(how)) {
            passed++;
        }
        GregGuns.LOG.info(
            "I7Q {} {} completed={} claimed={} rewards={} {}",
            key,
            ok && "auto".equals(how) ? "PASS" : "FAIL",
            how,
            q.hasClaimed(pid),
            expected.size(),
            missing.isEmpty() ? "" : "missing=" + missing);
        refresh(p, id);
        p.inventory.clearInventory(null, -1);
        phase = 3;
    }

    // --- items ---------------------------------------------------------------------------------------------------

    /**
     * Picks the stack to give for a task item: a recipe output for that item and damage, one that carries the task's
     * NBT when the task matches NBT; else the task's own stack, noted.
     */
    private ItemStack source(String key, BigItemStack big, boolean ignoreNbt, List<String> sources) {
        ItemStack want = big.getBaseStack();
        if (!placeholder(key, "task", want)) {
            return null;
        }
        List<ItemStack> made = producers.get(keyOf(want));
        if (made == null || made.isEmpty()) {
            sources.add(describe(want) + "<-listed");
            return want.copy();
        }
        if (ignoreNbt) {
            sources.add(describe(want) + "<-recipe");
            return made.get(0)
                .copy();
        }
        for (ItemStack m : made) {
            if (contains(m.getTagCompound(), want.getTagCompound())) {
                sources.add(describe(want) + "<-recipe(nbt)");
                return m.copy();
            }
        }
        fail(key, "no recipe output of " + describe(want) + " carries the task NBT " + want.getTagCompound());
        return want.copy();
    }

    private List<ItemStack> rewardStacks(String key, BigItemStack big) {
        List<ItemStack> stacks = big.getCombinedStacks();
        for (ItemStack s : stacks) {
            if (!placeholder(key, "reward", s)) {
                return new ArrayList<>();
            }
            List<ItemStack> made = producers.get(keyOf(s));
            if (made != null && !made.isEmpty()
                && (s.hasTagCompound() || made.get(0)
                    .hasTagCompound())) {
                boolean same = false;
                for (ItemStack m : made) {
                    same |= ItemStack.areItemStackTagsEqual(m, s);
                }
                if (!same) {
                    out.note(
                        key + ": reward "
                            + describe(s)
                            + " has NBT "
                            + s.getTagCompound()
                            + ", no recipe output has it (first: "
                            + made.get(0)
                                .getTagCompound()
                            + ")");
                }
            }
        }
        return stacks;
    }

    /** @return false (and fails the quest) when the stack is BetterQuesting's stand-in for an unknown item */
    private boolean placeholder(String key, String where, ItemStack s) {
        if (s == null || s.getItem() == null) {
            fail(key, where + " item is null");
            return false;
        }
        if (PLACEHOLDER.equals(Item.itemRegistry.getNameForObject(s.getItem()))) {
            fail(key, where + " item is missing from the pack: " + s.getTagCompound());
            return false;
        }
        return true;
    }

    private void give(EntityPlayerMP p, String key, ItemStack src, int count) {
        int left = count;
        while (left > 0) {
            ItemStack s = src.copy();
            s.stackSize = Math.min(left, Math.max(1, s.getMaxStackSize()));
            left -= s.stackSize;
            if (!p.inventory.addItemStackToInventory(s)) {
                fail(key, "the inventory is full; " + left + " of " + describe(src) + " not given");
                return;
            }
        }
    }

    /** Indexes every GT, crafting and smelting recipe output by item and damage. */
    @SuppressWarnings("unchecked")
    private void indexProducers() {
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            for (GTRecipe r : map.getAllRecipes()) {
                if (r.mOutputs != null) {
                    for (ItemStack s : r.mOutputs) {
                        addProducer(s);
                    }
                }
            }
        }
        for (IRecipe r : (List<IRecipe>) CraftingManager.getInstance()
            .getRecipeList()) {
            addProducer(r.getRecipeOutput());
        }
        for (Object s : FurnaceRecipes.smelting()
            .getSmeltingList()
            .values()) {
            addProducer((ItemStack) s);
        }
    }

    private void addProducer(ItemStack s) {
        if (s == null || s.getItem() == null) {
            return;
        }
        producers.computeIfAbsent(keyOf(s), k -> new ArrayList<>())
            .add(s);
    }

    private static String keyOf(ItemStack s) {
        return Item.itemRegistry.getNameForObject(s.getItem()) + ":" + s.getItemDamage();
    }

    private static String describe(ItemStack s) {
        return keyOf(s) + "x" + s.stackSize + (s.hasTagCompound() ? s.getTagCompound() : "");
    }

    /**
     * @return whether every tag in {@code want} is in {@code have} with the same value (BetterQuesting's partial match)
     */
    static boolean contains(NBTTagCompound have, NBTTagCompound want) {
        if (want == null || want.hasNoTags()) {
            return true;
        }
        if (have == null) {
            return false;
        }
        for (Object k : want.func_150296_c()) {
            String key = (String) k;
            NBTBase w = want.getTag(key);
            NBTBase h = have.getTag(key);
            if (h == null) {
                return false;
            }
            if (w instanceof NBTTagCompound && h instanceof NBTTagCompound) {
                if (!contains((NBTTagCompound) h, (NBTTagCompound) w)) {
                    return false;
                }
            } else if (!w.equals(h)) {
                return false;
            }
        }
        return true;
    }

    private static void refresh(EntityPlayerMP p, UUID id) {
        QuestCache qc = (QuestCache) p.getExtendedProperties(QuestCache.LOC_QUEST_CACHE.toString());
        if (qc != null) {
            qc.markQuestDirty(id);
            qc.updateCache(p);
        }
    }

    private void fail(String key, String message) {
        out.check(false, key + ": " + message);
        GregGuns.LOG.info("I7Q {} problem: {}", key, message);
    }
}
