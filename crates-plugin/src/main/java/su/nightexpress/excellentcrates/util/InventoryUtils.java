package su.nightexpress.excellentcrates.util;

import java.util.List;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class InventoryUtils {

    private InventoryUtils() {
    }

    public static boolean canFitItems(Player player, List<ItemStack> items) {
        return canFitItems(player.getInventory().getStorageContents(), player.getInventory().getMaxStackSize(), items);
    }

    public static boolean canFitItems(ItemStack[] storage, int maxInventoryStackSize, List<ItemStack> items) {
        if (items.isEmpty()) return true;

        ItemStack[] clonedStorage = new ItemStack[storage.length];
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null) {
                clonedStorage[i] = storage[i].clone();
            }
        }

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;

            int remaining = item.getAmount();
            int maxStack = Math.min(maxInventoryStackSize, item.getMaxStackSize());

            while (remaining > 0) {
                int toPlace = Math.min(remaining, maxStack);
                boolean placed = false;

                // 1. Try stacking onto existing matching items
                for (ItemStack slot : clonedStorage) {
                    if (slot != null && slot.isSimilar(item)) {
                        int spaceInSlot = maxStack - slot.getAmount();
                        if (spaceInSlot > 0) {
                            int toAdd = Math.min(toPlace, spaceInSlot);
                            slot.setAmount(slot.getAmount() + toAdd);
                            remaining -= toAdd;
                            toPlace -= toAdd;
                            placed = true;
                            if (toPlace <= 0) break;
                        }
                    }
                }

                if (toPlace <= 0) {
                    continue;
                }

                // 2. Put in first available empty slot
                for (int i = 0; i < clonedStorage.length; i++) {
                    if (clonedStorage[i] == null || clonedStorage[i].getType().isAir()) {
                        ItemStack newSlot = item.clone();
                        newSlot.setAmount(toPlace);
                        clonedStorage[i] = newSlot;
                        remaining -= toPlace;
                        placed = true;
                        break;
                    }
                }

                if (!placed) {
                    return false;
                }
            }
        }

        return true;
    }

    public static int simulateRemoval(ItemStack[] storage, Predicate<ItemStack> predicate, int absAmount) {
        int removed = 0;

        for (int i = 0; i < storage.length; i++) {
            ItemStack itemStack = storage[i];
            if (itemStack == null || itemStack.getType().isAir()) continue;

            if (!predicate.test(itemStack)) continue;

            int toRemove = Math.min(itemStack.getAmount(), absAmount - removed);
            int newAmount = itemStack.getAmount() - toRemove;
            if (newAmount <= 0) {
                storage[i] = null;
            }
            else {
                itemStack.setAmount(newAmount);
            }
            removed += toRemove;

            if (removed >= absAmount) {
                break;
            }
        }

        return removed;
    }

    public static int countInventorySpace(Player player, ItemStack itemToCount) {
        int space = 0;
        int maxStackSize = Math.min(player.getInventory().getMaxStackSize(), itemToCount.getMaxStackSize());
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                space += maxStackSize;
            }
            else if (item.isSimilar(itemToCount)) {
                space += Math.max(0, maxStackSize - item.getAmount());
            }
        }
        return space;
    }

    public static int countInventoryAmount(Player player, Predicate<ItemStack> predicate) {
        int balance = 0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            if (predicate.test(item)) {
                balance += item.getAmount();
            }
        }

        return balance;
    }

    public static int removeFromInventory(Player player, Predicate<ItemStack> predicate, int absAmount) {
        int removed = 0;

        for (ItemStack itemStack : player.getInventory().getContents()) {
            if (itemStack == null || itemStack.getType().isAir()) continue;

            if (!predicate.test(itemStack)) continue;

            int toRemove = Math.min(itemStack.getAmount(), absAmount - removed);
            itemStack.setAmount(itemStack.getAmount() - toRemove);
            removed += toRemove;

            if (removed >= absAmount) {
                break;
            }
        }

        return removed;
    }
}
