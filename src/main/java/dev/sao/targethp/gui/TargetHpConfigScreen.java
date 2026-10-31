package dev.sao.targethp.gui;

import dev.sao.targethp.config.SaoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** In-game editor for the bar colours, thresholds and positions. */
public final class TargetHpConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int WIDGET_HEIGHT = 20;

    private final Screen parent;
    private final SaoConfig config = SaoConfig.get();

    private int titleY;
    private int mobHeaderY;
    private int bossHeaderY;

    public TargetHpConfigScreen(Screen parent) {
        super(Text.literal("SAO Target HP 設定"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int leftX = centerX - 155;
        int rightX = centerX + 5;
        int widgetWidth = 150;

        int top = Math.max(44, this.height / 2 - 96);
        this.titleY = top - 20;
        this.mobHeaderY = top;
        int rowY = top + 12;

        addDrawableChild(new ValueSlider(leftX, rowY, widgetWidth, WIDGET_HEIGHT, "イエローになるHP", 0, 100, 1, true,
                () -> config.mob.cautionThresholdPercent, v -> config.mob.cautionThresholdPercent = v));
        addDrawableChild(new ValueSlider(rightX, rowY, widgetWidth, WIDGET_HEIGHT, "レッドになるHP", 0, 100, 1, true,
                () -> config.mob.dangerThresholdPercent, v -> config.mob.dangerThresholdPercent = v));

        rowY += ROW_HEIGHT;
        addDrawableChild(new ValueSlider(leftX, rowY, widgetWidth, WIDGET_HEIGHT, "モブ 横位置", -2, 2, 0.05, false,
                () -> config.mob.offsetX, v -> config.mob.offsetX = v));
        addDrawableChild(new ValueSlider(rightX, rowY, widgetWidth, WIDGET_HEIGHT, "モブ 縦位置", -2, 2, 0.05, false,
                () -> config.mob.offsetY, v -> config.mob.offsetY = v));

        this.bossHeaderY = rowY + ROW_HEIGHT + 14;
        rowY = this.bossHeaderY + 12;
        addDrawableChild(new ValueSlider(leftX, rowY, widgetWidth, WIDGET_HEIGHT, "ボス 横位置", -600, 600, 1, false,
                () -> config.boss.offsetX, v -> config.boss.offsetX = (int) Math.round(v)));
        addDrawableChild(new ValueSlider(rightX, rowY, widgetWidth, WIDGET_HEIGHT, "ボス 縦位置", -100, 600, 1, false,
                () -> config.boss.offsetY, v -> config.boss.offsetY = (int) Math.round(v)));

        rowY += ROW_HEIGHT;
        addDrawableChild(new ValueSlider(leftX, rowY, widgetWidth, WIDGET_HEIGHT, "ボス 行間", 8, 80, 1, false,
                () -> config.boss.spacing, v -> config.boss.spacing = (int) Math.round(v)));

        int buttonY = rowY + ROW_HEIGHT + 12;
        int buttonWidth = 92;
        int gap = 6;
        int totalWidth = buttonWidth * 4 + gap * 3;
        int buttonX = centerX - totalWidth / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("配置をドラッグ"), b -> openLayoutEditor())
                .dimensions(buttonX, buttonY, buttonWidth, WIDGET_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("色設定..."), b -> openColorScreen())
                .dimensions(buttonX + buttonWidth + gap, buttonY, buttonWidth, WIDGET_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("初期値に戻す"), b -> resetDefaults())
                .dimensions(buttonX + (buttonWidth + gap) * 2, buttonY, buttonWidth, WIDGET_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("完了"), b -> close())
                .dimensions(buttonX + (buttonWidth + gap) * 3, buttonY, buttonWidth, WIDGET_HEIGHT).build());
    }

    private void openLayoutEditor() {
        if (this.client != null) {
            this.client.setScreen(new LayoutEditorScreen(this));
        }
    }

    private void openColorScreen() {
        if (this.client != null) {
            this.client.setScreen(new ColorConfigScreen(this));
        }
    }

    private void resetDefaults() {
        config.resetToDefaults();
        if (this.client != null) {
            this.client.setScreen(new TargetHpConfigScreen(this.parent));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, this.titleY, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("モブゲージ"), centerX - 155, this.mobHeaderY, 0xFFADADAD);
        context.drawTextWithShadow(this.textRenderer, Text.literal("ボスバー"), centerX - 155, this.bossHeaderY, 0xFFADADAD);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("config/saotargethp.json に保存されます"), centerX, this.height - 14, 0xFF909090);
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    /** Slider bound directly to a config value. */
    private static final class ValueSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final double step;
        private final boolean percent;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;

        private ValueSlider(int x, int y, int width, int height, String label, double min, double max,
                            double step, boolean percent, DoubleSupplier getter, DoubleConsumer setter) {
            super(x, y, width, height, Text.empty(), toNormalized(getter.getAsDouble(), min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.percent = percent;
            this.getter = getter;
            this.setter = setter;
            updateMessage();
        }

        private static double toNormalized(double value, double min, double max) {
            if (max <= min) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, (value - min) / (max - min)));
        }

        @Override
        protected void updateMessage() {
            // The super constructor calls this before our fields are assigned.
            if (this.getter == null) {
                return;
            }
            double value = this.getter.getAsDouble();
            String formatted = this.percent
                    ? Math.round(value) + "%"
                    : String.format(Locale.ROOT, "%.2f", value);
            setMessage(Text.literal(this.label + ": " + formatted));
        }

        @Override
        protected void applyValue() {
            double raw = this.min + this.value * (this.max - this.min);
            if (this.step > 0.0D) {
                raw = Math.round(raw / this.step) * this.step;
            }
            this.setter.accept(Math.max(this.min, Math.min(this.max, raw)));
            updateMessage();
        }
    }
}
