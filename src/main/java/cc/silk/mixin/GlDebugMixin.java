package cc.silk.mixin;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gl.GlDebug")
public class GlDebugMixin {

    @Inject(method = "info", at = @At("HEAD"), cancellable = true)
    private static void onDebugInfo(int source, int type, int id, int severity, int length, long messagePtr,
            long userParamPtr, CallbackInfo ci) {
        String message = MemoryUtil.memUTF8(messagePtr, length);

        // Only suppress the useless "non-debug context" spam
        if (id == 1014 && message.contains("non-debug context")) {
            ci.cancel();
        }
    }
}
