package com.example.rqchallenge.employees;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

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

    /**
     * Helper method to parse singular Employee object from map of name-value pairs using the
     * 'output' naming convention ('employee_name', 'salary' etc.).
     *
     * @param input Map of name value pairs
     * @return Employee
     */
    public static Employee fromMapOutput(Map<String, Object> input) {
        Employee employee = new Employee();
        employee.setName((String) input.get("employee_name"));
        employee.setSalary((int) input.get("employee_salary"));
        employee.setAge((int) input.get("employee_age"));
        employee.setProfileImage((String) input.get("profile_image"));
        Object id = input.get("id");
        employee.setId(id == null ? 0 : Integer.parseInt(id + ""));
        return employee;
    }

    public static Employee fromMapOutputCreate(Map<String, Object> input) {
        Employee employee = new Employee();
        employee.setName((String) input.get("name"));
        employee.setSalary(toInt(input.get("salary")));
        employee.setAge((toInt(input.get("age"))));
        employee.setProfileImage((String) input.get("profile_image"));
        employee.setId(toInt(input.get("id")));
        return employee;
    }

    private static int toInt(Object obj) {
        if (obj == null) {
            throw new NumberFormatException("Could not parse null value to integer");
        } else {
            return Integer.parseInt(obj.toString());
        }
    }

}
