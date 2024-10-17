package net.pedroksl.advanced_ae.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.pedroksl.advanced_ae.common.definitions.AAEMenus;
import net.pedroksl.advanced_ae.common.definitions.AAESlotSemantics;
import net.pedroksl.advanced_ae.common.definitions.AAEText;
import net.pedroksl.advanced_ae.common.entities.ReactionChamberEntity;
import net.pedroksl.advanced_ae.network.packet.FluidTankStackUpdatePacket;
import net.pedroksl.advanced_ae.recipes.ReactionChamberRecipes;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.stacks.AEFluidKey;
import appeng.api.util.IConfigManager;
import appeng.core.localization.Tooltips;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.OutputSlot;
import appeng.util.ConfigMenuInventory;

public class ReactionChamberMenu extends UpgradeableMenu<ReactionChamberEntity> implements IProgressProvider {

    @GuiSync(2)
    public int maxProcessingTime = -1;

    @GuiSync(3)
    public int processingTime = -1;

    @GuiSync(7)
    public YesNo autoExport = YesNo.NO;

    public final int INPUT_FLUID_SIZE = 16;
    public final int OUTPUT_FLUID_SIZE = 16;

    private static final String FLUSH_FLUID = "flushFluid";
    private static final String FLUSH_FLUID_OUT = "flushFluidOut";
    private static final String CONFIGURE_OUTPUT = "configureOutput";

    private final List<Slot> inputs = new ArrayList<>(9);
    private final AppEngSlot tank;
    private final AppEngSlot outTank;

    public ReactionChamberMenu(int id, Inventory ip, ReactionChamberEntity host) {
        super(AAEMenus.REACTION_CHAMBER, id, ip, host);

        var inputs = host.getInput();

        for (var x = 0; x < inputs.size(); x++) {
            this.inputs.add(x, this.addSlot(new AppEngSlot(inputs, x), SlotSemantics.MACHINE_INPUT));
        }

        var output = new OutputSlot(host.getOutput(), 0, null);
        this.addSlot(output, SlotSemantics.MACHINE_OUTPUT);

        this.addSlot(this.tank = new AppEngSlot(host.getTank().createMenuWrapper(), 0), SlotSemantics.STORAGE);
        this.tank.setEmptyTooltip(() -> List.of(
                AAEText.TankEmpty.text(), AAEText.TankAmount.text(0, 16000).withStyle(Tooltips.NORMAL_TOOLTIP_TEXT)));

        this.addSlot(
                this.outTank = new AppEngSlot(new OutputConfigInventory(host.getOutTank()), 0),
                AAESlotSemantics.FLUID_OUT);
        this.outTank.setEmptyTooltip(() -> List.of(
                AAEText.TankEmpty.text(), AAEText.TankAmount.text(0, 16000).withStyle(Tooltips.NORMAL_TOOLTIP_TEXT)));

        registerClientAction(FLUSH_FLUID, this::clearFluid);
        registerClientAction(FLUSH_FLUID_OUT, this::clearFluidOut);
        registerClientAction(CONFIGURE_OUTPUT, this::configureOutput);
    }

    protected void loadSettingsFromHost(IConfigManager cm) {
        this.autoExport = this.getHost().getConfigManager().getSetting(Settings.AUTO_EXPORT);
    }

    @Override
    protected void standardDetectAndSendChanges() {
        if (isServerSide()) {
            this.maxProcessingTime = getHost().getMaxProcessingTime();
            this.processingTime = getHost().getProcessingTime();

            var genInput = this.getHost().getTank().getStack(0);
            FluidStack inputFluid = FluidStack.EMPTY;
            if (genInput != null && genInput.what() != null) {
                inputFluid = ((AEFluidKey) genInput.what()).toStack(((int) genInput.amount()));
            }

            var genOutput = this.getHost().getOutTank().getStack(0);
            FluidStack outputFluid = FluidStack.EMPTY;
            if (genOutput != null && genOutput.what() != null) {
                outputFluid = ((AEFluidKey) genOutput.what()).toStack(((int) genOutput.amount()));
            }

            sendPacketToClient(new FluidTankStackUpdatePacket(inputFluid, outputFluid));
        }
        super.standardDetectAndSendChanges();
    }

    @Override
    public boolean isValidForSlot(Slot s, ItemStack is) {
        if (this.inputs.contains(s)) {
            return ReactionChamberRecipes.isValidIngredient(is, this.getHost().getLevel());
        } else if (s == this.outTank) {
            return false;
        }
        return true;
    }

    @Override
    public int getCurrentProgress() {
        return this.processingTime;
    }

    @Override
    public int getMaxProgress() {
        return this.maxProcessingTime;
    }

    public YesNo getAutoExport() {
        return autoExport;
    }

    public void clearFluid() {
        if (isClientSide()) {
            sendClientAction(FLUSH_FLUID);
            return;
        }

        this.getHost().clearFluid();
    }

    public void clearFluidOut() {
        if (isClientSide()) {
            sendClientAction(FLUSH_FLUID_OUT);
            return;
        }

        this.getHost().clearFluidOut();
    }

    public void configureOutput() {
        if (isClientSide()) {
            sendClientAction(CONFIGURE_OUTPUT);
            return;
        }

        var locator = getLocator();
        if (locator != null && isServerSide()) {
            OutputDirectionMenu.open(
                    ((ServerPlayer) this.getPlayer()),
                    getLocator(),
                    this.getHost().getAllowedOutputs());
        }
    }

    public static class OutputConfigInventory extends ConfigMenuInventory {

        public OutputConfigInventory(GenericStackInv inv) {
            super(inv);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
