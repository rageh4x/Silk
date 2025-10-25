package cc.silk.module.modules.client;

import cc.silk.event.impl.network.EventPacket;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.utils.mc.ChatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;

public class Debugger extends Module {
    public Debugger() {
        super("Debugger", "Debugs inv packets (dev purposes)", -1, Category.CLIENT);
    }

    @EventHandler
    public void onPacketSend(EventPacket e) {
        if (isNull()) return;
        if (e.getPacket() == null) return;
        if (!(e.getPacket() instanceof ClickSlotC2SPacket packet))
            return;

        ChatUtil.addChatMessage("""
                ClickSlotPacket
                  syncId: %s
                  revision: %s
                  slot: %s
                  button: %s
                  actionType: %s
                  modifiedItems: %s
                  stack: %s
                """.formatted(
                packet.getSyncId(),
                packet.getRevision(),
                packet.getSlot(),
                packet.getButton(),
                packet.getActionType(),
                packet.getModifiedStacks(),
                packet.getStack()
        ));
    }
}
