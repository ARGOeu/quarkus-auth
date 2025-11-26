# QUARKUS-AUTH

## Integration Guide

This document explains how to **install**, **configure**, and **use** the `quarkus-auth` library inside any Quarkus project.

---

## 1. Install the JAR locally

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

## 2. Add the dependency to your project

In your **api** module's `pom.xml`:

```xml
<dependency>
    <groupId>org.grnet</groupId>
    <artifactId>quarkus-auth</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
---

## 3. Required `application.properties` Configuration

These properties **must** be added to make entitlement checks work.

---
### **OIDC Authentication**

```properties
quarkus.oidc.client-id=backend-service
quarkus.oidc.credentials.secret=secret
quarkus.oidc.authorization-path=/protocol/openid-connect/auth
quarkus.oidc.token-path=/protocol/openid-connect/token
quarkus.oidc.discovery-enabled=false
quarkus.oidc.introspection-path=/protocol/openid-connect/token/introspect
```
---
### **Entitlement Configuration**
These properties define the **parent group** and the **namespace** under which user entitlements are issued in the JWT token:

```properties
quarkus.auth.entitlements.parent.group=parent-group-name
quarkus.auth.entitlements.namespace=urn:mace:grnet.gr:einfra:login-devel
```
---
### **Custom User ID Claim**

The unique user identifier extracted from the token:

```properties
api.oidc.user.unique.id=voperson_id
```

---

## 4. Using Entitlement Annotations

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

## 5. Example Full Usage

```java
@Path("/v1/admin")
@Authenticated
@CheckEntitlements(requireSuperAdmin = true)
public class AdminEndpoint {
}
```
---

## 6. Summary

To use `quarkus-auth`, you must:

1. Install the `.jar` using `mvn install:install-file`
2. Add the dependency to `pom.xml`
3. Include all required properties in `application.properties`
4. Use `@CheckEntitlements(...)` where access control is needed

Your Quarkus project is now fully integrated with central entitlement-based authorization.