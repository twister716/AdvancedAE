package net.pedroksl.advanced_ae.common.items.armors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.pedroksl.advanced_ae.common.definitions.AAEComponents;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.ids.AEComponents;
import appeng.api.networking.GridHelper;

public interface IUpgradeableItem {
    List<UpgradeType> getPossibleUpgrades();

    List<UpgradeType> getAppliedUpgrades(ItemStack stack);

    default List<UpgradeType> getPassiveUpgrades(ItemStack itemStack) {
        List<UpgradeType> abilityList = new ArrayList<>();
        getAppliedUpgrades(itemStack).forEach(up -> {
            if (up.applicationType == UpgradeType.ApplicationType.PASSIVE) abilityList.add(up);
        });
        return abilityList;
    }

    default boolean isUpgradeEnabled(ItemStack stack, UpgradeType upgrade) {
        return stack.getOrDefault(AAEComponents.UPGRADE_TOGGLE.get(upgrade), false);
    }

    default boolean isUpgradePowered(ItemStack stack, UpgradeType upgrade) {
        return isUpgradePowered(stack, upgrade, null);
    }

    default boolean isUpgradePowered(ItemStack stack, UpgradeType upgrade, Level level) {
        // Use internal buffer
        var energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energy != null && energy.getEnergyStored() > upgrade.getCost()) return true;

        // If that failed, try to pull from the grid
        if (level != null && stack.has(AEComponents.WIRELESS_LINK_TARGET)) {
            var host = GridHelper.getNodeHost(
                    level,
                    Objects.requireNonNull(stack.get(AEComponents.WIRELESS_LINK_TARGET))
                            .pos());
            if (host != null && host.getGridNode(null) != null) {
                var node = host.getGridNode(null).getGrid();
                var energyService = node.getEnergyService();
                var extracted =
                        energyService.extractAEPower(upgrade.getCost(), Actionable.SIMULATE, PowerMultiplier.CONFIG);
                return extracted >= upgrade.getCost() - 0.01;
            }
        }
        return false;
    }

    default boolean isUpgradeEnabledAndPowered(ItemStack stack, UpgradeType upgrade) {
        return isUpgradeEnabled(stack, upgrade) && isUpgradePowered(stack, upgrade);
    }

    default boolean isUpgradeEnabledAndPowered(ItemStack stack, UpgradeType upgrade, @Nullable Level level) {
        return isUpgradeEnabled(stack, upgrade) && isUpgradePowered(stack, upgrade, level);
    }

    default boolean isUpgradeAllowed(UpgradeType type) {
        return getPossibleUpgrades().contains(type);
    }

    default boolean hasUpgrade(ItemStack stack, UpgradeType type) {
        return stack.has(AAEComponents.UPGRADE_TOGGLE.get(type));
    }

    default boolean applyUpgrade(ItemStack stack, UpgradeType type) {
        if (!isUpgradeAllowed(type) || hasUpgrade(stack, type)) {
            return false;
        }

        getAppliedUpgrades(stack).add(type);
        stack.set(AAEComponents.UPGRADE_TOGGLE.get(type), true);
        if (type.getSettingType() == UpgradeType.SettingType.NUM_INPUT) {
            stack.set(AAEComponents.UPGRADE_VALUE.get(type), type.getSettings().maxValue);
        }
        if (type.getSettingType() == UpgradeType.SettingType.FILTER) {
            stack.set(AAEComponents.UPGRADE_FILTER.get(type), new ArrayList<>());
        }
        if (type.getSettingType() == UpgradeType.SettingType.NUM_AND_FILTER) {
            stack.set(AAEComponents.UPGRADE_VALUE.get(type), type.getSettings().maxValue);
            stack.set(AAEComponents.UPGRADE_FILTER.get(type), new ArrayList<>());
        }
        return true;
    }

    default boolean removeUpgrade(ItemStack stack, UpgradeType type) {
        if (getAppliedUpgrades(stack).contains(type)) {
            stack.remove(AAEComponents.UPGRADE_TOGGLE.get(type));
            stack.remove(AAEComponents.UPGRADE_VALUE.get(type));
            stack.remove(AAEComponents.UPGRADE_FILTER.get(type));
            getAppliedUpgrades(stack).remove(type);
            return true;
        }
        return false;
    }

    default void tickUpgrades(Level level, Player player, ItemStack stack) {
        for (var upgrade : getAppliedUpgrades(stack)) {
            if (upgrade.applicationType == UpgradeType.ApplicationType.PASSIVE && isUpgradeEnabled(stack, upgrade)) {
                upgrade.ability.execute(level, player, stack);
            }
        }
    }
}