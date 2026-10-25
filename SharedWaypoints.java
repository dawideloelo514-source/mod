package pl.sharedwaypoints;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedWaypoints implements ModInitializer {
    public static final String MOD_ID = "sharedwaypoints";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Maksymalna liczba waypointów na jednego gracza. */
    public static final int MAX_PER_PLAYER = 100;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(Payloads.UpsertC2S.ID, Payloads.UpsertC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(Payloads.DeleteC2S.ID, Payloads.DeleteC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.SyncS2C.ID, Payloads.SyncS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.UpsertS2C.ID, Payloads.UpsertS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(Payloads.DeleteS2C.ID, Payloads.DeleteS2C.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(WaypointStore::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> WaypointStore.save());

        // Nowy gracz z modem dostaje wszystkie waypointy
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            if (ServerPlayNetworking.canSend(p, Payloads.SyncS2C.ID)) {
                ServerPlayNetworking.send(p, new Payloads.SyncS2C(WaypointStore.all()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.UpsertC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = ctx.server();
            Waypoint in = payload.waypoint();
            if (in.id == null || in.name == null || in.dimension == null) return;

            String name = in.name.trim();
            if (name.isEmpty()) return;
            if (name.length() > 32) name = name.substring(0, 32);
            in.name = name;
            in.color = in.color & 0xFFFFFF;

            Waypoint existing = WaypointStore.get(in.id);
            boolean isOp = server.getPlayerManager().isOperator(player.getGameProfile());

            if (existing != null) {
                if (!existing.owner.equals(player.getUuid()) && !isOp) return;
                in.owner = existing.owner;
                in.ownerName = existing.ownerName;
            } else {
                if (WaypointStore.countOwnedBy(player.getUuid()) >= MAX_PER_PLAYER) return;
                in.owner = player.getUuid();
                in.ownerName = player.getGameProfile().getName();
            }

            WaypointStore.put(in);
            broadcast(server, new Payloads.UpsertS2C(in), Payloads.UpsertS2C.ID);
        });

        ServerPlayNetworking.registerGlobalReceiver(Payloads.DeleteC2S.ID, (payload, ctx) -> {
            ServerPlayerEntity player = ctx.player();
            MinecraftServer server = ctx.server();
            Waypoint existing = WaypointStore.get(payload.waypointId());
            if (existing == null) return;
            boolean isOp = server.getPlayerManager().isOperator(player.getGameProfile());
            if (!existing.owner.equals(player.getUuid()) && !isOp) return;

            WaypointStore.remove(existing.id);
            broadcast(server, new Payloads.DeleteS2C(existing.id), Payloads.DeleteS2C.ID);
        });
    }

    private static void broadcast(MinecraftServer server, CustomPayload payload, CustomPayload.Id<?> id) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(p, id)) {
                ServerPlayNetworking.send(p, payload);
            }
        }
    }
}
