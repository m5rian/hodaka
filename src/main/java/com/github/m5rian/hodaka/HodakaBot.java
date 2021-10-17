package com.github.m5rian.hodaka;

import com.github.m5rian.hodaka.commands.Ban;
import com.github.m5rian.hodaka.commands.Resources;
import com.github.m5rian.hodaka.commands.Unbans;
import com.github.m5rian.hodaka.yaml.config.Config;
import com.github.m5rian.jdaCommandHandler.CommandListener;
import com.github.m5rian.jdaCommandHandler.commandServices.DefaultCommandService;
import com.github.m5rian.jdaCommandHandler.commandServices.DefaultCommandServiceBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.util.Arrays;

public class HodakaBot {

    public final static DefaultCommandService COMMAND_SERVICE = new DefaultCommandServiceBuilder()
            .setDefaultPrefix("!")
            .registerSlashCommandClasses(new Resources(), new Unbans(), new Ban())
            .build();
    public static JDA jda;
    public static Config config;

    public static void main(String[] args) throws LoginException, InterruptedException {
        config = Config.init();
        jda = JDABuilder.create(config.token, Arrays.asList(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS))
                .addEventListeners(
                        new Listeners(),
                        new CommandListener(COMMAND_SERVICE))
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build()
                .awaitReady();
        config.load();
    }

}
