package net.pedroksl.advanced_ae.gui;

import java.util.List;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.definitions.AAEComponents;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.definitions.AAESlotSemantics;
import net.pedroksl.advanced_ae.common.inventory.QuantumArmorMenuHost;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.upgrades.QuantumUpgradeBaseItem;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import net.pedroksl.advanced_ae.network.packet.quantumarmor.QuantumArmorUpgradeStatePacket;

import appeng.api.inventories.InternalInventory;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.DisabledSlot;

public class QuantumArmorConfigMenu extends AEBaseMenu implements ISubMenuHost, IProgressProvider {

    @GuiSync(2)
    public int maxProcessingTime;

    @GuiSync(3)
    public int processingTime = -1;

    private final QuantumArmorMenuHost<?> host;

    private final Slot inputSlot;

    private boolean markDirty = false;

    private static final String SELECT_SLOT = "select_slot";
    private static final String REQUEST_UNINSTALL = "request_uninstall";

    public QuantumArmorConfigMenu(int id, Inventory playerInventory, QuantumArmorMenuHost<?> host) {
        super(AAEMenus.QUANTUM_ARMOR_CONFIG, id, playerInventory, host);
        this.host = host;
        this.createPlayerInventorySlots(playerInventory);

        this.inputSlot = this.addSlot(new UpgradeSlot(host.getInventory(), 0), SlotSemantics.MACHINE_INPUT);

        int indexOfFirstQuantum = -1;
        for (int i = 3; i >= 0; i--) {
            var index = Inventory.INVENTORY_SIZE + i;
            var slot = new DisabledSlot(playerInventory, index);
            if (indexOfFirstQuantum == -1 && slot.getItem().getItem() instanceof QuantumArmorBase) {
                indexOfFirstQuantum = index;
            }
            this.addSlot(slot, AAESlotSemantics.ARMOR);
        }

        this.host.setSelectedItemSlot(indexOfFirstQuantum);

        maxProcessingTime = this.host.getMaxProcessingTime();
        this.host.setProgressChangedHandler(this::progressChanged);
        this.host.setUpgradeAppliedWatcher(() -> this.markDirty = true);
        this.host.setInventoryChangedHandler(this::onChangeInventory);

        registerClientAction(SELECT_SLOT, Integer.class, this::setSelectedItemSlot);
        registerClientAction(REQUEST_UNINSTALL, UpgradeType.class, this::requestUninstall);
    }

    private void progressChanged(int progress) {
        this.processingTime = progress;
    }

    private void onChangeInventory(InternalInventory inv, int slot) {}

    @Override
    public void returnToMainMenu(Player player, ISubMenu iSubMenu) {
        this.host.returnToMainMenu(player, iSubMenu);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return this.host.getItemStack();
    }

    @Override
    public int getCurrentProgress() {
        return this.processingTime;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProcessingTime;
    }

    public boolean isArmorSlot(Slot slot) {
        return this.getSlots(AAESlotSemantics.ARMOR).contains(slot);
    }

    public void setSelectedItemSlot(int index) {
        if (isClientSide()) {
            sendClientAction(SELECT_SLOT, index);
            return;
        }

        this.host.setSelectedItemSlot(index);
    }

    public int getSelectedSlotIndex() {
        return this.host.getSelectedSlotIndex();
    }

    public void toggleUpgradeEnable(UpgradeType upgradeType, boolean state) {
        var slotIndex = this.host.getSelectedSlotIndex();
        var stack = getPlayer().getInventory().getItem(slotIndex);
        if (stack.getItem() instanceof QuantumArmorBase item) {
            if (item.getPossibleUpgrades().contains(upgradeType)) {
                if (item.hasUpgrade(stack, upgradeType)) {
                    stack.set(AAEComponents.UPGRADE_TOGGLE.get(upgradeType), state);
                    this.markDirty = true;
                }
            }
        }
    }

    public void updateUpgradeValue(UpgradeType upgradeType, int value) {
        var slotIndex = this.host.getSelectedSlotIndex();
        var stack = getPlayer().getInventory().getItem(slotIndex);
        if (stack.getItem() instanceof QuantumArmorBase item) {
            if (item.getPossibleUpgrades().contains(upgradeType)) {
                if (item.hasUpgrade(stack, upgradeType)) {
                    stack.set(AAEComponents.UPGRADE_VALUE.get(upgradeType), value);
                    this.markDirty = true;
                }
            }
        }
    }

    public void updateUpgradeFilter(UpgradeType upgradeType, List<TagKey<Item>> filter) {
        var slotIndex = this.host.getSelectedSlotIndex();
        var stack = getPlayer().getInventory().getItem(slotIndex);
        if (stack.getItem() instanceof QuantumArmorBase item) {
            if (item.getPossibleUpgrades().contains(upgradeType)) {
                if (item.hasUpgrade(stack, upgradeType)) {
                    stack.set(AAEComponents.UPGRADE_FILTER.get(upgradeType), filter);
                    this.markDirty = true;
                }
            }
        }
    }

    public void requestUninstall(UpgradeType upgradeType) {
        if (isClientSide()) {
            sendClientAction(REQUEST_UNINSTALL, upgradeType);
            return;
        }

        boolean upgradeRemoved = false;
        var slotIndex = this.host.getSelectedSlotIndex();
        var stack = getPlayer().getInventory().getItem(slotIndex);
        if (stack.getItem() instanceof QuantumArmorBase item) {
            if (item.getPossibleUpgrades().contains(upgradeType)) {
                if (item.hasUpgrade(stack, upgradeType)) {
                    upgradeRemoved = item.removeUpgrade(stack, upgradeType);
                }
            }
        }

        var upgradeStack = upgradeType.item().stack();
        if (upgradeRemoved) {
            if (!getPlayer().getInventory().add(upgradeStack)) {
                getPlayer().drop(upgradeStack, false);
            }
        }
        this.markDirty = true;
    }

    private void updateClient() {
        sendPacketToClient(new QuantumArmorUpgradeStatePacket());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (markDirty) {
            updateClient();
        }
    }

    private static class UpgradeSlot extends AppEngSlot {
        public UpgradeSlot(InternalInventory inv, int invSlot) {
            super(inv, invSlot);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (stack.getItem() instanceof QuantumUpgradeBaseItem upgrade) {
                return upgrade.getType() != UpgradeType.EMPTY;
            }
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }
    }
}