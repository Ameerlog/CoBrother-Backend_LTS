package com.cobrother.web.Entity.feedback;

public class FeedbackRequest {
    private String feedbackType;
    private String message;
    private String pageUrl;

    public FeedbackRequest() {}

    public FeedbackRequest(String feedbackType, String message, String pageUrl) {
        this.feedbackType = feedbackType;
        this.message = message;
        this.pageUrl = pageUrl;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }
}
