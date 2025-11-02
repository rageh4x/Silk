package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.network.EventDisconnect;
import cc.silk.utils.IMinecraft;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.event.impl.world.WorldChangeEvent;
import cc.silk.gui.ClickGui;
import cc.silk.module.modules.client.ClickGUIModule;
import cc.silk.module.modules.client.Client;
import cc.silk.profiles.ProfileManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin implements IMinecraft {

    @Shadow
    public ClientWorld world;
    @Shadow
    public HitResult crosshairTarget;
    @Shadow
    public ClientPlayerEntity player;
    @Shadow
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    @Final
    private RenderTickCounter.Dynamic renderTickCounter;

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void setTitle(CallbackInfoReturnable<String> cir) {
        if (SilkClient.INSTANCE == null || SilkClient.mc == null) return;

        var optionalClientModule = SilkClient.INSTANCE.getModuleManager().getModule(Client.class);
        if (optionalClientModule.isPresent()) {
            Client client = optionalClientModule.get();
            if (client.isEnabled() && client.getTitle()) {
                cir.setReturnValue("Silk 1.21.1");
            }
        }
    }

    @Inject(method = "run", at = @At("HEAD"))
    public void runInject(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null) {
            ProfileManager profileManager = SilkClient.INSTANCE.getProfileManager();
            profileManager.loadProfile("default");
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (SilkClient.INSTANCE == null || SilkClient.mc == null) return;

        if (world != null) {
            SilkClient.INSTANCE.getSilkEventBus().post(new TickEvent());
        }

        var optionalClickGuiModule = SilkClient.INSTANCE.getModuleManager().getModule(ClickGUIModule.class);
        if (optionalClickGuiModule.isPresent()) {
            ClickGUIModule clickGuiModule = optionalClickGuiModule.get();
            if (clickGuiModule.isEnabled() && SilkClient.mc.currentScreen == null && world != null) {
                SilkClient.mc.setScreen(new ClickGui());
            }
            else if (!clickGuiModule.isEnabled() && SilkClient.mc.currentScreen instanceof ClickGui) {
                SilkClient.mc.setScreen(null);
            }
        }
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stopInject(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null) {
            ProfileManager profileManager = SilkClient.INSTANCE.getProfileManager();
            profileManager.saveProfile("default", true);
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    public void onWorldChangeInject(ClientWorld newWorld, CallbackInfo ci) {
        if (SilkClient.INSTANCE != null && SilkClient.mc != null) {
            SilkClient.INSTANCE.getSilkEventBus().post(new WorldChangeEvent(newWorld));
        }
    }
    @Inject(method = "onDisconnected", at = @At("HEAD"))
    public final void onDisconnected(CallbackInfo ci) {
        EventDisconnect event = new EventDisconnect();
        SilkClient.INSTANCE.getSilkEventBus().post(event);
    }
}
