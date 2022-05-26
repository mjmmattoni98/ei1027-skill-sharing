package com.aams.skillsharing.model;

import lombok.Data;

@Data
public class Skill implements Comparable<Skill> {
    private String name;
    private String description;
    private SkillLevel level;
    private boolean canceled;

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
