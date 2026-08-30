package dev.sao.targethp.mixin;

import dev.sao.targethp.mixin.accessor.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/** Replaces the vanilla boss bar renderer with an original SAO-inspired presentation. */
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {
    private static final int BAR_WIDTH = 300;
    private static final int BAR_HEIGHT = 8;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void saotargethp$render(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<UUID, ClientBossBar> bars = ((BossBarHudAccessor) this).saotargethp$getBossBars();
        if (bars.isEmpty() || client.player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int centerX = screenWidth / 2;
        int y = 18;

        for (ClientBossBar bar : bars.values()) {
            String name = bar.getName().getString();
            int nameWidth = client.textRenderer.getWidth(name);
            int nameX = centerX - nameWidth / 2;
            int left = centerX - BAR_WIDTH / 2;
            int right = left + BAR_WIDTH;

            int gold = 0xFFE8D59A;
            int white = 0xFFF4F4F4;
            int dark = 0xE6000000;
            int frame = 0xFFD0CBC0;
            int empty = 0xFF303030;
            int fill = 0xFFC94A4A;
            int highlight = 0xFFFF9A8A;

            context.drawText(client.textRenderer, Text.literal(name), nameX, y - 13, white, true);
            context.fill(left - 5, y - 4, right + 5, y + BAR_HEIGHT + 4, dark);
            context.fill(left - 3, y - 2, right + 3, y + BAR_HEIGHT + 2, frame);
            context.fill(left - 1, y, right + 1, y + BAR_HEIGHT, 0xFF151515);
            context.fill(left, y + 1, right, y + BAR_HEIGHT - 1, empty);

            int fillWidth = Math.round(BAR_WIDTH * Math.max(0.0f, Math.min(1.0f, bar.getPercent())));
            if (fillWidth > 0) {
                context.fill(left, y + 1, left + fillWidth, y + BAR_HEIGHT - 1, fill);
                context.fill(left, y + 1, left + fillWidth, y + 3, highlight);
            }

            context.fill(left - 12, y - 4, left - 2, y - 3, gold);
            context.fill(right + 2, y - 4, right + 12, y - 3, gold);
            y += 28;
        }

        ci.cancel();
    }
}
