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
    private static final int BAR_WIDTH = 430;
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_SPACING = 28;
    private static final int FIRST_BAR_Y = 24;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void saotargethp$render(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<UUID, ClientBossBar> bars = ((BossBarHudAccessor) this).saotargethp$getBossBars();
        if (bars.isEmpty() || client.player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int centerX = screenWidth / 2;
        int y = FIRST_BAR_Y;

        for (ClientBossBar bar : bars.values()) {
            String name = bar.getName().getString();
            int nameWidth = client.textRenderer.getWidth(name);
            int nameX = centerX - nameWidth / 2;
            int left = centerX - BAR_WIDTH / 2;
            int white = 0xFFF4F4F4;
            float percent = Math.max(0.0f, Math.min(1.0f, bar.getPercent()));

            context.drawText(client.textRenderer, Text.literal(name), nameX, y - 11, white, true);

            // Matches the reference: a long, slim gauge with a thin dark rim and a
            // shallow diagonal cut at the far-right tip.
            drawReferenceBar(context, left - 2, y - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4, 1.0f, 0xFF0E0E0E);
            drawReferenceBar(context, left - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, 1.0f, 0xFF2D312A);
            drawReferenceBar(context, left, y, BAR_WIDTH, BAR_HEIGHT, 1.0f, 0xFF162016);
            drawReferenceBar(context, left + 1, y + 1, BAR_WIDTH - 2, BAR_HEIGHT - 2, percent, 0xFF55A83A);

            // A narrow top sheen makes the green fill read like the supplied image.
            drawReferenceBar(context, left + 1, y + 1, BAR_WIDTH - 2, 2, percent, 0xFF79C95A);
            y += BAR_SPACING;
        }

        ci.cancel();
    }

    /** Fills a reference-style silhouette one scanline at a time without external textures. */
    private static void drawReferenceBar(DrawContext context, int left, int top, int width, int height,
                                         float percent, int color) {
        if (width <= 0 || height <= 0 || percent <= 0.0f) return;

        int right = left + width;
        int bevel = Math.max(2, Math.round(height * 1.35f));
        int fillWidth = Math.round(width * Math.min(1.0f, percent));
        int fillRight = left + fillWidth;
        // The fill keeps the same angled tip as the frame so the green edge never looks square.
        float fillBevel = Math.min(bevel, fillWidth * 0.85f);

        for (int row = 0; row < height; row++) {
            float progress = height == 1 ? 0.0f : row / (float) (height - 1);
            int silhouetteRight = right - Math.round(bevel * progress);
            int fillRowRight = fillRight - Math.round(fillBevel * progress);

            int rowRight = Math.min(silhouetteRight, fillRowRight);
            if (rowRight > left) {
                context.fill(left, top + row, rowRight, top + row + 1, color);
            }
        }
    }
}
