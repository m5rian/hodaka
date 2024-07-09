package com.github.m5rian.hodaka.yaml.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Channels {
    @JsonProperty
    public String designSubmissions;
    @JsonProperty
    public String voice;
    @JsonProperty
    public String unbanRequest;
    @JsonProperty
    public String banLog;
}
