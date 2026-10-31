package dev.sao.targethp.gui;

import dev.sao.targethp.config.SaoConfig;
import dev.sao.targethp.render.BarShapes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Drag-to-position editor. Both previews follow the real geometry, so the boss bar
 * lands exactly where it will be drawn while the mob gauge is a schematic preview
 * (its on-screen size depends on the distance to the target).
 */
public final class LayoutEditorScreen extends Screen {
    private static final double PIXELS_PER_BLOCK = 30.0D;
    private static final float PREVIEW_PIN_SIZE = 18.0f;
    private static final int BOSS_WIDTH = 430;
    private static final int BOSS_HEIGHT = 12;

    private final TargetHpConfigScreen parent;
    private final SaoConfig config = SaoConfig.get();

    private Drag drag = Drag.NONE;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private double dragStartOffsetX;
    private double dragStartOffsetY;

    private int pinX;
    private int pinY;
    private int mobLeft;
    private int mobTop;
    private int mobRight;
    private int mobBottom;
    private int bossLeft;
    private int bossTop;
    private int bossRight;
    private int bossBottom;

    public LayoutEditorScreen(TargetHpConfigScreen parent) {
        super(Text.literal("配置の調整"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int gap = 8;
        addDrawableChild(ButtonWidget.builder(Text.literal("位置を初期化"), b -> resetPositions())
                .dimensions(this.width / 2 - buttonWidth - gap / 2, this.height - 28, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("完了"), b -> close())
                .dimensions(this.width / 2 + gap / 2, this.height - 28, buttonWidth, 20).build());
        layout();
    }

    private void layout() {
        this.pinX = this.width / 2;
        this.pinY = this.height / 2;

        double anchorX = this.pinX + config.mob.offsetX * PIXELS_PER_BLOCK;
        double anchorY = this.pinY - (config.mob.offsetY - 1.5D) * PIXELS_PER_BLOCK;

        double barWidth = PREVIEW_PIN_SIZE * 9.6D;
        double barHeight = PREVIEW_PIN_SIZE * 0.68D;
        double gap = PREVIEW_PIN_SIZE * 0.46D;

        this.mobRight = (int) Math.round(anchorX - PREVIEW_PIN_SIZE - gap);
        this.mobLeft = (int) Math.round(this.mobRight - barWidth);
        this.mobTop = (int) Math.round(anchorY - barHeight / 2.0D);
        this.mobBottom = (int) Math.round(anchorY + barHeight / 2.0D);

        this.bossLeft = this.pinX + config.boss.offsetX - BOSS_WIDTH / 2;
        this.bossTop = config.boss.offsetY;
        this.bossRight = this.bossLeft + BOSS_WIDTH;
        this.bossBottom = this.bossTop + BOSS_HEIGHT;
    }

    private void resetPositions() {
        config.mob.offsetX = 0.0D;
        config.mob.offsetY = 0.0D;
        config.boss.offsetX = 0;
        config.boss.offsetY = 24;
        config.normalize();
        layout();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        layout();

        int centerX = this.width / 2;
        drawPin(context);
        drawMobGauge(context);
        drawBossBar(context);

        // Text is drawn last so it stays readable wherever the bars are dragged.
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 6, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format(Locale.ROOT, "モブ X %.2f  Y %.2f", config.mob.offsetX, config.mob.offsetY)),
                6, 6, 0xFFD8D8D8);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(String.format(Locale.ROOT, "ボス X %d  Y %d", config.boss.offsetX, config.boss.offsetY)),
                6, 16, 0xFFD8D8D8);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("バーをドラッグして移動できます"), centerX, this.height - 40, 0xFFB0B0B0);
    }

    private void drawPin(DrawContext context) {
        int radius = 9;
        int color = 0xFFE51B45;
        for (int row = -radius; row <= radius; row++) {
            int half = radius - Math.abs(row);
            context.fill(this.pinX - half, this.pinY + row, this.pinX + half + 1, this.pinY + row + 1, color);
        }
    }

    private void drawMobGauge(DrawContext context) {
        int width = this.mobRight - this.mobLeft;
        int height = this.mobBottom - this.mobTop;
        if (width <= 0 || height <= 2) {
            return;
        }
        BarShapes.drawTaperedBar(context, this.mobLeft - 2, this.mobTop - 2, width + 4, height + 4,
                1.0f, 1.6f, config.mob.outlineArgb);
        BarShapes.drawTaperedBar(context, this.mobLeft, this.mobTop, width, height,
                1.0f, 1.6f, config.mob.backgroundArgb);
        BarShapes.drawTaperedBar(context, this.mobLeft, this.mobTop, width, height,
                0.78f, 1.6f, config.mob.normalArgb);
        BarShapes.drawTaperedBar(context, this.mobLeft, this.mobTop, width, Math.max(3, height / 3),
                0.78f, 1.6f, config.mob.normalHighlightArgb);
    }

    private void drawBossBar(DrawContext context) {
        BarShapes.drawTaperedBar(context, this.bossLeft - 2, this.bossTop - 2, BOSS_WIDTH + 4, BOSS_HEIGHT + 4,
                1.0f, 1.35f, config.boss.outlineArgb);
        BarShapes.drawTaperedBar(context, this.bossLeft - 1, this.bossTop - 1, BOSS_WIDTH + 2, BOSS_HEIGHT + 2,
                1.0f, 1.35f, config.boss.rimArgb);
        BarShapes.drawTaperedBar(context, this.bossLeft, this.bossTop, BOSS_WIDTH, BOSS_HEIGHT,
                1.0f, 1.35f, config.boss.backgroundArgb);
        BarShapes.drawTaperedBar(context, this.bossLeft + 1, this.bossTop + 1, BOSS_WIDTH - 2, BOSS_HEIGHT - 2,
                0.78f, 1.35f, config.boss.fillArgb);
        BarShapes.drawTaperedBar(context, this.bossLeft + 1, this.bossTop + 1, BOSS_WIDTH - 2, 2,
                0.78f, 1.35f, config.boss.highlightArgb);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (inside(this.mobLeft, this.mobTop, this.mobRight, this.mobBottom, mouseX, mouseY)) {
            beginDrag(Drag.MOB, mouseX, mouseY, config.mob.offsetX, config.mob.offsetY);
            return true;
        }
        if (inside(this.bossLeft, this.bossTop, this.bossRight, this.bossBottom, mouseX, mouseY)) {
            beginDrag(Drag.BOSS, mouseX, mouseY, config.boss.offsetX, config.boss.offsetY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.drag == Drag.MOB) {
            config.mob.offsetX = round2(this.dragStartOffsetX + (mouseX - this.dragStartMouseX) / PIXELS_PER_BLOCK);
            config.mob.offsetY = round2(this.dragStartOffsetY - (mouseY - this.dragStartMouseY) / PIXELS_PER_BLOCK);
            config.normalize();
            return true;
        }
        if (this.drag == Drag.BOSS) {
            config.boss.offsetX = (int) Math.round(this.dragStartOffsetX + (mouseX - this.dragStartMouseX));
            config.boss.offsetY = (int) Math.round(this.dragStartOffsetY + (mouseY - this.dragStartMouseY));
            config.normalize();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.drag = Drag.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private void beginDrag(Drag kind, double mouseX, double mouseY, double offsetX, double offsetY) {
        this.drag = kind;
        this.dragStartMouseX = mouseX;
        this.dragStartMouseY = mouseY;
        this.dragStartOffsetX = offsetX;
        this.dragStartOffsetY = offsetY;
    }

    private static boolean inside(int left, int top, int right, int bottom, double x, double y) {
        return x >= left - 6 && x <= right + 6 && y >= top - 7 && y <= bottom + 7;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    private enum Drag {
        NONE,
        MOB,
        BOSS
    }
}
