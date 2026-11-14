package edu.sezg534.codereview.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RefactoringSuggestion {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("rationale")
    private String rationale;

    @JsonProperty("before_snippet")
    private String beforeSnippet;

    @JsonProperty("after_snippet")
    private String afterSnippet;

    public RefactoringSuggestion() {}

    public RefactoringSuggestion(String id, String title, String rationale,
                                 String beforeSnippet, String afterSnippet) {
        this.id = id;
        this.title = title;
        this.rationale = rationale;
        this.beforeSnippet = beforeSnippet;
        this.afterSnippet = afterSnippet;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getBeforeSnippet() { return beforeSnippet; }
    public void setBeforeSnippet(String beforeSnippet) { this.beforeSnippet = beforeSnippet; }

    public String getAfterSnippet() { return afterSnippet; }
    public void setAfterSnippet(String afterSnippet) { this.afterSnippet = afterSnippet; }
}
