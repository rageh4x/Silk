package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.network.EventDisconnect;
import cc.silk.event.impl.network.EventPacket;
import cc.silk.event.types.TransferOrder;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void receivePacketEventInject(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        postPacketEvent(packet, TransferOrder.RECEIVE, ci);
    }

    private static void postPacketEvent(Packet<?> packet, TransferOrder order, CallbackInfo ci) {
        if (SilkClient.INSTANCE == null) return;
        EventPacket eventPacket = new EventPacket(packet, order);
        SilkClient.INSTANCE.getSilkEventBus().post(eventPacket);
        if (eventPacket.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void sendPacketEventInject(Packet<?> packet, CallbackInfo ci) {
        postPacketEvent(packet, TransferOrder.SEND, ci);
    }

    @Inject(method = "handleDisconnection", at = @At("HEAD"))
    private void handleDisconnectionInject(CallbackInfo ci) {
        if (SilkClient.INSTANCE != null) {
            SilkClient.INSTANCE.getSilkEventBus().post(new EventDisconnect());
        }
    }
}
