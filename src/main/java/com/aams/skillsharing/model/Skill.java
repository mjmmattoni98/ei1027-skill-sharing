package com.aams.skillsharing.model;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class Skill implements Comparable<Skill> {
    private String name;
    private String description;
    private SkillLevel level;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
    private LocalDate finishDate = null;

    public void setLevel(String level) {
        this.level = SkillLevel.fromId(level);
    }

    public String getLevel() {
        if (this.level == null)
            return null;
        return this.level.getId();
    }

    @Override
    public int compareTo(Skill o) {
        return this.getName().compareTo(o.getName());
    }

}
