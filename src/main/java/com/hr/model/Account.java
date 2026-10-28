package com.hr.model;

public class Account {
    private String username;
    private Role role;
    private String employeeId;

    public Account(String username, Role role, String employeeId) {
        this.username = username;
        this.role = role;
        this.employeeId = employeeId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
