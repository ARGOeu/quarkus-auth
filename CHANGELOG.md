# Changelog

---

All notable changes to this project will be documented in this file.

According to [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) , the `Unreleased` section serves the following purposes:

-   People can see what changes they might expect in upcoming releases.
-   At release time, you can move the `Unreleased` section changes into a new release version section.

## Types of changes

---

-   `Added` for new features.
-   `Changed` for changes in existing functionality.
-   `Removed` for now removed features.
-   `Fixed` for any bug fixes.
-   `Security` in case of vulnerabilities.
-   `Deprecated` for soon-to-be removed features.


## Unreleased
---

## 1.0.0 - 2025-06-25

This initial release delivers the foundation of the Quarkus Auth Extension, providing a complete solution for securing APIs through RCIAM authentication, Group Management authorization, user administration, role management, and dynamic endpoint protection.

The Quarkus Auth Extension is a Quarkus extension designed to provide complete authentication and authorization capabilities for Quarkus REST APIs. It centralizes API security by handling user authentication, role management, and endpoint access control, allowing development teams to secure their APIs with minimal effort.

Authentication is performed through the RCIAM platform, while authorization is managed through the RCIAM Group Management service. Both services are based on Keycloak, ensuring a secure and standards-compliant identity and access management solution.

### Key Features

#### Authentication
- Full integration with RCIAM for user authentication.  
- Secure validation of authenticated users and access tokens.
- Seamless adoption within Quarkus-based APIs.

#### Authorization
- Role-based access control (RBAC) powered by the RCIAM Group Management service.
- Dynamic authorization rules based on user roles and permissions.
- Centralized management of access rights across API resources.

#### User Management

The extension provides built-in capabilities for managing authenticated users, including:

- User registration in the Group Management service.
- Assignment of roles to users.
- Removal of roles from users.
- Retrieval of all available roles.
- Retrieval of all registered users.
- Access to user profiles.
- Visibility of role assignments for each user.

#### Dynamic Endpoint Protection

One of the core features of the extension is the ability to dynamically secure API endpoints.

Administrators can:

- Protect all available API endpoints without implementing custom authorization logic.
- Define which roles are allowed to access specific endpoints.
- Update endpoint access permissions dynamically through role-to-endpoint mappings.
- Enforce fine-grained access control across the entire API.