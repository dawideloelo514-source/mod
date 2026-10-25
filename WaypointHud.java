package pl.sharedwaypoints;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/** Rysuje waypointy na ekranie (rzutowanie 3D -> 2D). */
public final class WaypointHud {
    private WaypointHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientConfig cfg = ClientConfig.get();
        if (!cfg.showHud || mc.player == null || mc.world == null || mc.options.hudHidden) return;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d pos = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());

        double sy = Math.sin(yaw), cy = Math.cos(yaw);
        double sp = Math.sin(pitch), cp = Math.cos(pitch);

        // wektory kamery
        double fx = -sy * cp, fy = -sp, fz = cy * cp;      // przód
        double rx = -cy, ry = 0, rz = -sy;                 // prawo
        double ux = -sy * sp, uy = cp, uz = cy * sp;       // góra

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        double fovRad = Math.toRadians(mc.options.getFov().getValue());
        double scale = (h / 2.0) / Math.tan(fovRad / 2.0);

        String dim = mc.world.getRegistryKey().getValue().toString();

        for (Waypoint wp : ClientWaypoints.all()) {
            if (!wp.dimension.equals(dim)) continue;
            if (cfg.hidden.contains(wp.id.toString())) continue;

            double dx = wp.x + 0.5 - pos.x;
            double dy = wp.y + 0.5 - pos.y;
            double dz = wp.z + 0.5 - pos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (cfg.maxDistance > 0 && dist > cfg.maxDistance) continue;

            double camX = dx * rx + dy * ry + dz * rz;
            double camY = dx * ux + dy * uy + dz * uz;
            double camZ = dx * fx + dy * fy + dz * fz;
            if (camZ < 0.05) continue; // za plecami

            int px = (int) (w / 2.0 + camX / camZ * scale);
            int py = (int) (h / 2.0 - camY / camZ * scale);
            if (px < -30 || px > w + 30 || py < -30 || py > h + 30) continue;

            int rgb = 0xFF000000 | wp.color;
            // znacznik (romb z obwódką)
            ctx.fill(px - 4, py - 4, px + 4, py + 4, 0xFF000000);
            ctx.fill(px - 3, py - 3, px + 3, py + 3, rgb);

            int textY = py - 18;
            if (cfg.showNames) {
                ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(wp.name), px, textY, rgb);
                textY += 10;
            }
            if (cfg.showDistance) {
                ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(formatDist(dist)), px, py + 7, 0xFFDDDDDD);
            }
        }
    }

    private static String formatDist(double d) {
        if (d < 1000) return (int) d + " m";
        return String.format(Locale.ROOT, "%.1f km", d / 1000.0);
    }
}
