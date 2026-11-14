package edu.sezg534.codereview.llm;

import edu.sezg534.codereview.model.ReviewRequest;

public interface LlmClient {
    /**
     * Executes the LLM completion against the provided review request.
     * @param request Input code review request
     * @return Raw JSON response string from the model
     * @throws Exception If communication or processing fails
     */
    String generateReview(ReviewRequest request) throws Exception;

    /**
     * Indicates whether this client is backed by a live external API.
     */
    boolean isLiveApi();
}
