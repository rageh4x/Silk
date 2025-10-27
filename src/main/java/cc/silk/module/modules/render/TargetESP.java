package cc.silk.module.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.event.impl.render.EventRender3D;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

public final class TargetESP extends Module {

    private static final Identifier FIREFLY_TEXTURE = Identifier.of("silk", "imgs/firefly.png");

    private final ModeSetting targets = new ModeSetting("Targets", "Players", "Players", "Living");
    private final NumberSetting layers = new NumberSetting("Layers", 1, 5, 3, 1);
    private final NumberSetting orbsPerLayer = new NumberSetting("Orbs Per Layer", 5, 20, 14, 1);
    private final NumberSetting orbSize = new NumberSetting("Orb Size", 0.1, 0.8, 0.3, 0.05);
    private final NumberSetting speed = new NumberSetting("Speed", 0.5, 5.0, 2.5, 0.1);
    private final NumberSetting heightOffset = new NumberSetting("Height Offset", 0.0, 2.0, 1.0, 0.1);
    private final ColorSetting color = new ColorSetting("Color", new Color(120, 240, 255, 220));

    public TargetESP() {
        super("Target ESP", "Ghost-like spiral orbs around players", -1, Category.RENDER);
        addSettings(targets, layers, orbsPerLayer, orbSize, speed, heightOffset, color);
    }

    @EventHandler
    private void onRender3D(EventRender3D e) {
        if (isNull())
            return;

        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, FIREFLY_TEXTURE);
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

        float size = orbSize.getValueFloat();
        float yOff = heightOffset.getValueFloat();
        Color c = color.getValue();
        int numLayers = layers.getValueInt();
        int orbs = orbsPerLayer.getValueInt();

        float iAge = (System.currentTimeMillis() % 100000) / 50.0f;

        for (var entity : mc.world.getEntities()) {
            if (!shouldRender(entity))
                continue;

            double tPosX = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta - cam.x;
            double tPosY = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta - cam.y + yOff;
            double tPosZ = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta - cam.z;

            if (mc.player.canSee(entity)) {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.disableDepthTest();
            }

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR);

            float ageMultiplier = iAge * speed.getValueFloat();

            for (int j = 0; j < numLayers; j++) {
                float jOffset = j * 120;
                float jMultiplier = j + 1;

                for (int i = 0; i <= orbs; i++) {
                    float iFloat = (float) i;
                    double radians = Math
                            .toRadians(((iFloat / 1.5f + iAge * speed.getValueFloat()) * 8 + jOffset) % 2880);
                    double sinQuad = Math.sin(Math.toRadians(ageMultiplier + i * jMultiplier) * 3f) / 1.8f;
                    float offset = iFloat / orbs;

                    int orbColor = applyOpacity(c.getRGB(), offset);

                    double ghostX = Math.cos(radians) * entity.getWidth();
                    double ghostY = sinQuad;
                    double ghostZ = Math.sin(radians) * entity.getWidth();

                    MatrixStack matrices = new MatrixStack();
                    matrices.translate(tPosX + ghostX, tPosY + ghostY, tPosZ + ghostZ);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));

                    Matrix4f matrix = matrices.peek().getPositionMatrix();

                    buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(orbColor);
                    buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(orbColor);
                    buffer.vertex(matrix, size, -size, 0).texture(1f, 0).color(orbColor);
                    buffer.vertex(matrix, -size, -size, 0).texture(0, 0).color(orbColor);
                }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());

            if (mc.player.canSee(entity)) {
                RenderSystem.depthMask(true);
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private int applyOpacity(int colorInt, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(colorInt, true);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity))
                .getRGB();
    }

    private boolean shouldRender(net.minecraft.entity.Entity entity) {
        if (entity == mc.player)
            return false;
        if (targets.isMode("Players"))
            return entity instanceof PlayerEntity;
        return entity instanceof LivingEntity;
    }

}
