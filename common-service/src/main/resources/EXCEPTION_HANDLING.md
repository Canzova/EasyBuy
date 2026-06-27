# Centralized Exception Handling using Common Service

## The Problem (Before)

Every microservice had its own copy of the same exception handling code.

```
cart-order-service/
  └── exception/GlobalExceptionHandler.java   ← duplicate

user-service/
  └── exception/GlobalExceptionHandler.java   ← duplicate

product-category-service/
  └── exception/GlobalExceptionHandler.java   ← duplicate
```

If you wanted to add a new exception or change the error response format,
you had to update **every single service**. This is bad.

---

## The Solution (After)

Move everything into `common-service` once. Every service pulls it automatically.

```
common-service/
  └── exceptions/
        ├── GlobalExceptionHandler.java   ← one place, used by all
        ├── ExceptionResponse.java
        └── customException/
              ├── ResourceNotFoundException.java
              ├── ResourceAlreadyExistsException.java
              ├── EmailAlreadyExistsException.java
              ├── ResourceEmptyException.java
              ├── BusinessException.java
              └── ImageUploadFailedException.java
```

---

## How It Works — Step by Step

### Step 1 — common-service has all the exceptions and handler

**Custom Exceptions** (simple classes that extend RuntimeException):

```java
// Use this when something is not found in DB
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

```java
// Use this when you try to create something that already exists
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
```

**GlobalExceptionHandler** (catches all exceptions and returns proper HTTP responses):

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // When ResourceNotFoundException is thrown anywhere → return 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleResourceNotFoundException(...) {
        return buildResponse(e.getMessage(), 404, HttpStatus.NOT_FOUND, request);
    }

    // When ResourceAlreadyExistsException is thrown anywhere → return 409
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ExceptionResponse> handleResourceAlreadyExistsException(...) {
        return buildResponse(e.getMessage(), 409, HttpStatus.CONFLICT, request);
    }

    // ... and so on for all exceptions
}
```

**ExceptionResponse** (the standard error format returned to clients):

```json
{
  "message": "Cart not found for user 123",
  "errorCode": 404,
  "error": "NOT_FOUND",
  "path": "/api/cart/123",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Step 2 — Auto-Configuration magic

This is what makes `GlobalExceptionHandler` automatically available in ALL services
without any extra configuration.

We created this file in common-service:

```
common-service/
  └── src/main/resources/
        └── META-INF/
              └── spring/
                    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

The file contains just one line:
```
com.easybuy.common.exceptions.GlobalExceptionHandler
```

**Why `src/main/resources`?**
Maven copies everything inside `src/main/resources` into the root of the jar as-is.
So when common-service is built, the jar looks like this:

```
common-service.jar
  ├── com/easybuy/common/exceptions/GlobalExceptionHandler.class
  └── META-INF/spring/AutoConfiguration.imports   ← Spring Boot finds this here
```

Spring Boot reads `META-INF/spring/AutoConfiguration.imports` from every jar
on the classpath when the application starts. It then registers whatever beans
are listed in that file.

Think of it like a menu — Spring Boot asks every jar:
> "Do you have any beans you want me to register?"

common-service.jar replies:
> "Yes! Please register GlobalExceptionHandler"

---

### Step 3 — Services just add common-service as a dependency

In each microservice's `pom.xml`:

```xml
<dependency>
    <groupId>com.easybuy</groupId>
    <artifactId>common-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

That's it. Nothing else needed.

---

## How a Service Uses a Custom Exception

In any microservice (e.g. cart-order-service), just import and throw:

```java
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import com.easybuy.common.exceptions.customException.ResourceEmptyException;

