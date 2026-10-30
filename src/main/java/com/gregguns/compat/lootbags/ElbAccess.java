package com.gregguns.compat.lootbags;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.EnumRarity;

/**
 * Every touch of EnhancedLootBags' internals, by reflection: ELB has no API, and GregGuns (MIT) does not link against
 * it
 * (GPL-3.0) or need its jar to build. Handles are looked up once by {@link #connect()}; if anything is missing (another
 * ELB version) the lookup fails and GregGuns leaves ELB alone. Pinned to ELB 1.3.5, the version GTNH 2.9.0 ships.
 *
 * <p>
 * ELB keeps two group lists in its {@code LootGroupsHandler}: the local one (read from {@code LootBags.xml}; used to
 * open bags, for icons, the tooltip and NEI) and the client one (the server's group headers, synced at login; used for
 * names, rarity and the creative tab). At startup both fields hold the same object. Every change here is copy on
 * write: a new {@code LootGroups} is filled and then stored in the field, so a reader on another thread (the client in
 * single player) never iterates a list that is being changed.
 */
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/EnhancedLootBags.java#L50
// (public static LootGroupsHandler LootGroupHandler, set in ELB's preInit, L76-L78)
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/LootGroupsHandler.java#L56-L69
// (the _mLootGroups and _mClientSideLootGroups fields and their getters; ReloadLootGroups L400-L438 assigns both;
// sendClientUpdate() L450-L467 sends the header copy to every player)
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/serializer/LootGroups.java
// (JAXB-annotated LootGroups / LootGroup / Drop; getLootTable() and getDrops() return the live ArrayLists;
// updateMaxWeight() refreshes the cached weight sum)
// Source:
// https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/serializer/LootGroupsFactory.java
// (createLootGroup and createDrop)
final class ElbAccess {

    private static final String PKG = "eu.usrv.enhancedlootbags.";

    private final Object handler;
    private final Class<?> groupsClass;
    private final Field localField;
    private final Field clientField;
    private final Method getLootTable;
    private final Method getGroupId;
    private final Method getDrops;
    private final Method updateMaxWeight;
    private final Method getItemName;
    private final Method sendClientUpdate;
    private final Object factory;
    private final Method createGroup;
    private final Method createDrop;

    private ElbAccess(Object handler) throws ReflectiveOperationException {
        this.handler = handler;
        Class<?> handlerClass = handler.getClass();
        groupsClass = Class.forName(PKG + "core.serializer.LootGroups");
        Class<?> groupClass = Class.forName(PKG + "core.serializer.LootGroups$LootGroup");
        Class<?> dropClass = Class.forName(PKG + "core.serializer.LootGroups$LootGroup$Drop");
        localField = handlerClass.getDeclaredField("_mLootGroups");
        clientField = handlerClass.getDeclaredField("_mClientSideLootGroups");
        localField.setAccessible(true);
        clientField.setAccessible(true);
        getLootTable = groupsClass.getMethod("getLootTable");
        getGroupId = groupClass.getMethod("getGroupID");
        getDrops = groupClass.getMethod("getDrops");
        updateMaxWeight = groupClass.getMethod("updateMaxWeight");
        getItemName = dropClass.getMethod("getItemName");
        sendClientUpdate = handlerClass.getDeclaredMethod("sendClientUpdate");
        sendClientUpdate.setAccessible(true);
        Class<?> factoryClass = Class.forName(PKG + "core.serializer.LootGroupsFactory");
        factory = factoryClass.newInstance();
        createGroup = factoryClass.getMethod(
            "createLootGroup",
            int.class,
            String.class,
            EnumRarity.class,
            int.class,
            int.class,
            boolean.class);
        createDrop = factoryClass
            .getMethod("createDrop", String.class, String.class, int.class, boolean.class, int.class, int.class);
    }

    /**
     * Looks up ELB's handler and every member GregGuns uses.
     *
     * @return the access, or {@code null} if ELB is not loaded yet or its internals differ from 1.3.5
     */
    static ElbAccess connect() {
        try {
            Object handler = Class.forName(PKG + "EnhancedLootBags")
                .getField("LootGroupHandler")
                .get(null);
            if (handler == null) {
                // ELB sets the handler in its preInit; GregGuns must load after it (the @Mod dependencies string).
                com.gregguns.GregGuns.LOG.error("EnhancedLootBags has not run its preInit yet; no GregGuns loot bags");
                return null;
            }
            return new ElbAccess(handler);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            com.gregguns.GregGuns.LOG
                .warn("EnhancedLootBags internals are not the 1.3.5 ones; no GregGuns loot bags", e);
            return null;
        }
    }

