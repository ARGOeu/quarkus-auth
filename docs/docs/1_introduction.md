---
id: introduction
title: Introduction
slug: /
---

# What is Quarkus Auth?

The **Quarkus Auth Library** provides resource-based authorization for REST APIs.

It allows developers to secure endpoints without writing authorization logic in every method.

Instead of manually checking permissions, the library:

- extracts the requested resource
- evaluates user roles
- validates entitlements from Keycloak
- decides access automatically

---

# Core Idea

Authorization is based on:
ROLE + RESOURCE_TYPE + RESOURCE_ID
This allows fine-grained access control per resource instance.
