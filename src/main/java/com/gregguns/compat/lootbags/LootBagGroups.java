package com.gregguns.compat.lootbags;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The GregGuns loot bag groups as plain data: the EnhancedLootBags-format XML that ships in the jar, read with the
 * JDK's
 * DOM parser, and the rules that need no Minecraft or EnhancedLootBags type (the reserved id range, the item name
 * format). {@link LootBagInjector} hands the same XML to EnhancedLootBags; this class is what the unit tests and the
 * injector's bookkeeping read. Plain Java.
 */
// Spec: docs/gregguns-spec/06-items/pack-loot.md (EnhancedLootBags)
public final class LootBagGroups {

    /** EnhancedLootBags' FML mod id (MOD-INDEX.md). */
    public static final String MOD_ID = "enhancedlootbags";
    /** The registry name of EnhancedLootBags' one bag item; the group id is its damage. */
    public static final String BAG_ITEM = "enhancedlootbags:lootbag";
    /** The groups in the jar. */
    public static final String RESOURCE = "/assets/gregguns/lootbags/groups.xml";
    /** A pack's replacement for {@link #RESOURCE}, in the config folder; used instead of it when present. */
    public static final String OVERRIDE_FILE = "gregguns-lootbags.xml";
    /** First group id GregGuns reserves (the pack's own groups use 0 to 59). */
    public static final int FIRST_ID = 1700;
    /** Last group id GregGuns reserves. */
    public static final int LAST_ID = 1799;
    /** Prefix of every GregGuns group name, which is also the name's lang key. */
    public static final String NAME_PREFIX = "gregguns.lootbag.group.";

    private LootBagGroups() {}

    /** One drop of a group: a {@code Loot} element. */
    public static final class Drop {

        public final String identifier;
        public final String itemName;
        public final int amount;
        public final String nbt;
        public final int chance;
        public final String itemGroup;
        public final int limit;
        public final boolean randomAmount;

        Drop(Element e) {
            identifier = e.getAttribute("Identifier");
            itemName = e.getAttribute("ItemName");
            amount = intAttr(e, "Amount");
            nbt = e.getAttribute("NBTTag");
            chance = intAttr(e, "Chance");
            itemGroup = e.getAttribute("ItemGroup");
            limit = intAttr(e, "LimitedDropCount");
            randomAmount = Boolean.parseBoolean(e.getAttribute("RandomAmount"));
        }
    }

    /** One group: a {@code LootGroup} element. */
    public static final class Group {

        public final int id;
        public final String name;
        public final int rarity;
        public final int minItems;
        public final int maxItems;
        public final boolean combineTrash;
        public final int trashGroup;
        public final String icon;
        public final List<Drop> drops;

        Group(Element e) {
            id = intAttr(e, "GroupMetaID");
            name = e.getAttribute("GroupName");
            rarity = intAttr(e, "Rarity");
            minItems = intAttr(e, "MinItems");
            maxItems = intAttr(e, "MaxItems");
            combineTrash = Boolean.parseBoolean(e.getAttribute("CombineTrashGroup"));
            trashGroup = intAttr(e, "TrashGroup");
            icon = e.hasAttribute("LootbagIcon") ? e.getAttribute("LootbagIcon") : null;
            List<Drop> d = new ArrayList<>();
            NodeList loot = e.getElementsByTagName("Loot");
            for (int i = 0; i < loot.getLength(); i++) {
                d.add(new Drop((Element) loot.item(i)));
            }
            drops = Collections.unmodifiableList(d);
        }

        /** @return the short key after {@link #NAME_PREFIX}, for example {@code recruit} */
        public String key() {
            return name.startsWith(NAME_PREFIX) ? name.substring(NAME_PREFIX.length()) : name;
        }
    }

