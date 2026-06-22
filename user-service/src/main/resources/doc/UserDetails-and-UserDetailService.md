# Spring Security Core Interfaces: UserDetails and UserDetailsService

These two interfaces are the absolute foundation of Spring Security. You can think of them as the bridge between **your custom database** and **Spring Security’s internal engine**.

Spring Security is incredibly powerful, but it is entirely blind to your application's specific business logic. It doesn't know if your users are stored in PostgreSQL, MongoDB, or an active directory. It also doesn't know if your user table has fields like `firstName`, `email`, or `dateOfBirth`.

To make the framework work with your specific setup, you have to translate your data into a standard format Spring Security understands. That is exactly what these two interfaces do.

---

## 1. `UserDetails` (The "Standard ID Card")

### What is it?
`UserDetails` is an interface that represents a standard user in Spring Security. It forces a class to have specific methods that Spring needs to check during a login attempt, such as:
* `getUsername()`
* `getPassword()`
* `getAuthorities()` (Roles/Permissions)
* `isAccountNonExpired()`, `isAccountNonLocked()`, etc.

### Why do we make our custom user implement it?
You likely have an `@Entity` class called `User` that maps to your database. Spring Security cannot work directly with your `User` class because it doesn't know what methods or fields it has.

By making your `User` class implement `UserDetails`, you are telling Spring Security: *"Hey, treat this entity as an official security record."* When you implement the interface, you map your fields to Spring's requirements. For example, because the system relies on emails for authentication, you simply override the `getUsername()` method to return the user's `email` instead of a traditional username. When Spring asks for the "username" to build the JWT subject or verify the login, it seamlessly gets the email.

---

## 2. `UserDetailsService` (The "Database Locator")

### What is it?
`UserDetailsService` is a core interface that contains only one method:
`UserDetails loadUserByUsername(String username)`

### Why do we implement it?
When someone tries to log in, the `AuthenticationManager` steps in and says, *"I need to check if this person exists, and I need their hashed password to see if it matches what they just typed in."*

However, Spring Security doesn't know how to write a database query. By creating a class that implements `UserDetailsService`, you are teaching Spring Security exactly **how to fetch a user from your specific database**.

Inside your implementation, you typically do something like this:
1. Take the incoming string (the email).
2. Use your custom `UserRepository` to find the user in the database (`userRepository.findByEmail(email)`).
3. If they don't exist, throw a `UsernameNotFoundException`.
4. If they do exist, return that `User` object (which Spring can accept, because it implements `UserDetails`!).

---

## The TL;DR Analogy: The Nightclub Bouncer

Imagine Spring Security is a bouncer at a nightclub.

* **`UserDetailsService` is the Guest List:** The bouncer doesn't inherently know who is invited. You have to give the bouncer a mechanism to look people up. Your implementation is the instruction manual: *"Go to this specific database table and look up this email."*
* **`UserDetails` is the Standardized ID Card:** The bouncer only knows how to read one specific type of ID card. Your custom user entity might be a passport, a driver's license, or a student ID. By implementing `UserDetails`, you put your custom user data into the standard plastic sleeve that the bouncer knows exactly how to read.