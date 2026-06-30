---
id: keycloak-structure
title: Keycloak Structure
---

# Keycloak Structure

This section explains how the Quarkus Auth library uses **Keycloak groups** to represent authorization rules.

Before understanding the structure, it is important to understand what Keycloak is used for in this system.

---

# What is Keycloak (in this context)?

Keycloak is an identity and access management system.

In this library, we use Keycloak for two things:

## 1. User identity
Keycloak knows:
- Who the user is (Alice, Bob, etc.)

## 2. Group membership
Keycloak assigns users to **groups**, which represent permissions.

👉 In this library:
> Groups are used as a way to define what a user can access.

We do NOT store permissions in code.  
We store them in Keycloak groups.

---

# How authorization is represented

Instead of storing permissions like:

- "Alice can edit Book 1"

We represent permissions as a **group hierarchy**:
ROLE → RESOURCE TYPE → RESOURCE

This means:

- ROLE = what you can do
- RESOURCE TYPE = domain (Book, User, etc.)
- RESOURCE = specific item

---

# Recommended Keycloak Structure

Each role contains a resource type, and each resource type contains resources.
```text
BOOK-ADMIN
└── BOOK
	├── BOOK-1
	├── BOOK-2

BOOK-VIEWER
└── BOOK
	├── BOOK-1
	├── BOOK-2
	├── BOOK-3

USER-ADMIN
└── USER
	├── USER-1	
	├── USER-2


```
---

# What this means in Keycloak

This structure means:

## 1. Groups are hierarchical

A group can contain sub-groups.

Example:
- BOOK-ADMIN (role group)
  - BOOK (resource type)
    - BOOK-1 (specific resource)

---

## 2. Users are assigned to leaf nodes

A user is NOT assigned just to a role.

Instead, they are assigned to a **specific resource under a role**.

Example:
```text

BOOK-ADMIN
└── BOOK
	└── BOOK-1
		└── Alice

```
---

# How to read this

If Alice is inside:

BOOK-ADMIN → BOOK → BOOK-1

It means:

> Alice can perform BOOK-ADMIN actions on BOOK-1 only.

---

# Resulting Entitlement

From the structure above, the system generates:

BOOK-ADMIN / BOOK / BOOK-1

---

# Why this structure exists

This design allows:

## 1. Fine-grained control
Instead of:
- "user can manage all books"

We can define:
- "user can manage only BOOK-1"

---

## 2. Scalable permissions
Works for:
- Books
- Users
- Projects
- Any future domain

---

## 3. No hardcoded permissions
Everything is stored in Keycloak, not in code.

---

# Example (End-to-End)

## Step 1 — User assignment

Alice is assigned:
```text
BOOK-ADMIN
└── BOOK
    └── BOOK-1
```
---

## Step 2 — System reads it as entitlement
BOOK-ADMIN / BOOK / BOOK-1

---

## Step 3 — Request happens
GET /books/BOOK-1

---

## Step 4 — Authorization check

System checks:

- Does Alice have BOOK-ADMIN on BOOK-1?

✔ Yes → Access granted  
✖ No → Access denied

---

# Summary

Keycloak is used as:

- A user identity system
- A group-based permission system

In this library:

> Groups = Authorization structure  
> Nested groups = Role → Resource Type → Resource  
> Leaves = actual permissions  

