package net.rsprot.protocol.channel

import io.netty.util.AttributeKey
import net.rsprot.protocol.cryptography.StreamCipherPair

public object ChannelAttributes {
    public val STREAM_CIPHER_PAIR: AttributeKey<StreamCipherPair> =
        AttributeKey.newInstance("prot_stream_cipher_pair")
}
