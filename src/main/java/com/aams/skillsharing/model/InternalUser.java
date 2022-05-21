package com.aams.skillsharing.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class InternalUser implements Serializable {
    private String username;
    private String password;
    private boolean isSkp;
    private int balanceHours;

    public String getUrlMainPage() {
        return "skill/list";
    }
}
