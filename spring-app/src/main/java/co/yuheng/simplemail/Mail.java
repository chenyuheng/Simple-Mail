package co.yuheng.simplemail;

import com.google.gson.Gson;

public class Mail {
    private int id;
    private String from;
    private String to;
    private String subject;
    private String date;
    private String content;

    static Gson gson = new Gson();

    public Mail(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return this.from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return this.to;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return this.date;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return this.subject.replaceAll("\r\n", "<br>");
    }

    public void setContent(String content) {
        this.content = content.replaceAll("\r\n", "<br>");
    }

    public boolean hasBasicInfo() {
        return from != null && to != null && subject != null && date != null;
    }

    public boolean hasContent() {
        return content != null;
    }

    public String toString() {
        return gson.toJson(this);
    }
}