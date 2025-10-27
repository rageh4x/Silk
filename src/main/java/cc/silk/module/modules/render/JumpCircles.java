package cc.silk.module.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.event.impl.render.EventRender3D;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

public final class JumpCircles extends Module {

    private static final Identifier CIRCLE_TEXTURE = Identifier.of("silk", "imgs/circle.png");

    private final ModeSetting targets = new ModeSetting("Targets", "Self", "Self", "Players");
    private final NumberSetting duration = new NumberSetting("Duration", 0.2, 3.0, 1.0, 0.05);
    private final NumberSetting maxRadius = new NumberSetting("Max Radius", 0.3, 5.0, 2.2, 0.05);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255, 200));
    private final Deque<Ring> rings = new ArrayDeque<>();
    private boolean wasOnGround = true;

    public JumpCircles() {
        super("Jump Circles", "Animated ring displayed when jumping", -1, Category.RENDER);
        addSettings(targets, duration, maxRadius, color);
    }

    private int applyOpacity(int colorInt, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(colorInt, true);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity))
                .getRGB();
    }

    @EventHandler
    private void onTick(TickEvent e) {
        if (isNull())
            return;
        long now = System.currentTimeMillis();
        long life = (long) (duration.getValue() * 1000.0);

        if (mc.player != null) {
            boolean onGround = mc.player.isOnGround();
            if (wasOnGround && !onGround) {
                if (targets.isMode("Self") || targets.isMode("Players")) {
                    rings.addLast(new Ring(mc.player.getPos(), now, true));
                }
            }
            wasOnGround = onGround;
        }

        if (targets.isMode("Players")) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player)
                    continue;
                if (p.isOnGround() || p.prevY == p.getY())
                    continue;
                rings.addLast(new Ring(p.getPos(), now, false));
            }
        }

        while (!rings.isEmpty() && now - rings.peekFirst().startTime > life)
            rings.removeFirst();
    }

    @EventHandler
    private void onRender3D(EventRender3D event) {
        if (isNull() || rings.isEmpty())
            return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, CIRCLE_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        long now = System.currentTimeMillis();
        long life = (long) (duration.getValue() * 1000.0);
        float maxR = maxRadius.getValueFloat();
        Color c = color.getValue();

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR);

        boolean hasVertices = false;

        for (Ring ring : rings) {
            float t = (float) Math.max(0.0, Math.min(1.0, (now - ring.startTime) / (double) life));
            float radius = maxR * t;
            float alpha = (1.0f - t);
            if (alpha <= 0.01f)
                continue;

            hasVertices = true;

            int ringColor = applyOpacity(c.getRGB(), alpha);

            double cx = ring.origin.x - cam.x;
            double cy = ring.origin.y - cam.y + 0.01;
            double cz = ring.origin.z - cam.z;

            MatrixStack matrices = new MatrixStack();
            matrices.translate(cx, cy, cz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            buffer.vertex(matrix, -radius, -radius, 0).texture(0f, 0f).color(ringColor);
            buffer.vertex(matrix, -radius, radius, 0).texture(0f, 1f).color(ringColor);
            buffer.vertex(matrix, radius, radius, 0).texture(1f, 1f).color(ringColor);
            buffer.vertex(matrix, radius, -radius, 0).texture(1f, 0f).color(ringColor);
        }

        if (hasVertices) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private record Ring(Vec3d origin, long startTime, boolean self) {
    }
}
