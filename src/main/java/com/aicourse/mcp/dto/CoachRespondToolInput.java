package com.aicourse.mcp.dto;

import com.aicoach.dto.AiCoachRequest;

import java.util.List;

public class CoachRespondToolInput {

    private Long courseId;
    private Long lessonId;
    private String message;
    private List<String> previousQuizQuestions;
    private List<AiCoachRequest.ChatMessage> chatHistory;

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getPreviousQuizQuestions() {
        return previousQuizQuestions;
    }

    public void setPreviousQuizQuestions(List<String> previousQuizQuestions) {
        this.previousQuizQuestions = previousQuizQuestions;
    }

    public List<AiCoachRequest.ChatMessage> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<AiCoachRequest.ChatMessage> chatHistory) {
        this.chatHistory = chatHistory;
    }

    public AiCoachRequest toCoachRequest() {
        AiCoachRequest request = new AiCoachRequest();
        request.setCourseId(courseId);
        request.setLessonId(lessonId);
        request.setMessage(message);
        request.setPreviousQuizQuestions(previousQuizQuestions);
        request.setChatHistory(chatHistory);
        return request;
    }
}

