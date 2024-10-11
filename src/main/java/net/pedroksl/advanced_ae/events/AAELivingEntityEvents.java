package net.pedroksl.advanced_ae.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.pedroksl.advanced_ae.common.definitions.AAEComponents;
import net.pedroksl.advanced_ae.common.items.armors.QuantumArmorBase;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;

import appeng.api.config.Actionable;

public class AAELivingEntityEvents {

    @SubscribeEvent
    public static void invulnerability(EntityInvulnerabilityCheckEvent event) {
        Entity target = event.getEntity();
        if (target instanceof Player player) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (stack.getItem() instanceof QuantumArmorBase item
                    && item.isUpgradeEnabledAndPowered(stack, UpgradeType.LAVA_IMMUNITY)) {
                if (event.getSource().is(DamageTypes.LAVA)
                        || event.getSource().is(DamageTypes.IN_FIRE)
                        || event.getSource().is(DamageTypes.ON_FIRE)) {
                    player.setRemainingFireTicks(0);
                    event.setInvulnerable(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void breath(LivingBreatheEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
            Level level = player instanceof ServerPlayer serverPlayer ? serverPlayer.level() : null;
            if (stack.getItem() instanceof QuantumArmorBase item
                    && item.isUpgradeEnabledAndPowered(stack, UpgradeType.WATER_BREATHING, level))
                event.setCanBreathe(true);
        }
    }

    @SubscribeEvent
    public static void jumpEvent(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.FEET);
            Level level = player instanceof ServerPlayer serverPlayer ? serverPlayer.level() : null;
            if (stack.getItem() instanceof QuantumArmorBase item
                    && item.isUpgradeEnabledAndPowered(stack, UpgradeType.JUMP_HEIGHT, level))
                UpgradeType.JUMP_HEIGHT.ability.execute(player.level(), player, stack);
        }
    }

    @SubscribeEvent
    public static void LivingFallDamage(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.FEET);
            if (stack.getItem() instanceof QuantumArmorBase item) {
                if (item.extractAEPower(stack, 10, Actionable.SIMULATE) > 0) {
                    event.setDistance(0.0f);
                    return;
                }
                if (item.isUpgradeEnabledAndPowered(stack, UpgradeType.JUMP_HEIGHT, player.level())) {
                    int upgrade = stack.getOrDefault(AAEComponents.UPGRADE_VALUE.get(UpgradeType.JUMP_HEIGHT), 0);
                    if (player.isSprinting()) upgrade += 2;
                    event.setDistance(event.getDistance() - upgrade);
                }
            }
        }
    }
}