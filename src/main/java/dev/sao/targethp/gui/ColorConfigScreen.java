package dev.sao.targethp.gui;

import dev.sao.targethp.config.SaoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** HEX colour editor for every gauge colour. */
public final class ColorConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int FIELD_WIDTH = 110;
    private static final int COLUMN_WIDTH = 200;

    private final TargetHpConfigScreen parent;
    private final SaoConfig config = SaoConfig.get();
    private final List<Row> rows = new ArrayList<>();

    private int titleY;

    public ColorConfigScreen(TargetHpConfigScreen parent) {
        super(Text.literal("色設定"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rows.clear();

        List<Entry> entries = List.of(
                new Entry("通常", () -> config.mob.normalColor, v -> config.mob.normalColor = v),
                new Entry("通常ハイライト", () -> config.mob.normalHighlightColor, v -> config.mob.normalHighlightColor = v),
                new Entry("警戒", () -> config.mob.cautionColor, v -> config.mob.cautionColor = v),
                new Entry("警戒ハイライト", () -> config.mob.cautionHighlightColor, v -> config.mob.cautionHighlightColor = v),
                new Entry("危険", () -> config.mob.dangerColor, v -> config.mob.dangerColor = v),
                new Entry("危険ハイライト", () -> config.mob.dangerHighlightColor, v -> config.mob.dangerHighlightColor = v),
                new Entry("残像", () -> config.mob.trailColor, v -> config.mob.trailColor = v),
                new Entry("背景", () -> config.mob.backgroundColor, v -> config.mob.backgroundColor = v),
                new Entry("外枠", () -> config.mob.outlineColor, v -> config.mob.outlineColor = v),
                new Entry("ボス塗り", () -> config.boss.fillColor, v -> config.boss.fillColor = v),
                new Entry("ボス強調", () -> config.boss.highlightColor, v -> config.boss.highlightColor = v),
                new Entry("ボス背景", () -> config.boss.backgroundColor, v -> config.boss.backgroundColor = v),
                new Entry("ボスリム", () -> config.boss.rimColor, v -> config.boss.rimColor = v),
                new Entry("ボス外枠", () -> config.boss.outlineColor, v -> config.boss.outlineColor = v),
                new Entry("ボス名", () -> config.boss.nameColor, v -> config.boss.nameColor = v));

        int centerX = this.width / 2;
        int leftColumnX = centerX - COLUMN_WIDTH - 2;
        int rightColumnX = centerX + 2;
        int top = Math.max(46, this.height / 2 - 88);
        this.titleY = top - 32;

        int perColumn = 8;
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int column = i / perColumn;
            int row = i % perColumn;
            int x = column == 0 ? leftColumnX : rightColumnX;
            int y = top + row * ROW_HEIGHT;

            TextFieldWidget field = new TextFieldWidget(this.textRenderer,
                    x + 86, y, FIELD_WIDTH, 18, Text.literal(entry.label));
            field.setMaxLength(9);
            field.setText(entry.getter.get());
            addDrawableChild(field);
            this.rows.add(new Row(field, entry.label, x, y, entry.getter, entry.setter));
        }

        int buttonY = top + perColumn * ROW_HEIGHT + 10;
        int buttonWidth = 110;
        int gap = 8;
        int buttonX = centerX - buttonWidth - gap / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("初期値に戻す"), b -> resetDefaults())
                .dimensions(buttonX, buttonY, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("完了"), b -> close())
                .dimensions(buttonX + buttonWidth + gap, buttonY, buttonWidth, 20).build());
    }

    private void resetDefaults() {
        config.resetToDefaults();
        for (Row row : this.rows) {
            row.field.setText(row.getter.get());
        }
    }

    private void apply() {
        for (Row row : this.rows) {
            String text = row.field.getText().trim();
            if (!SaoConfig.isValidColor(text)) {
                // Keep the previous value when the entry is not a valid colour.
                row.field.setText(row.getter.get());
                continue;
            }
            row.setter.accept(text.startsWith("#") ? text : "#" + text);
        }
        config.save();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, this.titleY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("形式: #RRGGBB または #AARRGGBB（先頭2桁が透明度）"),
                centerX, this.titleY + 12, 0xFF909090);
        for (Row row : this.rows) {
            context.drawTextWithShadow(this.textRenderer, Text.literal(row.label), row.x, row.y + 5, 0xFFD0D0D0);
        }
    }

    @Override
    public void close() {
        apply();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private record Entry(String label, Supplier<String> getter, Consumer<String> setter) {
    }

    private static final class Row {
        private final TextFieldWidget field;
        private final String label;
        private final int x;
        private final int y;
        private final Supplier<String> getter;
        private final Consumer<String> setter;

        private Row(TextFieldWidget field, String label, int x, int y,
                    Supplier<String> getter, Consumer<String> setter) {
            this.field = field;
            this.label = label;
            this.x = x;
            this.y = y;
            this.getter = getter;
            this.setter = setter;
        }
    }
}
