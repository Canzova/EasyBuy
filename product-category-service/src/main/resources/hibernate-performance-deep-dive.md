# Hibernate's Dirty Secrets: N+1 Queries, Lazy Loading, and How to Actually Fix Them

*A developer's honest notes on the performance traps that Hibernate sets — and the mental models that finally made everything click.*

---

There's a specific kind of frustration that comes with Hibernate. Your code looks perfectly clean. Your entities are mapped correctly. Everything works in local testing. Then you go to production, flip on SQL logging, and watch in horror as 500 queries fire off for what you thought was a single database call.

This happened to me. These notes are my attempt to make sense of it — starting from the basics of how Hibernate stores objects, through the treacherous world of lazy loading, and finally arriving at the real solutions that work in production.

---

## First, a Quick Word on How Hibernate Stores Large Data

Before we get to the interesting stuff, let's talk about something that comes up when you first start modeling entities: what do you do with large data fields?

When you store a large object — a long string, a file, a binary payload — Hibernate (and your database) needs a special column type for it. In JPA, you signal this with the `@Lob` annotation:

```java
@Lob
@Column(name = "description", columnDefinition = "CLOB")
private String description;

@Lob
private byte[] profileImage; // stored as BLOB
```

**CLOB** (Character Large Object) is for text — think product descriptions, HTML content, or documents. Its storage is effectively unbounded, unlike a `VARCHAR` which maxes out at 255 characters.

**BLOB** (Binary Large Object) is for binary data — images, videos, PDFs. This is what you'd use if you're storing files directly in the database (though whether *you should* is a whole other conversation).

The key insight: `@Lob` changes how Hibernate maps the field to the underlying column type, and some drivers handle LOB loading differently (sometimes lazily by default). Worth knowing, but let's move on to the problems that *actually* hurt you in production.

---

## The N+1 Problem: A Trap Hiding in Plain Sight

Let me set up the classic scenario. You have an e-commerce app with `Product` and `Review` entities:

```java
@Entity
public class Product {
    @Id
    private Long id;
    private String name;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<Review> reviews;
}
```

Now say you want to display a product list page that shows each product along with its reviews. You write something like:

```java
List<Product> products = productRepo.findAll(); // Query 1: fetch N products

for (Product p : products) {
    sout(p.getReviews()); // N more queries — one per product
}
```

What just happened? Hibernate fired **1 query** to fetch all products, then **N additional queries** — one for each product's reviews. That's **N+1 queries total**.

If you have 100 products, that's 101 database round trips. If you have 1000? You do the math.

```
SELECT * FROM product;                          -- Query 1
SELECT * FROM review WHERE product_id = 1;      -- Query 2
SELECT * FROM review WHERE product_id = 2;      -- Query 3
SELECT * FROM review WHERE product_id = 3;      -- Query 4
...                                             -- up to N+1
```

### Why Does This Happen?

Because of **lazy loading**. By default on `@OneToMany`, Hibernate marks the `reviews` collection as a *proxy* — a placeholder that doesn't actually hit the database until you touch it. The moment you call `p.getReviews()` inside that loop, Hibernate issues a fresh query.

This is what Hibernate is *actually* doing internally: it fires the parent query, returns your objects, but holds back the child collections. Each object gets a lazy proxy in place of the real collection. The second you try to use that proxy — boom, another query.

This design isn't stupid. It actually makes sense as a default. Loading 1000 reviews when you're displaying a list of products and only want their names would be wasteful. The problem is when you *do* need the child data and don't realize the consequence.

---

## Lazy vs. Eager: What's the Difference, Really?

The `FetchType` annotation controls when Hibernate loads associated data.

| FetchType | When data is loaded | Default for |
|-----------|---------------------|-------------|
| `LAZY` | Only when you access the field | `@OneToMany`, `@ManyToMany` |
| `EAGER` | Immediately, with the parent query | `@ManyToOne`, `@OneToOne` |

### The Mental Model

Think of `LAZY` like a lazy employee: they won't do anything until you specifically ask them. `EAGER` is the overenthusiastic one: they bring everything to your desk whether you asked for it or not.

