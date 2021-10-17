package com.github.m5rian.hodaka.yaml.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.m5rian.hodaka.HodakaBot;
import com.github.m5rian.hodaka.yaml.Packs;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Config {
    @JsonProperty
    public String guildId;
    @JsonProperty
    public Prefixes prefixes;

    @JsonProperty
    private Colours colours;
    public Color green;
    public Color red;

    @JsonProperty
    public Roles roles;
    @JsonProperty
    public Channels channels;
    @JsonProperty
    public Categories categories;

    @JsonProperty
    public String designerRolePrefix;

    public int designerRolesCount;
    public int designerRolesPositionStart;

    @JsonProperty
    public String submissionEvaluation; // Days until to evaluate the reactions
    public final Long DAY_IN_MILLIS = 6400000L;


    public static Config load() {
        Config config = null;
        try {
            final URL url = Packs.class.getClassLoader().getResource("config.yaml");
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            config = mapper.readValue(url, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Guild guild = HodakaBot.jda.getGuildById(config.guildId);

        Config finalConfig = config;
        config.designerRolesCount = (int) guild.getRoles().stream().filter(role -> role.getName().startsWith(finalConfig.designerRolePrefix)).count();

        final java.util.List<Role> roles = guild.getRoles();
        List<Role> sortedRoles = new ArrayList<>(roles);
        Collections.reverse(sortedRoles);
        config.designerRolesPositionStart = sortedRoles.stream().filter(role -> role.getName().startsWith(finalConfig.designerRolePrefix)).findFirst().get().getPosition();

        config.green = Color.decode(config.colours.green);
        config.red = Color.decode(config.colours.red);

        return config;
    }
}
