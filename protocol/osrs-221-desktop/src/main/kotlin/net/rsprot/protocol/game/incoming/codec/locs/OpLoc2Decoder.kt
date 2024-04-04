package net.rsprot.protocol.game.incoming.codec.locs

import net.rsprot.buffer.JagByteBuf
import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.game.incoming.locs.OpLocEvent
import net.rsprot.protocol.game.incoming.prot.GameClientProt
import net.rsprot.protocol.message.codec.MessageDecoder

public class OpLoc2Decoder : MessageDecoder<OpLocEvent> {
    override val prot: ClientProt = GameClientProt.OPLOC2

    override fun decode(buffer: JagByteBuf): OpLocEvent {
        val controlKey = buffer.g1Alt3() == 1
        val z = buffer.g2()
        val id = buffer.g2Alt2()
        val x = buffer.g2Alt2()
        return OpLocEvent(
            id,
            x,
            z,
            controlKey,
            2,
        )
    }
}
