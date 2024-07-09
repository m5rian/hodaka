package com.github.m5rian.hodaka

import com.github.m5rian.hodaka.commands.Ban
import com.github.m5rian.hodaka.commands.Resources
import com.github.m5rian.hodaka.commands.Unbans
import com.github.m5rian.hodaka.yaml.config.Config
import com.github.m5rian.jdaCommandHandler.CommandListener
import com.github.m5rian.jdaCommandHandler.commandServices.DefaultCommandService
import com.github.m5rian.jdaCommandHandler.commandServices.DefaultCommandServiceBuilder
import dev.minn.jda.ktx.injectKTX
import dev.minn.jda.ktx.listener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import java.util.*

object HodakaBot {
    val COMMAND_SERVICE: DefaultCommandService = DefaultCommandServiceBuilder()
        .setDefaultPrefix("!")
        .registerSlashCommandClasses(Resources(), Unbans(), Ban())
        .build()
    var jda: JDA? = null
    @JvmStatic
    lateinit var config: Config;

    fun main(args: Array<String>) {
        jda = JDABuilder.create("TOKEN", listOf(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS))
            .injectKTX()
            .addEventListeners(
                Listeners(),
                CommandListener(COMMAND_SERVICE))
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build()
            .awaitReady()
        config = Config.load()
    }
}