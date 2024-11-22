package net.pedroksl.advanced_ae.gui.advpatternprovider;

import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.*;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.ConfigButtonPacket;
import appeng.menu.SlotSemantics;
import com.glodblock.github.appflux.util.helpers.IUpgradableMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class AdvPatternProviderGui extends AEBaseScreen<AdvPatternProviderContainer> {

	private final SettingToggleButton<YesNo> blockingModeButton;
	private final SettingToggleButton<LockCraftingMode> lockCraftingModeButton;
	private final ToggleButton showInPatternAccessTerminalButton;
	private final AdvPatternProviderLockReason lockReason;

	public AdvPatternProviderGui(AdvPatternProviderContainer menu, Inventory playerInventory, Component title,
	                             ScreenStyle style) {
		super(menu, playerInventory, title, style);

		this.blockingModeButton = new ServerSettingToggleButton<>(Settings.BLOCKING_MODE, YesNo.NO);
		this.addToLeftToolbar(this.blockingModeButton);

		lockCraftingModeButton = new ServerSettingToggleButton<>(Settings.LOCK_CRAFTING_MODE, LockCraftingMode.NONE);
		this.addToLeftToolbar(lockCraftingModeButton);

		widgets.addOpenPriorityButton();

		this.showInPatternAccessTerminalButton = new ToggleButton(Icon.PATTERN_ACCESS_SHOW,
				Icon.PATTERN_ACCESS_HIDE,
				GuiText.PatternAccessTerminal.text(), GuiText.PatternAccessTerminalHint.text(),
				btn -> selectNextPatternProviderMode());
		this.addToLeftToolbar(this.showInPatternAccessTerminalButton);

		this.lockReason = new AdvPatternProviderLockReason(this);
		widgets.add("lockReason", this.lockReason);

		this.widgets.add("upgrades", new UpgradesPanel(menu.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));
		if (((IUpgradableMenu) menu).getToolbox().isPresent()) {
			this.widgets.add("toolbox", new ToolboxPanel(style, ((IUpgradableMenu) menu).getToolbox().getName()));
		}
	}

	@Override
	protected void updateBeforeRender() {
		super.updateBeforeRender();

		this.lockReason.setVisible(menu.getLockCraftingMode() != LockCraftingMode.NONE);
		this.blockingModeButton.set(this.menu.getBlockingMode());
		this.lockCraftingModeButton.set(this.menu.getLockCraftingMode());
		this.showInPatternAccessTerminalButton.setState(this.menu.getShowInAccessTerminal() == YesNo.YES);
	}

	private void selectNextPatternProviderMode() {
		final boolean backwards = isHandlingRightClick();
		NetworkHandler.instance().sendToServer(new ConfigButtonPacket(Settings.PATTERN_ACCESS_TERMINAL, backwards));
	}

	private List<Component> getCompatibleUpgrades() {
		ArrayList<Component> list = new ArrayList<>();
		list.add(GuiText.CompatibleUpgrades.text());
		list.addAll(Upgrades.getTooltipLinesForMachine(((IUpgradableMenu) this.menu).getUpgrades().getUpgradableItem()));
		return list;
	}
}