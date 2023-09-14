package com.testmateback.dTestmate.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalDetails {
    private String indexes; // 추가
    private String goalSubject;
    private String goalSemester;
    private long totalGoals;
    private long checkedGoals;
    private

    // 추가
    String getIndexes() {
        return indexes;
    }
}
