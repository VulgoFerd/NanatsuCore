package JinRyuu.JRMCore.p;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.minecraft.entity.player.EntityPlayer;

public class JRMCorePTpSyncHandler extends CAmh<JRMCorePTpSync> {
    public JRMCorePTpSyncHandler() {
    }

    @Override
    public IMessage handleClientMessage(EntityPlayer player, JRMCorePTpSync message, MessageContext ctx) {
        JinRyuu.JRMCore.TpNetwork.accept(message.tpLong);
        return null;
    }
}