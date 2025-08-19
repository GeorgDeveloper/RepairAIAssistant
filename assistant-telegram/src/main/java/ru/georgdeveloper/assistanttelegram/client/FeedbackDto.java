package ru.georgdeveloper.assistanttelegram.client;

public class FeedbackDto {
    public String request;
    public String response;

    public FeedbackDto() {}
    public FeedbackDto(String request, String response) {
        this.request = request;
        this.response = response;
    }
}