    private static int intAttr(Element e, String name) {
        String v = e.getAttribute(name);
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(e.getTagName() + " " + name + " is not a number: '" + v + "'", ex);
        }
    }

    /**
     * Parses a file in EnhancedLootBags' {@code LootBags.xml} format. Doctype declarations are refused, so the file
     * cannot pull in external entities.
     *
     * @param xml the file's text
     * @return the groups in file order
     * @throws IllegalArgumentException if the text is not well-formed or a number attribute is not a number
     */
    public static List<Group> parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(new InputSource(new StringReader(xml)));
            List<Group> out = new ArrayList<>();
            NodeList groups = doc.getDocumentElement()
                .getElementsByTagName("LootGroup");
            for (int i = 0; i < groups.getLength(); i++) {
                out.add(new Group((Element) groups.item(i)));
            }
            return Collections.unmodifiableList(out);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("not a LootGroups file: " + e.getMessage(), e);
        }
    }

    /**
     * @param groups parsed groups
     * @return their ids
     */
    public static Set<Integer> ids(List<Group> groups) {
        Set<Integer> ids = new HashSet<>();
        for (Group g : groups) {
            ids.add(g.id);
        }
        return ids;
    }

    /**
     * @param damage a bag item's damage
     * @return true if it is in the id range GregGuns reserves for its groups
     */
    public static boolean isReservedId(int damage) {
        return damage >= FIRST_ID && damage <= LAST_ID;
    }

    /**
     * Problems with a set of groups that would break or surprise EnhancedLootBags: ids outside the reserved range,
     * duplicate ids, names or drop identifiers, a trash group that is not in the set, more than one item without
     * {@code MinItems == MaxItems} (EnhancedLootBags then rolls 1 to MaxItems), a rarity outside vanilla's four, a
     * non-positive weight or amount, and a group whose every drop is limited (its bag would stop opening).
     *
     * @param groups parsed groups
     * @return one line per problem; empty when the set is sound
     */
    public static List<String> problems(List<Group> groups) {
        List<String> p = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (Group g : groups) {
            if (!isReservedId(g.id)) p.add("group " + g.id + " is outside " + FIRST_ID + "-" + LAST_ID);
            if (!ids.add(g.id)) p.add("group id " + g.id + " appears twice");
            if (!names.add(g.name)) p.add("group name " + g.name + " appears twice");
            if (!g.name.startsWith(NAME_PREFIX)) p.add("group " + g.id + " name does not start with " + NAME_PREFIX);
            if (g.minItems < 1 || g.minItems != g.maxItems) p.add("group " + g.id + ": MinItems must equal MaxItems");
            if (g.rarity < 0 || g.rarity > 3) p.add("group " + g.id + ": Rarity must be 0 to 3");
            if (g.drops.isEmpty()) p.add("group " + g.id + " has no drops");
            Set<String> dropIds = new HashSet<>();
            boolean unlimited = false;
            for (Drop d : g.drops) {
                if (!dropIds.add(d.identifier)) p.add("group " + g.id + ": drop " + d.identifier + " appears twice");
                if (d.chance <= 0 || d.amount <= 0)
                    p.add("group " + g.id + ": drop " + d.identifier + " weight/amount");
                if (splitItemName(d.itemName) == null) p.add("group " + g.id + ": bad item name " + d.itemName);
                if (d.limit == 0) unlimited = true;
            }
            if (!unlimited && !g.drops.isEmpty()) p.add("group " + g.id + " has only limited drops");
        }
        for (Group g : groups) {
            if (g.combineTrash && !ids.contains(g.trashGroup)) {
                p.add("group " + g.id + " trash group " + g.trashGroup + " is not a GregGuns group");
            }
        }
        return p;
    }

    /**
     * Splits an item name in YAMCore's {@code mod:name[:meta]} format, the one {@code ItemName} uses.
     *
     * @param itemName the name
     * @return {mod, name, meta} with meta {@code null} when absent, or {@code null} if the name is malformed
     */
    // Source:
    // https://github.com/GTNewHorizons/Yamcl/blob/0.7.5/src/main/java/eu/usrv/yamcore/auxiliary/ItemDescriptor.java
    // (fromString: "modid:name" with an optional ":meta")
    public static String[] splitItemName(String itemName) {
        if (itemName == null) return null;
        String[] parts = itemName.split(":");
        if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return new String[] { parts[0], parts[1], null };
        }
        if (parts.length == 3 && !parts[0].isEmpty() && !parts[1].isEmpty() && parts[2].matches("\\d+")) {
            return parts;
        }
        return null;
    }

    /**
     * @param itemName a drop's {@code ItemName}
     * @param exists   whether the registry has (mod, name) as an item or a block
     * @return true if the name is well-formed and names a registered item or block
     */
    public static boolean resolvable(String itemName, BiPredicate<String, String> exists) {
        String[] p = splitItemName(itemName);
        return p != null && exists.test(p[0], p[1]);
    }

    /**
     * @return the groups XML shipped in the jar
     * @throws IOException if the resource is missing
     */
    public static String readResource() throws IOException {
        try (InputStream in = LootBagGroups.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IOException(RESOURCE + " is not in the jar");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
