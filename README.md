# Quarkus OIDC Authentication & Authorization Using User Entitlements

## 1. Summary

**quarkus-auth** is a library that provides a unified authentication and authorization model for APIs built on OpenID Connect (OIDC).
It uses a pair of OIDC clients — a public client and a confidential client — enabling frontend applications to perform secure authentication flows, while the backend API validates access tokens through token introspection using a client secret.

The library requires basic OIDC configuration (client ID, client secret, token/introspection endpoints) and integrates with any OIDC-compliant Identity Provider, such as Keycloak.

For authorization, the library leverages user entitlements, which are retrieved during token introspection. By configuring the entitlement namespace and the hierarchical parent-group structure, the library enables fine-grained access control based on roles and organizational grouping.

In summary, quarkus-auth provides:

- Secure OIDC authentication using paired public + confidential clients.
- Token introspection via the confidential client.
- Authorization based on entitlements with support for nested group hierarchy.
- Flexible configuration compatible with any OIDC provider.
- Simple integration with Quarkus-based APIs.

---

## 2. Prerequisites

Before integrating and using the authorization library within the API, the following prerequisites must be met to ensure a fully functional and secure authorization flow.

### 2.1 OIDC Provider With Entitlement Support

A fully functional OpenID Connect (OIDC) provider is required.  Τhe provider must support:

- Issuance of access tokens to public and confidential clients.
- Token introspection via a confidential client.
- Inclusion of entitlements in the introspection response when the entitlements scope is requested.

### 2.2 Required OAuth2 Scope

The application interacting with the public client must request the entitlements scope during the OAuth2 authorization flow. Without this scope:

- Entitlements will not be returned by the OIDC provider.
- The authorization library will be unable to evaluate user permissions.
- All entitlement-based authorization checks will fail.

### 2.3 Required Configuration Parameters

The library must be configured to understand which entitlement namespace and parent-group hierarchy apply to the API.

The following parameters must be set:

```
api.auth.entitlements.parent-group=parent-group-name
api.auth.entitlements.namespace=urn:mace:grnet.gr:einfra:login-devel
api.auth.entitlements.super-admin-role=super_admin
```

These settings define:

- The entitlement namespace that the library should match.
- The root parent group, which may include multiple hierarchical subgroups.
- Defines the role name assigned to the parent group that grants super admin privileges for the API. Any user belonging to a group with this role is treated as a super admin and is given full administrative access.

### 2.4 Consistent Entitlement Structure

All entitlements must follow the expected naming and structural convention:

```
<namespace>:<parent-group>[/<subgroup>...]:role=<role-name>
```

This hierarchical model allows entitlements to be associated with:

- The parent group directly.
- Any nested subgroup.
- Arbitrarily deep group structures.

The authorization library resolves entitlements across all subgroup levels under the configured parent-group root.

### 2.5 Quarkus Application Environment

The library requires:

- A Quarkus-based application.
- Configuration via application.properties or environment variables.
- Java 17+.

## 3. Authentication Model: Public and Confidential Client Pair

### 3.1 Overview

Our library uses an authentication model based on a pair of OIDC clients: a public client and a confidential client secured with a client secret. 
The public client is responsible for requesting access tokens from the Identity Provider. 
Since it is public, it does not store or use a client secret and is intended for environments such as browser applications. 
Once the public client initiates authentication, it obtains an access token.

### 3.2 Token Request and Validation

When the public client retrieves an access token and sends it to the API. The API performs a verification step through token introspection. 
This introspection is carried out by the second client in the pair, the confidential client, which possesses a client secret and is treated as a trusted backend application.

The API forwards the received token to the Identity Provider’s introspection endpoint while authenticating as the confidential client using its client ID and client secret. 
The Identity Provider responds with information describing whether the token is active, along with its subject, expiration timestamp, scopes and any other relevant attributes. 
Using this information, the API decides whether access should be granted. If the token has expired or is marked as inactive, the API denies the request.

### 3.3 Requirements for API Integration

In order to integrate with our API, you must create this pair of clients in the Identity Provider. 
The public client handles obtaining access tokens, and the confidential client performs introspection and authorization checks on behalf of the backend.
Both clients together establish the authentication mechanism required for accessing the protected API endpoints.

We use Keycloak internally, but you may use any OIDC-compliant provider. Regardless of the IdP, you must first create a pair of OIDC clients: a public client and a confidential client

#### 3.3.1 Creating a Public Client in Keycloak

To create a public client in Keycloak, a user must configure a client that does not use a client secret and supports flows typically intended for applications that cannot safely store secrets. 

