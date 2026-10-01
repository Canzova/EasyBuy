# Password Reset — Step-by-Step Implementation & Setup Guide

This guide provides a crisp, step-by-step walkthrough of how the password reset system is implemented, explains the code design, clarifies why a `resetToken` is industry-standard for production, and explains how to configure and connect both **Resend** and **AWS SES**.

---

## 1. Deep Dive: Why Use a `resetToken`? (The "Claim Ticket" Architecture)

### The Core Problem: Why Can't We Do It Simpler?

When designing password reset flows, developers often wonder:
1. *Why not just send `(email, otp, newPassword)` in one single API endpoint?*
2. *Why not just have OTP verification set a flag `is_verified = true` in the DB for that email, and then let `/reset-password` take only `(email, newPassword)`?*

Both alternatives suffer from either severe UX failure or catastrophic security vulnerabilities. Here is why the industry-standard **`resetToken`** is required:

---

### Alternative A: One Single Endpoint `POST /reset-password {email, otp, newPassword}`

#### Why it fails in production:
1. **Broken Multi-Step UI/UX:**
   - Every consumer application (Amazon, Google, Netflix, Uber) guides users through a **3-screen wizard**:
     - **Screen 1:** User enters email &rarr; Clicks "Send Code".
     - **Screen 2:** User enters the 6-digit OTP &rarr; Clicks "Verify".
     - **Screen 3:** User enters the new password and confirms it &rarr; Clicks "Save Password".
   - If there is only one combined endpoint, the frontend **cannot validate whether the OTP was typed correctly on Screen 2**!
   - The user would be forced to enter their new password on Screen 2 without even knowing if the code in their inbox works. If they made a typo in the OTP, they'd have to re-enter both their new password and the code again.
2. **CPU Exhaustion / DoS Vulnerability:**
   - Password hashing with BCrypt is intentionally slow and CPU-heavy (taking ~80ms-150ms per hash).
   - If an attacker attempts to brute-force a 6-digit OTP against a combined endpoint, your server's CPU will be maxed out computing BCrypt hashes on every single failed OTP attempt!
   - Splitting verification into Step 2 checks the OTP cheaply in memory/database ($O(1)$) with zero BCrypt hashing overhead.

---

### Alternative B: Storing `is_verified = true` in the Database

Suppose Step 2 verifies the OTP and simply sets `is_verified = true` on the user record in PostgreSQL, and then Step 3 accepts `POST /reset-password {email, newPassword}`.

#### The "Sitting on Screen 3" Account Takeover Attack:
```
Attacker                                      Legitimate User (Alice)
   |                                                    |
   |                                     1. Alice requests OTP & enters it.
   |                                     2. Server checks OTP: Valid!
   |                                     3. Server sets DB: is_verified = true for alice@example.com
   |                                     4. Alice's browser navigates to Screen 3:
   |                                        "Enter new password..."
   |                                        (Alice takes 2 minutes thinking or is distracted)
   |                                                    |
5. Attacker knows Alice's email address.                |
   Attacker sends:                                      |
   POST /reset-password                                 |
   {"email": "alice@example.com", "newPassword": "pwned"}
   |                                                    |
6. Server checks DB:                                    |
   "Is is_verified == true for alice@example.com?"      |
   Yes! (Because Alice just verified it!)               |
7. Server hashes "pwned" and overwrites Alice's DB!     |
   ===> ACCOUNT HIJACKED WITHOUT ATTACKER EVER SEEING THE OTP! <===
```

**The Fatal Flaw of `is_verified`:**
Storing a boolean flag `is_verified = true` in the DB proves *that the email was verified by someone*, but **it DOES NOT PROVE that the caller on Screen 3 is the same person/client who verified the OTP on Screen 2!**

---

### The Solution: The `resetToken` as an Unguessable "Claim Ticket"

#### Real-World Analogy: The Coat Check / Valet Parking
- When you hand your car to a valet or your jacket to a coat check, the attendant does **not** pin a note on the wall saying *"A blue jacket is ready to be collected"*. If they did that, any stranger walking by could claim it!
- Instead, the attendant hands **you** a unique, physical claim ticket stub (e.g. `#83921`).
- When returning to pick up your car or jacket, you **must present that exact ticket stub**. Whoever holds the ticket gets the coat.

