---
id: authorization-model
title: Authorization Model
---

# Authorization Model

The system uses a **two-layer authorization model**.

---

# Layer 1 — Endpoint Access (Role-based)

Roles define which endpoints a user can access.

Example roles:

- BOOK-ADMIN
- BOOK-VIEWER

If the user does not have the required role → request is rejected immediately.

---

# Layer 2 — Resource Access (Entitlement-based)

After role validation, the system checks **resource ownership**.

Example entitlement:
BOOK-ADMIN / BOOK / BOOK-1

Meaning:

- Role: BOOK-ADMIN
- Resource Type: BOOK
- Resource: BOOK-1

---

# Authorization Flow

1. Check role permission for endpoint  
2. Extract resource from request  
3. Build entitlement key  
4. Match against user entitlements  
5. Allow or deny request  

---

# Decision Rule

| Condition | Result |
|----------|--------|
| Missing role | Denied |
| Missing entitlement | Denied |
| Match found | Granted |
