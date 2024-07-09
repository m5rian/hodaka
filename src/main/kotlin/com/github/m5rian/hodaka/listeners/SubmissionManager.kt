package com.github.m5rian.hodaka.listeners

import com.github.m5rian.hodaka.HodakaBot
import com.github.m5rian.hodaka.HodakaBot.config
import com.github.m5rian.hodaka.Utilities.urlToByteArray
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import javax.annotation.Nonnull
import kotlin.math.roundToInt

object SubmissionManager {
    private val submissionWaiters:MutableMap<Long, String> = HashMap()

    fun loadSubmissions(event:ReadyEvent) {
        val submissions:MessagePaginationAction = event.jda.getGuildById(config.guildId)!!.getTextChannelById(config.channels.designSubmissions)!!.iterableHistory
        submissions.stream().filter { message:Message ->
            // Message isn't written by the bot
            if (message.author.idLong != event.jda.selfUser.idLong) {
                message.delete().queue() // Delete message
                return@filter false
            }
            val embed = message.embeds[0] // Get embed of message
            return@filter (embed.color === config.green && embed.color === config.red)
        }.forEach { submission:Message ->
            val embed = submission.embeds[0] // Get embed of submission
            val userId = embed.author!!.url!!.split("/").toTypedArray()[embed.author!!.url!!.split("/").toTypedArray().size - 1].toLong()
            val timeCreated = submission.timeCreated.toInstant().toEpochMilli()
            // Time to react is over
            if (timeCreated + config.submissionEvaluation.toDouble() * config.DAY_IN_MILLIS > System.currentTimeMillis()) {
                evaluateReactions(submission.id) // Evaluate reactions
            } else {
                val delay = Math.round(System.currentTimeMillis() - config.submissionEvaluation.toDouble() * config.DAY_IN_MILLIS + timeCreated)
                println("Milliseconds to wait until design auswertung" + TimeUnit.MILLISECONDS.toMinutes(delay))
                submissionWaiters[userId] = submission.id // Put design in waiting queue
                Timer().schedule(object:TimerTask() {
                    override fun run() {
                        evaluateReactions(submission.id) // Evaluate reactions
                    }
                }, delay)
            }
        }
    }

    fun handleSubmission(event:GuildMessageReceivedEvent) {
        val member:Member = event.member!! // Get member
        val message:String = event.message.contentRaw // Get content
        val attachment:Message.Attachment? = if (event.message.attachments.isEmpty()) null else event.message.attachments[0] // Get attachment

        // Member already submitted a design
        if (submissionWaiters.containsKey(member.idLong)) {
            event.message.delete().queue() // Delete submission
            event.author.openPrivateChannel().queue { dm:PrivateChannel ->
                dm.sendMessage("You already submitted a design. Please wait until the reactions are evaluated").queue()
            }
            return
        }

        // Create embed builder for submission
        val submission = EmbedBuilder().setAuthor(member.effectiveName, "https://discord.com/users/" + member.id, member.user.effectiveAvatarUrl).setColor(member.color).setTimestamp(Instant.now())
        // Message has attachments
        val messageAction:MessageAction = if (attachment != null) {
            // Submission contains also text
            if (!message.isBlank()) submission.setDescription(message)
            val file = urlToByteArray(attachment.url) // Get byte array from url
            val fileName = "submission_" + event.author.id + "_" + System.currentTimeMillis() + "." + attachment.url.split("\\.").toTypedArray()[attachment.url.split("\\.").toTypedArray().size - 1]
            submission.setImage("attachment://$fileName") // Add image to embed
            // Prepare embed
            event.channel.sendMessageEmbeds(submission.build()).addFile(file, fileName)
        } else {
            submission.setDescription(message) // Set description
            // Prepare embed
            event.channel.sendMessageEmbeds(submission.build())
        }

        // Send embed
        messageAction.queue { submissionMessage:Message ->
            // Add reactions
            submissionMessage.addReaction("\u2B06").queue()
            submissionMessage.addReaction("\u2B07").queue()
            submissionWaiters[member.idLong] = submissionMessage.id // Put design in waiting queue
            Timer().schedule(object:TimerTask() {
                override fun run() {
                    evaluateReactions(submissionMessage.id)
                }
            }, (config.submissionEvaluation.toDouble() * config.DAY_IN_MILLIS).roundToInt())
            event.message.delete().queue() // Delete original message
        }
    }

