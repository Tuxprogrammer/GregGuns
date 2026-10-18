/**
 * Plain-Java gun logic: stats, fire control, spread and zoom, reload state machine, ammo accounting and the NBT
 * layout. Nothing here touches the world, an entity or an {@code Item}; the item glue drives it.
 *
 * <h2>How the item glue drives this package</h2>
 * <ol>
 * <li>Keep one {@link com.gregguns.gun.logic.GunController} per player on the <b>server</b> (and a mirror on the
 * client for prediction). It owns the cooldown (shared by every gun the player holds, as in the Techguns spec), the
 * reload machine and the zoom state. Drop it when the player logs out.</li>
 * <li>Each server tick call {@link com.gregguns.gun.logic.GunController#tick}. Read the stack with
 * {@link com.gregguns.gun.logic.GunNbt#read} before and write it back with
 * {@link com.gregguns.gun.logic.GunNbt#write} after.</li>
 * <li>Packets only carry intents: trigger pressed, trigger released, zoom toggle, force reload. The server calls the
 * matching controller method after its own validation. A packet never carries an ammo count or a zoom flag.</li>
 * <li>NPCs use {@link com.gregguns.gun.logic.NpcFiring} with
 * {@link com.gregguns.gun.logic.AmmoInventory#INFINITE}.</li>
 * </ol>
 *
 * <p>
 * Ammo type ids are plain strings that must equal the ammo module's {@code AmmoType} ids. See
 * {@link com.gregguns.gun.logic.GunCatalogue.AmmoIds}.
 */
package com.gregguns.gun.logic;
