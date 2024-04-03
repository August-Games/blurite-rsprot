package net.rsprot.protocol.game.incoming

import net.rsprot.protocol.message.IncomingMessage
import net.rsprot.protocol.util.CombinedId

/**
 * If1 button events are sent whenever a player clicks on an older
 * if1-type component.
 * @property interfaceId the interface id the player interacted with
 * @property componentId the component id on that interface the player interacted with
 */
@Suppress("MemberVisibilityCanBePrivate")
public class If1ButtonEvent(
    private val combinedId: CombinedId,
) : IncomingMessage {
    public val interfaceId: Int
        get() = combinedId.interfaceId
    public val componentId: Int
        get() = combinedId.componentId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as If1ButtonEvent

        return combinedId == other.combinedId
    }

    override fun hashCode(): Int {
        return combinedId.hashCode()
    }

    override fun toString(): String {
        return "If1ButtonEvent(" +
            "interfaceId=$interfaceId, " +
            "componentId=$componentId" +
            ")"
    }
}
