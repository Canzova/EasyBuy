# 1. Relationship Between `PESSIMISTIC_WRITE` and `@Transactional`

A pessimistic write lock only makes sense inside a transaction.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<InventoryItem> findByIdForUpdate(Long id);
```

generates something similar to:

```sql
SELECT * FROM inventory_item
WHERE id = ?
FOR UPDATE;
```

When this query runs:

* The row is locked.
* Other transactions trying to update the same row must wait.
* The lock remains active until the transaction commits or rolls back.

Example:

```java
@Transactional
public void reserveStock(Long id) {
    InventoryItem item = repository.findByIdForUpdate(id);

    item.setAvailableQuantity(...);
    repository.save(item);
}
```

Flow:

```text
Transaction starts
    ↓
SELECT FOR UPDATE
    ↓
Row locked
    ↓
Update entity
    ↓
Commit
    ↓
Lock released
```

Without a transaction, the lock is ineffective or may fail.

---

# 2. Why `@Transactional(readOnly = true)` Is Used

Your service has:

```java
@Service
@Transactional
public class InventoryServiceImpl
```

This means **every method is transactional by default**.

For methods that only read data:

```java
getById()
getBySku()
getByProductId()
getAll()
getLowStock()
```

you override it with:

```java
@Transactional(readOnly = true)
```

because these methods:

* don't call `save()`
* don't update entities
* don't delete data

---

# 3. Benefits of `readOnly = true`

### Intent

Tells developers:

> This method should only read data.

---

### Hibernate Optimization

Hibernate skips some dirty-checking and flush-related work.

```java
@Transactional(readOnly = true)
public InventoryResponse getById(Long id)
```

is slightly more efficient than a normal read-write transaction.

---

### Prevents Accidental Updates

Bad code:

```java
@Transactional(readOnly = true)
public InventoryResponse getById(Long id) {
    InventoryItem item = findEntity(id);
    item.setAvailableQuantity(0); // accidental
    return toResponse(item);
}
```

The read-only transaction makes accidental writes less likely to be persisted.

---

# 4. Why Not Use `readOnly = true` With `PESSIMISTIC_WRITE`

These two ideas conflict:

```java
@Transactional(readOnly = true)
```

means:

> I only want to read.

while

```java
@Lock(PESSIMISTIC_WRITE)
```

means:

> I want a write-level lock because I may modify this row.

Therefore methods like:

```java
reserveStock()
releaseStock()
adjustStock()
```

should use a normal:

```java
@Transactional
```

not:

```java
@Transactional(readOnly = true)
```

---

# 5. How It Applies to Your Code

### Read-only methods

```java
getById()
getBySku()
getByProductId()
getAll()
getLowStock()
```

Use:

```java
@Transactional(readOnly = true)
```

because they only fetch data.

---

### Write methods

```java
create()
update()
adjustStock()
reserveStock()
releaseStock()
reserveStockByProductId()
releaseStockByProductId()
delete()
```

Use:

```java
@Transactional
```

because they:

* modify rows,
* call `save()`,
* call `delete()`,
* or use `PESSIMISTIC_WRITE` locks.

---

# One-Line Rule

**Use `@Transactional(readOnly = true)` for pure reads. Use normal `@Transactional` whenever data may be modified or a `PESSIMISTIC_WRITE` lock is acquired.**