A user begins by navigating to the “Clients” area of the Keycloak administration console and choosing to create a new client. 
The client must be set to the OpenID Connect type, and the access type should be configured as public.
Since public clients cannot store secrets, Keycloak automatically disables client authentication for them. Redirect URIs and Web Origins must also be configured, to ensure that the client can properly complete authentication flows. 
Once these settings follow the recommended instructions, the public client is ready to request tokens from Keycloak.

#### 3.3.2 Creating a Confidential (Client Secret) Client in Keycloak

A confidential client in Keycloak is designed to run on the server side and securely store a client secret. 

A user starts by creating a new client and selecting OpenID Connect as the client protocol. 
The access type must be set to confidential, which activates client authentication and enables the generation and use of a client secret. 
Within the client’s settings, the administrator can review or regenerate the client secret under the “Credentials” tab.
Once these settings follow the recommended instructions, the confidential client can be used by backend services to validate tokens through introspection.


### 3.4 Using Client Secret Authentication with Our API

Our API uses OpenID Connect (OIDC) authentication based on the Client Credentials Flow as described above. To access protected endpoints, you must authenticate using:

- Client ID
- Client Secret
- The base URL of the OpenID Connect (OIDC) server
- Token Endpoint of your Identity Provider (IdP)
- The relative path or absolute URL of the OpenID Connect (OIDC) authorization endpoint
- Relative path or absolute URL of the OIDC RFC7662 introspection endpoint

We use Keycloak internally, but you may use any OIDC-compliant provider.

#### 3.4.1 Required `application.properties` Configuration

These properties **must** be added to make authentication work.

---

```properties
quarkus.oidc.client-id=backend-service
quarkus.oidc.credentials.secret=secret
quarkus.oidc.auth-server-url=https://login-devel.einfra.grnet.gr/auth/realms/einfra
quarkus.oidc.token-path=/protocol/openid-connect/token
quarkus.oidc.authorization-path=/protocol/openid-connect/auth
quarkus.oidc.introspection-path=/protocol/openid-connect/token/introspect
```
---

### 3.5 Using the Public Client to Obtain an Access Token

Applications that cannot securely store secrets, such as browser-based applications, must use the public client to obtain an access token from Keycloak. 
The public client is intended for environments where the application runs on the end user’s device and cannot protect confidential credentials. 
To authenticate, the application initiates the appropriate OIDC flow, typically the Authorization Code flow, and requests an access token using the public client configuration.

The applications is responsible for specifying the scopes required for the access token. Different APIs may require different scopes depending on the information they need to access during token introspection. 
For example, our API typically expects scopes such as `voperson_id`, `entitlements`, and `profile` in order to retrieve the relevant user information.
However, the exact set of scopes should be determined by the application according to the specific API endpoints it intends to call. 
Once the application receives the access token with the appropriate scopes, it must include the token in the `Authorization` header using `Bearer Authentication` when making requests to the API. 
The API then performs token introspection and uses the scopes and other token attributes to evaluate access permissions for the requested operation.

---

## 4. Authorization Model Based on User Entitlements

### 4.1 Overview

Our API uses an authorization model that relies on the user’s entitlements. Each user may have one or more entitlements that represent the permissions or roles assigned to them. 
When a client sends a request with an access token, the API performs token introspection using the confidential client to validate the token and retrieve the user’s entitlements. 
These entitlements are then evaluated to determine whether the user is allowed to access the requested resource or perform the requested action. 
Access to API endpoints is granted only if the user’s entitlements meet the requirements defined for the resource. 
This model allows fine-grained authorization and ensures that each user can only access the resources for which they have explicit entitlements, providing a secure and flexible way to manage access control across the API.

### 4.2 Required Scope for Entitlement Retrieval

For the authorization process to be executed correctly, the application interacting with the public client must request the `entitlements` scope.
Without this scope, the library cannot retrieve the user’s entitlements during token introspection, and therefore the library will be unable to evaluate whether the user is authorized to access the requested resources.

### 4.3 Entitlement Structure & Required Configuration Parameters

To ensure that the authorization library can correctly interpret and validate user entitlements, it must know which entitlements are relevant for the authorization process.
Each entitlement follows a predefined hierarchical structure of the form:

```<namespace>:<parent-group>:role=<role-name>```

- `namespace`: Identifies the broader domain or authority under which the entitlements are issued.

- `parent-group`: Defines the organizational scope or group hierarchy in which the entitlement is applicable.

- `role-name`: The specific role assigned to the user within the namespace and group context.

To allow the library to correctly filter and evaluate only the entitlements that belong to the authorization model of this API, the following configuration parameters must be set:

---
```properties
api.auth.entitlements.parent-group=parent-group-name
api.auth.entitlements.namespace=urn:mace:grnet.gr:einfra:login-devel
api.auth.entitlements.super-admin-role=super_admin
```
---
These parameters instruct the authorization library to inspect only the entitlements that match the configured namespace and the parent group hierarchy.