public OrderResponse checkout(UUID userId, CheckoutRequest request) {

    Cart cart = cartRepository.findByUserId(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

    if (cart.getCartItemList().isEmpty()) {
        throw new ResourceEmptyException("Cannot checkout, cart is empty");
    }

    // ...
}
```

When `ResourceNotFoundException` is thrown:
1. Spring catches it via `GlobalExceptionHandler`
2. Returns this response automatically:

```json
{
  "message": "Cart not found for user: abc-123",
  "errorCode": 404,
  "error": "NOT_FOUND",
  "path": "/api/cart/checkout",
  "timestamp": "2024-01-15T10:30:00"
}
```

No extra code needed in the service. The handler in common-service does it all.

---

## Exception Reference Table

| Exception | HTTP Status | When to Use |
|---|---|---|
| `ResourceNotFoundException` | 404 NOT FOUND | Entity not found in DB or external service |
| `ResourceAlreadyExistsException` | 409 CONFLICT | Duplicate entry (e.g. same product added twice) |
| `EmailAlreadyExistsException` | 409 CONFLICT | Email already registered during signup |
| `ResourceEmptyException` | 400 BAD REQUEST | Resource exists but is empty (e.g. empty cart on checkout) |
| `BusinessException` | 400 BAD REQUEST | Business rule violated (e.g. insufficient stock) |
| `ImageUploadFailedException` | 400 BAD REQUEST | Upload to external storage failed |

### Spring Built-in Exceptions (also handled automatically)

| Exception | HTTP Status | When It Occurs |
|---|---|---|
| `MethodArgumentNotValidException` | 400 BAD REQUEST | `@Valid` field validation failed — lists all failing fields |
| `MethodArgumentTypeMismatchException` | 400 BAD REQUEST | Wrong type in path/param (e.g. string passed where UUID expected) |
| `HttpMessageNotReadableException` | 400 BAD REQUEST | Request body is missing or JSON is malformed |
| `NoResourceFoundException` | 404 NOT FOUND | URL endpoint doesn't exist |
| `Exception` | 500 INTERNAL SERVER ERROR | Any unhandled exception — catch-all |

---

## How to Add a New Custom Exception

1. Create the exception class in common-service:

```java
// common-service/src/main/java/com/easybuy/common/exceptions/customException/PaymentFailedException.java
package com.easybuy.common.exceptions.customException;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String message) {
        super(message);
    }
}
```

2. Register it in `GlobalExceptionHandler` in common-service:

```java
@ExceptionHandler(PaymentFailedException.class)
public ResponseEntity<ExceptionResponse> handlePaymentFailedException(PaymentFailedException e, HttpServletRequest request) {
    return buildResponse(e.getMessage(), 402, HttpStatus.PAYMENT_REQUIRED, request);
}
```

3. Run `mvn install` on common-service.

4. Use it in any service:

```java
throw new PaymentFailedException("Payment declined for order: " + orderId);
```

No changes needed to any service's configuration.

---

## Why common-service is NOT an Executable Jar

common-service is a **library**, not an application. Libraries need to be plain jars
so other services can use them as dependencies.

By default, `spring-boot-maven-plugin` repackages jars into fat executable jars
which **cannot be used as dependencies**. To prevent this, common-service has:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <skip>true</skip>
    </configuration>
</plugin>
```

`skip=true` tells Maven: "build a plain jar, not an executable one".

---

## Why spring-web is `provided` scope in common-service

`GlobalExceptionHandler` uses `@RestControllerAdvice`, `ResponseEntity`, and
`HttpServletRequest` — all from `spring-web`. But every microservice already
has `spring-web` on its classpath.

If common-service declared `spring-web` with default scope, it would get bundled
twice — once from common-service and once from the service itself. Using `provided`
scope means:

> "I need this to compile, but don't bundle it — the consuming service will provide it at runtime."

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <scope>provided</scope>
</dependency>
```

---

## Summary

```
Before:  Each service had its own GlobalExceptionHandler (lots of duplication)

After:   common-service has ONE GlobalExceptionHandler
         META-INF/spring/AutoConfiguration.imports tells Spring Boot to register it
         Every service that depends on common-service gets it for free
         Services only need to throw the right exception — handling is automatic
```
