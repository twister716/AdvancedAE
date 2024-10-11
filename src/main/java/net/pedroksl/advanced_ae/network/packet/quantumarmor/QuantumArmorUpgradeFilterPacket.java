package net.pedroksl.advanced_ae.network.packet.quantumarmor;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import net.pedroksl.advanced_ae.gui.QuantumArmorConfigMenu;

import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.ServerboundPacket;

public record QuantumArmorUpgradeFilterPacket(UpgradeType upgradeType, List<TagKey<Item>> filter)
        implements ServerboundPacket {
    public static final StreamCodec<RegistryFriendlyByteBuf, QuantumArmorUpgradeFilterPacket> STREAM_CODEC =
            StreamCodec.ofMember(QuantumArmorUpgradeFilterPacket::write, QuantumArmorUpgradeFilterPacket::decode);

    public static final Type<QuantumArmorUpgradeFilterPacket> TYPE =
            CustomAppEngPayload.createType("aae_upgrade_filter");

    @Override
    public Type<QuantumArmorUpgradeFilterPacket> type() {
        return TYPE;
    }

    public static QuantumArmorUpgradeFilterPacket decode(RegistryFriendlyByteBuf stream) {
        var upgradeType = stream.readEnum(UpgradeType.class);
        var filter = ByteBufCodecs.fromCodec(Codec.list(TagKey.codec(Registries.ITEM)))
                .decode(stream);
        return new QuantumArmorUpgradeFilterPacket(upgradeType, filter);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeEnum(upgradeType);
        ByteBufCodecs.fromCodec(Codec.list(TagKey.codec(Registries.ITEM))).encode(data, filter);
    }

    @Override
    public void handleOnServer(ServerPlayer serverPlayer) {
        if (serverPlayer.containerMenu instanceof QuantumArmorConfigMenu menu) {
            menu.updateUpgradeFilter(upgradeType, filter);
        }
    }
}