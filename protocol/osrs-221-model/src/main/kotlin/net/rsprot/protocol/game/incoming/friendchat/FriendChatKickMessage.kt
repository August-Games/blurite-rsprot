package net.rsprot.protocol.game.incoming.friendchat

import net.rsprot.protocol.message.IncomingMessage

/**
 * Friend chat kick is sent when the owner requests to click another
 * player from their friend chat.
 * @property name the name of the player to kick
 */
public class FriendChatKickMessage(
    public val name: String,
) : IncomingMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FriendChatKickMessage

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "FriendChatKickMessage(name='$name')"
    }
}
