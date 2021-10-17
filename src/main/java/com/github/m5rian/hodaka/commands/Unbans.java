package com.github.m5rian.hodaka.commands;

import com.github.m5rian.jdaCommandHandler.CommandHandler;
import com.github.m5rian.jdaCommandHandler.slashCommand.Argument;
import com.github.m5rian.jdaCommandHandler.slashCommand.SlashCommandContext;
import com.github.m5rian.jdaCommandHandler.slashCommand.SlashCommandEvent;
import com.github.m5rian.jdaCommandHandler.slashCommand.Subcommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.github.m5rian.hodaka.HodakaBot.config;

public class Unbans implements CommandHandler {

    @SlashCommandEvent(
            name = "unbans",
            description = "Manage an unban request",
            subcommands = {
                    @Subcommand(name = "accept", description = "Accept an unban", args = {
                            @Argument(type = OptionType.USER, name = "member", description = "Member to unban", required = true)
                    }),
                    @Subcommand(name = "deny", description = "Deny an unban", args = {
                            @Argument(type = OptionType.USER, name = "member", description = "Member to unban", required = true)
                    })
            }
    )
    public void onUnbanCommand(SlashCommandContext sctx) {
        // Member doesn't have moderator permissions
        if (sctx.getMember().getRoles().stream().noneMatch(role -> config.roles.moderators.contains(role.getId()))) {
            sctx.reply("Who do you think are you? Well not a mod, that's for sure...").setEphemeral(true).queue();
            return;
        }

        if (sctx.getEvent().getSubcommandName().equals("accept")) {
            onUnbanAccept(sctx);
        } else if (sctx.getEvent().getSubcommandName().equals("deny")) {
            onUnbanDeny(sctx);
        }
    }

    private void onUnbanAccept(SlashCommandContext sctx) {
        final TextChannel channel = (TextChannel) sctx.getChannel();
        channel.getMemberPermissionOverrides().forEach(override -> channel.getManager().removePermissionOverride(override.getMember()).queue()); // Remove permission override for members
        channel.getManager().setParent(sctx.getGuild().getCategoryById(config.categories.achievedUnbanRequests)).queue(); // Set parent to achieved bans
        channel.getManager().setName(config.prefixes.emojis.unbans.accept + config.prefixes.symbols.channels + channel.getName().split(config.prefixes.symbols.channels)[1]).queue(); // Change channel name

        final Member member = sctx.getEvent().getOptionsByName("member").get(0).getAsMember();
        sctx.getGuild().addRoleToMember(member, sctx.getGuild().getRoleById(config.roles.member)).queue();

        sctx.reply("Unbanned").queue();
    }

    private void onUnbanDeny(SlashCommandContext sctx) {
        final TextChannel channel = (TextChannel) sctx.getChannel();
        channel.getMemberPermissionOverrides().forEach(override -> channel.getManager().removePermissionOverride(override.getMember()).queue()); // Remove permission override for members
        channel.getManager().setParent(sctx.getGuild().getCategoryById(config.categories.achievedUnbanRequests)).queue(); // Set parent to achieved bans
        channel.getManager().setName(config.prefixes.emojis.unbans.deny + config.prefixes.symbols.channels + channel.getName().split(config.prefixes.symbols.channels)[1]).queue(); // Change channel name

        sctx.reply("Unban request denied").queue();
    }

    public static void onUnbanRequest(GuildMessageReceivedEvent event) {
        // Does the member already have a unban request, which is active?
        final boolean unbanRequestActive = event.getChannel().getParent().getTextChannels().stream().anyMatch(channel -> {
            if (channel.getIdLong() == event.getChannel().getIdLong()) return false;
            return event.getMember().hasAccess(channel);
        });
        // Member has already a unban request running
        if (unbanRequestActive) {
            event.getMessage().delete().queue();
            return;
        }

        long id = 0L;
        final List<TextChannel> unbanRequests = new ArrayList<>();
        unbanRequests.addAll(event.getGuild().getCategoryById(config.categories.achievedUnbanRequests).getTextChannels()); // Add achieved unban requests
        // Add active unban requests
        event.getGuild().getCategoryById(config.categories.unbanRequests).getTextChannels().forEach(channel -> {
            if (channel.getIdLong() != event.getChannel().getIdLong()) unbanRequests.add(channel);
        });

        if (unbanRequests.size() > 1) {
            unbanRequests.sort((channel1, channel2) -> {
                final int channelName1 = Integer.parseInt(channel1.getName().split(config.prefixes.symbols.channels)[1]);
                final int channelName2 = Integer.parseInt(channel1.getName().split(config.prefixes.symbols.channels)[1]);

                return Integer.compare(channelName1, channelName2);  // Both are equal
            });
            Collections.reverse(unbanRequests); // Reverse list

            id = Long.parseLong(unbanRequests.get(0).getName().split(config.prefixes.symbols.channels)[1]) + 1;
        } else if (unbanRequests.size() == 1) id = 1;

        event.getMessage().delete().queue(); // Delete original message
        final String channelName = "\uD83D\uDCDD" + config.prefixes.symbols.channels + id;
        event.getChannel().getParent().createTextChannel(channelName)
                .addMemberPermissionOverride(event.getMember().getIdLong(), List.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_WRITE), List.of())
                .addRolePermissionOverride(Long.parseLong(config.roles.member), List.of(), List.of(Permission.VIEW_CHANNEL))
                .queue(channel -> channel.sendMessage("> " + event.getMessage().getContentRaw()).queue());
    }

}
