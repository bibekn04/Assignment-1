package edu.sezg534.codereview.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Finding {
    @JsonProperty("id")
    private String id;

    @JsonProperty("severity")
    private Severity severity;

    @JsonProperty("category")
    private Category category;

    @JsonProperty("line_range")
    private String lineRange;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("recommendation")
    private String recommendation;

    public Finding() {}

    public Finding(String id, Severity severity, Category category, String lineRange,
                   String title, String description, String recommendation) {
        this.id = id;
        this.severity = severity;
        this.category = category;
        this.lineRange = lineRange;
        this.title = title;
        this.description = description;
        this.recommendation = recommendation;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getLineRange() { return lineRange; }
    public void setLineRange(String lineRange) { this.lineRange = lineRange; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
}
