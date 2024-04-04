package net.rsprot.protocol.game.incoming.codec.players

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.game.incoming.players.OpPlayerMessage
import net.rsprot.protocol.game.incoming.prot.GameClientProt
import net.rsprot.protocol.message.codec.MessageDecoder
import net.rsprot.protocol.tools.MessageDecodingTools

public class OpPlayer2Decoder : MessageDecoder<OpPlayerMessage> {
    override val prot: ClientProt = GameClientProt.OPPLAYER2

    override fun decode(
        buffer: JagByteBuf,
        tools: MessageDecodingTools,
    ): OpPlayerMessage {
        val controlKey = buffer.g1Alt3() == 1
        val index = buffer.g2Alt2()
        return OpPlayerMessage(
            index,
            controlKey,
            2,
        )
    }
}
