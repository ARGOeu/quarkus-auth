---
id: param-ref
title: Understanding @ParamRef
---

# 7. Understanding `@ParamRef`

`@ParamRef` tells the library where to find the resource identifier in an HTTP request.

---

## Example

```java
@ParamRef(
   param = "bookId",
   type = ParamType.PATH,
   referTo = BookResource.class
)
```

---

## Meaning

| Property | Meaning |
|----------|--------|
| `param` | The name of the request parameter |
| `type` | Where the parameter is located (PATH, QUERY, BODY, etc.) |
| `referTo` | The resource type it represents |

---

## Explanation

This annotation allows the authorization system to:

- Extract the correct identifier from the request
- Map it to a domain resource
- Build the correct entitlement for evaluation

---

## Summary

`@ParamRef` is the bridge between:

- HTTP request data  
- Resource model  
- Authorization engine  

It ensures the system knows exactly which **resource instance** is being accessed.