Neither is inherently wrong. `LAZY` is great for performance when you don't always need child data. `EAGER` is convenient but dangerous if the association is large.

### What Happens With EAGER?

If you set `FetchType.EAGER` on your reviews:

```java
@OneToMany(mappedBy = "product", fetch = FetchType.EAGER)
private List<Review> reviews;
```

Now every time Hibernate loads a `Product`, it *also* loads all reviews — even if you only needed the product's name. It's overhead you didn't ask for.

Worse: if your `Product` also has `@Eager` categories, you're loading categories *and* reviews every time. The database is doing work on every query that most of your callers don't need.

**The production rule of thumb:** Always make relationships `LAZY`. Load data explicitly when you need it.

---

## A Critical Gotcha: Lazy Loading Needs a Transaction

Here's something that caught me off guard. With `FetchType.LAZY`, the child collection can only be fetched while you're **inside a transaction**. 

```java
// This works fine — inside a @Transactional method
@Transactional
public void doSomething() {
    Product p = productRepo.findById(1L).get();
    p.getReviews(); // Works — transaction is still open
}

// This will blow up — no transaction
public void doSomethingElse() {
    Product p = productRepo.findById(1L).get(); // transaction closes here
    p.getReviews(); // LazyInitializationException!
}
```

If you try to access a lazy collection after the transaction has closed, you'll get `LazyInitializationException`. This is Hibernate telling you: "I can't go to the database right now — the session is closed."

---

## Solving N+1: The Three Real Approaches

Okay, so you know the problem. Let's talk about actual solutions, because this is where the real nuance lives.

### Solution 1: JPQL with JOIN FETCH

The most direct fix is to tell Hibernate explicitly: "When you fetch products, join and load reviews *in the same query*."

```java
@Query("SELECT p FROM Product p JOIN FETCH p.reviews")
List<Product> findAllWithReviews();
```

This generates a single SQL JOIN:

```sql
SELECT p.*, r.*
FROM product p
INNER JOIN review r ON r.product_id = p.id
```

One query. Done.

But there's an important distinction between `JOIN` and `JOIN FETCH`:

- **JOIN** — joins the two tables in SQL, but does **not** load the related object into memory. You can filter by it, but Hibernate doesn't hydrate the collection.
- **JOIN FETCH** — joins the tables AND loads the related collection into the in-memory entity graph.

If you use plain `JOIN` and then try to access `p.getReviews()`, you're back to N+1. The key is `JOIN FETCH`.

### The Cartesian Product Problem (And Why JOIN FETCH Gets Tricky)

Here's where it gets thorny. When you JOIN FETCH on a collection, the result set has a *cartesian product* structure:

| product_id | product_name | review_id | review_text |
|------------|-------------|-----------|-------------|
| 1 | iPhone | 101 | Great! |
| 1 | iPhone | 102 | Love it |
| 2 | MacBook | 103 | Too expensive |
| 2 | MacBook | 104 | But worth it |

The product row is duplicated for each review. Hibernate resolves this and gives you the correct entity objects — but the result set at the database level is larger than necessary.

This becomes a serious problem **with pagination**. 

If you have `LIMIT 10` in your query (for page 1 of 10 products), the database applies that limit *before* Hibernate can consolidate the cartesian rows. So Hibernate might get 10 rows, but those could represent only 3 distinct products (each with multiple reviews). You'd get 3 products, not 10.

Hibernate detects this mismatch. It logs a warning:

```
HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory!
```

What Hibernate does instead: it fetches **all** the data from the database (ignoring your LIMIT), then applies pagination in memory. This works correctly but loads your entire dataset into the JVM heap — which on a large table is a memory disaster.

```
Without Pagination:
  Hibernate fetches Products → consolidates in memory → gives you correct results ✓

With Pagination + JOIN FETCH:
  Hibernate can't apply SQL LIMIT correctly → loads EVERYTHING in memory → applies LIMIT
  → Correct results but TERRIBLE performance ✗
```

This is the cartesian product + pagination trap. Don't fall into it.

### Solution 2: Batch Fetching

This is the approach that often doesn't get talked about enough, and it's genuinely elegant.

