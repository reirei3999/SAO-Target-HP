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
    private static final int BAR_WIDTH = 380;
    private static final int BAR_HEIGHT = 18;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void saotargethp$render(DrawContext context, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Map<UUID, ClientBossBar> bars = ((BossBarHudAccessor) this).saotargethp$getBossBars();
        if (bars.isEmpty() || client.player == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int centerX = screenWidth / 2;
        int y = 25;

        for (ClientBossBar bar : bars.values()) {
            String name = bar.getName().getString();
            int nameWidth = client.textRenderer.getWidth(name);
            int nameX = centerX - nameWidth / 2;
            int left = centerX - BAR_WIDTH / 2;
            int white = 0xFFF4F4F4;

            context.drawText(client.textRenderer, Text.literal(name), nameX, y - 12, white, true);

            // The silhouette matches the reference: a long upper gauge with a stepped,
            // diagonal lower edge halfway across and a beveled right tip.
            drawReferenceBar(context, left - 3, y - 3, BAR_WIDTH + 6, BAR_HEIGHT + 6, 1.0f, 0xFF141414);
            drawReferenceBar(context, left - 1, y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, 1.0f, 0xFF2D312A);
            drawReferenceBar(context, left + 2, y + 2, BAR_WIDTH - 4, BAR_HEIGHT - 4, 1.0f, 0xFF162016);
            drawReferenceBar(context, left + 2, y + 2, BAR_WIDTH - 4, BAR_HEIGHT - 4,
                    Math.max(0.0f, Math.min(1.0f, bar.getPercent())), 0xFF55A83A);

            // A narrow top sheen makes the green fill read like the supplied image.
            drawReferenceBar(context, left + 3, y + 3, BAR_WIDTH - 6, 3,
                    Math.max(0.0f, Math.min(1.0f, bar.getPercent())), 0xFF79C95A);
            y += 40;
        }

        ci.cancel();
    }

    /** Fills a reference-style silhouette one scanline at a time without external textures. */
    private static void drawReferenceBar(DrawContext context, int left, int top, int width, int height,
                                         float percent, int color) {
        if (width <= 0 || height <= 0 || percent <= 0.0f) return;

        int right = left + width;
        int hingeX = left + width / 2;
        int upperHeight = Math.max(2, Math.round(height * 0.62f));
        int bevel = Math.max(2, Math.round(height * 0.45f));
        int fillRight = left + Math.round(width * Math.min(1.0f, percent));

        for (int row = 0; row < height; row++) {
            int silhouetteRight;
            if (row < upperHeight) {
                // The far-right end slopes inward as it reaches the gauge's lower edge.
                silhouetteRight = right - Math.round(bevel * (row / (float) upperHeight));
            } else {
                // The lower edge steps inward at the center and slopes down to the left half.
                float progress = (row - upperHeight) / (float) Math.max(1, height - upperHeight);
                silhouetteRight = hingeX + bevel - Math.round(bevel * progress);
            }

            int rowRight = Math.min(silhouetteRight, fillRight);
            if (rowRight > left) {
                context.fill(left, top + row, rowRight, top + row + 1, color);
            }
        }
    }
}
