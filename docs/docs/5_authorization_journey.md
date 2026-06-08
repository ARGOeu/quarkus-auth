---
id: authorization-journey
title: Authorization Journey
---

# 4. Authorization Journey

The following steps describe what happens when a user gains access to a resource in the system.

---

## Step 1 – User Has No Access

A new user enters the system.

**User:** Alice  

**Current permissions:**
- None

---

## Step 2 – Administrator Grants Access

An administrator decides that Alice should manage a specific resource.

The administrator assigns:

- **Role:** BOOK-ADMIN  
- **Resource Type:** BOOK  
- **Resource:** BOOK-1  

---

## Step 3 – User Becomes Group Member in Keycloak

Keycloak membership becomes:
```text
BOOK-ADMIN
└── BOOK
	└── BOOK-1
		└── Alice
```		
		
---

## Step 4 – Entitlement Is Created

The system generates the following entitlement:
BOOK-ADMIN / BOOK / BOOK-1


This entitlement is now associated with **Alice**.

---

## Step 5 – User Calls an API

Alice sends a request:
GET /books/BOOK-1


---

## Step 6 – Authorization Library Evaluates Access

The library evaluates the request by comparing:

### Requested Resource
- BOOK-1

### Required Role
- BOOK-ADMIN

### User Entitlements
BOOK-ADMIN / BOOK / BOOK-1


---

## Step 7 – Access Granted

A matching entitlement exists.

### Result
✓ Access Granted


The endpoint executes successfully.

---

## Summary


The authorization flow is based on matching:
Role + Resource Type + Resource ID


from Keycloak groups to runtime API requests.

