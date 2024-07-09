package com.github.m5rian.hodaka.commands;

import com.github.m5rian.jdaCommandHandler.CommandHandler;
import com.github.m5rian.jdaCommandHandler.slashCommand.Argument;
import com.github.m5rian.jdaCommandHandler.slashCommand.SlashCommandContext;
import com.github.m5rian.jdaCommandHandler.slashCommand.SlashCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import static com.github.m5rian.hodaka.HodakaBot.config;

public class Ban implements CommandHandler {

    @SlashCommandEvent(
            name = "ban",
            description = "Ban a member",
            args = {
                    @Argument(type = OptionType.USER, name = "member", description = "The member to ban", required = true),
                    @Argument(type = OptionType.STRING, name = "reason", description = "A brief description on why the member got banned")
            }
    )
    public void onBanCommand(SlashCommandContext sctx) {
        // Member doesn't have moderator permissions
        if (sctx.getMember().getRoles().stream().noneMatch(role -> config.roles.moderators.contains(role.getId()))) {
            sctx.reply("Who do you think are you? Well not a mod, that's for sure...").setEphemeral(true).queue();
            return;
        }

        final Member member = sctx.getEvent().getOptionsByName("member").get(0).getAsMember();
        final Role role = sctx.getGuild().getRoleById(config.roles.member);
        sctx.getGuild().removeRoleFromMember(member, role).queue();

        String message = String.format("Moderator: %-33s Member: %-33s",sctx.getMember().getAsMention(), member.getAsMention());
        if (sctx.getEvent().getOptionsByName("reason").size() != 0) {
            message += "\nReason:" + sctx.getEvent().getOptionsByName("reason").get(0).getAsString();
        }
        sctx.getGuild().getTextChannelById(config.channels.banLog).sendMessage(message).queue();
        sctx.reply("Banned " + member.getAsMention()).queue();
    }
}
