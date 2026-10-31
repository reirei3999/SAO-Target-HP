package dev.sao.targethp.gui;

import dev.sao.targethp.config.SaoConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** On/off switches for the HUD, plus the F1 (hidden HUD) behaviour. */
public final class DisplayConfigScreen extends Screen {
    private static final int TOGGLE_WIDTH = 240;
    private static final int TOGGLE_HEIGHT = 20;
    private static final int ROW_HEIGHT = 26;

    private final TargetHpConfigScreen parent;
    private final SaoConfig config = SaoConfig.get();

    private int titleY;
    private int helpY;

    public DisplayConfigScreen(TargetHpConfigScreen parent) {
        super(Text.literal("表示設定"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int x = centerX - TOGGLE_WIDTH / 2;
        int top = Math.max(56, this.height / 2 - 88);
        this.titleY = top - 22;
        this.helpY = top + ROW_HEIGHT * 4 + 6;

        int y = top;
        addToggle(x, y, "MOD全体", () -> config.display.enabled, v -> config.display.enabled = v);
        y += ROW_HEIGHT;
        addToggle(x, y, "モブゲージ", () -> config.display.mobBar, v -> config.display.mobBar = v);
        y += ROW_HEIGHT;
        addToggle(x, y, "ボスバー", () -> config.display.bossBar, v -> config.display.bossBar = v);
        y += ROW_HEIGHT;
        addToggle(x, y, "F1中も表示", () -> config.display.showWhenHudHidden,
                v -> config.display.showWhenHudHidden = v);

        int buttonWidth = 120;
        int doneY = Math.max(this.helpY + 48, this.height - 32);
        addDrawableChild(ButtonWidget.builder(Text.literal("完了"), b -> close())
                .dimensions(centerX - buttonWidth / 2, doneY, buttonWidth, TOGGLE_HEIGHT).build());
    }

    private void addToggle(int x, int y, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        ButtonWidget button = ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), b -> {
            boolean value = !getter.getAsBoolean();
            setter.accept(value);
            b.setMessage(toggleText(label, value));
        }).dimensions(x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT).build();
        addDrawableChild(button);
    }

    private static Text toggleText(String label, boolean enabled) {
        return Text.literal(label + ": " + (enabled ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, this.titleY, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("F1でHUDを隠したときに、このMODのゲージを表示するか選べます"),
                centerX, this.helpY, 0xFFB0B0B0);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("OFFにすると描画とターゲット検索も止まるので負荷が下がります"),
                centerX, this.helpY + 12, 0xFF909090);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("設定は config/saotargethp.json に保存されます"),
                centerX, this.helpY + 24, 0xFF909090);
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
