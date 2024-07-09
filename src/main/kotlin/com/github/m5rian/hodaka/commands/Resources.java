package com.github.m5rian.hodaka.commands;

import com.github.m5rian.hodaka.Utilities;
import com.github.m5rian.hodaka.yaml.Packs;
import com.github.m5rian.jdaCommandHandler.CommandHandler;
import com.github.m5rian.jdaCommandHandler.slashCommand.Argument;
import com.github.m5rian.jdaCommandHandler.slashCommand.SlashCommandContext;
import com.github.m5rian.jdaCommandHandler.slashCommand.SlashCommandEvent;
import com.github.m5rian.jdaCommandHandler.slashCommand.Subcommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Resources implements CommandHandler {
    private final float itemsPerPage = 10.0f;

    @SlashCommandEvent(
            name = "resource",
            description = "Find packs, rigs and more",
            subcommands = {
                    @Subcommand(name = "find", description = "Search for packs, rigs and more", args = {
                            @Argument(type = OptionType.STRING, name = "query", description = "The search query", required = true)
                    }),
                    @Subcommand(name = "discover", description = "Browse through a handful of packs, rigs and more")
            }
    )
    public void onResource(SlashCommandContext sctx) {
        // Resource find
        if (sctx.getEvent().getSubcommandName().equals("find")) {
            onResourceFind(sctx);
        }
        // Resource discover
        else if (sctx.getEvent().getSubcommandName().equals("discover")) {
            send(sctx, Packs.load());
        }
    }

    private void onResourceFind(SlashCommandContext sctx) {
        final String query = sctx.getEvent().getOptionsByName("query").get(0).getAsString().toLowerCase();

        EmbedBuilder packs = new EmbedBuilder();

        Packs.load().stream().filter(pack -> {
            return pack.getName().toLowerCase().contains(query) || pack.getTags().stream().anyMatch(tag -> tag.equals(query));
        }).forEach(pack -> {
            packs.appendDescription(String.format("[%s](%s) - %s", pack.getName(), pack.getDownload(), pack.getPlatform().getFileEnding()));
        });
        if (packs.getDescriptionBuilder().isEmpty()) {
            packs.appendDescription("No packs found");
        }

        sctx.replyEmbeds(packs.build()).queue();
    }

    /**
     * @param packs      A {@link List<Packs.Pack>} to get the packs from.
     * @param startIndex The index to start getting the packs.
     * @return Returns {@link Resources#itemsPerPage} {@link Packs.Pack}s of {@param packs} from the {@param startIndex}.
     */
    private List<Packs.Pack> getPacks(List<Packs.Pack> packs, int startIndex) {
        final List<Packs.Pack> packsBundle = new ArrayList<>(); // Create list for packs to return

        for (int i = startIndex; i < startIndex + itemsPerPage; i++) {
            if (packs.size() == i) break; // No more packs
            packsBundle.add(packs.get(i));
        }
        return packsBundle;
    }

    private void send(SlashCommandContext sctx, List<Packs.Pack> packs) {
        final int pages = (packs.size() / itemsPerPage) % 1 == 0 ? (int) (packs.size() / itemsPerPage) : (int) (packs.size() / itemsPerPage + 1);

        final Button buttonBack = Button.primary(sctx.getMember().getId() + "_next", Emoji.fromUnicode("\u2B05"));
        final Button buttonNext = Button.primary(sctx.getMember().getId() + "_back", Emoji.fromUnicode("\u27A1"));

        final List<Packs.Pack> currentPacks = getPacks(packs, 0); // Get packs for first page

        final EmbedBuilder message = new EmbedBuilder()
                .setFooter("1/" + pages);
        currentPacks.forEach(pack -> message.appendDescription(String.format("[%s](%s) - %s%n", pack.getName(), pack.getDownload(), pack.getPlatform().getFileEnding())));

        sctx.replyEmbeds(message.build()).addActionRow(List.of(
                buttonBack.asDisabled(), // Back button
                pages > 1 ? buttonNext : buttonNext.asDisabled())) // Next button
                .queue(success -> {
                    onButtonEvent(buttonNext.getId(), e -> onButtonClick(e, ButtonOption.NEXT, packs, pages, buttonBack, buttonNext));
                    onButtonEvent(buttonBack.getId(), e -> onButtonClick(e, ButtonOption.BACK, packs, pages, buttonBack, buttonNext));

                    Utilities.EXECUTOR_SERVICE.schedule(() -> {
                        System.out.println("HIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIi");
                        // Disable buttons
                        success.editOriginalComponents(ActionRow.of(buttonBack.asDisabled(), buttonNext.asDisabled())).queue();
                        // Sto listening to button events
                        finishButton(buttonNext.getId());
                        finishButton(buttonBack.getId());
                    }, 5, TimeUnit.MINUTES);
                });
    }

    private EmbedBuilder getEmbed(int page, int pages, List<Packs.Pack> packs) {
        final EmbedBuilder embed = new EmbedBuilder()
                .setFooter(page + "/" + pages);
        packs.forEach(pack -> embed.appendDescription(String.format("[%s](%s) - %s%n", pack.getName(), pack.getDownload(), pack.getPlatform().getFileEnding())));
        return embed;
    }

    private void onButtonClick(ButtonClickEvent event, ButtonOption button, List<Packs.Pack> packs, int pages, Button buttonBack, Button buttonNext) {
        int currentPage = Integer.parseInt(event.getMessage().getEmbeds().get(0).getFooter().getText().split("/")[0]); // Get current page
        currentPage += button == ButtonOption.NEXT ? 1 : -1; // Update page

        final List<Packs.Pack> nextPagePacks = getPacks(packs, (int) ((currentPage - 1) * itemsPerPage));

        final EmbedBuilder embed = getEmbed(currentPage, pages, nextPagePacks);
        event.getMessage().editMessageEmbeds(embed.build()).queue();

        if (currentPage == 1) {
            event.getChannel().editMessageComponentsById(event.getMessageId(), ActionRow.of(
                    buttonBack.asDisabled(),
                    buttonNext.asEnabled())
            ).queue();
        } else if (pages == currentPage) {
            event.getChannel().editMessageComponentsById(event.getMessageId(), ActionRow.of(
                    buttonBack.asEnabled(),
                    buttonNext.asDisabled())
            ).queue();
        }

        event.deferEdit().queue();
    }

    enum ButtonOption {
        BACK,
        NEXT;
    }

}
