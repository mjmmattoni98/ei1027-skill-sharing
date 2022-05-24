package com.aams.skillsharing.dao;

import com.aams.skillsharing.model.Skill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SkillDao {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);
    }

    public void addSkill(Skill skill) throws DuplicateKeyException {
        jdbcTemplate.update("INSERT INTO skill VALUES (?,?,?::skill_level,?,?)",
                skill.getName(),
                skill.getDescription(),
                skill.getLevel(),
                skill.getStartDate(),
                skill.getFinishDate()
        );
    }

    public void deleteSkill(Skill skill) {
        jdbcTemplate.update("DELETE FROM skill WHERE name = ?",
                skill.getName()
        );
    }

    public void deleteSkill(String name){
        jdbcTemplate.update("DELETE FROM skill WHERE name = ?",
                name
        );
    }

    public void disableSkill(Skill skill) {
        jdbcTemplate.update("UPDATE skill SET finish_date = ? WHERE name = ?",
                LocalDate.now(),
                skill.getName()
        );
    }

    public void disableSkill(String name) {
        jdbcTemplate.update("UPDATE skill SET finish_date = ? WHERE name = ?",
                LocalDate.now(),
                name
        );
    }

    public void updateSkill(Skill skill) {
        jdbcTemplate.update("UPDATE skill SET description = ?, level = ?::skill_level, start_date = ?, finish_date = ? " +
                        "WHERE name = ?",
                skill.getDescription(),
                skill.getLevel(),
                skill.getStartDate(),
                skill.getFinishDate(),
                skill.getName()
        );
    }

    public Skill getSkill(String name){
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM skill WHERE name = ?",
                    new SkillRowMapper(),
                    name
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Skill> getSkills(){
        try {
            return jdbcTemplate.query("SELECT * FROM skill",
                    new SkillRowMapper()
            );
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    public List<Skill> getAvailableSkills () {
        try {
            return jdbcTemplate.query("select * from skill WHERE (current_date) >= start_date AND COALESCE((current_date <= finish_date),true)",
                    new SkillRowMapper()
            );
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }

    public List<Skill> getDisabledSkills () {
        try {
            return jdbcTemplate.query("select * from skill WHERE (current_date) < start_date OR COALESCE((current_date > finish_date),false)",
                    new SkillRowMapper()
            );
        } catch (EmptyResultDataAccessException e) {
            return new ArrayList<>();
        }
    }
}
