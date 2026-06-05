---

# 1. Why not use `@CreatedDate` and `@LastModifiedDate`?

Your entity currently uses:

```java
@PrePersist
void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
void onUpdate() {
    updatedAt = Instant.now();
}
```

instead of:

```java
@CreatedDate
private Instant createdAt;

@LastModifiedDate
private Instant updatedAt;
```

---

## What `@CreatedDate` and `@LastModifiedDate` need

These are **Spring Data JPA Auditing** annotations.

To use them properly, you must enable auditing:

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

And usually:

```java
@EntityListeners(AuditingEntityListener.class)
@Entity
public class InventoryItem {
}
```

Then:

```java
@CreatedDate
private Instant createdAt;

@LastModifiedDate
private Instant updatedAt;
```

are automatically populated.

---

## Why might someone choose `@PrePersist` instead?

### Simpler

No auditing configuration needed.

Everything is self-contained inside the entity.

```java
@Entity
public class InventoryItem {
    @PrePersist
    ...
}
```

Works immediately.

---

### Pure JPA

`@PrePersist` and `@PreUpdate` are JPA standards.

```java
@PrePersist
@PreUpdate
```

work with:

* Hibernate
* EclipseLink
* OpenJPA

whereas:

```java
@CreatedDate
@LastModifiedDate
```

are Spring Data specific.

---

### Small Projects

For a simple inventory service:

```java
createdAt
updatedAt
```

might be the only auditing requirement.

Using lifecycle callbacks is often enough.

---

# 2. What exactly are `@PrePersist` and `@PreUpdate`?

These are JPA lifecycle callbacks.

---

## `@PrePersist`

Runs before INSERT.

Example:

```java
InventoryItem item = new InventoryItem();

repository.save(item);
```

Flow:

```text
PrePersist
   ↓
INSERT
```

Your code:

```java
@PrePersist
void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
}
```

ensures timestamps exist before saving.

---

## `@PreUpdate`

Runs before UPDATE.

Example:

```java
item.setAvailableQuantity(100);

repository.save(item);
```

Flow:

```text
PreUpdate
   ↓
UPDATE
```

Your code:

```java
@PreUpdate
void onUpdate() {
    updatedAt = Instant.now();
}
```

updates the timestamp automatically.

---

# 3. Why use `uniqueConstraints`?

You have:

```java
@Table(
    name = "inventories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_inventory_sku",
            columnNames = "sku"
        ),
        @UniqueConstraint(
            name = "uk_inventory_product_id",
            columnNames = "productId"
        )
    }
)
```

This tells the database:

> No two rows may have the same SKU.

and

> No two rows may have the same Product ID.

Example:

### Valid

```text
SKU      ProductId
ABC123   UUID1
XYZ999   UUID2
```

### Invalid

```text
SKU      ProductId
ABC123   UUID1
ABC123   UUID2
```

Database rejects it.

---

# 4. Why not rely only on service validation?

You already have:

```java
repository.existsBySku(sku)
```

But that's not enough.

Imagine:

Thread A:

```text
existsBySku -> false
```

Thread B:

```text
existsBySku -> false
```

Both insert.

Now duplicates exist.

Database-level unique constraints prevent this race condition.

---

# 5. Difference Between `@UniqueConstraint` and `@Column(unique=true)`

You have both:

```java
@Column(unique = true)
private String sku;
```

and

```java
@UniqueConstraint(columnNames = "sku")
```

---

## `@Column(unique=true)`

Column-level shortcut.

```java
@Column(unique = true)
private String sku;
```

Hibernate usually generates:

```sql
UNIQUE(sku)
```

---

## `@UniqueConstraint`

Table-level definition.

```java
@Table(
   uniqueConstraints = {
      @UniqueConstraint(...)
   }
)
```

More flexible.

Can create composite constraints.

Example:

```java
@UniqueConstraint(
    columnNames = {
        "productId",
        "warehouseLocation"
    }
)
```

Meaning:

```text
Same product can exist in multiple warehouses

BUT

Not twice in same warehouse
```

This cannot be expressed using:

```java
@Column(unique=true)
```

---

## In your code

For SKU:

```java
@Column(unique = true)
```

and

```java
@UniqueConstraint(...)
```

are redundant.

One is enough.

Many teams prefer:

```java
@UniqueConstraint
```

because constraint names become explicit:

```sql
uk_inventory_sku
```

which helps debugging production errors.

---

# 6. Why not create indexes instead?

This is where many developers get confused.

---

## Unique Constraint

Guarantees correctness.

```sql
UNIQUE(sku)
```

Prevents duplicates.

---

## Index

Improves lookup speed.

```sql
INDEX(sku)
```

Allows duplicates.

Example:

```text
SKU
ABC123
ABC123
ABC123
```

Perfectly valid if only an index exists.

---

## Unique Index

Many databases implement unique constraints internally using a unique index.

Conceptually:

| Feature           | Prevent Duplicates | Speeds Queries |
| ----------------- | ------------------ | -------------- |
| Index             | ❌                  | ✅              |
| Unique Constraint | ✅                  | Usually ✅      |
| Unique Index      | ✅                  | ✅              |

---

# 7. Should `productId` and `sku` be indexed?

Yes.

Since you frequently query:

```java
findBySku(...)
findByProductId(...)
findByProductIdForUpdate(...)
```

indexes are beneficial.

However, because they are unique columns:

```java
UNIQUE(sku)
UNIQUE(productId)
```

most databases automatically create unique indexes behind the scenes.

So you often get indexing automatically.

---

# Quick Summary

### `@PrePersist`

Runs before INSERT.

```java
@PrePersist
void onCreate()
```

Used to initialize timestamps/default values.

---

### `@PreUpdate`

Runs before UPDATE.

```java
@PreUpdate
void onUpdate()
```

Used to update timestamps.

---

### `@CreatedDate` / `@LastModifiedDate`

Spring Data auditing alternative.

Requires:

```java
@EnableJpaAuditing
```

and auditing configuration.

---

### `@UniqueConstraint`

Database rule:

```text
No duplicate values allowed
```

Protects data integrity.

---

### `@Column(unique=true)`

Simple shortcut for single-column uniqueness.

---

### Index

Improves search speed:

```java
findBySku(...)
findByProductId(...)
```

but does **not** prevent duplicates.

---

### In Your Entity

The `@UniqueConstraint` declarations and `@Column(unique = true)` on `sku` are effectively duplicating the same uniqueness rule. In a real project, I would usually keep the named `@UniqueConstraint` and remove `unique=true` for consistency and clearer database constraint names.
