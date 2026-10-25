package pl.sharedwaypoints;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WaypointsClient implements ClientModInitializer {
    public static KeyBinding ADD_KEY;
    public static KeyBinding SETTINGS_KEY;

    @Override
    public void onInitializeClient() {
        ClientConfig.load();

        ADD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sharedwaypoints.add", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "category.sharedwaypoints"));
        SETTINGS_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sharedwaypoints.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.sharedwaypoints"));

        // odbieranie danych z serwera
        ClientPlayNetworking.registerGlobalReceiver(Payloads.SyncS2C.ID,
                (payload, ctx) -> ClientWaypoints.replaceAll(payload.list()));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.UpsertS2C.ID,
                (payload, ctx) -> ClientWaypoints.put(payload.waypoint()));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DeleteS2C.ID,
                (payload, ctx) -> ClientWaypoints.remove(payload.waypointId()));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientWaypoints.clear());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (ADD_KEY.wasPressed()) {
                if (mc.player != null && mc.currentScreen == null) {
                    mc.setScreen(new WaypointEditScreen(null, null));
                }
            }
            while (SETTINGS_KEY.wasPressed()) {
                if (mc.player != null && mc.currentScreen == null) {
                    mc.setScreen(new WaypointSettingsScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register(WaypointHud::render);
    }
}
