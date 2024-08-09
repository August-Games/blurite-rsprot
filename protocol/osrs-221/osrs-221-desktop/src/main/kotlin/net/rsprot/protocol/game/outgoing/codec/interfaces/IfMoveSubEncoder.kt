package net.rsprot.protocol.game.outgoing.codec.interfaces

import net.rsprot.buffer.JagByteBuf
import net.rsprot.crypto.cipher.StreamCipher
import net.rsprot.protocol.ServerProt
import net.rsprot.protocol.game.outgoing.interfaces.IfMoveSub
import net.rsprot.protocol.game.outgoing.prot.GameServerProt
import net.rsprot.protocol.message.codec.MessageEncoder

public class IfMoveSubEncoder : MessageEncoder<IfMoveSub> {
    override val prot: ServerProt = GameServerProt.IF_MOVESUB

    override fun encode(
        streamCipher: StreamCipher,
        buffer: JagByteBuf,
        message: IfMoveSub,
    ) {
        buffer.p4Alt2(message.sourceCombinedId.combinedId)
        buffer.p4Alt2(message.destinationCombinedId.combinedId)
    }
}
