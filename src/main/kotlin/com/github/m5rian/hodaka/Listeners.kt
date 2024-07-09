package com.github.m5rian.hodaka

import com.github.m5rian.hodaka.commands.Unbans
import com.github.m5rian.hodaka.listeners.CustomVoiceChannels
import com.github.m5rian.hodaka.listeners.SubmissionManager
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import javax.annotation.Nonnull
import com.github.m5rian.hodaka.HodakaBot.config;

class Listeners : ListenerAdapter() {
    //JDA Events
    override fun onReady(@Nonnull event: ReadyEvent) {
        Thread {
            try {
                Thread.sleep(5000)
                SubmissionManager.loadSubmissions(event) // Load not evaluated submissions
            } catch (exception: InterruptedException) {
                exception.printStackTrace()
            }
        }.start()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        try {
            if (event.guild.id != config.guildId) return  // Wrong guild
            if (event.author.isBot) return

            // Design submission
            if (event.channel.id == config.channels.designSubmissions) {
                SubmissionManager.handleSubmission(event)
            } else if (event.channel.id == config.channels.unbanRequest) {
                Unbans.onUnbanRequest(event)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Guild Voice Events
    override fun onGuildVoiceJoin(@Nonnull event: GuildVoiceJoinEvent) {
        try {
            if (event.guild.id != config.guildId) return  // Wrong guild
            CustomVoiceChannels.onVoiceJoin(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGuildVoiceMove(@Nonnull event: GuildVoiceMoveEvent) {
        try {
            if (event.guild.id != config.guildId) return  // Wrong guild
            CustomVoiceChannels.onVoiceLeave(event.channelLeft)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGuildVoiceLeave(@Nonnull event: GuildVoiceLeaveEvent) {
        try {
            if (event.guild.id != config.guildId) return  // Wrong guild
            CustomVoiceChannels.onVoiceLeave(event.channelLeft)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}