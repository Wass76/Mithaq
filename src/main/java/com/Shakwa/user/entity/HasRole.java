package com.Shakwa.user.entity;

/**
 * Interface for entities that have a role
 * Used for polymorphism between User, Citizen, and Employee
 */
public interface HasRole {
    Role getRole();
    void setRole(Role role);
}



