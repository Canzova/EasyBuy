The key thing is that **`jjwt-jackson` is not Jackson itself**. It's an **adapter module that lets JJWT use Jackson** for JSON serialization/deserialization.

When you use JJWT 0.12.x, there are typically three dependencies:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

### Why isn't Spring's Jackson enough?

Spring Boot gives you:

```xml
spring-boot-starter-web
```

which pulls in:

```text
jackson-databind
jackson-core
jackson-annotations
```

These libraries provide Jackson itself.

However, JJWT doesn't automatically know:

> "Use Jackson to convert JWT claims and headers to/from JSON."

To avoid forcing everyone to use Jackson, JJWT supports multiple JSON processors:

* Jackson (`jjwt-jackson`)
* Gson (`jjwt-gson`)
* Other implementations via serializers/deserializers

So the JJWT team split JSON support into separate modules.

---

### What does `jjwt-jackson` actually provide?

It contains classes such as:

```java
io.jsonwebtoken.jackson.io.JacksonSerializer
io.jsonwebtoken.jackson.io.JacksonDeserializer
```

and wiring so JJWT can do:

```java
Jwts.builder()
    .claims(claims)
    .compact();
```

Internally:

```text
Map<String, Object>
        ↓
JacksonSerializer
        ↓
JSON
        ↓
JWT Payload
```

and when parsing:

```text
JWT Payload
        ↓
JSON
        ↓
JacksonDeserializer
        ↓
Claims Map
```

---

### What happens if you remove `jjwt-jackson`?

You may see runtime errors like:

```text
Unable to find an implementation for Serializer
```

or

```text
Unable to find an implementation for Deserializer
```

because `jjwt-api` and `jjwt-impl` don't include a JSON implementation by default.

---

### Why didn't JJWT just depend on Jackson directly?

To keep the library flexible and lightweight.

Some projects use:

* Jackson
* Gson
* JSON-B
* Android environments

If JJWT hard-depended on Jackson, everyone would get Jackson whether they wanted it or not.

Instead:

```text
jjwt-api
    +
jjwt-impl
    +
choose one:
    jjwt-jackson
    jjwt-gson
```

---

### In a Spring Boot application

Even though Spring Boot already includes Jackson, you still need:

```xml
<artifactId>jjwt-jackson</artifactId>
```

because JJWT needs its Jackson integration module, not just the Jackson libraries themselves.

Think of it like:

```text
Spring Boot gives you:
    Jackson engine

jjwt-jackson gives JJWT:
    the adapter that knows how to drive that engine
```

That's why both are present in most Spring Boot + JJWT examples.
