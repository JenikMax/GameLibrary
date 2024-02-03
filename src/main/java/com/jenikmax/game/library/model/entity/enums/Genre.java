package com.jenikmax.game.library.model.entity.enums;

public enum Genre {
    RPG("rpg"),
    ACTION("action"),
    ADVENTURE("adventure"),
    JRPG("jrpg");

    private String name;

    Genre(String name){
        this.name = name;
    }

    public String getName(){ return name;}
}
