/**
 * GregGuns trades for the pack's Vending Machine (mod {@code vendingmachine}). {@link VendingCatalog} (what is sold,
 * for
 * how many GregGuns Coins, behind which quest) and {@link QuestGate} are pure data and unit test;
 * {@link VendingTradeNbt} maps them to the machine's trade-database NBT;
 * {@link com.gregguns.compat.VendingMachineCompat}
 * is the only class that reaches the Vending Machine, by reflection, and only when it is loaded.
 */
package com.gregguns.compat.vending;
