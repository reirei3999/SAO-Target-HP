package dev.sao.targethp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.hit.HitResult;

/**
 * Client-only SAO-inspired target HUD. All visuals are procedurally drawn.
 */
public final class SaoTargetHpClient implements ClientModInitializer {
    private static final double TARGET_RANGE = 128.0D;
    private static final float FADE_SPEED = 0.16f;
    private static final float SLIDE_SPEED = 0.20f;
    private static final float HEALTH_FOLLOW = 0.24f;
    private static final float DAMAGE_TRAIL_FOLLOW = 0.045f;
    private static final double HEALTH_BAR_VERTICAL_OFFSET = 1.5D;

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
        // Keep the marker at a stable distance-based size; it must not pulse or breathe.
        float size = (float) Math.max(0.22D, Math.min(0.70D, 0.28D + distance * 0.0032D));
        float pinAlpha = alpha * 0.92f;

        VertexConsumer consumer = consumers.getBuffer(RenderLayer.getDebugQuads());

        // Offset the gauge 1.5 blocks downward in world space, then rotate it to face the player.
        matrices.push();
        matrices.translate(x, y - HEALTH_BAR_VERTICAL_OFFSET, z);
        matrices.multiply(camera.getRotation());
        drawTargetHealthBar(consumer, matrices, size);
        matrices.pop();

        // The pin remains above the target and is also camera-facing.
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        // Dark outer diamond.
        drawDiamond(consumer, matrices, size * 1.16f, 0.010f, 0xB0000000);
        // SAO-like red inner diamond.
        drawDiamond(consumer, matrices, size, 0.012f, withAlpha(0xFFE51B45, pinAlpha));
        // Small bright center gives the marker a glassy/highlighted feel.
        drawDiamond(consumer, matrices, size * 0.28f, 0.014f, withAlpha(0xFFFF8AA2, pinAlpha * 0.75f));

        matrices.pop();
    }

    /** Draws a slim, beveled HP gauge based on the supplied reference image. */
    private static void drawTargetHealthBar(VertexConsumer consumer, MatrixStack matrices, float pinSize) {
        float barWidth = pinSize * 8.4f;
        float barHeight = pinSize * 0.92f;
        float gap = pinSize * 0.46f;
        float left = -pinSize - gap - barWidth;
        float right = -pinSize - gap;
        float top = barHeight * 0.50f;
        float bottom = -top;
        float bevel = barHeight * 0.48f;
        float inset = Math.max(0.025f, pinSize * 0.12f);

        // The three nested silhouettes create the dark outline, gray rim, and dark empty interior.
        drawBeveledBar(consumer, matrices, left, right, top, bottom, bevel, 0.010f,
                withAlpha(0xE8000000, alpha));
        drawBeveledBar(consumer, matrices, left + inset, right - inset, top - inset, bottom + inset,
                Math.max(0.0f, bevel - inset), 0.012f, withAlpha(0xFF2F332D, alpha));

        float innerInset = inset * 1.65f;
        float innerLeft = left + innerInset;
        float innerRight = right - innerInset;
        float innerTop = top - innerInset;
        float innerBottom = bottom + innerInset;
        float innerBevel = Math.max(0.0f, bevel - innerInset);
        drawBeveledBar(consumer, matrices, innerLeft, innerRight, innerTop, innerBottom, innerBevel, 0.014f,
                withAlpha(0xFF152018, alpha));

        // Retain the delayed damage trail, while the live fill keeps the angled right edge from the reference.
        float innerWidth = innerRight - innerLeft;
        float trailRight = innerLeft + innerWidth * Math.max(0.0f, Math.min(1.0f, damageTrail));
        if (trailRight > innerLeft) {
            drawBeveledBar(consumer, matrices, innerLeft, trailRight, innerTop, innerBottom,
                    Math.min(innerBevel, Math.max(0.0f, (trailRight - innerLeft) * 0.45f)), 0.016f,
                    withAlpha(0xFF8F6A2C, alpha * 0.72f));
        }

        float hp = Math.max(0.0f, Math.min(1.0f, displayedHealth));
        // Use the target's current HP for the warning colour, while retaining the smooth fill animation.
        float currentHp = target == null ? hp : healthRatio(target);
        int fillColor = 0xFF58A83B;
        int highlightColor = 0xFF83D25B;
        if (currentHp <= 0.20f) {
            fillColor = 0xFFD9413A;
            highlightColor = 0xFFFF7666;
        } else if (currentHp <= 0.40f) {
            fillColor = 0xFFE0AF32;
            highlightColor = 0xFFFFDD69;
        }
        float fillRight = innerLeft + innerWidth * hp;
        if (fillRight > innerLeft) {
            drawBeveledBar(consumer, matrices, innerLeft, fillRight, innerTop, innerBottom,
                    Math.min(innerBevel, Math.max(0.0f, (fillRight - innerLeft) * 0.45f)), 0.018f,
                    withAlpha(fillColor, alpha));

            float highlightBottom = innerTop - Math.max(0.015f, pinSize * 0.08f);
            drawBeveledBar(consumer, matrices, innerLeft, fillRight, innerTop, highlightBottom,
                    Math.min(innerBevel, Math.max(0.0f, (fillRight - innerLeft) * 0.45f)), 0.020f,
                    withAlpha(highlightColor, alpha * 0.72f));
        }

        if (hitFlash > 0.02f) {
            drawBeveledBar(consumer, matrices, innerLeft, innerRight, innerTop,
                    innerTop - Math.max(0.012f, pinSize * 0.055f), innerBevel, 0.022f,
                    withAlpha(0xFFFFFFFF, alpha * hitFlash * 0.30f));
        }
    }

    private static void drawBeveledBar(VertexConsumer consumer, MatrixStack matrices,
                                       float left, float right, float top, float bottom, float bevel,
                                       float depth, int argb) {
        if (right <= left || top <= bottom) return;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        MatrixStack.Entry entry = matrices.peek();

        consumer.vertex(entry.getPositionMatrix(), left, top, depth).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), right, top, depth).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), right - bevel, bottom, depth).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), left, bottom, depth).color(r, g, b, a);
    }

    private static void drawDiamond(VertexConsumer consumer, MatrixStack matrices, float size, float depth, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        MatrixStack.Entry entry = matrices.peek();

        // Four vertices form a camera-facing diamond.
        consumer.vertex(entry.getPositionMatrix(), 0.0f, size, depth).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), size * 0.62f, 0.0f, depth).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), 0.0f, -size, depth).color(r, g, b, a);
        consumer.vertex(entry.getPositionMatrix(), -size * 0.62f, 0.0f, depth).color(r, g, b, a);
    }

    private static int withAlpha(int argb, float opacity) {
        int a = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * opacity)));
        return (argb & 0x00FFFFFF) | (a << 24);
    }
}
