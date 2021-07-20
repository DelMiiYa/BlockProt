/*
 * Copyright (C) 2021 spnda
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
package de.sean.blockprot.bukkit.inventories

import de.sean.blockprot.bukkit.BlockProt
import de.sean.blockprot.bukkit.TranslationKey
import de.sean.blockprot.bukkit.Translator
import de.sean.blockprot.bukkit.events.BlockAccessEditMenuEvent
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

class BlockLockInventory : BlockProtInventory() {
    private var redstone: Boolean = false

    override fun getSize() = InventoryConstants.singleLine
    override fun getTranslatedInventoryName() = Translator.get(TranslationKey.INVENTORIES__BLOCK_LOCK)

    override fun onClick(event: InventoryClickEvent, state: InventoryState) {
        if (state.block == null) return
        val block: Block = state.block!!
        val item = event.currentItem ?: return

        val player = event.whoClicked as Player
        val inv: Inventory

        when {
            // As there are some conditions in which the item in the inventory differs from
            // the actual blocks type, we use the blocks type here.
            BlockProt.getDefaultConfig().isLockable(block.type) && event.slot == 0 -> {
                applyChanges(block, player, true, true) {
                    it.lockBlock(
                        player,
                    )
                }
            }
            item.type == Material.REDSTONE || item.type == Material.GUNPOWDER -> {
                redstone = !redstone
                setItemStack(
                    1,
                    if (redstone) Material.REDSTONE else Material.GUNPOWDER,
                    if (redstone) TranslationKey.INVENTORIES__REDSTONE__DISALLOW
                    else TranslationKey.INVENTORIES__REDSTONE__ALLOW
                )
            }
            item.type == Material.PLAYER_HEAD -> {
                inv = FriendManageInventory().fill(player)
                closeAndOpen(player, inv)
            }
            item.type == Material.OAK_SIGN -> {
                closeAndOpen(
                    player,
                    BlockInfoInventory().fill(player, BlockNBTHandler(block))
                )
            }
            else -> closeAndOpen(player, null) // This also includes Material.BLACK_STAINED_GLASS_PANE
        }
        event.isCancelled = true
    }

    override fun onClose(event: InventoryCloseEvent, state: InventoryState) {
        if (state.friendSearchState == InventoryState.FriendSearchState.FRIEND_SEARCH && state.block != null) {
            applyChanges(state.block!!, event.player as Player, false, false) {
                return@applyChanges it.lockRedstoneForBlock(
                    event.player.uniqueId.toString(),
                    redstone,
                )
            }
        }
    }

    fun fill(player: Player, material: Material, handler: BlockNBTHandler): Inventory {
        val state = InventoryState.get(player.uniqueId) ?: return inventory

        val playerUuid = player.uniqueId.toString()
        val owner = handler.owner
        redstone = handler.redstone

        if (owner.isEmpty()) {
            setItemStack(
                0,
                getProperMaterial(material),
                TranslationKey.INVENTORIES__LOCK
            )
        } else if (owner == playerUuid ||
            state.menuAccess == BlockAccessEditMenuEvent.MenuAccess.ADMIN
        ) {
            setItemStack(
                0,
                getProperMaterial(material),
                TranslationKey.INVENTORIES__UNLOCK
            )
        }
        if (owner == playerUuid && state.menuAccess.ordinal >= BlockAccessEditMenuEvent.MenuAccess.NORMAL.ordinal) {
            setItemStack(
                1,
                if (redstone) Material.REDSTONE else Material.GUNPOWDER,
                if (redstone) TranslationKey.INVENTORIES__REDSTONE__DISALLOW
                else TranslationKey.INVENTORIES__REDSTONE__ALLOW
            )
            setItemStack(
                2,
                Material.PLAYER_HEAD,
                TranslationKey.INVENTORIES__FRIENDS__MANAGE
            )
        }
        if (owner.isNotEmpty() &&
            state.menuAccess.ordinal >= BlockAccessEditMenuEvent.MenuAccess.INFO.ordinal
        ) {
            setItemStack(
                InventoryConstants.lineLength - 2,
                Material.OAK_SIGN,
                TranslationKey.INVENTORIES__BLOCK_INFO
            )
        }
        setBackButton()
        return inventory
    }
}
