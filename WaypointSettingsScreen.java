package pl.sharedwaypoints;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Ekran ustawień ([G]) + lista waypointów. */
public class WaypointSettingsScreen extends Screen {
    private static final int PER_PAGE = 6;

    private int page = 0;
    private int seenVersion;
    private final List<Waypoint> rows = new ArrayList<>();
    private int listTop;

    public WaypointSettingsScreen() {
        super(Text.literal("Ustawienia waypointów"));
    }

    private static Text toggle(String label, boolean on) {
        return Text.literal(label + ": ").append(Text.literal(on ? "TAK" : "NIE")
                .styled(s -> s.withColor(on ? 0x55FF55 : 0xFF5555)));
    }

    private static String distLabel(int d) {
        return d == 0 ? "bez limitu" : d + " m";
    }

    @Override
    protected void init() {
        ClientConfig c = ClientConfig.get();
        seenVersion = ClientWaypoints.version;
        MinecraftClient mc = MinecraftClient.getInstance();
        int cx = width / 2;
        int y = 28;

        addDrawableChild(ButtonWidget.builder(toggle("Pokazuj waypointy", c.showHud), b -> {
            c.showHud = !c.showHud;
            c.save();
            b.setMessage(toggle("Pokazuj waypointy", c.showHud));
        }).dimensions(cx - 155, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(toggle("Nazwy", c.showNames), b -> {
            c.showNames = !c.showNames;
            c.save();
            b.setMessage(toggle("Nazwy", c.showNames));
        }).dimensions(cx + 5, y, 150, 20).build());

        y += 24;
        addDrawableChild(ButtonWidget.builder(toggle("Dystans", c.showDistance), b -> {
            c.showDistance = !c.showDistance;
            c.save();
            b.setMessage(toggle("Dystans", c.showDistance));
        }).dimensions(cx - 155, y, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zasięg: " + distLabel(c.maxDistance)), b -> {
            c.cycleDistance();
            c.save();
            b.setMessage(Text.literal("Zasięg: " + distLabel(c.maxDistance)));
        }).dimensions(cx + 5, y, 150, 20).build());

        listTop = y + 40;

        // lista waypointów
        List<Waypoint> all = ClientWaypoints.all();
        int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= pages) page = pages - 1;
        rows.clear();
        for (int i = page * PER_PAGE; i < Math.min(all.size(), (page + 1) * PER_PAGE); i++) rows.add(all.get(i));

        for (int i = 0; i < rows.size(); i++) {
            Waypoint wp = rows.get(i);
            int ry = listTop + i * 24;
            boolean mine = mc.player != null && wp.owner.equals(mc.player.getUuid());
            boolean hidden = c.hidden.contains(wp.id.toString());

            addDrawableChild(ButtonWidget.builder(
                    Text.literal(hidden ? "Ukryty" : "Widoczny"), b -> {
                        String key = wp.id.toString();
                        if (!c.hidden.remove(key)) c.hidden.add(key);
                        c.save();
                        clearAndInit();
                    }).dimensions(cx - 30, ry, 60, 20).build());

            ButtonWidget edit = addDrawableChild(ButtonWidget.builder(Text.literal("Edytuj"),
                    b -> MinecraftClient.getInstance().setScreen(new WaypointEditScreen(this, wp)))
                    .dimensions(cx + 34, ry, 50, 20).build());
            edit.active = mine;

            ButtonWidget del = addDrawableChild(ButtonWidget.builder(Text.literal("Usuń"), b -> {
                if (ClientPlayNetworking.canSend(Payloads.DeleteC2S.ID)) {
                    ClientPlayNetworking.send(new Payloads.DeleteC2S(wp.id));
                }
            }).dimensions(cx + 88, ry, 50, 20).build());
            del.active = mine;
        }

        int by = height - 52;
        ButtonWidget prev = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            clearAndInit();
        }).dimensions(cx - 155, by, 40, 20).build());
        prev.active = page > 0;

        ButtonWidget next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            clearAndInit();
        }).dimensions(cx + 115, by, 40, 20).build());
        next.active = page < pages - 1;

        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj waypoint"),
                b -> MinecraftClient.getInstance().setScreen(new WaypointEditScreen(this, null)))
                .dimensions(cx - 100, by, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(cx - 100, height - 28, 200, 20).build());
    }

    @Override
    public void tick() {
        if (seenVersion != ClientWaypoints.version) clearAndInit();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, 10, 0xFFFFFFFF);

        int total = ClientWaypoints.all().size();
        int pages = Math.max(1, (total + PER_PAGE - 1) / PER_PAGE);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Waypointy (" + total + ")  -  strona " + (page + 1) + "/" + pages),
                cx, listTop - 14, 0xFFAAAAAA);

        if (rows.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Brak waypointów. Naciśnij [I] w grze, aby dodać."),
                    cx, listTop + 10, 0xFF888888);
        }

        for (int i = 0; i < rows.size(); i++) {
            Waypoint wp = rows.get(i);
            int ry = listTop + i * 24;
            String label = textRenderer.trimToWidth(wp.name, 115);
            ctx.drawTextWithShadow(textRenderer, label, cx - 155, ry + 2, 0xFF000000 | wp.color);
            String sub = wp.x + " " + wp.y + " " + wp.z + (wp.ownerName.isEmpty() ? "" : "  · " + wp.ownerName);
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(sub, 120), cx - 155, ry + 11, 0xFF888888);
        }
    }
}
