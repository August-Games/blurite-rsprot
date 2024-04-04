package net.rsprot.protocol.game.incoming.codec.locs

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.game.incoming.locs.OpLocTMessage
import net.rsprot.protocol.game.incoming.prot.GameClientProt
import net.rsprot.protocol.message.codec.MessageDecoder
import net.rsprot.protocol.tools.MessageDecodingTools
import net.rsprot.protocol.util.gCombinedId

public class OpLocTDecoder : MessageDecoder<OpLocTMessage> {
    override val prot: ClientProt = GameClientProt.OPLOCT

    override fun decode(
        buffer: JagByteBuf,
        tools: MessageDecodingTools,
    ): OpLocTMessage {
        val selectedObj = buffer.g2Alt1()
        val x = buffer.g2Alt3()
        val z = buffer.g2Alt3()
        val selectedCombinedId = buffer.gCombinedId()
        val id = buffer.g2Alt2()
        val controlKey = buffer.g1Alt1() == 1
        val selectedSub = buffer.g2Alt3()
        return OpLocTMessage(
            id,
            x,
            z,
            controlKey,
            selectedCombinedId,
            selectedSub,
            selectedObj,
        )
    }
}
