---
id: core-concepts
title: Core Concepts
---

# 2. Core Concepts

Before diving into the technical details, it’s important to understand the core concepts used by the library.

---

## Resource

A **resource** is anything that can be protected.

### Examples
- User  
- Book  
- Project  
- Organization  

For this documentation we will use:

### User
Represents a person using the system.

**Examples:**
- User: Alice  
- User: Bob  

### Book
Represents a book in the system.

**Examples:**
- Book: Clean Code  
- Book: Design Patterns  

---

## Role

A **role** describes what a user is allowed to do.

### Examples
- BOOK-ADMIN  
- BOOK-VIEWER  
- USER-ADMIN  

Think of a role as a job description.

For example:

**BOOK-ADMIN** means:

> "This user can manage books."

---

## Group

The library expects Keycloak groups to be organized by role.

### Example structure

- BOOK-ADMIN  
- BOOK-VIEWER  
- USER-ADMIN  

Each role group contains the resources that role applies to.

### Example
```text
BOOK-ADMIN
├── BOOK
    ├── BOOK-1
    ├── BOOK-2
    └── BOOK-3
```

Meaning:

Members of `BOOK-ADMIN` have administrator permissions for `BOOK-1`, `BOOK-2`, and `BOOK-3`.

---

## Entitlement

An entitlement is the combination of:

- Role  
- Resource  

### Examples

- BOOK-ADMIN / BOOK/ BOOK-1  
  → User is an administrator of BOOK-1  

- BOOK-VIEWER / BOOK/ BOOK-2  
  → User can view BOOK-2  

---

### Summary

Entitlements are what the library evaluates when authorizing requests.
```

