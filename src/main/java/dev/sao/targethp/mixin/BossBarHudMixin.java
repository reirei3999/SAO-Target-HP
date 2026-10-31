package dev.sao.targethp.mixin;

import dev.sao.targethp.SaoTargetHpClient;
import dev.sao.targethp.config.SaoConfig;
import dev.sao.targethp.mixin.accessor.BossBarHudAccessor;
import dev.sao.targethp.render.BarShapes;
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
    private static final int BAR_WIDTH = 430;
    private static final int BAR_HEIGHT = 12;
    private static final float TAPER = 1.35f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void saotargethp$render(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // The custom renderer replaces vanilla boss bars entirely, so switching the boss
        // bar off (or hiding it with F1) must cancel the vanilla render rather than fall
        // through and draw the default purple bars.
        if (!SaoTargetHpClient.isBossBarAllowed(client)) {
            ci.cancel();
            return;
        }

        Map<UUID, ClientBossBar> bars = ((BossBarHudAccessor) this).saotargethp$getBossBars();
        if (bars.isEmpty() || client.player == null) return;

        SaoConfig.BossBar settings = SaoConfig.get().boss;
        int screenWidth = client.getWindow().getScaledWidth();
        int centerX = screenWidth / 2 + settings.offsetX;
        int y = settings.offsetY;

        for (ClientBossBar bar : bars.values()) {
            String name = bar.getName().getString();
            int nameWidth = client.textRenderer.getWidth(name);
            int nameX = centerX - nameWidth / 2;
            int left = centerX - BAR_WIDTH / 2;
            float percent = Math.max(0.0f, Math.min(1.0f, bar.getPercent()));

            context.drawText(client.textRenderer, Text.literal(name), nameX, y + settings.nameOffsetY,
                    settings.nameArgb, true);

            // Matches the reference: a long, slim gauge with a thin dark rim and a
            // shallow diagonal cut at the far-right tip.
            BarShapes.drawTaperedBar(context, left - 2, y - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4, 1.0f,
                    TAPER, settings.outlineArgb);
            BarShapes.drawTaperedBar(context, left - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, 1.0f,
                    TAPER, settings.rimArgb);
            BarShapes.drawTaperedBar(context, left, y, BAR_WIDTH, BAR_HEIGHT, 1.0f,
                    TAPER, settings.backgroundArgb);
            BarShapes.drawTaperedBar(context, left + 1, y + 1, BAR_WIDTH - 2, BAR_HEIGHT - 2, percent,
                    TAPER, settings.fillArgb);

            // A narrow top sheen makes the green fill read like the supplied image.
            BarShapes.drawTaperedBar(context, left + 1, y + 1, BAR_WIDTH - 2, 2, percent,
                    TAPER, settings.highlightArgb);
            y += settings.spacing;
        }

        ci.cancel();
    }
}
