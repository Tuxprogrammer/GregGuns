package com.gregguns.core.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RegistrationLedgerTest {

    @Test
    void sameNameIsFineAcrossKindsButNotWithinOne() {
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.claimName(RegistrationLedger.Kind.ITEM, "x");
        assertDoesNotThrow(() -> ledger.claimName(RegistrationLedger.Kind.BLOCK, "x"));
        assertThrows(IllegalStateException.class, () -> ledger.claimName(RegistrationLedger.Kind.ITEM, "x"));
    }

    @Test
    void entityIdsMustBeUniqueAndNonNegative() {
        RegistrationLedger ledger = new RegistrationLedger();
        ledger.claimEntityId(0, "a");
        assertThrows(IllegalStateException.class, () -> ledger.claimEntityId(0, "b"));
        assertThrows(IllegalArgumentException.class, () -> ledger.claimEntityId(-1, "c"));
        assertDoesNotThrow(() -> ledger.claimEntityId(1, "d"));
    }
}