### 4.4 Assigning Entitlements to a User

Entitlements determine what resources and operations a user is allowed to access through the API. To ensure that the quarkus-auth library can perform authorization checks, the appropriate entitlements must be assigned to each user through your Identity Provider.

#### 4.4.1 Using RCIAM Group Management

In our environment, entitlements are managed through **RCIAM Group Management**, which automatically generates entitlements based on the user’s membership in groups and subgroups.
More information is available in the [official RCIAM documentation](https://docs.google.com/document/d/1qOWSx3yp0pul5_3-YGxwnG2I4WCqppJrTZVtB-1yLA4/edit?tab=t.0#heading=h.gjra7in8t9p7).

Whenever a user is added to a group, the corresponding entitlement is issued following the standardized structure:

```
<namespace>:<parent-group>/optional/subgroups:role=<role-name>
```
These entitlements will then be included in the user’s OIDC attributes and retrieved through token introspection.

#### 4.4.2 Assigning Entitlements via OIDC User Attributes

If you are not using RCIAM or wish to assign entitlements manually, you can add them directly to the user’s OIDC attributes through your Identity Provider (e.g., Keycloak).
Most IdPs allow configuring a custom attribute (entitlements) which can hold one or more values following the library’s required format.

Once these attributes are set, the **quarkus-auth** library will automatically read and evaluate them during token introspection, applying your authorization rules accordingly.

---

## 5. Dynamic Authorization Endpoints

This module provides dynamic authorization management for REST endpoints based on RCIAM group membership. It allows you to define secured endpoints declaratively using annotations, expose them dynamically, and map them to user groups and roles stored in a database.

### 5.1 Secured Endpoint Annotation

---
```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SecuredEndpoint {

    String resource();
    String description();
}
```
---

- resource: The resource type (e.g., TENANT).
- description: Human-readable description of the endpoint.

These annotations define the potential secured endpoints that the system will recognize.

#### 5.1.1 Dynamic Endpoint Discovery

A Quarkus extension scans the application at build time for all @SecuredEndpoint annotations. It then exposes a dynamic endpoint at:


```bash
GET /secured-endpoints
```

This endpoint returns a JSON list of all potential secured endpoints:


The library exposes the annotation:

```
@CheckEntitlements 
```

You can use it in these forms:

---

### Super Admin Only

```java
@CheckEntitlements(requireSuperAdmin = true)
public class AdminEndpoint {    }
```

Users must hold the `super_admin` entitlement assigned to members of your parent group.

---

### Check Specific Group + Role

```java
@CheckEntitlements(group = "groupName", role = "roleName")
public class StatusEndpoint {    }
```

The user must:

1. Be in the specified **group**
2. Have the specified **role** inside that group

---

## 6. Example Full Usage

```java
@Path("/v1/admin")
@Authenticated
@CheckEntitlements(requireSuperAdmin = true)
public class AdminEndpoint {
}
```
---

## 7. Install the JAR locally

You must download the latest build of the **quarkus-auth** library.

---

### **Option A — Download from GitHub Actions**
Open the Actions page:

🔗 **https://github.com/ARGOeu/quarkus-auth/actions**

1. Select the latest workflow run on **branch `devel`**
2. Scroll down to **Artifacts**
3. Download the archive (e.g. `quarkus-auth.zip`)
4. Extract it and locate the file: `quarkus-auth-1.0.0-SNAPSHOT.jar`
---

### **Option B — Download via GitHub CLI**

```bash
gh run download \
$(gh run list --repo ARGOeu/quarkus-auth --branch devel --limit 1 --json databaseId -q ".[0].databaseId") \
--repo ARGOeu/quarkus-auth \
--dir .
```
---


### After downloading the JAR

Install it into your local Maven repository:


```bash
mvn install:install-file    
-Dfile=<path-to-file>    
-DgroupId=org.grnet    
-DartifactId=quarkus-auth    
-Dversion=1.0.0-SNAPSHOT    
-Dpackaging=jar    
-DgeneratePom=true
```
This will place the library under your local Maven directory:
```
~/.m2/repository/org/grnet/quarkus-auth/1.0.0-SNAPSHOT/
```

---

## 8. Add the dependency to your project

In your **api** module's `pom.xml`:

```xml
<dependency>
    <groupId>org.grnet</groupId>
    <artifactId>quarkus-auth</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
---
## 9. Summary

To use `quarkus-auth`, you must:

1. Install the `.jar` using `mvn install:install-file`
2. Add the dependency to `pom.xml`
3. Include all required properties in `application.properties`
4. Use `@CheckEntitlements(...)` where access control is needed

Your Quarkus project is now fully integrated with central entitlement-based authorization.