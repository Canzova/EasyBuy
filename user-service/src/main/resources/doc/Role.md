This confusion is extremely common because Spring Security uses the words **role** and **authority** in a way that makes them seem identical, even though they're slightly different concepts.

## Think of it this way

### In your business/domain

You have roles:

```java
ADMIN
USER
GUEST
```

These answer:

> "What kind of user is this?"

Example:

```java
user.getRole() == Role.ADMIN
```

No `ROLE_` prefix is needed here.

---

### In Spring Security

Spring Security stores permissions as **GrantedAuthority** strings.

Example:

```java
new SimpleGrantedAuthority("ROLE_ADMIN")
```

or

```java
new SimpleGrantedAuthority("READ_PRODUCTS")
```

These answer:

> "What authorities does this user have?"

---

## Why does `ROLE_` exist?

Spring Security reserves the prefix `ROLE_` to distinguish roles from other authorities.

For example:

```text
ROLE_ADMIN
ROLE_USER

READ_PRODUCTS
WRITE_PRODUCTS
DELETE_PRODUCTS
```

Here:

```text
ROLE_ADMIN
```

is a role authority.

```text
READ_PRODUCTS
```

is a permission authority.

---

## What happens with `hasRole()`?

When you write:

```java
.hasRole("ADMIN")
```

Spring secretly converts it to:

```java
.hasAuthority("ROLE_ADMIN")
```

Internally:

```text
hasRole("ADMIN")
       ↓
checks for
ROLE_ADMIN
```

That's why your user must have:

```java
new SimpleGrantedAuthority("ROLE_ADMIN")
```

---

## What happens with `hasAuthority()`?

If you write:

```java
.hasAuthority("ROLE_ADMIN")
```

Spring checks exactly:

```text
ROLE_ADMIN
```

No conversion.

Similarly:

```java
.hasAuthority("READ_PRODUCTS")
```

checks exactly:

```text
READ_PRODUCTS
```

---

## Real-world example

Your enum:

```java
public enum Role {
    ADMIN,
    USER
}
```

Database:

| id | username | role  |
| -- | -------- | ----- |
| 1  | John     | ADMIN |

When user logs in:

```java
new SimpleGrantedAuthority(
    "ROLE_" + user.getRole().name()
)
```

creates:

```text
ROLE_ADMIN
```

Now Spring Security sees:

```text
Authorities:
[
  ROLE_ADMIN
]
```

Then:

```java
.hasRole("ADMIN")
```

works because Spring looks for:

```text
ROLE_ADMIN
```

---

## Where should you use each?

### Domain model / Database

Use:

```java
ADMIN
USER
GUEST
```

Example:

```java
user.getRole()
```

or

```java
Role.ADMIN
```

---

### GrantedAuthority

Use:

```java
ROLE_ADMIN
ROLE_USER
```

Example:

```java
new SimpleGrantedAuthority("ROLE_ADMIN")
```

---

### Security configuration

Usually:

```java
.hasRole("ADMIN")
```

NOT:

```java
.hasRole("ROLE_ADMIN") // Wrong
```

because Spring adds the prefix automatically.

---

## Quick rule to remember

### Business code

```java
ADMIN
USER
```

### Security authority strings

```java
ROLE_ADMIN
ROLE_USER
```

### Security checks

```java
hasRole("ADMIN")
```

Spring converts it to:

```text
ROLE_ADMIN
```

---

## One final mental model

Imagine a user:

```java
Role.ADMIN
```

When they log in:

```java
Role.ADMIN
      ↓
"ROLE_ADMIN"
      ↓
GrantedAuthority
      ↓
Spring Security
```

So:

* **`ADMIN`** = your application's role.
* **`ROLE_ADMIN`** = Spring Security's authority representation of that role.

They're related, but they live in different layers of the application.
