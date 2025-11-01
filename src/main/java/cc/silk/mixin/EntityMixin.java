package cc.silk.mixin;

import cc.silk.module.modules.render.OutlineESP;
import cc.silk.module.modules.render.ShaderTest;
import cc.silk.SilkClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        
        OutlineESP outlineESP = OutlineESP.getInstance();
        if (outlineESP != null && outlineESP.isEnabled()) {
            if (outlineESP.shouldEntityGlow(entity)) {
                cir.setReturnValue(true);
                return;
            }
        }
        
        if (SilkClient.INSTANCE != null && SilkClient.INSTANCE.getModuleManager() != null) {
            ShaderTest shaderTest = SilkClient.INSTANCE.getModuleManager().getModule(ShaderTest.class).orElse(null);
            if (shaderTest != null && shaderTest.isEnabled() && shaderTest.shouldEntityGlow(entity)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;
        
        if (SilkClient.INSTANCE != null && SilkClient.INSTANCE.getModuleManager() != null) {
            ShaderTest shaderTest = SilkClient.INSTANCE.getModuleManager().getModule(ShaderTest.class).orElse(null);
            if (shaderTest != null && shaderTest.isEnabled() && shaderTest.shouldEntityGlow(entity)) {
                java.awt.Color color = shaderTest.getEntityColor(entity);
                if (color != null) {
                    float glowMult = (float) shaderTest.getGlowMultiplier().getValue();
                    glowMult = Math.max(0.0f, Math.min(10.0f, glowMult));

                    float brightnessMultiplier = 1.0f + (glowMult - 1.0f) * 0.3f;
                    int r = Math.min(255, (int) (color.getRed() * brightnessMultiplier));
                    int g = Math.min(255, (int) (color.getGreen() * brightnessMultiplier));
                    int b = Math.min(255, (int) (color.getBlue() * brightnessMultiplier));
                    
                    java.awt.Color glowColor = new java.awt.Color(r, g, b, color.getAlpha());
                    cir.setReturnValue(glowColor.getRGB());
                }
            }
        }
    }
}
