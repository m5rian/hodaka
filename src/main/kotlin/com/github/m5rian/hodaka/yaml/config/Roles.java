package com.github.m5rian.hodaka.yaml.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Roles {
    @JsonProperty public String member;
    @JsonProperty public List<String> moderators;
}
