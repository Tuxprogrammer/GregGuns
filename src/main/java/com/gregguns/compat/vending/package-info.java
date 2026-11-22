/**
 * GregGuns trades for the pack's Vending Machine (mod {@code vendingmachine}). {@link VendingCatalog} (what is sold,
 * for
 * how many GregGuns Coins, behind which pack quest) and {@link QuestGate} are pure data and unit test;
 * {@link VendingTradeNbt} maps them to the machine's trade-database NBT;
 * {@link com.gregguns.compat.VendingMachineCompat} merges them into the machine, by reflection, only when it is
 * loaded. {@link VendingCoin} makes the GregGuns Coin a wallet coin kind through the late mixins in
 * {@code com.gregguns.mixins.late.vending}; {@link VendingCurrency}, the only other class linked to the machine,
 * self-tests that patch.
 */
package com.gregguns.compat.vending;
