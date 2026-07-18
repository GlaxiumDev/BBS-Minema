package Glaxium.Minema.ui;

import Glaxium.Minema.MinemaConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Deliberately a plain vanilla {@link Screen}, not one of BBS mod's own
 * {@code UIScreen}/{@code UIOverlayPanel} elements -- those are designed to
 * live inside BBS mod's own UI tree and need an existing {@code UIContext}
 * to open at all. Building this standalone means
 * {@code MinecraftClient#setScreen(new MinemaSettingsScreen(...))} just
 * works no matter what's currently on screen: nothing open, a vanilla
 * screen open, or BBS's own dashboard open (setScreen() simply replaces
 * it) -- one implementation, reachable identically from the F4-adjacent
 * keybind (BBSMinema#minemaSettingsKey / Shift+F4) and from the button
 * added into BBS's own video settings panel (see MinemaSettingsButton).
 *
 * Visually deliberately its own thing rather than mimicking BBS's blue
 * panel chrome -- dark violet/magenta theme, no dependency on BBS's font
 * renderer, icon atlas, or UI framework at all.
 */
public class MinemaSettingsScreen extends Screen
{
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_TOP_OFFSET = 100;
    private static final int PANEL_BOTTOM_OFFSET = 110;
    private static final int ROW_HEIGHT = 22;

    private static final int COLOR_BACKDROP = 0xCC120A1E;
    private static final int COLOR_PANEL = 0xE81C1030;
    private static final int COLOR_BORDER = 0xFFB24DFF;
    private static final int COLOR_TITLE = 0xFFE79CFF;
    private static final int COLOR_LABEL = 0xFF8A5CFF;
    private static final int COLOR_FOOTER = 0xFF8C7EA8;

    private final Screen previousScreen;
    private final MinemaConfig config = MinemaConfig.INSTANCE;
    private final List<RowLabel> rowLabels = new ArrayList<>();

    public MinemaSettingsScreen(Screen previousScreen)
    {
        super(Text.literal("Minema Settings"));

        this.previousScreen = previousScreen;
    }

    @Override
    protected void init()
    {
        this.rowLabels.clear();

        int centerX = this.width / 2;
        int y = this.panelTop() + 34;

        y = this.addToggleRow(centerX, y, "Record in-game audio",
                () -> this.config.recordGameAudio,
                (value) -> { this.config.recordGameAudio = value; this.config.save(); });

        y = this.addToggleRow(centerX, y, "Generate .wav audio file",
                () -> this.config.generateWavFile,
                (value) -> { this.config.generateWavFile = value; this.config.save(); });

        y = this.addToggleRow(centerX, y, "Capture depth pass",
                () -> this.config.captureDepth,
                (value) -> { this.config.captureDepth = value; this.config.save(); });

        y = this.addDepthDistanceRow(centerX, y);

        y = this.addToggleRow(centerX, y, "Sync engine to capture",
                () -> this.config.syncEngine,
                (value) -> { this.config.syncEngine = value; this.config.save(); });

        y += 10;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), (b) -> this.close())
                .dimensions(centerX - 60, y, 120, 20)
                .build());
    }

    private int panelTop()
    {
        return this.height / 2 - PANEL_TOP_OFFSET;
    }

    private int panelBottom()
    {
        return this.height / 2 + PANEL_BOTTOM_OFFSET;
    }

    private int addToggleRow(int centerX, int y, String label, BooleanSupplier getter, Consumer<Boolean> setter)
    {
        int right = centerX + PANEL_WIDTH / 2 - 10;
        int left = centerX - PANEL_WIDTH / 2 + 10;
        int toggleWidth = 50;

        ButtonWidget[] holder = new ButtonWidget[1];

        holder[0] = ButtonWidget.builder(toggleLabel(getter.getAsBoolean()), (b) ->
        {
            boolean newValue = !getter.getAsBoolean();

            setter.accept(newValue);
            holder[0].setMessage(toggleLabel(newValue));
        }).dimensions(right - toggleWidth, y, toggleWidth, 20).build();

        this.addDrawableChild(holder[0]);
        this.rowLabels.add(new RowLabel(label, left, y + 6));

        return y + ROW_HEIGHT;
    }

    private int addDepthDistanceRow(int centerX, int y)
    {
        int right = centerX + PANEL_WIDTH / 2 - 10;
        int left = centerX - PANEL_WIDTH / 2 + 10;

        this.rowLabels.add(new RowLabel("Depth capture distance (" + (int) this.config.captureDepthDistance + ")", left, y + 6));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("-"), (b) -> this.adjustDepthDistance(-16))
                .dimensions(right - 66, y, 20, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("+"), (b) -> this.adjustDepthDistance(16))
                .dimensions(right - 20, y, 20, 20)
                .build());

        return y + ROW_HEIGHT;
    }

    private void adjustDepthDistance(int delta)
    {
        double next = Math.max(1, Math.min(1024, this.config.captureDepthDistance + delta));

        this.config.captureDepthDistance = next;
        this.config.save();

        // Simplest way to refresh the "(128)" label text -- rebuilds every
        // row, but this only runs on a +/- click, not per-frame.
        this.clearChildren();
        this.init();
    }

    private static Text toggleLabel(boolean value)
    {
        return value
                ? Text.literal("ON").formatted(Formatting.GREEN)
                : Text.literal("OFF").formatted(Formatting.RED);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)
    {
        context.fill(0, 0, this.width, this.height, COLOR_BACKDROP);

        int centerX = this.width / 2;
        int left = centerX - PANEL_WIDTH / 2;
        int right = centerX + PANEL_WIDTH / 2;
        int top = this.panelTop();
        int bottom = this.panelBottom();

        context.fill(left, top, right, bottom, COLOR_PANEL);
        context.fill(left, top, right, top + 1, COLOR_BORDER);
        context.fill(left, bottom - 1, right, bottom, COLOR_BORDER);
        context.fill(left, top, left + 1, bottom, COLOR_BORDER);
        context.fill(right - 1, top, right, bottom, COLOR_BORDER);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("MINEMA").formatted(Formatting.BOLD), centerX, top + 10, COLOR_TITLE);

        for (RowLabel row : this.rowLabels)
        {
            context.drawTextWithShadow(this.textRenderer, row.text, row.x, row.y, COLOR_LABEL);
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("F4 -- Record Video    Shift+F4 -- This panel").formatted(Formatting.ITALIC),
                centerX, bottom + 6, COLOR_FOOTER);
    }

    @Override
    public void close()
    {
        this.client.setScreen(this.previousScreen);
    }

    @Override
    public boolean shouldPause()
    {
        return false;
    }

    private static final class RowLabel
    {
        private final Text text;
        private final int x;
        private final int y;

        private RowLabel(String text, int x, int y)
        {
            this.text = Text.literal(text);
            this.x = x;
            this.y = y;
        }
    }
}