#### How It Works in Our Architecture:
1. **Step 2 (Verify OTP):**
   - The user submits the 6-digit OTP.
   - The server verifies it.
   - Instead of marking the entire account as open for password resets, the server generates a **cryptographically unguessable, 128-bit UUIDv4 token**:
     ```
     resetToken: "550e8400-e29b-41d4-a716-446655440000"
     ```
   - The server saves this token in the DB with an expiration time (15 minutes) and returns it **strictly in the HTTP response body to Alice's browser session**.
2. **Step 3 (Reset Password):**
   - Alice's browser sends:
     ```http
     POST /api/users/reset-password
     {
       "resetToken": "550e8400-e29b-41d4-a716-446655440000",
       "newPassword": "NewSecurePassword123!"
     }
     ```
   - Notice: **Alice doesn't even need to send her email address!** The `resetToken` uniquely identifies the verified request.
   - Even if the attacker knows Alice's email, the attacker **does not have the `resetToken`** because it was sent directly to Alice's browser.
   - The attacker cannot guess a 128-bit UUID (there are $3.4 \times 10^{38}$ possibilities).
3. **Burn After Reading (Single-Use):**
   - The moment the password is updated, the server marks `is_used = true`.
   - The token is destroyed immediately. If anyone intercepts it later or tries to replay it, the request is instantly rejected.
4. **Session Invalidation:**
   - All active JWT refresh tokens for the user are deleted from the database. Any active sessions on other devices are terminated immediately.

---

## 2. Step-by-Step Implementation Blueprint

When implementing this in any microservice, follow these 5 clear steps:

```
Step 1: DB Entity & Repo ──> Step 2: Email Abstraction ──> Step 3: DTOs & Validation ──> Step 4: Business Logic ──> Step 5: Controller & Gateway
```

---

### Step 1: Database Entity & Repository
* **Entity:** [`PasswordResetToken.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/entity/PasswordResetToken.java)
  * Stores `email`, `otp`, `otpExpiryTime`, `resetToken`, `resetTokenExpiryTime`, and `isUsed`.
  * Indexed on `email` and `resetToken` for fast $O(1)$ lookups.
* **Repository:** [`PasswordResetTokenRepository.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/repository/PasswordResetTokenRepository.java)
  * Provides an atomic query to invalidate old OTPs when a user requests a new one:
    ```java
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.isUsed = true WHERE p.email = :email AND p.isUsed = false")
    void invalidateExistingTokens(@Param("email") String email);
    ```

---

### Step 2: Pluggable Email Provider (Strategy Pattern)
Never tightly couple your business logic to a third-party vendor (like Resend). Use an interface:

* **Interface:** [`EmailService.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/service/email/EmailService.java)
  ```java
  public interface EmailService {
      void sendOtpEmail(String toEmail, String otp);
  }
  ```
* **Resend Implementation:** [`ResendEmailService.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/service/email/ResendEmailService.java)
  * Uses Spring's built-in `RestClient` (no external SDK dependency conflicts).
  * Annotated with `@ConditionalOnProperty(name = "mail.provider", havingValue = "resend", matchIfMissing = true)`.
* **AWS SES Implementation:** [`AwsSesEmailService.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/service/email/AwsSesEmailService.java)
  * Annotated with `@ConditionalOnProperty(name = "mail.provider", havingValue = "aws-ses")`.

---

### Step 3: DTOs & Input Validation
Create immutable, validated DTOs using Jakarta Validation annotations:
* [`ForgotPasswordRequest.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/dto/ForgotPasswordRequest.java): `@NotBlank`, `@Email`.
* [`VerifyOtpRequest.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/dto/VerifyOtpRequest.java): `@NotBlank`, `@Pattern(regexp = "^\\d{6}$")`.
* [`VerifyOtpResponse.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/dto/VerifyOtpResponse.java): Returns the `resetToken`.
* [`ResetPasswordRequest.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/dto/ResetPasswordRequest.java): `@NotBlank resetToken`, `@Size(min = 5) newPassword`.

---

### Step 4: Core Business Logic ([`UserServiceImplementation.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/service/implementations/UserServiceImplementation.java))

Each operation is wrapped in `@Transactional` to ensure data consistency:

