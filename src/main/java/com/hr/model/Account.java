package com.hr.model;

import com.hr.model.enums.Role;

public class Account {
    private final String employeeId;
    private final Role role;
    private final boolean mustChangePassword;

    public Account(String employeeId, Role role, boolean mustChangePassword) {
        this.employeeId = employeeId;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public String getEmployeeId() { 
        return employeeId; 
    }

    public Role getRole()                 { 
        return role; 
    }
    
    public boolean isMustChangePassword() { return mustChangePassword; }
    public boolean isManager()            { return role == Role.MANAGER; }
}
