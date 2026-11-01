/**
 * Carpenter's Blocks (LGPL-2.1): every piece's light value is worked out as its chunk loads, so the pack's lighting
 * engine never asks a piece first (D345). Reached through reflection only; nothing here links against its classes,
 * and the primer installs only when the mod is loaded.
 */
package com.gregguns.compat.carpenters;
