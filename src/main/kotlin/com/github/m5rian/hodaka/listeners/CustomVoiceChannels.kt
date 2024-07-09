package com.github.m5rian.hodaka.listeners

import com.github.m5rian.hodaka.HodakaBot
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent

object CustomVoiceChannels {
    private const val voicePrefix = "\u2B50\u30FB"
    private val allow: Collection<Permission> = mutableListOf(
        Permission.MANAGE_CHANNEL,
        Permission.VOICE_DEAF_OTHERS,
        Permission.VOICE_MOVE_OTHERS
    )

    fun onVoiceJoin(event: GuildVoiceJoinEvent) {
        val joinedChannel = event.channelJoined.id // Get joined channel id
        val hubChannel: String = HodakaBot.config.channels.voice // Get voice call creation channel id
        // User wants to create a custom voice channel
        if (joinedChannel == hubChannel) {
            val category = event.channelJoined.parent // Get category of custom voice calls
            val memberName = event.member.effectiveName
            category!!.createVoiceChannel(voicePrefix + memberName) // Set name
                .addMemberPermissionOverride(event.member.idLong, allow, emptyList()) // Set permission for author
                .queue { channel: VoiceChannel? ->  // Create channel
                    event.guild.moveVoiceMember(event.member, channel).queue() // Move member
                }
        }
    }

    fun onVoiceLeave(channel: VoiceChannel) {
        // Voice call is empty and voice call is a custom voice call
        if (channel.members.size == 0 && channel.name.startsWith(voicePrefix)) {
            channel.delete().queue() // Delete channel
        }
    }
}