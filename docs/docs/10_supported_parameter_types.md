---
id: supported-parameter-types
title: Supported Parameter Types
---

# 8. Supported Parameter Types

The library can extract resource identifiers from different parts of an HTTP request.

---

## PATH

Used when the resource identifier is part of the URL path.

### Example Request

```http
GET /books/BOOK-1
```

### Extracted Value

```text
BOOK-1
```

The value is read from the URL path.

---

## QUERY

Used when the resource identifier is provided as a query parameter.

### Example Request

```http
GET /books?bookId=BOOK-1
```

### Extracted Value

```text
BOOK-1
```

The value is read from the query parameters.

---

## BODY

Used when the resource identifier is contained in the request body.

### Example Request

```json
{
  "bookId": "BOOK-1"
}
```

### Extracted Value

```text
BOOK-1
```

The value is read from the request body.

---

## HEADER

Used when the resource identifier is provided in an HTTP header.

### Example Request

```http
X-Book-Id: BOOK-1
```

### Extracted Value

```text
BOOK-1
```

The value is read from the request header.

---

## Summary

| Parameter Type | Source |
|---------------|--------|
| `PATH` | URL path |
| `QUERY` | Query parameters |
| `BODY` | Request body |
| `HEADER` | HTTP headers |

Choose the parameter type that matches where the resource identifier is provided in the request.
