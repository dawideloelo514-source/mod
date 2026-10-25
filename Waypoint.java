package pl.sharedwaypoints;

import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class Waypoint {
    public UUID id;
    public String name;
    public int x, y, z;
    public int color;          // 0xRRGGBB
    public String dimension;   // np. minecraft:overworld
    public UUID owner;
    public String ownerName;

    public Waypoint() {}

    public Waypoint(UUID id, String name, int x, int y, int z, int color, String dimension) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.dimension = dimension;
        this.owner = new UUID(0, 0);
        this.ownerName = "";
    }

    public void write(PacketByteBuf b) {
        b.writeUuid(id);
        b.writeString(name, 64);
        b.writeInt(x);
        b.writeInt(y);
        b.writeInt(z);
        b.writeInt(color);
        b.writeString(dimension, 128);
        b.writeUuid(owner);
        b.writeString(ownerName, 64);
    }

    public static Waypoint read(PacketByteBuf b) {
        Waypoint w = new Waypoint();
        w.id = b.readUuid();
        w.name = b.readString(64);
        w.x = b.readInt();
        w.y = b.readInt();
        w.z = b.readInt();
        w.color = b.readInt();
        w.dimension = b.readString(128);
        w.owner = b.readUuid();
        w.ownerName = b.readString(64);
        return w;
    }
}
