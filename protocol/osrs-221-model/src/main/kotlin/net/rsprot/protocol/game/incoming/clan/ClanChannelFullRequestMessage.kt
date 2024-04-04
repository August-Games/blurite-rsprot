package net.rsprot.protocol.game.incoming.clan

import net.rsprot.protocol.message.IncomingMessage

/**
 * Clan channel requests are made whenever the server sends a clanchannel
 * delta update, but the client does not have a clan defined at that id.
 * In order to fix the problem, the client will then request for a full
 * clan update for that clan id.
 * @property clanId the id of the clan to request, ranging from 0 to 3 (inclusive),
 * or a negative value if the request is for a guest-clan
 */
public class ClanChannelFullRequestMessage(
    public val clanId: Int,
) : IncomingMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClanChannelFullRequestMessage

        return clanId == other.clanId
    }

    override fun hashCode(): Int {
        return clanId
    }

    override fun toString(): String {
        return "ClanChannelFullRequestMessage(clanId=$clanId)"
    }
}
