package com.github.m5rian.hodaka.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Packs {

    @JsonProperty
    private List<Pack> packs;

    public static List<Pack> load() {
        final List<Pack> packs = new ArrayList<>();
        try {
            final URL url = Packs.class.getClassLoader().getResource("packs.yaml");
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            final Packs yaml = mapper.readValue(url, Packs.class);
            packs.addAll(yaml.packs);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packs;
    }

    public static class Pack {
        @JsonProperty
        private String name;
        @JsonProperty
        private String download;
        @JsonProperty
        private Platform platform;
        @JsonProperty
        private List<String> tags;

        public String getName() {
            return this.name;
        }

        public String getDownload() {
            return this.download;
        }

        public Platform getPlatform() {
            return platform;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    public enum Platform {
        PSD("Photoshop", null),
        BLEND("Blender", null),
        C4D("Cinema 4D", null);

        private final String program;
        private final String emoji;

        Platform(String program, String emoji) {
            this.program = program;
            this.emoji = emoji;
        }

        public String getFileEnding() {
            return this.name().toLowerCase();
        }

        public String getProgram() {
            return this.program;
        }

        public String getEmoji() {
            return this.emoji;
        }
    }
}
