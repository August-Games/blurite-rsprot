package net.rsprot.protocol.game.outgoing.codec.interfaces

import net.rsprot.buffer.JagByteBuf
import net.rsprot.crypto.cipher.StreamCipher
import net.rsprot.protocol.ServerProt
import net.rsprot.protocol.game.outgoing.interfaces.IfSetHide
import net.rsprot.protocol.game.outgoing.prot.GameServerProt
import net.rsprot.protocol.message.codec.MessageEncoder

public class IfSetHideEncoder : MessageEncoder<IfSetHide> {
    override val prot: ServerProt = GameServerProt.IF_SETHIDE

    override fun encode(
        streamCipher: StreamCipher,
        buffer: JagByteBuf,
        message: IfSetHide,
    ) {
        buffer.p4Alt1(message.combinedId.combinedId)
        buffer.p1(if (message.hidden) 1 else 0)
    }
}
