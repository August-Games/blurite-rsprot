package net.rsprot.protocol.game.incoming.codec.players

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.game.incoming.players.OpPlayer
import net.rsprot.protocol.game.incoming.prot.GameClientProt
import net.rsprot.protocol.message.codec.MessageDecoder

public class OpPlayer8Decoder : MessageDecoder<OpPlayer> {
    override val prot: ClientProt = GameClientProt.OPPLAYER8

    override fun decode(buffer: JagByteBuf): OpPlayer {
        val index = buffer.g2Alt1()
        val controlKey = buffer.g1Alt2() == 1
        return OpPlayer(
            index,
            controlKey,
            8,
        )
    }
}
