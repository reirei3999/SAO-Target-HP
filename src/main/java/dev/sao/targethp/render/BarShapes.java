package dev.sao.targethp.render;

import net.minecraft.client.gui.DrawContext;

/** Shared 2D bar silhouettes used by the boss bar HUD and the layout editor. */
public final class BarShapes {
    private BarShapes() {
    }

    /**
     * Draws a long, slim bar whose right end is cut at a shallow diagonal.
     *
     * @param percent     fill amount, 0..1
     * @param taperFactor diagonal run as a multiple of the bar height
     */
    public static void drawTaperedBar(DrawContext context, int left, int top, int width, int height,
                                      float percent, float taperFactor, int color) {
        if (width <= 0 || height <= 0 || percent <= 0.0f) {
            return;
        }

        int right = left + width;
        int bevel = Math.max(2, Math.round(height * taperFactor));
        int fillWidth = Math.round(width * Math.min(1.0f, percent));
        int fillRight = left + fillWidth;
        // Keep the same shallow tip angle at every fill level; a nearly empty bar simply
        // shrinks to a sliver at the start instead of the tip turning into a vertical edge.
        int fillBevel = Math.min(bevel, fillWidth);

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
