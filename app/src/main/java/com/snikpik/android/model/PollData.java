package com.snikpik.android.model;

public class PollData {
    private String answer;
    private String answerId;
    private String timestamp;
    private String device_id;

    public PollData(){
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp(){
        return timestamp;
    }

    public String getAnswer() {
        return answer;
    }

    public String getAnswerPath(){
        return answerId;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getDevice_id(){
        return device_id;
    }
}