```java
// 1. Forgot Password
public void processForgotPassword(ForgotPasswordRequest request) {
    userRepository.findByUsername(request.getEmail())
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
    // Invalidate old OTPs so only 1 active code exists
    passwordResetTokenRepository.invalidateExistingTokens(request.getEmail());

    // Generate cryptographically secure 6-digit OTP
    String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
    
    // Save record with 5 min TTL
    passwordResetTokenRepository.save(new PasswordResetToken(...));

    // Send email asynchronously/via REST
    emailService.sendOtpEmail(request.getEmail(), otp);
}

// 2. Verify OTP
public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
    PasswordResetToken token = passwordResetTokenRepository
        .findFirstByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail())
        .orElseThrow(() -> new BusinessException("No active OTP request found"));

    if (LocalDateTime.now().isAfter(token.getOtpExpiryTime()))
        throw new BusinessException("OTP has expired");
    if (!token.getOtp().equals(request.getOtp()))
        throw new BusinessException("Invalid OTP");

    // Issue unique reset token valid for 15 minutes
    String resetToken = UUID.randomUUID().toString();
    token.setResetToken(resetToken);
    token.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(15));
    passwordResetTokenRepository.save(token);

    return new VerifyOtpResponse("OTP verified successfully", resetToken);
}

// 3. Reset Password
public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token = passwordResetTokenRepository
        .findByResetTokenAndIsUsedFalse(request.getResetToken())
        .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

    if (LocalDateTime.now().isAfter(token.getResetTokenExpiryTime()))
        throw new BusinessException("Reset token expired");

    // Update password with BCrypt
    User user = userRepository.findByUsername(token.getEmail()).orElseThrow(...);
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    // Consume token & kill all active refresh tokens (global session revocation)
    token.setUsed(true);
    passwordResetTokenRepository.save(token);
    refreshTokenRepository.deleteByUser(user);
}
```

---

