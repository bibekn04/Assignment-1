package edu.sezg534.codereview.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ReviewResponse {
    @JsonProperty("intent")
    private Intent intent;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("overall_quality")
    private Quality overallQuality;

    @JsonProperty("findings")
    private List<Finding> findings = new ArrayList<>();

    @JsonProperty("refactoring_suggestions")
    private List<RefactoringSuggestion> refactoringSuggestions = new ArrayList<>();

    @JsonProperty("clarification_needed")
    private boolean clarificationNeeded = false;

    @JsonProperty("clarification_message")
    private String clarificationMessage;

    public ReviewResponse() {}

    public static ReviewResponse clarificationRequired(Intent intent, String message) {
        ReviewResponse resp = new ReviewResponse();
        resp.setIntent(intent != null ? intent : Intent.REVIEW);
        resp.setSummary("Clarification required before code review can proceed.");
        resp.setOverallQuality(Quality.POOR);
        resp.setClarificationNeeded(true);
        resp.setClarificationMessage(message);
        return resp;
    }

    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Quality getOverallQuality() { return overallQuality; }
    public void setOverallQuality(Quality overallQuality) { this.overallQuality = overallQuality; }

    public List<Finding> getFindings() { return findings; }
    public void setFindings(List<Finding> findings) {
        this.findings = findings != null ? findings : new ArrayList<>();
    }

    public List<RefactoringSuggestion> getRefactoringSuggestions() { return refactoringSuggestions; }
    public void setRefactoringSuggestions(List<RefactoringSuggestion> refactoringSuggestions) {
        this.refactoringSuggestions = refactoringSuggestions != null ? refactoringSuggestions : new ArrayList<>();
    }

    public boolean isClarificationNeeded() { return clarificationNeeded; }
    public void setClarificationNeeded(boolean clarificationNeeded) {
        this.clarificationNeeded = clarificationNeeded;
    }

    public String getClarificationMessage() { return clarificationMessage; }
    public void setClarificationMessage(String clarificationMessage) {
        this.clarificationMessage = clarificationMessage;
    }
}
