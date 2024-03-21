package net.rsprot.protocol.message.codec.incoming

import net.rsprot.protocol.ClientProt
import net.rsprot.protocol.ProtRepository
import net.rsprot.protocol.message.IncomingMessage
import net.rsprot.protocol.message.codec.MessageDecoder
import java.util.function.BiConsumer

public class IncomingMessageRepository<R, P : ClientProt> internal constructor(
    private val protRepository: ProtRepository<P>,
    private val decoders: Array<MessageDecoder<*>?>,
    private val consumers: Array<BiConsumer<R, out IncomingMessage>?>,
) {
    public fun getDecoder(opcode: Int): MessageDecoder<*> {
        return decoders[opcode]
            ?: throw IllegalArgumentException("Opcode $opcode is not registered.")
    }

    public fun getConsumer(opcode: Int): BiConsumer<R, out IncomingMessage>? {
        return consumers.getOrNull(opcode)
    }

    public fun getSize(opcode: Int): Int {
        return protRepository.getSize(opcode)
    }
}
