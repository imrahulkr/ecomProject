package com.ecommerce.project.notification.email.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailRequest {
    private String to;
    private String from;
    private String subject;
    private String htmlBody;
    private String textBody;
    private List<EmailAttachment> attachments = new ArrayList<>();
    private Map<String,String> metadata = new HashMap<>();
    private EmailType emailType;
    private EmailPriority priority = EmailPriority.NORMAL;

    private EmailRequest(){}

    public static Builder builder(){ return new Builder(); }

    public static class Builder{
        private final EmailRequest emailRequest = new EmailRequest();
        public Builder to(String to){ emailRequest.to = to; return this; }
        public Builder from(String from){ emailRequest.from = from; return this; }
        public Builder subject(String subject){ emailRequest.subject = subject; return this; }
        public Builder htmlBody(String htmlBody){ emailRequest.htmlBody = htmlBody; return this; }
        public Builder textBody(String textBody){ emailRequest.textBody = textBody; return this; }
        public Builder attachments(List<EmailAttachment> attachments){ emailRequest.attachments = attachments; return this; }
        public Builder metadata(Map<String,String> metadata){ emailRequest.metadata = metadata; return this; }
        public Builder emailType(EmailType emailType){ emailRequest.emailType = emailType; return this; }
        public Builder priority(EmailPriority priority){ emailRequest.priority = priority; return this; }
        public EmailRequest build(){ return emailRequest; }
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public List<EmailAttachment> getAttachments() {
        return attachments;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public EmailType getEmailType() {
        return emailType;
    }

    public EmailPriority getPriority() {
        return priority;
    }
}