    suspend fun evaluateReactions(@Nonnull messageId:String?) {
        val guild:Guild = HodakaBot.jda!!.getGuildById(config.guildId)!! // Get guild

        val message:Message = guild.getTextChannelById(config.channels.designSubmissions)!!.retrieveMessageById(messageId!!).await()
        val memberId:String = message.embeds[0].author!!.url!!.split("/")[message.embeds[0].author!!.url!!.split("/").size - 1]
        // Get member
        guild.getMemberById(memberId)?.let {
            submissionWaiters.remove(it.idLong) // Remove member from queuing submissions
            // Get design votes
            var upvotes = message.reactions.stream().filter { reaction:MessageReaction ->
                reaction.reactionEmote.isEmoji && reaction.reactionEmote.emoji == "\u2B06"
            }.findFirst().get().count.toLong()
            var downvotes = message.reactions.stream().filter { reaction:MessageReaction ->
                reaction.reactionEmote.isEmoji && reaction.reactionEmote.emoji == "\u2B07"
            }.findFirst().get().count.toLong()

            // Get own votes
            val ownUpvote = message.reactions.stream().filter { reaction:MessageReaction ->
                reaction.reactionEmote.isEmoji && reaction.reactionEmote.emoji == "\u2B06"
            }.anyMatch { reaction:MessageReaction ->
                reaction.retrieveUsers().complete().stream().anyMatch { user:User -> user.idLong == it.idLong }
            }
            val ownDownvote = message.reactions.stream().filter { reaction:MessageReaction ->
                reaction.reactionEmote.isEmoji && reaction.reactionEmote.emoji == "\u2B07"
            }.anyMatch { reaction:MessageReaction ->
                reaction.retrieveUsers().complete().stream().anyMatch { user:User -> user.idLong == member.idLong }
            }
            if (ownUpvote) upvotes--
            if (ownDownvote) downvotes--

            if (upvotes > downvotes) rankup()
            else downrank()

        }
    }

    private fun rankup(message:Message, member:Member) {
        val guild:Guild = message.guild; // Get guild

        val currentRole:Optional<Role> = it.roles.stream().filter(Predicate { role:Role -> role.name.startsWith(config.designerRolePrefix) }).findFirst() // Get members current designer role
        val submission:EmbedBuilder = EmbedBuilder(message.embeds[0]).setColor(config.green).setTimestamp(Instant.now()) // Create embed

        // Member is unranked
        if (currentRole.isEmpty) {
            val role = guild.roles.stream().filter { r:Role -> r.position == config.designerRolesPositionStart }.findFirst().get() // Get first designer role
            guild.addRoleToMember(member, role).queue() // Add new designer role
            // Set embed text
            submission.appendDescription(
                String.format(
                    "%n%n%s joined officially the designers with %s", member.asMention, role.asMention
                )
            )
        } else {
            val position = currentRole.get().position // Get position of current designer role
            val nextRole = guild.roles.stream().filter { role:Role -> role.position == position + 1 }.findFirst().get() // Get role above current designer role
            // Member reached maximum designer level
            if (!nextRole.name.startsWith(config.designerRolePrefix)) {
                // Set message
                submission.appendDescription(
                    java.lang.String.format(
                        "%n%n%s has already the highest designer role and stays %s (%s/%s)", member.asMention, currentRole.get().asMention, config.designerRolesCount, config.designerRolesCount
                    )
                )
            } else {
                val designerLevel:Int = nextRole.position - config.designerRolesPositionStart // Get current designer level
                // Set message
                submission.appendDescription(
                    java.lang.String.format(
                        "%n%n%s got a uprank and is now %s (%s/%s)", member.asMention, nextRole.asMention, designerLevel, config.designerRolesCount
                    )
                )
                guild.removeRoleFromMember(member, currentRole.get()).queue() // Remove old role
                guild.addRoleToMember(member, nextRole).queue() // Add new designer role
            }
        }
        message.channel.sendMessage(member.asMention).setEmbeds(submission.build()).queue() // Send final message
        message.delete().queue() // Delete submission
    }

    private fun downrank(message:Message, member:Member) {
        val guild:Guild = message.guild; // Get guild

        val currentRole:Optional<Role> = it.roles.stream().filter(Predicate { role:Role -> role.name.startsWith(config.designerRolePrefix) }).findFirst() // Get members current designer role
        val submission:EmbedBuilder = EmbedBuilder(message.embeds[0]).setColor(config.red).setTimestamp(Instant.now()) // Create embed

        // Member is unranked
        if (currentRole.isEmpty) {
            // Set message
            submission.appendDescription(
                java.lang.String.format(
                    "%n%n%s didn't rank up, maybe next time? (%s/%s)", member.asMention, 0, config.designerRolesCount
                )
            )
        }
        // Member owns a designer rank
        if (currentRole.isPresent) {
            // Member has already lowest designer level
            if (currentRole.get().position == config.designerRolesPositionStart) {
                // Set message
                submission.appendDescription(
                    java.lang.String.format(
                        "%n%n%s got a downrank and lost all his roles... R.I.P. (%s/%s)", member.asMention, 0, config.designerRolesCount
                    )
                )
                guild.removeRoleFromMember(member, currentRole.get()).queue() // Remove role
            } else {
                val nextRole = guild.roles.stream().filter { role:Role ->
                    role.position == currentRole.get().position + -1
                }.findFirst().get()
                val designerLevel:Int = nextRole.position - config.designerRolesPositionStart
                // Set message
                submission.appendDescription("\n\n${member.asMention} got a downrank and is now ${nextRole.asMention} ($designerLevel/${config.designerRolesCount})}")
                guild.removeRoleFromMember(member, currentRole.get()).queue() // Remove old role
                guild.addRoleToMember(member, nextRole).queue() // Add new, lower role
            }
        }
        message.channel.sendMessage(member.asMention).setEmbeds(submission.build()).queue() // Send final message
        message.delete().queue() // Delete submission
    }
}