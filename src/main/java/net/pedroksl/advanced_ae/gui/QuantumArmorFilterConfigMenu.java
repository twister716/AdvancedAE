package net.pedroksl.advanced_ae.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.pedroksl.advanced_ae.api.ISetAmountMenuHost;
import net.pedroksl.advanced_ae.common.definitions.AAEComponents;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.ISubMenuHost;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.slot.FakeSlot;
import appeng.util.ConfigInventory;

public class QuantumArmorFilterConfigMenu extends AEBaseMenu implements ISubMenu, ISetAmountMenuHost {

    @GuiSync(7)
    public UpgradeType type;

    public int slotIndex;
    private final ISubMenuHost host;

    protected final ConfigInventory inv;

    protected final FakeSlot[] slots = new FakeSlot[9];

    protected static final String OPEN_AMOUNT_MENU = "open_amount_menu";

    public QuantumArmorFilterConfigMenu(MenuType<?> type, int id, Inventory playerInventory, ISubMenuHost host) {
        super(type, id, playerInventory, host);
        this.host = host;
        createPlayerInventorySlots(playerInventory);

        var filterQuantities = type == AAEMenus.QUANTUM_ARMOR_FILTER_CONFIG;
        if (filterQuantities) {
            this.inv = ConfigInventory.configStacks(9)
                    .changeListener(this::onSlotChanged)
                    .allowOverstacking(true)
                    .build();
        } else {
            this.inv = ConfigInventory.configTypes(9)
                    .changeListener(this::onSlotChanged)
                    .build();
        }
        var wrappedInv = inv.createMenuWrapper();

        for (var x = 0; x < inv.size(); x++) {
            slots[x] = new FakeSlot(wrappedInv, x);
            this.addSlot(slots[x], SlotSemantics.CONFIG);
        }

        registerClientAction(OPEN_AMOUNT_MENU, Integer.class, this::openAmountMenu);
    }

    public QuantumArmorFilterConfigMenu(int id, Inventory playerInventory, ISubMenuHost host) {
        this(AAEMenus.QUANTUM_ARMOR_FILTER_CONFIG, id, playerInventory, host);
    }

    @Override
    public ISubMenuHost getHost() {
        return this.host;
    }

    public static void open(
            ServerPlayer player,
            MenuHostLocator locator,
            int slotIndex,
            List<GenericStack> filterList,
            UpgradeType type) {
        MenuOpener.open(AAEMenus.QUANTUM_ARMOR_FILTER_CONFIG, player, locator);

        if (player.containerMenu instanceof QuantumArmorFilterConfigMenu cca) {
            cca.setSlotIndex(slotIndex);
            cca.setUpgradeType(type);
            cca.setFilterList(filterList);
            cca.broadcastChanges();
        }
    }

    public void setSlotIndex(int index) {
        this.slotIndex = index;
    }

    public void setUpgradeType(UpgradeType type) {
        this.type = type;
    }

    public boolean isConfigSlot(Slot slot) {
        return this.getSlots(SlotSemantics.CONFIG).contains(slot);
    }

    public void setFilterList(List<GenericStack> filterList) {
        for (var x = 0; x < inv.size(); x++) {
            if (x < filterList.size()) {
                var filter = filterList.get(x);
                if (filter != null) {
                    if (filter.what() instanceof AEItemKey key) {
                        var stack = key.toStack();
                        stack.setCount((int) filter.amount());
                        this.slots[x].set(stack);
                        continue;
                    }
                }
            }
            this.slots[x].set(ItemStack.EMPTY);
        }
    }

    public void onSlotChanged() {
        if (isClientSide()) {
            return;
        }

        updateItemStack();
    }

    private void updateItemStack() {
        List<GenericStack> filterList = makeFilterList();

        var stack = getPlayer().getInventory().getItem(this.slotIndex);
        if (stack.getItem() instanceof QuantumArmorBase item) {
            if (item.getPossibleUpgrades().contains(this.type)) {
                if (item.hasUpgrade(stack, this.type)) {
                    stack.set(AAEComponents.UPGRADE_FILTER.get(this.type), filterList);
                }
            }
        }
    }

    protected List<GenericStack> makeFilterList() {
        List<GenericStack> filterList = new ArrayList<>();
        new ArrayList<>();
        for (var x = 0; x < inv.size(); x++) {
            var stack = this.slots[x].getItem();
            if (!stack.isEmpty()) {
                filterList.add(GenericStack.fromItemStack(stack));
            }
        }
        return filterList;
    }

    public void openAmountMenu(int index) {
        if (isClientSide()) {
            sendClientAction(OPEN_AMOUNT_MENU, index);
            return;
        }

        var slot = this.getSlot(index);

        GenericStack currentStack = GenericStack.fromItemStack(slot.getItem());
        if (currentStack != null) {
            var locator = getLocator();
            if (locator != null && isServerSide()) {
                SetAmountMenu.open(
                        ((ServerPlayer) this.getPlayer()),
                        getLocator(),
                        currentStack,
                        (newStack) -> PacketDistributor.sendToServer(new InventoryActionPacket(
                                InventoryAction.SET_FILTER, slot.index, GenericStack.wrapInItemStack(newStack))),
                        this,
                        slot.getMaxStackSize());
            }
        }
    }

    @Override
    public void returnFromSetAmountMenu() {
        List<GenericStack> filterList = makeFilterList();

        Player player = getPlayerInventory().player;
        if (player instanceof ServerPlayer serverPlayer) {
            QuantumArmorFilterConfigMenu.open(serverPlayer, getLocator(), this.slotIndex, filterList, this.type);
        }
    }
}
