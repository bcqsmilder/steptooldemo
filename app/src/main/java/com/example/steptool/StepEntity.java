package com.example.steptool;

/**
 * Create by bcq on 2020/7/10
 * Email:352719965@qq.com
 */
 class StepEntity {
    private String curDate;//当天的日期
    private String steps; //当天的步数

    public String getCurDate() {
        return curDate;
    }

    public void setCurDate(String curDate) {
        this.curDate = curDate;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "StepEntity{" +
                "curDate='" + curDate + '\'' +
                ", steps='" + steps + '\'' +
                '}';
    }
}
