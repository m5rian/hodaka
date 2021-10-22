package com.github.m5rian.hodaka.listeners;

import com.github.m5rian.hodaka.HodakaBot;
import com.github.m5rian.hodaka.Utilities;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.m5rian.hodaka.HodakaBot.config;

public class SubmissionManager {
    private final static Map<Long, String> submissionWaiters = new HashMap<>();

    public static void loadSubmissions(ReadyEvent event) {
        final MessagePaginationAction submissions = event.getJDA().getGuildById(config.guildId).getTextChannelById(config.channels.designSubmissions).getIterableHistory();
        submissions.stream().filter(message -> {
            // Message isn't written by the bot
            if (message.getAuthor().getIdLong() != event.getJDA().getSelfUser().getIdLong()) {
                message.delete().queue(); // Delete message
                return false;
            }

            final MessageEmbed embed = message.getEmbeds().get(0); // Get embed of message
            return embed.getColor() == config.green
                    && embed.getColor() == config.red;
        }).forEach(submission -> {
            final MessageEmbed embed = submission.getEmbeds().get(0); // Get embed of submission
            final long userId = Long.parseLong(embed.getAuthor().getUrl().split("/")[embed.getAuthor().getUrl().split("/").length - 1]);

            final long timeCreated = submission.getTimeCreated().toInstant().toEpochMilli();
            // Time to react is over
            if (timeCreated + TimeUnit.DAYS.toMillis(Long.parseLong(config.submissionEvaluation)) > System.currentTimeMillis()) {
                evaluateReactions(submission.getId()); // Evaluate reactions
            }
            // Still time to react
            else {
                final long delay = Math.round(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Long.parseLong(config.submissionEvaluation)) + timeCreated);
                System.out.println("Milliseconds to wait until design auswertung" + TimeUnit.MILLISECONDS.toMinutes(delay));
                submissionWaiters.put(userId, submission.getId()); // Put design in waiting queue
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        evaluateReactions(submission.getId()); // Evaluate reactions
                    }
                }, delay);
            }
        });
    }

    public static void handleSubmission(GuildMessageReceivedEvent event) {
        final Member member = event.getMember(); // Get member
        final String message = event.getMessage().getContentRaw();
        final Message.Attachment attachment = event.getMessage().getAttachments().isEmpty() ? null : event.getMessage().getAttachments().get(0);

        // Member already submitted a design
        if (submissionWaiters.containsKey(member.getIdLong())) {
            event.getMessage().delete().queue(); // Delete submission
            event.getAuthor().openPrivateChannel().queue(dm -> dm.sendMessage("You already submitted a design. Please wait until the reactions are evaluated").queue());
            return;
        }

        // Create embed builder for submission
        final EmbedBuilder submission = new EmbedBuilder()
                .setAuthor(member.getEffectiveName(), "https://discord.com/users/" + member.getId(), member.getUser().getEffectiveAvatarUrl())
                .setColor(member.getColor())
                .setTimestamp(Instant.now());

        MessageAction messageAction;
        // Message has attachments
        if (attachment != null) {
            // Submission contains also text
            if (!message.isBlank()) submission.setDescription(message);

            final byte[] file = Utilities.urlToByteArray(attachment.getUrl()); // Get byte array from url
            final String fileName = "submission_" + event.getAuthor().getId() + "_" + System.currentTimeMillis() + "." + attachment.getUrl().split("\\.")[attachment.getUrl().split("\\.").length - 1];
            submission.setImage("attachment://" + fileName); // Add image to embed
            // Prepare embed
            messageAction = event.getChannel()
                    .sendMessageEmbeds(submission.build())
                    .addFile(file, fileName);
        }
        // Message has no attachments
        else {
            submission.setDescription(message); // Set description
            // Prepare embed
            messageAction = event.getChannel().sendMessageEmbeds(submission.build());
        }

        // Send embed
        messageAction.queue(submissionMessage -> {
            // Add reactions
            submissionMessage.addReaction("\u2B06").queue();
            submissionMessage.addReaction("\u2B07").queue();

            submissionWaiters.put(member.getIdLong(), submissionMessage.getId()); // Put design in waiting queue
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    evaluateReactions(submissionMessage.getId());
                }
            }, Math.round(TimeUnit.DAYS.toMillis(Long.parseLong(config.submissionEvaluation))));

            event.getMessage().delete().queue(); // Delete original message
        });
    }

    public static void evaluateReactions(String messageId) {
        try {
            final Guild guild = HodakaBot.jda.getGuildById(config.guildId); // Get guild
            final Message message = guild.getTextChannelById(config.channels.designSubmissions).retrieveMessageById(messageId).complete();

            final Member member = guild.getMemberById(message.getEmbeds().get(0).getAuthor().getUrl().split("/")[message.getEmbeds().get(0).getAuthor().getUrl().split("/").length - 1]);
            submissionWaiters.remove(member.getIdLong());
            System.out.println(member.getIdLong());

            long upvotes = message.getReactions().stream().filter(reaction -> reaction.getReactionEmote().isEmoji() && reaction.getReactionEmote().getEmoji().equals("\u2B06")).findFirst().get().getCount();
            long downvotes = message.getReactions().stream().filter(reaction -> reaction.getReactionEmote().isEmoji() && reaction.getReactionEmote().getEmoji().equals("\u2B07")).findFirst().get().getCount();

            final boolean ownUpvote = message.getReactions().stream()
                    .filter(reaction -> reaction.getReactionEmote().isEmoji() && reaction.getReactionEmote().getEmoji().equals("\u2B06"))
                    .anyMatch(reaction -> reaction.retrieveUsers().complete().stream().anyMatch(user -> user.getIdLong() == member.getIdLong()));
            final boolean ownDownvote = message.getReactions().stream()
                    .filter(reaction -> reaction.getReactionEmote().isEmoji() && reaction.getReactionEmote().getEmoji().equals("\u2B07"))
                    .anyMatch(reaction -> reaction.retrieveUsers().complete().stream().anyMatch(user -> user.getIdLong() == member.getIdLong()));

            if (ownUpvote) upvotes--;
            if (ownDownvote) downvotes--;

            final Optional<Role> currentRole = member.getRoles().stream().filter(role -> role.getName().startsWith(config.designerRolePrefix)).findFirst();

            // Rankup
            if (upvotes > downvotes) {
                // Create embed
                final EmbedBuilder submission = new EmbedBuilder(message.getEmbeds().get(0))
                        .setColor(config.green)
                        .setTimestamp(Instant.now());

                // Member is unranked
                if (currentRole.isEmpty()) {
                    final Role role = guild.getRoles().stream().filter(r -> r.getPosition() == config.designerRolesPositionStart).findFirst().get(); // Get first designer role
                    guild.addRoleToMember(member, role).queue(); // Add new designer role
                    // Set embed text
                    submission.appendDescription(String.format("%n%n%s joined officially the designers with %s", member.getAsMention(), role.getAsMention()));
                }
                // Member has already a designer role
                else {
                    final int position = currentRole.get().getPosition(); // Get position of current designer role
                    final Role nextRole = guild.getRoles().stream().filter(role -> role.getPosition() == position + 1).findFirst().get(); // Get role above current designer role
                    // Member reached maximum designer level
                    if (!nextRole.getName().startsWith(config.designerRolePrefix)) {
                        // Set message
                        submission.appendDescription(String.format("%n%n%s has already the highest designer role and stays %s (%s/%s)",
                                member.getAsMention(), currentRole.get().getAsMention(), config.designerRolesCount, config.designerRolesCount));
                    }
                    // Member can still receive new roles
                    else {
                        final int designerLevel = nextRole.getPosition() - config.designerRolesPositionStart; // Get current designer level
                        // Set message
                        submission.appendDescription(String.format("%n%n%s got a uprank and is now %s (%s/%s)",
                                member.getAsMention(), nextRole.getAsMention(), designerLevel, config.designerRolesCount));
                        guild.removeRoleFromMember(member, currentRole.get()).queue(); // Remove old role
                        guild.addRoleToMember(member, nextRole).queue(); // Add new designer role
                    }
                }
                message.getChannel().sendMessage(member.getAsMention()).setEmbeds(submission.build()).queue(); // Send final message
                message.delete().queue(); // Delete submission
            }
            // Downrank
            else {
                // Create embed
                final EmbedBuilder submission = new EmbedBuilder(message.getEmbeds().get(0))
                        .setColor(config.red)
                        .setTimestamp(Instant.now());

                // Member is unranked
                if (currentRole.isEmpty()) {
                    // Set message
                    submission.appendDescription(String.format("%n%n%s didn't rank up, maybe next time? (%s/%s)",
                            member.getAsMention(), 0, config.designerRolesCount));
                }
                // Member owns a designer rank
                if (currentRole.isPresent()) {
                    // Member has already lowest designer level
                    if (currentRole.get().getPosition() == config.designerRolesPositionStart) {
                        // Set message
                        submission.appendDescription(String.format("%n%n%s got a downrank and lost all his roles... R.I.P. (%s/%s)",
                                member.getAsMention(), 0, config.designerRolesCount));
                        guild.removeRoleFromMember(member, currentRole.get()).queue(); // Remove role
                    }
                    // Member gets a normal downrank
                    else {
                        final Role nextRole = guild.getRoles().stream().filter(role -> role.getPosition() == currentRole.get().getPosition() + -1).findFirst().get();
                        int designerLevel = nextRole.getPosition() - config.designerRolesPositionStart;
                        // Set message
                        submission.appendDescription(String.format("%n%n%s got a downrank and is now %s (%s/%s)",
                                member.getAsMention(), nextRole.getAsMention(), designerLevel, config.designerRolesCount));

                        guild.removeRoleFromMember(member, currentRole.get()).queue(); // Remove old role
                        guild.addRoleToMember(member, nextRole).queue(); // Add new, lower role
                    }
                }

                message.getChannel().sendMessage(member.getAsMention()).setEmbeds(submission.build()).queue(); // Send final message
                message.delete().queue(); // Delete submission
            }
        } catch (ErrorResponseException ignored) {

        }
    }

}
