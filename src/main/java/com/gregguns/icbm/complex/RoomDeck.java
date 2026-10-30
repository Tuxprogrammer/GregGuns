package com.gregguns.icbm.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The room kinds a complex is dealt, like cards from a deck (D307). Round 1 holds every mandatory kind and
 * every optional kind once, shuffled together. Each later round holds every optional kind once more, freshly shuffled. A slot takes
 * the first card that fits it, in deck order: the undealt cards of the oldest open round, then those of the round after
 * it. So no optional kind is dealt twice while another one that fits the slot has not been dealt at all, and a kind no
 * slot can hold (a farm with no big slot left) never stops the rest.
 *
 * <p>
 * The mandatory kinds always get a slot: once the slots left are as many as the mandatory kinds still undealt, only
 * those are offered. Launch Control, the missile assembly floor, the airlock and the guard posts are placed by
 * their own code and are not in the deck. Pure and deterministic (one {@link Random}, lists only).
 */
// Spec: docs/gregguns-icbm-spec/08-worldgen/silo-layout.md (Rooms, The deck)
final class RoomDeck {

    private final List<RoomKind> optional = new ArrayList<RoomKind>();
    private final List<List<RoomKind>> rounds = new ArrayList<List<RoomKind>>();
    private final List<RoomKind> mandatoryLeft = new ArrayList<RoomKind>();
    private final Random rnd;
    private int left;

    /**
     * @param rnd   the plan's random
     * @param slots how many rooms the deck deals in all
     */
    RoomDeck(Random rnd, int slots) {
        this.rnd = rnd;
        this.left = slots;
        for (RoomKind k : RoomKind.values()) {
            if (!dealt(k)) continue;
            if (k.mandatory) mandatoryLeft.add(k);
            else optional.add(k);
        }
        List<RoomKind> first = new ArrayList<RoomKind>(mandatoryLeft);
        first.addAll(optional);
        Collections.shuffle(first, rnd);
        rounds.add(first);
    }

    /** @return whether the deck deals a kind (the rest are placed by their own code) */
    static boolean dealt(RoomKind k) {
        return k != RoomKind.ASSEMBLY_BAY && k != RoomKind.LAUNCH_CONTROL
            && k != RoomKind.MISSILE_ASSEMBLY
            && !k.small();
    }

    /** @return how many slots are still to deal */
    int left() {
        return left;
    }

    /** @return whether the deck has dealt every slot */
    boolean empty() {
        return left <= 0;
    }

    /**
     * @return the cards a slot tries, in order: only the undealt mandatory kinds when the slots left are no more than
     *         them, else the undealt cards of the oldest open round and then of the next round (no kind twice)
     */
    List<RoomKind> candidates() {
        if (left <= mandatoryLeft.size()) return new ArrayList<RoomKind>(mandatoryLeft);
        while (rounds.get(0)
            .isEmpty()) rounds.remove(0);
        if (rounds.size() < 2) rounds.add(freshRound());
        List<RoomKind> out = new ArrayList<RoomKind>(rounds.get(0));
        for (RoomKind k : rounds.get(1)) if (!out.contains(k)) out.add(k);
        return out;
    }

    /** Takes a card: from the oldest round that still holds it. */
    void deal(RoomKind k) {
        for (List<RoomKind> r : rounds) {
            if (r.remove(k)) break;
        }
        mandatoryLeft.remove(k);
        left--;
    }

    private List<RoomKind> freshRound() {
        List<RoomKind> r = new ArrayList<RoomKind>(optional);
        Collections.shuffle(r, rnd);
        return r;
    }
}
