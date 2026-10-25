package pl.sharedwaypoints;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Ekran dodawania ([I]) i edycji waypointu. */
public class WaypointEditScreen extends Screen {
    private static final int[] COLORS = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF, 0xFFFFFF};

    private final Screen parent;
    private final Waypoint editing;
    private TextFieldWidget nameField, xField, yField, zField;
    private ButtonWidget colorButton;
    private int colorIdx = 0;
    private String error = "";

    public WaypointEditScreen(Screen parent, Waypoint editing) {
        super(Text.literal(editing == null ? "Nowy waypoint" : "Edytuj waypoint"));
        this.parent = parent;
        this.editing = editing;
        if (editing != null) {
            for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == editing.color) colorIdx = i;
        }
    }

    private Text colorText() {
        int c = COLORS[colorIdx];
        return Text.literal("Kolor: ").append(Text.literal("████").styled(s -> s.withColor(c)));
    }

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int cx = width / 2;
        int top = height / 2 - 70;

        String name = "", x = "0", y = "64", z = "0";
        if (editing != null) {
            name = editing.name;
            x = String.valueOf(editing.x);
            y = String.valueOf(editing.y);
            z = String.valueOf(editing.z);
        } else if (mc.player != null) {
            BlockPos p = mc.player.getBlockPos();
            x = String.valueOf(p.getX());
            y = String.valueOf(p.getY());
            z = String.valueOf(p.getZ());
        }

        nameField = new TextFieldWidget(textRenderer, cx - 100, top, 200, 20, Text.literal("Nazwa"));
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.literal("Nazwa waypointu"));
        nameField.setText(name);

        xField = new TextFieldWidget(textRenderer, cx - 100, top + 36, 60, 20, Text.literal("X"));
        yField = new TextFieldWidget(textRenderer, cx - 30, top + 36, 60, 20, Text.literal("Y"));
        zField = new TextFieldWidget(textRenderer, cx + 40, top + 36, 60, 20, Text.literal("Z"));
        xField.setMaxLength(9);
        yField.setMaxLength(9);
        zField.setMaxLength(9);
        xField.setText(x);
        yField.setText(y);
        zField.setText(z);

        addDrawableChild(nameField);
        addDrawableChild(xField);
        addDrawableChild(yField);
        addDrawableChild(zField);

        colorButton = addDrawableChild(ButtonWidget.builder(colorText(), b -> {
            colorIdx = (colorIdx + 1) % COLORS.length;
            b.setMessage(colorText());
        }).dimensions(cx - 100, top + 66, 200, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                .dimensions(cx - 100, top + 96, 98, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(cx + 2, top + 96, 98, 20).build());

        setInitialFocus(nameField);
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            error = "Podaj nazwę waypointu";
            return;
        }
        int x, y, z;
        try {
            x = Integer.parseInt(xField.getText().trim());
            y = Integer.parseInt(yField.getText().trim());
            z = Integer.parseInt(zField.getText().trim());
        } catch (NumberFormatException e) {
            error = "Współrzędne muszą być liczbami całkowitymi";
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        if (!ClientPlayNetworking.canSend(Payloads.UpsertC2S.ID)) {
            error = "Serwer nie ma zainstalowanego moda!";
            return;
        }

        UUID id = editing != null ? editing.id : UUID.randomUUID();
        String dim = editing != null ? editing.dimension : mc.world.getRegistryKey().getValue().toString();
        Waypoint wp = new Waypoint(id, name, x, y, z, COLORS[colorIdx], dim);
        ClientPlayNetworking.send(new Payloads.UpsertC2S(wp));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int top = height / 2 - 70;
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, top - 25, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "X", cx - 100, top + 25, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Y", cx - 30, top + 25, 0xFFAAAAAA);
        ctx.drawTextWithShadow(textRenderer, "Z", cx + 40, top + 25, 0xFFAAAAAA);
        if (!error.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(error), cx, top + 124, 0xFFFF5555);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
