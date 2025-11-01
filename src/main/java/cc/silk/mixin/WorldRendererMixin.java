package cc.silk.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import cc.silk.SilkClient;
import cc.silk.event.impl.render.EventRender3D;
import cc.silk.module.modules.render.ShaderTest;
import cc.silk.utils.render.font.util.RendererUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "renderChunkDebugInfo", at = @At("HEAD"))
    public void renderChunkDebugInfoInject(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera, CallbackInfo ci) {
        if (SilkClient.INSTANCE == null) return;

        RendererUtils.lastProjMat.set(RenderSystem.getProjectionMatrix());
        RendererUtils.lastModMat.set(RenderSystem.getModelViewMatrix());
        RendererUtils.lastWorldSpaceMatrix.identity();

        SilkClient.INSTANCE.getSilkEventBus().post(new EventRender3D(matrices));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.BEFORE))
    private void onDrawOutlines(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null && SilkClient.INSTANCE.getModuleManager() != null) {
            ShaderTest shaderTest = SilkClient.INSTANCE.getModuleManager().getModule(ShaderTest.class).orElse(null);
            if (shaderTest != null && shaderTest.isEnabled()) {
                float width = Math.max(1.0f, Math.min(10.0f, shaderTest.getOutlineWidth().getValueFloat()));
                RenderSystem.lineWidth(width);
            }
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.AFTER))
    private void afterDrawOutlines(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null && SilkClient.INSTANCE.getModuleManager() != null) {
            ShaderTest shaderTest = SilkClient.INSTANCE.getModuleManager().getModule(ShaderTest.class).orElse(null);
            if (shaderTest != null && shaderTest.isEnabled()) {
                RenderSystem.lineWidth(1.0f);
            }
        }
    }
}
