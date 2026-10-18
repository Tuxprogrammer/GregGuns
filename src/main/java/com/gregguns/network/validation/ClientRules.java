package com.gregguns.network.validation;

/**
 * The rule set for client-bound packets: ranges and target lookups only, because the client has no sender to check
 * (GGS-01-10).
 *
 * Side: both (plain Java).
 */
public final class ClientRules extends Rules<ClientRules> {

    @Override
    protected ClientRules self() {
        return this;
    }
}
