# Summary

The **Quarkus Auth** Library provides automatic endpoint authorization based on:

* Keycloak group membership
* User entitlements
* Protected resources
* Endpoint definitions

When a request is received, the library performs the following steps:

1. Identifies the requested resource.
2. Retrieves the user's entitlements from Keycloak.
3. Determines the required access.
4. Compares the required access against the user's entitlements.
5. Allows or denies the request before the endpoint executes.

---

For application developers, securing an endpoint is usually as simple as adding:

```java
@SecuredEndpoint(...)
```

and defining which resource the endpoint accesses.