The idea: instead of fetching one collection per entity (N queries), Hibernate collects the IDs of all entities it needs to load children for, then fires a **single batched query**.

```java
@Entity
public class Product {
    @OneToMany(mappedBy = "product")
    @BatchSize(size = 16) // collect 16 IDs and fire one query
    private List<Review> reviews;
}
```

Here's what happens internally:

1. You fetch 100 products (1 query)
2. You start iterating and access `product.getReviews()`
3. Instead of immediately firing a query for just that product, Hibernate stores the ID
4. When it has collected 16 IDs, it fires: `SELECT * FROM review WHERE product_id IN (1, 2, 3, ..., 16)`
5. Then the next 16, and so on

So instead of 101 queries, you get roughly `1 + ceil(100 / 16) = 8` queries. That's a dramatic improvement with almost zero code change.

**Two ways to configure batch fetching:**

**Per-collection** — annotate directly on the field:
```java
@BatchSize(size = 16)
private List<Review> reviews;
```

**Global** — set it in your application properties and it applies everywhere:
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=25
```

The global config is particularly powerful because it applies to all lazy associations without you having to remember to annotate every one.

> **Always use batch fetching for `@ManyToMany` or `@OneToMany` relationships.** No exceptions in production.

### Solution 3: EntityGraph

`@EntityGraph` is a JPA standard way to specify what to eagerly fetch *per query*, without permanently changing your fetch type annotations.

```java
@EntityGraph(attributePaths = {"reviews"})
List<Product> findAll();
```

This tells Hibernate: "For this specific query only, eagerly fetch the reviews collection." It's like `JOIN FETCH` but cleaner, and it works with Spring Data JPA's repository layer nicely.

You can also define named entity graphs on the entity class:

```java
@Entity
@NamedEntityGraph(
    name = "product-with-reviews",
    attributeNodes = @NamedAttributeNode("reviews")
)
public class Product { ... }
```

And reference them by name in your repository.

The important caveat: `@EntityGraph` on a `@OneToMany` or `@ManyToMany` association also creates a cartesian product. So the same pagination problem applies — use it carefully when paging.

---

## Fetch Type Defaults by Relationship Type

Your notes have a clean summary of how Hibernate sets defaults based on relationship type — and what you should change them to.

| Mapping Type | Default FetchType | Should be |
|---|---|---|
| `@OneToOne` | EAGER | LAZY (if one side) |
| `@ManyToOne` | EAGER | LAZY |
| `@OneToMany` | LAZY | Stay LAZY, use batch or join fetch explicitly |
| `@ManyToMany` | LAZY | Stay LAZY, always use `@BatchSize` |

The pattern in the right column:
- **One on the right-hand side** → EAGER by default → change to LAZY
- **Many on the right-hand side** → LAZY by default → keep it LAZY

**Best practices (the ones worth tattooing on your wrist):**

1. **Always set `FetchType.LAZY` on all relationships.** Even `@ManyToOne` and `@OneToOne`.
2. **Load what you need explicitly** — using JPQL JOIN FETCH, EntityGraph, or batch fetching.
3. **Never mix JOIN FETCH with pagination.** Use batch fetching or subqueries instead.

---

## The Cartesian Product Problem with Multiple Collections

Things get hairier when your entity has *multiple* collections. Say `Product` has both `reviews` and `categories`:

```java
@Entity
public class Product {
    @OneToMany private List<Review> reviews;
    @OneToMany private List<Category> categories;
}
```

If you try to JOIN FETCH both:

```java
SELECT p FROM Product p JOIN FETCH p.reviews JOIN FETCH p.categories
```

Hibernate throws a `MultipleBagFetchException`. You can't JOIN FETCH multiple bags (unordered collections) in one query.

Even if it worked, the cartesian product would be out of control:

```
product 1 → 2 reviews, 2 categories = 4 rows (2 × 2)
product 2 → 3 reviews, 3 categories = 9 rows (3 × 3)
```

The row count explodes multiplicatively. The larger the collections, the worse this gets.

**What happens in practice:**

```
Product
  List<Category>  ─────┐
  List<Review>    ──────┼──► cartesian plane generated
                        │
                  errors / exception if not handled
