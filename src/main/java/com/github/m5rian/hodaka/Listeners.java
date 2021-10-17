package com.github.m5rian.hodaka;

import com.github.m5rian.hodaka.commands.Unbans;
import com.github.m5rian.hodaka.listeners.CustomVoiceChannels;
import com.github.m5rian.hodaka.listeners.SubmissionManager;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

import static com.github.m5rian.hodaka.HodakaBot.config;

public class Listeners extends ListenerAdapter {

    //JDA Events
    public void onReady(@Nonnull ReadyEvent event) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                SubmissionManager.loadSubmissions(event); // Load not evaluated submissions
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }).start();
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        try {
            if (!event.getGuild().getId().equals(config.guildId)) return; // Wrong guild
            if (event.getAuthor().isBot()) return;

            // Design submission
            if (event.getChannel().getId().equals(config.channels.designSubmissions)) {
                SubmissionManager.handleSubmission(event);
            }
            // Unban requests
            else if (event.getChannel().getId().equals(config.channels.unbanRequest)) {
                Unbans.onUnbanRequest(event);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Guild Voice Events
    public void onGuildVoiceJoin(@Nonnull GuildVoiceJoinEvent event) {
        try {
            if (!event.getGuild().getId().equals(config.guildId)) return; // Wrong guild

            CustomVoiceChannels.onVoiceJoin(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent event) {
        try {
            if (!event.getGuild().getId().equals(config.guildId)) return; // Wrong guild

            CustomVoiceChannels.onVoiceLeave(event.getChannelLeft());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        try {
            if (!event.getGuild().getId().equals(config.guildId)) return; // Wrong guild

            CustomVoiceChannels.onVoiceLeave(event.getChannelLeft());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
