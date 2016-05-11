package com.oligon.bienentracker.object;

import java.io.Serializable;

public class Activities implements Serializable {

    private String other = "";
    private int honeyRoom, brood, drones, empty, food, middle, box, escape, fence, diaper;

    public Activities() {
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public int getHoneyRoom() {
        return honeyRoom;
    }

    public void setHoneyRoom(int honeyRoom) {
        this.honeyRoom = honeyRoom;
    }

    public int getBrood() {
        return brood;
    }

    public void setBrood(int brood) {
        this.brood = brood;
    }

    public int getDrones() {
        return drones;
    }

    public void setDrones(int drones) {
        this.drones = drones;
    }

    public int getEmpty() {
        return empty;
    }

    public void setEmpty(int empty) {
        this.empty = empty;
    }

    public int getFood() {
        return food;
    }

    public void setFood(int food) {
        this.food = food;
    }

    public int getMiddle() {
        return middle;
    }

    public void setMiddle(int middle) {
        this.middle = middle;
    }

    public int getBox() {
        return box;
    }

    public void setBox(int box) {
        this.box = box;
    }

    public int getEscape() {
        return escape;
    }

    public void setEscape(int escape) {
        this.escape = escape;
    }

    public int getFence() {
        return fence;
    }

    public void setFence(int fence) {
        this.fence = fence;
    }

    public int getDiaper() {
        return diaper;
    }

    public void setDiaper(int diaper) {
        this.diaper = diaper;
    }
}
