package com.github.m5rian.hodaka.listeners;

import com.github.m5rian.hodaka.HodakaBot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class CustomVoiceChannels {
    private static final String voicePrefix = "\u2B50\u30FB";

    private static final Collection<Permission> allow = new ArrayList<>() {{
        add(Permission.MANAGE_CHANNEL);
        add(Permission.VOICE_MUTE_OTHERS);
        add(Permission.VOICE_DEAF_OTHERS);
        add(Permission.VOICE_MOVE_OTHERS);
    }};

    public static void onVoiceJoin(GuildVoiceJoinEvent event) {
        final String joinedChannel = event.getChannelJoined().getId(); // Get joined channel id
        final String hubChannel = HodakaBot.config.channels.voice; // Get voice call creation channel id
        // User wants to create a custom voice channel
        if (joinedChannel.equals(hubChannel)) {
            final Category category = event.getChannelJoined().getParent(); // Get category of custom voice calls

            final String memberName = event.getMember().getEffectiveName();
            category.createVoiceChannel(voicePrefix + memberName) // Set name
                    .addMemberPermissionOverride(event.getMember().getIdLong(), allow, Collections.emptyList()) // Set permission for author
                    .queue(channel -> { // Create channel
                        event.getGuild().moveVoiceMember(event.getMember(), channel).queue(); // Move member
                    });
        }
    }

    public static void onVoiceLeave(VoiceChannel channel) {
        // Voice call is empty and voice call is a custom voice call
        if (channel.getMembers().size() == 0 && channel.getName().startsWith(voicePrefix)) {
            channel.delete().queue(); // Delete channel
        }
    }

}
