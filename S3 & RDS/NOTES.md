# 📝 Project Notes

---

## 1. What is `@RequestPart` in Spring Boot?

### The simple idea

When a form sends data to your backend, it can send two very different types of things at the same time:
- **Text fields** — like a username, email, password (plain text)
- **Files** — like a profile picture (binary data)

Normal `@RequestBody` can only handle one thing — either JSON text OR a file, not both together.

`@RequestPart` solves this. It lets you receive **multiple parts in a single request**, where each part can be a different type — some text, some files.

This type of request is called `multipart/form-data`. Think of it like a package with multiple compartments — one compartment holds the user details, another holds the image file.

### Where it is used in this project

In `UserController.java`, the `createUser` endpoint receives both user data and a profile picture in one request:

```java
@PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<UserResultDTO> createUser(
    @RequestPart UserRequestDTO userRequestDTO,   // ← the text/JSON part
    @RequestPart(name = "images") MultipartFile images  // ← the file part
) {
    ...
}
```

### What each annotation means

| Annotation | What it does |
|------------|--------------|
| `@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)` | Tells Spring this endpoint expects a `multipart/form-data` request, not plain JSON |
| `@RequestPart UserRequestDTO userRequestDTO` | Grab the part named `"userRequestDTO"` from the request and deserialize it as a JSON object into `UserRequestDTO` |
| `@RequestPart(name = "images") MultipartFile images` | Grab the part named `"images"` from the request and treat it as an uploaded file |

### How the frontend sends this

On the frontend, a `FormData` object is used to build the multipart request:

```javascript
const formData = new FormData();
formData.append('userRequestDTO', new Blob([JSON.stringify(userData)], { type: 'application/json' }));
formData.append('images', imageFile);  // the actual file

fetch('/user/create', { method: 'POST', body: formData });
```

Notice `userRequestDTO` is wrapped in a `Blob` with `type: application/json`. This is important — without it, Spring doesn't know to deserialize it as JSON and will throw a 400 error.

### `@RequestPart` vs `@RequestParam` vs `@RequestBody`

| Annotation | Use when... |
|------------|-------------|
| `@RequestBody` | The entire request body is JSON. No files. |
| `@RequestParam` | Simple key=value pairs from a form or URL query string. Good for plain text fields, not objects or files. |
| `@RequestPart` | The request is `multipart/form-data` and you need to receive a mix of JSON objects and/or files together. |

---

## 2. How CORS Works and How to Enable it in Spring Boot

### What is CORS?

**CORS = Cross-Origin Resource Sharing**

A browser has a security rule: **a webpage can only talk to the same server it was loaded from**.

For example:
- Your HTML page is served from `http://localhost:8080`
- Your API is also at `http://localhost:8080`
- ✅ Same origin — browser allows it

But if your frontend was hosted on `http://localhost:3000` and your backend on `http://localhost:8080`:
- ❌ Different origin — browser **blocks** the request by default

This block happens **in the browser only**. Tools like Postman or curl are not browsers, so they don't care about CORS. That's why your API works fine in Postman but fails in the browser.

### How does CORS work technically?

Before making a real request (like POST or PUT), the browser sends a **preflight request** — an `OPTIONS` request — asking the server:

> "Hey, I'm a page from `http://localhost:3000`. Are you okay with me sending a POST request to you?"

The server must reply with headers saying:

> "Yes, I allow requests from `http://localhost:3000`."

If the server doesn't reply correctly, the browser cancels the real request and shows a CORS error.

### How to enable CORS in Spring Boot

In this project, CORS is configured in `src/main/java/com/s3/app/Config/CorsConfig.java`:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")           // apply to ALL endpoints
            .allowedOrigins("*")             // allow requests from any origin
            .allowedMethods("GET", "POST", "PUT", "DELETE")  // allowed HTTP methods
            .allowedHeaders("*");            // allow any headers
    }
}
```

**What each line means:**

| Line | What it does |
|------|-------------|
| `addMapping("/**")` | Apply this CORS rule to every URL in your app |
| `allowedOrigins("*")` | Accept requests from any website/origin. In production, replace `*` with your actual frontend URL e.g. `"https://myapp.netlify.app"` |
| `allowedMethods(...)` | Which HTTP methods are allowed. Always include the ones your frontend uses |
| `allowedHeaders("*")` | Accept any request headers (like `Content-Type`, `Authorization`) |

> ⚠️ Using `allowedOrigins("*")` is fine for development and learning projects.
> In a real production app, always specify the exact origin instead of `*`.

---

## 3. Why the S3 Bucket Needs Its Own CORS Configuration

### Wait, didn't we already configure CORS in Spring Boot?

Yes — but that only covers requests going **to your Spring Boot backend**.

Remember from section 1: during multipart upload, the browser **talks directly to S3** (bypasses your backend entirely). S3 is a completely separate server owned by AWS. Your Spring Boot CORS config has zero effect on S3.

So S3 also needs to be told: *"Hey, it's okay for browsers to send PUT requests to you directly."*

That's what the S3 CORS config does.

### The config explained line by line

Go to your S3 bucket → **Permissions** tab → **Cross-origin resource sharing (CORS)** → paste this:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT"],
    "AllowedOrigins": ["http://localhost:8080"],
    "ExposeHeaders": ["ETag"]
  }
]
```

| Field | What it means |
|-------|--------------|
| `AllowedHeaders` | S3 will accept requests with any headers from the browser. `"*"` means all headers are fine |
| `AllowedMethods` | Only `PUT` is needed here because the browser PUTs file chunks directly to S3 via the presigned URL |
| `AllowedOrigins` | Only allow requests coming from `http://localhost:8080` (your local app). Change this to your deployed frontend URL when you go live |
| `ExposeHeaders` | This is the critical one — explained below |

### Why is `ExposeHeaders: ["ETag"]` so important?

By default, browsers hide most response headers from JavaScript for security reasons.
When the browser PUTs a chunk to S3, S3 responds with an `ETag` header.
But JavaScript **cannot read that header** unless S3 explicitly says "this header is safe to expose".

Without `"ExposeHeaders": ["ETag"]`:
```javascript
const etag = putRes.headers.get('ETag'); // returns NULL
```

With `"ExposeHeaders": ["ETag"]`:
```javascript
const etag = putRes.headers.get('ETag'); // returns "\"abc123def456\""  ✅
```

If ETag is null, the `complete` request to S3 fails because S3 can't identify which parts to assemble.
This is the most common reason multipart uploads silently fail in the browser.

### Summary: Two CORS configs, two different servers

```
Browser
  |
  |-- POST /api/multipart/initiate --> Spring Boot (port 8080)
  |                                    ↑ Spring Boot CorsConfig handles this
  |
  |-- PUT chunk ----------------------> S3 (amazonaws.com)
                                        ↑ S3 Bucket CORS config handles this
```

They are completely independent. You need both.
