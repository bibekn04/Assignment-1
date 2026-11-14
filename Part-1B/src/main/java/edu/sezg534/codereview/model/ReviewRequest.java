package edu.sezg534.codereview.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class ReviewRequest {
    @NotNull
    @JsonProperty("code")
    private String code;

    @JsonProperty("intent")
    private Intent intent = Intent.BOTH;

    @JsonProperty("language")
    private String language = "java";

    @JsonProperty("context")
    private String context = "";

    public ReviewRequest() {}

    public ReviewRequest(String code, Intent intent, String language, String context) {
        this.code = code;
        this.intent = intent != null ? intent : Intent.BOTH;
        this.language = language != null ? language : "java";
        this.context = context != null ? context : "";
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}