    /**
     * Turns ELB-format XML into ELB's own group objects with JAXB, as ELB reads its {@code LootBags.xml}, so every
     * attribute (the icon included) is set the way ELB would set it.
     *
     * @param xml the file text
     * @return the {@code LootGroup} objects, in file order
     * @throws ReflectiveOperationException if JAXB cannot bind ELB's classes
     */
    // Source:
    // https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/LootGroupsHandler.java#L404-L416
    List<Object> unmarshal(String xml) throws ReflectiveOperationException {
        try {
            javax.xml.bind.JAXBContext ctx = javax.xml.bind.JAXBContext.newInstance(groupsClass);
            Object groups = ctx.createUnmarshaller()
                .unmarshal(new StringReader(xml));
            return new ArrayList<>(table(groups));
        } catch (javax.xml.bind.JAXBException e) {
            throw new ReflectiveOperationException("JAXB could not read the GregGuns groups", e);
        }
    }

    /** @return ELB's local group list object (the one bags open from), or {@code null} */
    Object localGroups() throws ReflectiveOperationException {
        return localField.get(handler);
    }

    /** @return ELB's client group list object (names, rarity, creative tab), or {@code null} */
    Object clientGroups() throws ReflectiveOperationException {
        return clientField.get(handler);
    }

    /**
     * @param groups a {@code LootGroups} object
     * @return its live list of {@code LootGroup}
     */
    @SuppressWarnings("unchecked")
    List<Object> table(Object groups) throws ReflectiveOperationException {
        return (List<Object>) getLootTable.invoke(groups);
    }

    /**
     * @param group a {@code LootGroup}
     * @return its {@code GroupMetaID}
     */
    int id(Object group) throws ReflectiveOperationException {
        return (Integer) getGroupId.invoke(group);
    }

    /**
     * @param group a {@code LootGroup}
     * @return its live list of {@code Drop}
     */
    @SuppressWarnings("unchecked")
    List<Object> drops(Object group) throws ReflectiveOperationException {
        return (List<Object>) getDrops.invoke(group);
    }

    /**
     * @param drop a {@code Drop}
     * @return its {@code ItemName}
     */
    String itemName(Object drop) throws ReflectiveOperationException {
        return (String) getItemName.invoke(drop);
    }

    /** @param group a {@code LootGroup} whose drops changed; recomputes the cached weight sum */
    void updateMaxWeight(Object group) throws ReflectiveOperationException {
        updateMaxWeight.invoke(group);
    }

    /**
     * @param id        the group id
     * @param name      the group name (a lang key)
     * @param rarity    the rarity
     * @param items     items per bag (min and max)
     * @param withTrash whether the group merges its trash group
     * @return a new {@code LootGroup} with no drops
     */
    Object newGroup(int id, String name, EnumRarity rarity, int items, boolean withTrash)
        throws ReflectiveOperationException {
        return createGroup.invoke(factory, id, name, rarity, items, items, withTrash);
    }

    /**
     * @param itemName   the item, {@code mod:name[:meta]}
     * @param identifier the drop id (unique within its group)
     * @param amount     the stack size
     * @return a new unlimited {@code Drop} of weight 100
     */
    Object newDrop(String itemName, String identifier, int amount) throws ReflectiveOperationException {
        return createDrop.invoke(factory, itemName, identifier, amount, false, 100, 0);
    }

    /**
     * Replaces ELB's local list object with a copy that has {@code add} appended and {@code remove} left out. The
     * client list field is moved along when it held the same object (as it does until the first server sync).
     *
     * @param add    groups to append
     * @param remove groups to leave out, by identity
     * @return true if the field changed
     */
    boolean replaceLocal(List<Object> add, List<Object> remove) throws ReflectiveOperationException {
        Object old = localGroups();
        if (old == null || add.isEmpty() && remove.isEmpty()) return false;
        Object fresh = copyOf(old, add, remove);
        boolean sameAsClient = clientGroups() == old;
        localField.set(handler, fresh);
        if (sameAsClient) clientField.set(handler, fresh);
        return true;
    }

    /**
     * Replaces ELB's client list object (when it is not the local one) with a copy that has {@code add} appended.
     *
     * @param add groups to append
     * @return true if the field changed
     */
    boolean appendClient(List<Object> add) throws ReflectiveOperationException {
        Object old = clientGroups();
        if (add.isEmpty()) return false;
        if (old == null) old = groupsClass.newInstance();
        clientField.set(handler, copyOf(old, add, new ArrayList<>()));
        return true;
    }

    private Object copyOf(Object old, List<Object> add, List<Object> remove) throws ReflectiveOperationException {
        Object fresh = groupsClass.newInstance();
        List<Object> table = table(fresh);
        for (Object g : table(old)) {
            boolean dropped = false;
            for (Object r : remove) {
                if (r == g) dropped = true;
            }
            if (!dropped) table.add(g);
        }
        table.addAll(add);
        return fresh;
    }

    /** Server side: sends every connected player ELB's group headers again, as ELB does after a reload. */
    void sendClientUpdate() throws ReflectiveOperationException {
        sendClientUpdate.invoke(handler);
    }
}
