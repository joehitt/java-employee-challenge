package com.example.rqchallenge.employees;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Employee domain/model class.
 */
public class Employee {

    private int id;
    private String name;
    private int salary;
    private int age;
    private String profileImage;

    /**
     * Default constructor provided for JSON deserialization and ease of use.
     */
    @SuppressWarnings("unused")
    public Employee() {
    }

    public Employee(int id,
                    String name,
                    int salary,
                    int age,
                    String profileImage) {
        setId(id);
        setName(name);
        setSalary(salary);
        setAge(age);
        setProfileImage(profileImage);
    }

    /**
     * ID of the employee, positive non-zero, integer and unique.
     * The value is also server-assigned.
     */
    @SuppressWarnings("unused")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty("employeeName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("employeeSalary")
    public int getSalary() {
        return salary;
    }

    public void setSalary(int salary) {
        this.salary = salary;
    }

    @JsonProperty("employeeAge")
    @SuppressWarnings("unused")
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Relative URL link to a profile image, server-assigned.
     */
    @SuppressWarnings("unused")
    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

}
