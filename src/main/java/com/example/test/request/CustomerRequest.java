package com.example.test.request;

public class CustomerRequest {

    private String name;

    private String phone;

    private Integer age;

    // ACTIVE, BANNED (mặc định ACTIVE)
    private String status;

    // NORMAL, VIP (mặc định NORMAL)
    private String level;

    // Số đơn đã đặt (mặc định 0)
    private Integer orderCount;

    public CustomerRequest() {
    }

    public CustomerRequest(String name, String phone, Integer age) {
        this.name = name;
        this.phone = phone;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }
}
