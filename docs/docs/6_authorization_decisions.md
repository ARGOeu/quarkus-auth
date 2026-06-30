---
id: authorization-decisions
title: Authorization Decisions
---


# 5. How Authorization Decisions Are Made

Every secured endpoint produces an authorization key.

### Example:
BOOK-ADMIN:BOOK:BOOK-1

The library compares:
<table>
  <tr>
    <th>Required Access</th>
    <th>User Entitlements</th>
  </tr>
  <tr>
    <td>
      BOOK-ADMIN:BOOK:BOOK-1
    </td>
    <td>
      BOOK-ADMIN:BOOK:BOOK-1<br/>
      BOOK-VIEWER:BOOK:BOOK-2
    </td>
  </tr>
</table>

If a match is found:

## ALLOW

Otherwise:

## DENY
