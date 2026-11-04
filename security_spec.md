# Security Specification — Where'd It Go?

## 1. Data Invariants
1. **Strict Multi-Tenant Isolation**: `/users/{userId}`, `/users/{userId}/memories/{memoryId}`, and `/users/{userId}/borrowed_items/{borrowId}` can only be read, listed, created, updated, or deleted by `request.auth.uid == userId`.
2. **PII Protection**: User emails in `/users/{userId}` are never readable by other users. Invite code lookup `/space_codes/{inviteCode}` contains zero PII and disables `list` queries (`allow list: if false;`) to prevent code scraping.
3. **Shared Space Membership Gate**: `/shared_spaces/{spaceId}` can only be read/listed by users whose `uid` is in `memberUids` or whose verified email (`email_verified == true`) is in `invitedEmails`. Shared memories under `/shared_spaces/{spaceId}/memories/{memoryId}` require parent space membership on write and denormalized `request.auth.uid in resource.data.memberUids` on `list`.
4. **Temporal & Identity Immutability**: `createdAt`, `userId`, and `ownerId` are immutable across all updates (`incoming().createdAt == existing().createdAt`). Timestamps must satisfy `is timestamp && <= request.time`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/memories").get()` -> `PERMISSION_DENIED`
2. **Cross-User Memory Theft**: `bobDb.doc("users/alice/memories/m1").get()` -> `PERMISSION_DENIED`
3. **Cross-User Profile PII Read**: `bobDb.doc("users/alice").get()` -> `PERMISSION_DENIED`
4. **Shadow Field Injection on Memory Create**: `{ itemName: "Keys", location: "Desk", isAdmin: true }` -> `PERMISSION_DENIED` (`hasOnly` rejection)
5. **Identity Spoofing on Memory Create**: Alice writes to `users/alice/memories/m1` with `userId: "bob"` -> `PERMISSION_DENIED`
6. **Immutable `createdAt` Tampering on Update**: Alice updates `createdAt` on an existing memory -> `PERMISSION_DENIED`
7. **Value Poisoning on Update**: Alice updates `itemName` to an empty string `""` or 5000-character string -> `PERMISSION_DENIED`
8. **Future Timestamp Spoofing**: Alice creates a memory with `createdAt` set to year 2099 -> `PERMISSION_DENIED`
9. **Unauthorized Shared Space Access**: Bob (not in `memberUids` or `invitedEmails`) tries to read `/shared_spaces/space1` -> `PERMISSION_DENIED`
10. **Invite Code Scraping (`list`)**: Authenticated user calls `db.collection("space_codes").get()` -> `PERMISSION_DENIED`
11. **Unverified Email Spoofing on Shared Space**: Attacker with `email_verified: false` attempts to read a shared space via `invitedEmails` -> `PERMISSION_DENIED`
12. **Orphaned Shared Memory Write**: Member attempts to create a memory in `/shared_spaces/non_existent_space/memories/m1` -> `PERMISSION_DENIED` (`exists()` parent gate)
