package com.hr;

import com.hr.dao.EmployeeDAO;
import com.hr.model.Employee;
import com.hr.model.SalaryType;

import java.util.List;

public class TestDAO {

    public static void main(String[] args) {
        EmployeeDAO dao = new EmployeeDAO();
        List<Employee> list = dao.getAll();

        System.out.println("Số nhân viên: " + list.size());
        for (Employee e : list) {
            System.out.printf("%-8s | %-10s | %-8s | %-8s | %-10s | %-6s | %s%n",
                e.getEmployeeId(),
                e.getFullName(),
                e.getPosition(),
                e.getSalaryType(),
                formatSalary(e),
                e.getPhoneNumber(),
                e.getStatus());
        }
    }

    /** 10000000 FIXED -> 10tr/th, 25000 HOURLY -> 25k/h */
    private static String formatSalary(Employee e) {
        long rate = e.getSalaryRate().longValue();
        if (e.getSalaryType() == SalaryType.FIXED) {
            return (rate / 1_000_000.0) + "tr/th";
        }
        return (rate / 1000) + "k/h";
    }
}