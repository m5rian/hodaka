package com.github.m5rian.hodaka.yaml.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Unbans {
    @JsonProperty
    public String accept;
    @JsonProperty
    public String deny;
}
