package dev.sao.targethp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;

/**
 * Client-only SAO-inspired target HUD. All visuals are procedurally drawn.
 */
public final class SaoTargetHpClient implements ClientModInitializer {
    public static final int BAR_WIDTH = 250;
    private static final int BAR_HEIGHT = 8;
    // The reference layout places the HP bar above the target name.
    private static final int BAR_Y = 30;
    private static final int NAME_Y = 43;
    private static final double TARGET_RANGE = 128.0D;
    private static final float FADE_SPEED = 0.16f;
    private static final float SLIDE_SPEED = 0.20f;
    private static final float HEALTH_FOLLOW = 0.24f;
    private static final float DAMAGE_TRAIL_FOLLOW = 0.045f;

    private static LivingEntity target;
    private static float alpha;
    private static float slide;
    private static float displayedHealth;
    private static float damageTrail;
    private static float hitFlash;
    private static float acquirePulse;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(SaoTargetHpClient::tick);
        HudRenderCallback.EVENT.register(SaoTargetHpClient::renderHud);
        WorldRenderEvents.AFTER_ENTITIES.register(SaoTargetHpClient::renderTargetPin);
    }

    private static void tick(MinecraftClient client) {
        LivingEntity next = findTarget(client);

        if (next != null) {
            if (target != next) {
                target = next;
                float ratio = healthRatio(next);
                displayedHealth = ratio;
                damageTrail = ratio;
                hitFlash = 1.0f;
                acquirePulse = 1.0f;
                slide = 0.0f;
            }

            float current = healthRatio(next);
            if (current < displayedHealth - 0.002f) {
                hitFlash = 1.0f;
                damageTrail = Math.max(damageTrail, displayedHealth);
            }

            displayedHealth += (current - displayedHealth) * HEALTH_FOLLOW;
            damageTrail += (current - damageTrail) * DAMAGE_TRAIL_FOLLOW;
            alpha = Math.min(1.0f, alpha + FADE_SPEED);
            slide += (1.0f - slide) * SLIDE_SPEED;
            hitFlash *= 0.84f;
            acquirePulse *= 0.88f;
        } else {
            alpha = Math.max(0.0f, alpha - FADE_SPEED);
            slide += (0.0f - slide) * SLIDE_SPEED;
            hitFlash *= 0.84f;
            acquirePulse *= 0.88f;
            if (alpha <= 0.001f) {
                target = null;
                slide = 0.0f;
            }
        }
    }

    private static LivingEntity findTarget(MinecraftClient client) {
        if (client.player == null || client.world == null) return null;

        Vec3d start = client.player.getEyePos();
        Vec3d direction = client.player.getRotationVec(1.0F).normalize();
        Vec3d end = start.add(direction.multiply(TARGET_RANGE));

        // Do our own long-range crosshair ray instead of relying on Minecraft's
        // normal interaction reach. This lets the HUD lock onto targets up to 128 blocks away.
        Box searchBox = new Box(
                Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z),
                Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z)
        ).expand(1.0D);

        // A solid block stops the target ray, so mobs behind walls are not selected.
        var blockHit = client.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));
        double maxDistance = blockHit.getType() == HitResult.Type.MISS
                ? TARGET_RANGE * TARGET_RANGE
                : start.squaredDistanceTo(blockHit.getPos());

        LivingEntity best = null;
        double bestDistance = maxDistance;

        for (var entity : client.world.getOtherEntities(client.player, searchBox, e -> e instanceof LivingEntity)) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living instanceof PlayerEntity || living.isRemoved() || living.getHealth() <= 0.0F) continue;
            if (living.getType().getSpawnGroup() != SpawnGroup.MONSTER) continue;
            if (isVanillaBoss(living)) continue;

            var hit = living.getBoundingBox().expand(0.15D).raycast(start, end);
            if (hit.isEmpty()) continue;

            double distance = start.squaredDistanceTo(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }

        return best;
    }

    public static boolean isVanillaBoss(LivingEntity entity) {
        return entity instanceof EnderDragonEntity || entity instanceof WitherEntity;
    }

    private static float healthRatio(LivingEntity entity) {
        float max = entity.getMaxHealth();
        if (max <= 0.0f) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, entity.getHealth() / max));
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (target == null || alpha <= 0.0f || client.player == null) return;

        int width = client.getWindow().getScaledWidth();
        int center = width / 2;
        float eased = easeOutCubic(slide);
        int offset = Math.round((1.0f - eased) * 34.0f);
        int centerX = center + offset;
        int left = centerX - BAR_WIDTH / 2;
        int right = left + BAR_WIDTH;

        String name = target.getDisplayName().getString();
        int nameWidth = client.textRenderer.getWidth(name);
        int nameX = centerX - nameWidth / 2;
        int nameY = NAME_Y;

        int white = withAlpha(0xFFF4F4F4, alpha);
        int gold = withAlpha(0xFFE8D59A, alpha);
        int goldBright = withAlpha(0xFFFFEAB0, alpha);
        int dark = withAlpha(0xE6000000, alpha * 0.96f);
        int frame = withAlpha(0xFFAAA79F, alpha * 0.92f);
        int inner = withAlpha(0xFF151515, alpha);
        int empty = withAlpha(0xFF353535, alpha);
        float hp = Math.max(0.0f, Math.min(1.0f, displayedHealth));
        int fill;
        int fillHighlight;
        if (hp <= 0.20f) {
            fill = withAlpha(0xFFE04444, alpha);
            fillHighlight = withAlpha(0xFFFF9A9A, alpha * 0.90f);
        } else if (hp <= 0.40f) {
            fill = withAlpha(0xFFE8B83F, alpha);
            fillHighlight = withAlpha(0xFFFFE49A, alpha * 0.90f);
        } else {
            fill = withAlpha(0xFF74C51C, alpha);
            fillHighlight = withAlpha(0xFFB7F06A, alpha * 0.90f);
        }
        int trail = withAlpha(0xFFDB8A3A, alpha * 0.82f);

        // Nameplate.
        ctx.drawText(client.textRenderer, Text.literal(name), nameX, nameY, white, true);

        // SAO-like target brackets and central lock accent.
        drawCorner(ctx, left - 13, BAR_Y - 5, false, gold);
        drawCorner(ctx, right + 13, BAR_Y - 5, true, gold);
        ctx.fill(centerX - 1, BAR_Y + BAR_HEIGHT + 6, centerX + 1, BAR_Y + BAR_HEIGHT + 12, goldBright);
        ctx.fill(centerX - 5, BAR_Y + BAR_HEIGHT + 10, centerX + 5, BAR_Y + BAR_HEIGHT + 11, gold);

        // Subtle acquisition pulse.
        if (acquirePulse > 0.02f) {
            int pulse = withAlpha(0xFFF6E3A6, alpha * acquirePulse * 0.22f);
            int pulsePad = Math.round(3.0f + acquirePulse * 7.0f);
            ctx.fill(left - pulsePad, BAR_Y - pulsePad, right + pulsePad, BAR_Y - pulsePad + 1, pulse);
        }

        // Layered frame.
        ctx.fill(left - 5, BAR_Y - 5, right + 5, BAR_Y + BAR_HEIGHT + 5, dark);
        ctx.fill(left - 3, BAR_Y - 3, right + 3, BAR_Y + BAR_HEIGHT + 3, frame);
        ctx.fill(left - 1, BAR_Y - 1, right + 1, BAR_Y + BAR_HEIGHT + 1, inner);
        ctx.fill(left, BAR_Y, right, BAR_Y + BAR_HEIGHT, empty);

        // Damage trail stays behind the live HP bar.
        int trailWidth = Math.round(BAR_WIDTH * Math.max(0.0f, Math.min(1.0f, damageTrail)));
        if (trailWidth > 0) {
            drawBarFill(ctx, left, BAR_Y, trailWidth, trail);
        }

        int fillWidth = Math.round(BAR_WIDTH * hp);
        if (fillWidth > 0) {
            drawBarFill(ctx, left, BAR_Y, fillWidth, fill);
            ctx.fill(left, BAR_Y, Math.max(left, left + fillWidth - 4), BAR_Y + 2, fillHighlight);
        }

        // A brief white hit line gives damage a distinct SAO-like feedback cue.
        if (hitFlash > 0.02f) {
            int flash = withAlpha(0xFFFFFFFF, alpha * hitFlash * 0.34f);
            ctx.fill(left, BAR_Y, right, BAR_Y + 1, flash);
        }
    }

    /**
     * Renders the SAO-style red target pin above the currently locked entity.
     * It is a camera-facing diamond so it remains readable while the player
     * turns around the target.
     */
    private static void renderTargetPin(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        if (target == null || alpha <= 0.001f || target.isRemoved()) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Camera camera = context.camera();
        if (matrices == null || consumers == null || camera == null) return;

        Vec3d cameraPos = camera.getPos();
        double x = target.getX() - cameraPos.x;
        double y = target.getBodyY(0.5D) + target.getHeight() + 0.55D - cameraPos.y;
        double z = target.getZ() - cameraPos.z;
        double distance = Math.sqrt(x * x + y * y + z * z);
        float pulse = 1.0f + 0.08f * (float) Math.sin((System.nanoTime() / 1_000_000_000.0) * 4.0);
        float size = (float) Math.max(0.22D, Math.min(0.70D, 0.28D + distance * 0.0032D)) * pulse;
        float pinAlpha = alpha * 0.92f;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());

        VertexConsumer consumer = consumers.getBuffer(RenderLayer.getDebugQuads());

        // Dark outer diamond.
        drawDiamond(consumer, matrices, size * 1.16f, 0xB0000000);
        // SAO-like red inner diamond.
        drawDiamond(consumer, matrices, size, withAlpha(0xFFE51B45, pinAlpha));
        // Small bright center gives the marker a glassy/highlighted feel.
        drawDiamond(consumer, matrices, size * 0.28f, withAlpha(0xFFFF8AA2, pinAlpha * 0.75f));

        matrices.pop();
    }

    private static void drawDiamond(VertexConsumer consumer, MatrixStack matrices, float size, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        MatrixStack.Entry entry = matrices.peek();

        // Four vertices form a camera-facing diamond.
        consumer.vertex(entry.getPositionMatrix(), 0.0f, size, 0.0f).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), size * 0.62f, 0.0f, 0.0f).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), 0.0f, -size, 0.0f).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), -size * 0.62f, 0.0f, 0.0f).color(r, g, b, a);
    }

    private static void drawBarFill(DrawContext ctx, int x, int y, int width, int color) {
        if (width <= 0) return;
        // The reference UI has a clipped/trapezoid-like right edge. Approximate it
        // with progressively shorter rows so the fill remains texture-free.
        int h = BAR_HEIGHT;
        int edge = Math.min(4, width / 2);
        ctx.fill(x, y, x + Math.max(1, width - edge), y + h, color);
        if (edge > 0) {
            for (int row = 0; row < h; row++) {
                int inset = Math.max(0, edge - 1 - Math.min(edge - 1, row / 2));
                int rowRight = x + width - inset;
                if (rowRight > x + width - edge) {
                    ctx.fill(x + Math.max(0, width - edge), y + row, rowRight, y + row + 1, color);
                }
            }
        }
    }

    private static void drawCorner(DrawContext ctx, int x, int y, boolean rightSide, int color) {
        int dx = rightSide ? -1 : 1;
        ctx.fill(x, y, x + dx * 2, y + 2, color);
        ctx.fill(x, y, x + dx * 2, y + 10, color);
        ctx.fill(x, y, x + dx * 10, y + 2, color);
    }

    private static float easeOutCubic(float t) {
        float x = 1.0f - Math.max(0.0f, Math.min(1.0f, t));
        return 1.0f - x * x * x;
    }

    private static int withAlpha(int argb, float opacity) {
        int a = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * opacity)));
        return (argb & 0x00FFFFFF) | (a << 24);
    }
}
