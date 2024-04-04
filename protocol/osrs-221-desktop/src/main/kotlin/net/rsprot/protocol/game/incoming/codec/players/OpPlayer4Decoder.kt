package net.rsprot.protocol.game.incoming.codec.players

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.game.incoming.players.OpPlayerMessage
import net.rsprot.protocol.game.incoming.prot.GameClientProt
import net.rsprot.protocol.message.codec.MessageDecoder

public class OpPlayer4Decoder : MessageDecoder<OpPlayerMessage> {
    override val prot: ClientProt = GameClientProt.OPPLAYER4

    override fun decode(buffer: JagByteBuf): OpPlayerMessage {
        val index = buffer.g2Alt2()
        val controlKey = buffer.g1Alt2() == 1
        return OpPlayerMessage(
            index,
            controlKey,
            4,
        )
    }
}