### Step 5: Controller & API Gateway
* Expose the 3 endpoints in [`UserController.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/controller/UserController.java) under `/api/users/forgot-password`, `/verify-otp`, and `/reset-password`.
* In [`AuthenticationFilter.java`](file:///Users/canzova/IdeaProjects/MicroDevops/api-gateway/src/main/java/com/easybuy/api_gateway/filter/AuthenticationFilter.java), declare these routes in `isPublicEndpoint()` so unauthenticated users can access them through the Gateway.

---

## 3. Resend Setup Guide

### 1. Create Account & Get API Key
1. Go to [resend.com](https://resend.com) and create a free account.
2. In the dashboard, click **API Keys** &rarr; **Create API Key**.
3. Copy your key (starts with `re_...`).

### 2. Verified Sender vs. Testing Domain
* **Development/Testing:** Use Resend's default sandbox domain. The sender must be:
  `EasyBuy <onboarding@resend.dev>`
  *(Note: Free accounts can only send to the email you registered with Resend until you add a domain).*
* **Production:** Click **Domains** &rarr; **Add Domain** &rarr; Add the DNS records (DKIM, SPF) to your domain registrar (GoDaddy, Namecheap, Route53). Once verified, you can send from `support@yourdomain.com`.

### 3. Connect to Code
In `user-service/src/main/resources/application.properties` (or your Config Server):
```properties
mail.provider=resend
resend.api-key=re_123456789_abcdefg
resend.from-email=EasyBuy <onboarding@resend.dev>
```
*Or via environment variable:*
```bash
export RESEND_API_KEY="re_123456789_abcdefg"
```

---

## 4. AWS SES Setup Guide

### 1. Verify Identity in AWS SES
1. Open AWS Management Console &rarr; Navigate to **Amazon Simple Email Service (SES)**.
2. Under **Configuration**, click **Identities** &rarr; **Create Identity**.
3. Choose **Domain** (recommended) or **Email Address**.
4. If Email Address: Enter your sender email (e.g. `support@yourdomain.com`) and click the verification link sent to that inbox.
5. If in SES Sandbox: You must also verify any recipient email addresses you test with, or request production access via AWS Support.

### 2. IAM Policy & Credentials
Create an IAM User (or IAM Role for EKS / EC2) with the following permission:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["ses:SendEmail", "ses:SendRawEmail"],
      "Resource": "*"
    }
  ]
}
```

### 3. Connect to Code
When you want to switch from Resend to AWS SES:

1. **Add AWS SES SDK** to `user-service/pom.xml`:
   ```xml
   <dependency>
       <groupId>software.amazon.awssdk</groupId>
       <artifactId>sesv2</artifactId>
       <version>2.25.70</version>
   </dependency>
   ```

2. **Update `AwsSesEmailService.java`**:
   ```java
   @Service
   @ConditionalOnProperty(name = "mail.provider", havingValue = "aws-ses")
   @RequiredArgsConstructor
   public class AwsSesEmailService implements EmailService {
       private final SesV2Client sesClient;
       
       @Override
       public void sendOtpEmail(String toEmail, String otp) {
           SendEmailRequest request = SendEmailRequest.builder()
               .fromEmailAddress("support@yourdomain.com")
               .destination(d -> d.toAddresses(toEmail))
               .content(c -> c.simple(s -> s
                   .subject(sub -> sub.data("EasyBuy - Password Reset OTP"))
                   .body(b -> b.html(h -> h.data("<p>Your OTP is " + otp + "</p>")))
               ))
               .build();
           sesClient.sendEmail(request);
       }
   }
   ```

3. **Switch Configuration**:
   In `application.properties`:
   ```properties
   mail.provider=aws-ses
   aws.region=us-east-1
   ```
   **Zero changes to your controllers or business service!** Spring automatically disables `ResendEmailService` and activates `AwsSesEmailService`.

---

## 5. Architectural Deep Dives & Frequently Asked Questions

### 5.1. Why Are We Sending an HTML Page/Template in the Email?
There are two parts to this question:
1. **REST API Responses (HTTP):**
   - The Spring Boot REST Controller ([`UserController.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/controller/UserController.java)) **does NOT return an HTML page** to the client.
   - All three API endpoints (`/forgot-password`, `/verify-otp`, `/reset-password`) return clean, structured JSON payloads:
     - `POST /forgot-password` &rarr; `{"message": "OTP sent successfully..."}`
     - `POST /verify-otp` &rarr; `{"message": "...", "resetToken": "550e8400-..."}`
     - `POST /reset-password` &rarr; `{"message": "Password has been reset successfully..."}`
2. **Email Body (MIME Content):**
   - Inside the email sent to the user's inbox via Resend or AWS SES, we send an **HTML template** (`text/html`) rather than raw plain text (`text/plain`).
   - **Why?**
     - **Consumer Trust & Brand Credibility:** A plain text email saying `Your OTP is 123456` looks like spam, a phishing attempt, or an unmaintained service. Modern e-commerce users expect professional typography, brand colors (EasyBuy blue), and structured layouts.
     - **Visual Hierarchy & Usability:** The 6-digit OTP is highlighted in a large (32px), bold, letter-spaced badge on a dedicated card. On mobile phones, users can glance at it or long-press to copy without squinting at tiny text.
     - **Security Callouts:** A styled warning box explicitly alerts the user: *"If you did not request this password reset, please ignore this email or contact support immediately."*
     - **Cross-Client Compatibility:** Responsive inline CSS ensures consistent rendering across Gmail, Apple Mail, Outlook, and Yahoo Mail on both desktop and mobile screens.

---

### 5.2. What is `SecureRandom` and Why Are We Using It Instead of `Random`?
In Java, there are two primary random number generators:
1. `java.util.Random` (and `Math.random()`, which delegates to it)
2. `java.security.SecureRandom`

#### The Critical Vulnerability of `java.util.Random`:
* `java.util.Random` is a **Linear Congruential Generator (LCG)**. Its internal state is determined by a simple mathematical formula:
  $$X_{n+1} = (a \cdot X_n + c) \pmod m$$
* It has only a 48-bit seed.
* **Attack Scenario (Predicting Future OTPs):**
  If an attacker requests two consecutive OTPs for an account they control, they can collect those two numbers. Using basic algebraic math (e.g. an LCG solver tool), they can calculate the internal seed in **under 1 second**!
  Once the seed is known, the attacker can predict **every single future OTP generated for ANY user on the entire platform**, allowing full account takeover across the system!

#### Why `java.security.SecureRandom` is Mandatory:
* It is a **Cryptographically Secure Pseudo-Random Number Generator (CSPRNG)** conforming to FIPS 140-2 and RFC 4086 standards.
* It does not rely on a simple math formula; instead, it harvests **true entropy** from non-deterministic hardware sources provided by the host Operating System:
  - Linux/macOS: `/dev/urandom` and `/dev/random` (harvested from CPU jitter, hardware interrupts, disk activity, thermal sensors).
  - Windows: CryptGenRandom (CryptoAPI).
* Even if an attacker analyzes millions of past OTPs, it is mathematically impossible to predict the next number.
* Standard security audits (OWASP Top 10 A02: Cryptographic Failures, CWE-330: Use of Insufficiently Random Values) flag `java.util.Random` as an automatic security vulnerability when used for secrets, OTPs, or tokens.

---

### 5.3. Explaining `@Pattern` in DTOs (`VerifyOtpRequest.java`)

In [`VerifyOtpRequest.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/dto/VerifyOtpRequest.java):
```java
@NotBlank(message = "OTP is required")
@Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
private String otp;
```

#### What is `@Pattern`?
* It is a standard constraint from Jakarta Bean Validation (`jakarta.validation.constraints.Pattern`).
* It enforces that the annotated string property must match the specified Regular Expression (regex).

#### Breakdown of the Regex `^\\d{6}$`:
| Token | Meaning |
| :--- | :--- |
| `^` | **Start anchor:** Asserts the match must begin at the very first character (no leading spaces or prefixes allowed). |
| `\\d` | **Digit class:** Matches any single numeric digit character (`0` through `9`). Escaped as `\\d` in Java string literals. |
| `{6}` | **Quantifier:** Specifies that the preceding token (`\\d`) must occur **exactly 6 times** — no more, no less. |
| `$` | **End anchor:** Asserts the match must end at the very last character (no trailing spaces, letters, or suffixes allowed). |

#### Why Validate at the DTO Layer? (Fail-Fast Architecture)
* **Immediate Rejection (HTTP 400):** When `@Valid` is placed on the controller method parameter, Spring's validation filter runs before the service logic executes. If someone sends `"otp": "123"` or `"otp": "1234567"` or `"otp": "abc"`, Spring rejects it immediately with HTTP 400 Bad Request.
* **Database & Resource Protection:** No database queries are performed, no DB connection from the Hikari pool is held, and no CPU cycles are wasted on invalid input.
* **Defense-in-Depth:** Prevents SQL injection attempts, buffer overflows, or unexpected character sets from ever touching the service or repository layers.

---

### 5.4. Why the Sender Email is `EasyBuy <onboarding@resend.dev>` and Not `easyBuy@resend.dev`

This addresses two crucial concepts in email delivery and email standards:

#### 1. Why `EasyBuy <onboarding@resend.dev>` syntax? (RFC 5322 Standard)
Under the Internet Email Specification (RFC 5322 Section 3.4), an email "From" address can take two forms:
* **Address Only:** `onboarding@resend.dev` &rarr; In the user's inbox, the sender appears as *"onboarding"*. This looks robotic, unfamiliar, and impersonal.
* **Display Name + Angle Address:** `EasyBuy <onboarding@resend.dev>` &rarr; Email clients (Gmail, Apple Mail, Outlook) parse this into:
  - **Display Name (Friendly Name):** **EasyBuy** (displayed in bold in the inbox list).
  - **Actual Mailbox:** `<onboarding@resend.dev>` (shown when clicking sender details).
This ensures customers immediately recognize the email as coming from your brand: **EasyBuy**.

#### 2. Why `onboarding@resend.dev` and NOT `easyBuy@resend.dev`?
* **You Don't Own `resend.dev`:**
  - The domain `resend.dev` is owned and controlled exclusively by Resend, Inc.
  - Just like you cannot send an email from `easyBuy@google.com` or `easyBuy@microsoft.com` without owning google.com or microsoft.com, you cannot create custom mailboxes on `resend.dev`.
* **Resend's Free Sandbox Policy:**
  - To allow developers to start testing instantly without buying or configuring a domain, Resend provides a free shared testing mailbox: `onboarding@resend.dev`.
  - Resend's API will strictly reject any request attempting to send from an unverified address on their domain like `easyBuy@resend.dev` with an error: *"Domain not verified"*.
* **DNS Authentication (SPF, DKIM, DMARC):**
  - Modern email providers (Google, Microsoft) require cryptographic proof (DKIM signatures and SPF TXT records in DNS) proving that the sending server is authorized by the domain owner.
  - You cannot publish DNS records for `resend.dev` because you do not own it.

#### 3. How It Works in Production (Using Your Custom Domain):
In a real production environment, you never use `resend.dev`. Instead:
1. You purchase your own domain (e.g. `easybuy.com`).
2. In Resend (or AWS SES), you click **Add Domain** &rarr; `easybuy.com`.
3. You add the DNS records (DKIM CNAMEs, SPF TXT) to your DNS provider (Cloudflare, GoDaddy, AWS Route53).
4. Once verified, you configure:
   ```properties
   resend.from-email=EasyBuy <support@easybuy.com>
   # or
   resend.from-email=EasyBuy <no-reply@easybuy.com>
   ```
Now the email arrives with the clean, branded address `support@easybuy.com` and displays **EasyBuy** in the user's inbox with 100% deliverability!

---

### 5.5. Why Invalidate All Refresh Tokens on Password Reset? What If There Are Multiple Active Refresh Tokens?

#### 1. What If a User Has Multiple Active Refresh Tokens?
In a real system, a user might be logged into EasyBuy on their laptop (Chrome), their mobile phone (iOS app), and their tablet. Each device has its own `RefreshToken` record in the database (`@ManyToOne` relationship with `User`).

* **The Problem with `findByUser(user)`:**
  If the repository declares:
  ```java
  Optional<RefreshToken> findByUser(User user);
  ```
  and the database contains **more than 1 token** for that user, Spring Data JPA throws:
  `jakarta.persistence.NonUniqueResultException: query did not return a unique result: 2` (wrapped in `IncorrectResultSizeDataAccessException`).
  The password reset request would **crash with a 500 Internal Server Error**! Even if it didn't throw an error, deleting only 1 token would leave the other devices logged in.

* **The Clean Spring Data JPA Solution (`deleteByUser`):**
  You do not even need `@Query` or `@Modifying`! Spring Data JPA natively supports derived deletion queries:
  ```java
  void deleteByUser(User user);
  ```
  And in [`UserServiceImplementation.java`](file:///Users/canzova/IdeaProjects/MicroDevops/user-service/src/main/java/easybuy/user_service/service/implementations/UserServiceImplementation.java):
  ```java
  refreshTokenRepository.deleteByUser(user);
  ```
  This cleanly deletes **0, 1, or 50 active tokens** in one transaction without any `NonUniqueResultException`, ensuring that **all** active sessions across every device are terminated.

* **Spring Data JPA Derived Method vs. `@Query` Bulk Delete:**
  | Feature | `void deleteByUser(User user)` (Derived Method) | `@Modifying @Query("DELETE ...")` (JPQL Bulk Delete) |
  | :--- | :--- | :--- |
  | **Annotations Needed** | **None** (pure Spring Data JPA convention) | Requires `@Modifying` and `@Query` |
  | **Execution Mechanism** | Selects entities first, then removes them via EntityManager | Directly executes a single `DELETE FROM ... WHERE ...` |
  | **JPA Lifecycle Hooks** | Triggers `@PreRemove` / `@PostRemove` | Bypasses lifecycle hooks |
  | **Best Fit** | Clean, idiomatic Spring Data JPA without boilerplate | High-volume tables where running 1 SQL statement is preferred |

#### 2. Why Is This Security Practice Mandatory? (The Security Rationale)
Why do we delete refresh tokens when resetting a password?
* **Account Takeover / Stolen Credentials Remediation:**
  Suppose an attacker steals a user's password or phone and logs in. The user notices suspicious activity and quickly resets their password.
  - *If we did NOT delete refresh tokens:* The attacker's device already holds a valid `refreshToken`. When their short-lived access token expires, their device silently calls `POST /api/users/refresh-token`. The server grants a new access token! The attacker remains logged in for weeks or months. **The password reset would be completely ineffective at stopping an active attacker.**
* **Lost or Stolen Device:**
  A user leaves their phone in a taxi or airport. To protect their account, they log into a computer and reset their password. Deleting all refresh tokens ensures that the lost phone can never refresh its session and is locked out immediately once the short-lived access token expires.
* **Compliance with Industry Standards:**
  - **OWASP Session Management Cheat Sheet:** Mandates that changing a user's credentials MUST terminate all active sessions across all devices (Global Logout / Session Revocation).
  - **NIST SP 800-63B (Section 7):** Requires credential providers to revoke all active tokens/sessions upon password changes.

