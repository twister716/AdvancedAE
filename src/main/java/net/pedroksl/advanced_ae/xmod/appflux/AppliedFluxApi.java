package net.pedroksl.advanced_ae.xmod.appflux;

import com.glodblock.github.appflux.common.AFSingletons;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.pedroksl.advanced_ae.common.definitions.AAEBlocks;
import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.definitions.AAEText;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.upgrades.Upgrades;

public class AppliedFluxApi {

    public static void init() {
        try {
            Upgrades.add(
                    AFSingletons.INDUCTION_CARD,
                    AAEBlocks.ADV_PATTERN_PROVIDER,
                    1,
                    AAEText.AdvPatternProvider.getTranslationKey());
            Upgrades.add(
                    AFSingletons.INDUCTION_CARD,
                    AAEItems.ADV_PATTERN_PROVIDER,
                    1,
                    AAEText.AdvPatternProvider.getTranslationKey());
            Upgrades.add(AFSingletons.INDUCTION_CARD, AAEBlocks.SMALL_ADV_PATTERN_PROVIDER, 1);
            Upgrades.add(AFSingletons.INDUCTION_CARD, AAEItems.SMALL_ADV_PATTERN_PROVIDER, 1);
        } catch (Throwable ignored) {
            // NO-OP
        }
    }

    public static double rechargeQuantumGear(
            IGrid grid, double neededPower, Player player, ItemStack stack, QuantumArmorBase armor) {
        try {
            var storage = grid.getStorageService();
            var extracted = storage.getInventory()
                    .extract(
                            FluxKey.of(EnergyType.FE),
                            (long) (neededPower / PowerMultiplier.CONFIG.multiplier),
                            Actionable.MODULATE,
                            IActionSource.ofPlayer(player));

            armor.injectAEPower(stack, extracted * PowerMultiplier.CONFIG.multiplier, Actionable.MODULATE);

            neededPower -= extracted;
        } catch (Throwable ignored) {
            // NO_OP
        }
        return neededPower;
    }

    public static void rechargeInventory(IGrid grid, int afRate, Player player, IEnergyStorage cap) {
        try {
            var storage = grid.getStorageService();

            var extracted = storage.getInventory()
                    .extract(
                            FluxKey.of(EnergyType.FE),
                            (long) (afRate / PowerMultiplier.CONFIG.multiplier),
                            Actionable.MODULATE,
                            IActionSource.ofPlayer(player));
            var inserted = cap.receiveEnergy((int) extracted, false);
            storage.getInventory()
                    .insert(
                            FluxKey.of(EnergyType.FE),
                            extracted - inserted,
                            Actionable.MODULATE,
                            IActionSource.ofPlayer(player));
        } catch (Throwable ignored) {
            // NO_OP
        }
    }
}