```

The fix: use `Set` instead of `List` (Hibernate can handle two `Set` collections), or use separate queries per collection, or use `@BatchSize`.

You can also use DTO projections to select only what you need, avoiding the entity graph entirely.

---

## DTO Projections: The Often-Overlooked Solution

Sometimes the real fix isn't about *how* you fetch the entity — it's about *whether* you need the entity at all.

If you're building a product list page and only need `id`, `name`, and `price`, why hydrate the full entity with all its associations?

```java
// Instead of this:
List<Product> products = productRepo.findAll();

// Do this:
@Query("SELECT new com.app.dto.ProductSummary(p.id, p.name, p.price) FROM Product p")
List<ProductSummary> findProductSummaries();
```

You get exactly the data you need, no unnecessary joins, no lazy loading surprises. For read-heavy list views and APIs, this is often the cleanest solution.

---

## Putting It All Together: A Decision Framework

When you're writing a query that involves associations, ask yourself:

```
Do I need the associated data?
│
├── NO  → Keep LAZY. Don't load it.
│
└── YES → How many rows might there be?
           │
           ├── FEW (one-to-one / many-to-one)
           │    → JOIN FETCH or EntityGraph is fine
           │
           └── MANY (one-to-many / many-to-many)
                │
                ├── Am I paginating?
                │    YES → Use @BatchSize (global or per-collection)
                │          Do NOT use JOIN FETCH
                │
                └── No pagination?
                     → JOIN FETCH or EntityGraph is acceptable
                       Watch out for multiple collections (cartesian product)
```

---

## A Note on the Many-to-One / One-to-One Case

For `@ManyToOne` and `@OneToOne`, JOIN FETCH and EntityGraph don't create cartesian products because you're joining to a *single* entity, not a collection. The SQL result set doesn't duplicate rows.

So for these relationships, JOIN FETCH is completely safe even with pagination:

```java
// Safe to paginate with JOIN FETCH for @ManyToOne
@Query("SELECT p FROM Product p JOIN FETCH p.category")
Page<Product> findAllWithCategory(Pageable pageable);
```

The cartesian problem only appears when the "many" side is on the right — i.e., you're fetching a collection.

---

## Production Summary: What Actually Matters

Here's the condensed version you'd want on a sticky note on your monitor:

**The Core Rules:**

- `FetchType.LAZY` on everything. Always.
- Never call lazy collections outside a transaction.
- Never use JOIN FETCH with pagination on `@OneToMany`/`@ManyToMany`.
- Use `@BatchSize(size=16)` or the global `hibernate.default_batch_fetch_size=25` for collections.
- Use JOIN FETCH or EntityGraph for `@ManyToOne`/`@OneToOne` when you need the data.
- Use JPQL projections or DTOs for read-heavy endpoints.
- Enable SQL logging in development and actually *look* at it.

**The Hierarchy of Solutions (from safest to most powerful):**

| Problem | Recommended Fix |
|---|---|
| N+1 on a collection with pagination | `@BatchSize` or global batch size |
| N+1 on a collection without pagination | `JOIN FETCH` or `@EntityGraph` |
| N+1 on `@ManyToOne` | `JOIN FETCH` (safe, no cartesian issue) |
| Need only partial data | DTO projection with JPQL |
| Multiple collections needed | Separate queries + `@BatchSize` |

---

## Key Takeaways

Hibernate is doing a lot of work you don't see. The abstraction is powerful, but it hides real database behavior that matters enormously at scale.

The N+1 problem is almost always caused by one thing: touching a lazy collection in a loop. Once you can spot that pattern, you can fix it.

The cartesian product problem is subtler — it creeps in when you try to be clever with JOIN FETCH on collections, especially with pagination. The fix (batch fetching) is actually simpler than the thing it replaces.

And the biggest mental shift: stop thinking about what Hibernate *can* do, and start thinking about what SQL it's *actually generating*. Enable `spring.jpa.show-sql=true` in development. Read the logs. Count the queries. Your future self will thank you.

---

*These notes were written while genuinely working through Hibernate performance issues in a Spring Boot e-commerce project. The pain is real. The solutions are battle-tested.*
