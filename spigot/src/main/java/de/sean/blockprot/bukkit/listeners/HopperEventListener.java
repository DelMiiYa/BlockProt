/*
 * Copyright (C) 2021 - 2024 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Modifications made by DelMii on 23 Feb 2025
 * Original source: https://github.com/spnda/BlockProt
 */
package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

public class HopperEventListener implements Listener {

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        InventoryHolder sourceHolder = event.getSource().getHolder();
        if (sourceHolder == null) return;

        var config = BlockProt.getDefaultConfig();
        if (config.isWorldExcluded(sourceHolder)) return;

        if (event.getDestination().getType() != InventoryType.HOPPER && event.getSource().getType() != InventoryType.HOPPER) return;

        Block sourceBlock = getBlock(sourceHolder);
        if (sourceBlock == null || !config.isLockable(sourceBlock.getType())) return;

        BlockNBTHandler sourceHandler = new BlockNBTHandler(sourceBlock);
        if (!sourceHandler.isProtected()) return;

        boolean hopperProtection = sourceHandler.getRedstoneHandler().getHopperProtection();
        InventoryHolder destinationHolder = event.getDestination().getHolder();

        if (destinationHolder instanceof Container || destinationHolder instanceof DoubleChest) {
            handleBlockDestination(event, sourceHandler, hopperProtection, destinationHolder);
        } else if (destinationHolder instanceof Minecart) {
            if (hopperProtection) event.setCancelled(true);
        }
    }

    private void handleBlockDestination(InventoryMoveItemEvent event, BlockNBTHandler sourceHandler, boolean hopperProtection, InventoryHolder destinationHolder) {
        Block destinationBlock = getBlock(destinationHolder);
        if (destinationBlock == null) return;

        var config = BlockProt.getDefaultConfig();
        if (!config.isLockable(destinationBlock.getType())) {
            if (hopperProtection) event.setCancelled(true);
            return;
        }

        BlockNBTHandler destinationHandler = new BlockNBTHandler(destinationBlock);
        if (destinationHandler.isProtected()) {
            if (!destinationHandler.isOwner(sourceHandler.getOwner()) && hopperProtection) {
                event.setCancelled(true);
            }
        } else if (hopperProtection) {
            event.setCancelled(true);
        }
    }

    /**
     * Get the block from an InventoryHolder, avoiding unnecessary calls.
     */
    @Nullable
    private Block getBlock(InventoryHolder holder) {
        if (holder instanceof Container container) {
            return container.getBlock();
        } else if (holder instanceof DoubleChest doubleChest && doubleChest.getWorld() != null) {
            return doubleChest.getWorld().getBlockAt(doubleChest.getLocation());
        }
        return null;
    }
}

