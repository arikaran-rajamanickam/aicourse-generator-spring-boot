package com.sharing.dto;

public class QuizAttemptRequest {
    private Integer quizIndex;
    private Boolean correct;

    public QuizAttemptRequest() {
    }

    public Integer getQuizIndex() {
        return quizIndex;
    }

    public void setQuizIndex(Integer quizIndex) {
        this.quizIndex = quizIndex;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}

