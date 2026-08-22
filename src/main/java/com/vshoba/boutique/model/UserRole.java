package com.vshoba.boutique.model;

/**
 * The roles a user can have in the boutique.
 * Stored as a STRING in the database ("CUSTOMER", "ADMIN"),
 * so it is easy to read and safe to change later.
 */
public enum UserRole {
    CUSTOMER,
    ADMIN
}
