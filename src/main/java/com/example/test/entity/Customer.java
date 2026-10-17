
package com.example.test.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "age")
    private Integer age;

    // ACTIVE, BANNED
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    // NORMAL, VIP
    @Column(name = "level", nullable = false)
    private String level = "NORMAL";

    @Column(name = "order_count", nullable = false)
    private Integer orderCount = 0;

    public Customer() {
    }

    public Customer(
            Long id,
            String name,
            String phone,
            Integer age,
            String status,
            String level,
            Integer orderCount) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.age = age;
        this.status = status;
        this.level = level;
        this.orderCount = orderCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
