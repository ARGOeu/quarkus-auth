---
id: resource-definitions
title: Resource Definitions
---

# 9. Resource Definitions

Resources must implement the `ApiResource` interface.

---

## Example

```java
public enum BookResource implements ApiResource {

   BOOK;

   @Override
   public String resourceName() {
       return "book";
   }
}
```

---

## Purpose

Resource definitions provide a standardized way for the authorization library to identify and work with resource types.

In this example:

- `BookResource` defines the **BOOK** resource type.
- `resourceName()` returns the resource identifier used by the authorization system.
- The resource type can then be referenced by security annotations such as `@ParamRef`.

---

## Usage with `@ParamRef`

```java
@ParamRef(
    param = "bookId",
    type = ParamType.PATH,
    referTo = BookResource.class
)
```

The `referTo` attribute tells the library that the parameter represents a resource of type `BOOK`.

---

## Requirements

Resource types used by `@ParamRef` must:

- Be defined as an `enum`
- Implement the `ApiResource` interface
- Provide a valid implementation of `resourceName()`

---

## Summary

Every resource type in the authorization model must be represented by an enum implementing `ApiResource`.

This allows the library to consistently map request parameters to resource types and evaluate entitlements during authorization.
