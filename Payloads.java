package pl.sharedwaypoints;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class Payloads {
    private Payloads() {}

    private static <T> PacketCodec<RegistryByteBuf, T> codec(BiConsumer<PacketByteBuf, T> enc, Function<PacketByteBuf, T> dec) {
        return new PacketCodec<RegistryByteBuf, T>() {
            @Override
            public T decode(RegistryByteBuf buf) {
                return dec.apply(buf);
            }

            @Override
            public void encode(RegistryByteBuf buf, T value) {
                enc.accept(buf, value);
            }
        };
    }

    private static <T extends CustomPayload> CustomPayload.Id<T> id(String path) {
        return new CustomPayload.Id<>(Identifier.of(SharedWaypoints.MOD_ID, path));
    }

    // ---------- klient -> serwer ----------

    public record UpsertC2S(Waypoint waypoint) implements CustomPayload {
        public static final Id<UpsertC2S> ID = id("upsert_c2s");
        public static final PacketCodec<RegistryByteBuf, UpsertC2S> CODEC =
                codec((b, v) -> v.waypoint().write(b), b -> new UpsertC2S(Waypoint.read(b)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DeleteC2S(UUID waypointId) implements CustomPayload {
        public static final Id<DeleteC2S> ID = id("delete_c2s");
        public static final PacketCodec<RegistryByteBuf, DeleteC2S> CODEC =
                codec((b, v) -> b.writeUuid(v.waypointId()), b -> new DeleteC2S(b.readUuid()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ---------- serwer -> klient ----------

    public record SyncS2C(List<Waypoint> list) implements CustomPayload {
        public static final Id<SyncS2C> ID = id("sync_s2c");
        public static final PacketCodec<RegistryByteBuf, SyncS2C> CODEC = codec(
                (b, v) -> {
                    b.writeVarInt(v.list().size());
                    for (Waypoint w : v.list()) w.write(b);
                },
                b -> {
                    int n = b.readVarInt();
                    List<Waypoint> l = new ArrayList<>();
                    for (int i = 0; i < n; i++) l.add(Waypoint.read(b));
                    return new SyncS2C(l);
                });

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record UpsertS2C(Waypoint waypoint) implements CustomPayload {
        public static final Id<UpsertS2C> ID = id("upsert_s2c");
        public static final PacketCodec<RegistryByteBuf, UpsertS2C> CODEC =
                codec((b, v) -> v.waypoint().write(b), b -> new UpsertS2C(Waypoint.read(b)));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record DeleteS2C(UUID waypointId) implements CustomPayload {
        public static final Id<DeleteS2C> ID = id("delete_s2c");
        public static final PacketCodec<RegistryByteBuf, DeleteS2C> CODEC =
                codec((b, v) -> b.writeUuid(v.waypointId()), b -> new DeleteS2C(b.readUuid()));

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
