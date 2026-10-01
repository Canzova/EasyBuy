# AWS SES Password Reset Implementation & AWS Deployment Guide

This document provides a complete technical guide on the **AWS Simple Email Service (SES v2)** integration for the **Forgot Password & OTP** workflow in `user-service`. It covers the architectural design, sequence diagrams, detailed code modifications, local development setup, and comprehensive deployment instructions across **AWS EC2**, **AWS ECS**, **AWS EKS**, and other AWS services.

---

## Table of Contents
1. [Architecture Overview & Provider Abstraction](#1-architecture-overview--provider-abstraction)
2. [End-to-End Sequence Diagrams](#2-end-to-end-sequence-diagrams)
3. [Summary of Code Changes](#3-summary-of-code-changes)
4. [Local Development Configuration](#4-local-development-configuration)
5. [AWS SES Console & Domain Setup](#5-aws-ses-console--domain-setup)
6. [Deployment Guide: AWS EC2](#6-deployment-guide-aws-ec2)
7. [Deployment Guide: AWS ECS (Elastic Container Service)](#7-deployment-guide-aws-ecs-elastic-container-service)
8. [Deployment Guide: AWS EKS (Elastic Kubernetes Service)](#8-deployment-guide-aws-eks-elastic-kubernetes-service)
9. [Deployment on Other AWS Services (App Runner & Lambda)](#9-deployment-on-other-aws-services-app-runner--lambda)
10. [Security Best Practices & Config Server Integration](#10-security-best-practices--config-server-integration)

---

## 1. Architecture Overview & Provider Abstraction

The `user-service` utilizes the **Strategy Pattern** for transactional email delivery. The core domain logic depends exclusively on the generic `EmailService` interface:

```mermaid
classDiagram
    direction TB
    class EmailService {
        <<interface>>
        +sendOtpEmail(String toEmail, String otp) void
    }
    class ResendEmailService {
        -ResendProperties resendProperties
        -RestClient restClient
        +sendOtpEmail(String toEmail, String otp) void
    }
    class AwsSesEmailService {
        -SesV2Client sesV2Client
        -AwsSesProperties awsSesProperties
        +sendOtpEmail(String toEmail, String otp) void
    }
    class EmailTemplateHelper {
        <<utility>>
        +buildOtpHtmlTemplate(String otp)$ String
    }
    class AwsSesConfig {
        +sesV2Client() SesV2Client
    }

    EmailService <|.. ResendEmailService : @ConditionalOnProperty(mail.provider=resend)
    EmailService <|.. AwsSesEmailService : @ConditionalOnProperty(mail.provider=aws-ses)
    ResendEmailService ..> EmailTemplateHelper : uses
    AwsSesEmailService ..> EmailTemplateHelper : uses
    AwsSesConfig ..> AwsSesEmailService : injects SesV2Client
```

### Key Architectural Benefits
- **Zero Business Logic Coupling:** `UserServiceImplementation` interacts only with `EmailService`. Switching from Resend to AWS SES requires **zero lines of code change** in controllers or business services.
- **Dynamic Provider Selection:** Toggled via `mail.provider=aws-ses` or `mail.provider=resend` in `application.properties` (or external environment variables / Config Server).
- **Dual Credential Resolution:** In local development, credentials can be read directly from `.properties`. In cloud environments (EC2, ECS, EKS), the configuration automatically falls back to AWS `DefaultCredentialsProvider` (IAM Roles / IRSA), guaranteeing **zero hardcoded credentials in production**.

---

## 2. End-to-End Sequence Diagrams

### 2.1. Complete Forgot Password & Password Reset Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as Client / Frontend
    participant Gateway as API Gateway
    participant UserCtrl as UserController
    participant UserSvc as UserServiceImplementation
    participant DB as PostgreSQL
    participant SES as AWS SES v2 Service
    participant Mailbox as User Inbox

    %% Step 1: Forgot Password
    Note over User, Mailbox: 1. Request Password Reset OTP
    User->>Gateway: POST /users/auth/forgot-password (with email)
    Gateway->>UserCtrl: Forward request
    UserCtrl->>UserSvc: processForgotPassword(request)
    UserSvc->>DB: findByUsername(email)
    DB-->>UserSvc: User found
    UserSvc->>UserSvc: Generate Secure 6-Digit OTP
    UserSvc->>DB: Invalidate existing tokens for email
    UserSvc->>DB: Save PasswordResetToken (email, otp, expiry 5m)
    UserSvc->>SES: sendOtpEmail(email, otp)
    SES-->>UserSvc: 200 OK (MessageId returned)
    SES->>Mailbox: Deliver Branded HTML Email with OTP
    UserSvc-->>UserCtrl: Success message
    UserCtrl-->>User: 200 OK (OTP sent to user email)

    %% Step 2: Verify OTP
    Note over User, Mailbox: 2. Verify OTP & Receive Reset Token
    User->>Mailbox: Reads 6-Digit OTP
    User->>Gateway: POST /users/auth/verify-otp (with email and OTP)
    Gateway->>UserCtrl: Forward request
    UserCtrl->>UserSvc: verifyOtp(request)
    UserSvc->>DB: Fetch active token for email
    UserSvc->>UserSvc: Verify OTP match & check expiration
    UserSvc->>UserSvc: Generate UUID Reset Token (valid for 15m)
    UserSvc->>DB: Update token with resetToken & resetExpiry
    UserSvc-->>UserCtrl: VerifyOtpResponse(resetToken)
    UserCtrl-->>User: 200 OK (Return resetToken)

    %% Step 3: Reset Password
    Note over User, Mailbox: 3. Set New Password
    User->>Gateway: POST /users/auth/reset-password (with resetToken and newPassword)
    Gateway->>UserCtrl: Forward request
    UserCtrl->>UserSvc: resetPassword(request)
    UserSvc->>DB: findByResetTokenAndIsUsedFalse(resetToken)
    UserSvc->>UserSvc: Verify resetToken expiration
    UserSvc->>UserSvc: BCrypt encode new password
    UserSvc->>DB: Update User password
    UserSvc->>DB: Mark PasswordResetToken isUsed = true
    UserSvc->>DB: Revoke/delete existing Refresh Tokens
    UserSvc-->>UserCtrl: Success message
    UserCtrl-->>User: 200 OK (Password reset successful)
```

---

### 2.2. AWS SES Credential Resolution Flow

```mermaid
flowchart TD
    Start(["Spring Context Initializing"]) --> CheckProvider{"mail.provider == 'aws-ses'?"}
    CheckProvider -- "No" --> InitResend["Activate ResendEmailService"]
    CheckProvider -- "Yes" --> ReadProps["Read AwsSesProperties from application.properties"]
    ReadProps --> CheckStaticKeys{"Are access-key-id and secret-access-key<br/>provided and valid?"}
    
    CheckStaticKeys -- "Yes" --> StaticAuth["Build StaticCredentialsProvider<br/>with AwsBasicCredentials"]
    CheckStaticKeys -- "No" --> DefaultAuth["Build DefaultCredentialsProvider<br/>Resolves in order:<br/>1. Environment Variables<br/>2. System Properties<br/>3. Web Identity Token / EKS IRSA<br/>4. ECS Task Role Credentials<br/>5. EC2 Instance Profile Metadata<br/>6. AWS CLI credentials"]
    
    StaticAuth --> BuildSesClient["Initialize SesV2Client with Region and Credentials"]
    DefaultAuth --> BuildSesClient
    BuildSesClient --> SesBean["SesV2Client Spring Bean Ready"]
    SesBean --> ActivateService["Activate AwsSesEmailService"]
```

---

## 3. Summary of Code Changes

### 3.1. `user-service/pom.xml`
Added the official AWS SDK for Java 2.x SES v2 module:
```xml
<!-- AWS SDK v2 - Simple Email Service (SES v2) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>sesv2</artifactId>
    <version>2.25.70</version>
</dependency>
```

### 3.2. Configuration: `AwsSesProperties.java`
- Package: `easybuy.user_service.configuration`
- Prefix: `aws.ses`
- Encapsulates properties:
  - `region`: AWS Region (`us-east-1` by default)
  - `accessKeyId`: AWS IAM Access Key ID
  - `secretAccessKey`: AWS IAM Secret Access Key
  - `fromEmail`: Verified sender email address

### 3.3. Configuration: `AwsSesConfig.java`
- Package: `easybuy.user_service.configuration`
- Condition: `@ConditionalOnProperty(name = "mail.provider", havingValue = "aws-ses")`
- Constructs `SesV2Client` with intelligent dual authentication:
  - Uses `StaticCredentialsProvider` if keys are supplied in `.properties` (ideal for local testing).
  - Automatically falls back to `DefaultCredentialsProvider` when running on EC2, ECS, or EKS (ideal for cloud deployments).

### 3.4. Shared Utility: `EmailTemplateHelper.java`
- Package: `easybuy.user_service.service.email`
- Centralizes the responsive, branded HTML email template featuring:
  - Blue accent brand header (`EasyBuy`)
  - Highlighted dashed OTP display box
  - Expiry notice (5 minutes) and security warnings
  - Mobile-responsive CSS layout

### 3.5. Service: `ResendEmailService.java`
- Refactored to delegate HTML generation to `EmailTemplateHelper.buildOtpHtmlTemplate(otp)`, eliminating code duplication while preserving 100% of Resend capability.

### 3.6. Service: `AwsSesEmailService.java`
- Package: `easybuy.user_service.service.email`
- Implements `EmailService`.
- Uses `SesV2Client.sendEmail(SendEmailRequest)` to send transactional OTP emails.
- Catches `SesV2Exception`, logs AWS error codes (`MessageRejected`, `MailFromDomainNotVerifiedException`, etc.), and throws descriptive `BusinessException`.

### 3.7. Configuration: `application.properties`
Added AWS SES properties and switched the default provider:
```properties
# Active mail provider: resend | aws-ses
mail.provider=${MAIL_PROVIDER:aws-ses}

# AWS SES Configuration
aws.ses.region=${AWS_SES_REGION:us-east-1}
aws.ses.access-key-id=${AWS_ACCESS_KEY_ID:YOUR_AWS_ACCESS_KEY_ID}
aws.ses.secret-access-key=${AWS_SECRET_ACCESS_KEY:YOUR_AWS_SECRET_ACCESS_KEY}
aws.ses.from-email=${AWS_SES_FROM_EMAIL:support@easybuy.com}
```

### 3.8. Unit Tests: `AwsSesEmailServiceTest.java`
- Package: `easybuy.user_service.service.email`
- 3 test cases:
  1. `testSendOtpEmail_Success`: Verifies email payload, recipient, subject, and OTP body.
  2. `testSendOtpEmail_SesV2Exception`: Verifies exception handling and business exception wrapping for AWS errors.
  3. `testSendOtpEmail_GenericException`: Verifies unexpected network errors are safely handled.

---

## 4. Local Development Configuration

### 4.1. Option A: Configure in `application.properties`
Update `user-service/src/main/resources/application.properties` with your AWS credentials:
```properties
mail.provider=aws-ses
aws.ses.region=us-east-1
aws.ses.access-key-id=AKIAIOSFODNN7EXAMPLE
aws.ses.secret-access-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
aws.ses.from-email=support@yourdomain.com
```

### 4.2. Option B: Use Environment Variables (Recommended for safety)
Avoid committing keys to git by leaving placeholders in `.properties` and exporting environment variables:
```bash
export MAIL_PROVIDER=aws-ses
export AWS_SES_REGION=us-east-1
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
export AWS_SES_FROM_EMAIL=support@yourdomain.com
```

### 4.3. Testing the Endpoints Locally
You can test the 3 endpoints via cURL or Postman:

#### 1. Request OTP
```bash
curl -X POST http://localhost:8081/users/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "your-verified-email@example.com"}'
```
*Expected Response (`200 OK`):*
```json
{
  "message": "Password reset OTP sent to your email. Please check your inbox."
}
```

#### 2. Verify OTP
```bash
curl -X POST http://localhost:8081/users/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email": "your-verified-email@example.com", "otp": "123456"}'
```
*Expected Response (`200 OK`):*
```json
{
  "resetToken": "4d16db2c-20bf-488f-9a41-26ec0024765d",
  "message": "OTP verified successfully. You may now reset your password."
}
```

#### 3. Reset Password
```bash
curl -X POST http://localhost:8081/users/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "resetToken": "4d16db2c-20bf-488f-9a41-26ec0024765d",
    "newPassword": "NewStrongPassword2026!"
  }'
```
*Expected Response (`200 OK`):*
```json
{
  "message": "Password has been reset successfully. You can now login with your new password."
}
```

---

## 5. AWS SES Console & Domain Setup

Before AWS SES can send emails, verify sender identities and configure DNS records.

### 5.1. AWS SES Sandbox vs Production Mode
> [!IMPORTANT]
> **AWS SES Sandbox Mode:**
> All new AWS accounts start in the SES Sandbox.
> - **In Sandbox:** You can **ONLY** send emails to verified email addresses. Sending to an unverified recipient will throw `MessageRejected: Email address is not verified`.
> - **In Production:** You can send emails to any recipient worldwide.

#### Moving from Sandbox to Production:
1. Open the [AWS SES Console](https://console.aws.amazon.com/ses/).
2. In the navigation pane, choose **Account dashboard**.
3. Under **Sending limits**, choose **Request production access**.
4. Fill in:
   - Mail type: `Transactional`
   - Website URL: `https://yourdomain.com`
   - Description: *"Transactional OTP emails for user password reset in an e-commerce application."*
5. Approval usually takes 12–24 hours.

### 5.2. Verifying Sender Identity
1. Go to **SES Console** &rarr; **Identities** &rarr; **Create identity**.
2. Select **Email address** (e.g. `support@yourdomain.com`) or **Domain** (e.g. `yourdomain.com`).
3. If using an **Email address**, AWS sends a confirmation email. Click the verification link.
4. If using a **Domain**, AWS provides 3 CNAME records for Easy DKIM. Add these to your DNS provider (e.g. Route 53, Cloudflare, GoDaddy).

### 5.3. Recommended DNS Records (DKIM, SPF, DMARC)
- **SPF Record (TXT):**
  ```text
  v=spf1 include:amazonses.com ~all
  ```
- **DMARC Record (TXT):** Host: `_dmarc.yourdomain.com`
  ```text
  v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@yourdomain.com
  ```

---

## 6. Deployment Guide: AWS EC2

### 6.1. Why "IAM Instance Profile"? (Console vs AWS CLI)
You might wonder: *"Can't I just attach the IAM Role directly to my EC2 instance?"*
- **In the AWS Management Console (Web UI): YES!** You literally just select your EC2 instance &rarr; **Actions** &rarr; **Security** &rarr; **Modify IAM role** &rarr; pick your Role.
- **What happens behind the scenes:** An EC2 virtual machine cannot directly attach an IAM role at the OS/hardware level. AWS uses an internal container called an **IAM Instance Profile** to hold the role.
  - When you use the **AWS Console UI**, AWS **automatically creates an Instance Profile with the same name as your role and links it for you invisibly**.
  - When using the **AWS CLI or Terraform / CloudFormation**, AWS does not perform this hidden shortcut, so you must explicitly create the Instance Profile container and add the role to it.

### 6.2. Visual Architecture

```mermaid
flowchart LR
    subgraph VPC ["AWS Virtual Private Cloud - VPC"]
        subgraph EC2_VM ["EC2 Instance - user-service"]
            App["Spring Boot App - Java 25"]
            IMDS["AWS Instance Metadata Service - IMDSv2"]
        end
    end

    subgraph IAM_SEC ["AWS IAM"]
        Profile["IAM Instance Profile"]
        Role["IAM Role - UserServiceEC2Role"]
        Policy["Policy - ses:SendEmail"]
    end

    SES["AWS SES v2 Service"]

    Policy --> Role
    Role --> Profile
    Profile -.->|Attached to| EC2_VM
    App -->|Requests Temp STS Credentials| IMDS
    IMDS -->|Supplies 6-hour Token| App
    App -->|Sends OTP Email| SES
```

### 6.3. Quick Setup Steps

#### Step 1: Create IAM Policy (`ses-send-policy.json`)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowSesSendEmail",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
```
Create the policy:
```bash
aws iam create-policy --policy-name EasyBuySesSendPolicy --policy-document file://ses-send-policy.json
```

#### Step 2: Attach to EC2
- **Option A (AWS Console - Simplest):**
  1. Go to **IAM Console** &rarr; **Roles** &rarr; **Create Role**.
  2. Select **AWS Service** &rarr; **EC2**.
  3. Attach `EasyBuySesSendPolicy`. Name it `UserServiceEC2Role`.
  4. Go to **EC2 Console** &rarr; Select your instance &rarr; **Actions** &rarr; **Security** &rarr; **Modify IAM role** &rarr; Select `UserServiceEC2Role` &rarr; Click **Update IAM role**. Done!

- **Option B (AWS CLI):**
  ```bash
  # 1. Create trust policy
  cat <<EOF > ec2-trust.json
  {
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": { "Service": "ec2.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }]
  }
  EOF

  # 2. Create Role & Attach SES policy
  aws iam create-role --role-name UserServiceEC2Role --assume-role-policy-document file://ec2-trust.json
  aws iam attach-role-policy --role-name UserServiceEC2Role --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/EasyBuySesSendPolicy

  # 3. Create Instance Profile & link to EC2
  aws iam create-instance-profile --instance-profile-name UserServiceEC2Profile
  aws iam add-role-to-instance-profile --instance-profile-name UserServiceEC2Profile --role-name UserServiceEC2Role
  aws ec2 associate-iam-instance-profile --instance-id <INSTANCE_ID> --iam-instance-profile Name=UserServiceEC2Profile
  ```

#### Step 3: Run Service on EC2
Leave `access-key-id` and `secret-access-key` blank in `application.properties`. Spring Boot's AWS SDK automatically discovers the instance profile credentials via IMDS:
```bash
java -jar user-service-0.0.1-SNAPSHOT.jar \
  --mail.provider=aws-ses \
  --aws.ses.region=us-east-1 \
  --aws.ses.from-email=support@yourdomain.com
```

---

## 7. Deployment Guide: AWS ECS (Elastic Container Service)

### 7.1. Making ECS Crystal Clear: Why 2 IAM Roles?
When running Docker containers on ECS, AWS divides responsibilities between **the ECS Agent** and **your application code**:

| Role | Who Uses It? | Analogy | Permissions Needed |
| :--- | :--- | :--- | :--- |
| **1. Task Execution Role** (`executionRoleArn`) | **The AWS ECS Agent** (before your app runs) | The Hotel Bellhop | Pull Docker image from ECR, send container logs to CloudWatch. |
| **2. Task Role** (`taskRoleArn`) | **Your Spring Boot App** (inside the container) | The Hotel Guest | Call `ses:SendEmail` to dispatch password reset OTP emails. |

### 7.2. Simplified ECS Authentication Flow

```mermaid
sequenceDiagram
    autonumber
    participant ECS as ECS Engine / Agent
    participant App as user-service Container
    participant STS as AWS STS / Metadata Endpoint
    participant SES as AWS Simple Email Service

    Note over ECS: ECS Agent uses Task Execution Role
    ECS->>ECS: Pull user-service image from ECR & Start Container
    ECS->>App: Injects AWS_CONTAINER_CREDENTIALS_RELATIVE_URI

    Note over App, SES: App uses Task Role for Business Operations
    App->>App: User triggers Forgot Password (OTP needed)
    App->>STS: Request credentials (AWS SDK queries container URI)
    STS-->>App: Returns temporary STS credentials (for Task Role)
    App->>SES: sendEmail(SendEmailRequest) with OTP
    SES-->>App: 200 OK (MessageId returned)
```

### 7.3. Step-by-Step ECS Deployment

#### Step 1: Create Task Role with SES Permission
```bash
# 1. Trust policy for ECS
cat <<EOF > ecs-trust.json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "ecs-tasks.amazonaws.com" },
    "Action": "sts:AssumeRole"
  }]
}
EOF

# 2. Create the Role and attach our SES Send policy
aws iam create-role --role-name UserServiceEcsTaskRole --assume-role-policy-document file://ecs-trust.json
aws iam attach-role-policy --role-name UserServiceEcsTaskRole --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/EasyBuySesSendPolicy
```

#### Step 2: Build & Push Docker Image to Amazon ECR
```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build & Push
docker build -t user-service:latest -f user-service/Dockerfile user-service/
docker tag user-service:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/easybuy/user-service:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/easybuy/user-service:latest
```

#### Step 3: Register Task Definition (`task-definition.json`)
Notice how both roles are plugged in:
```json
{
  "family": "user-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/UserServiceEcsTaskRole",
  "containerDefinitions": [
    {
      "name": "user-service",
      "image": "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/easybuy/user-service:latest",
      "portMappings": [
        { "containerPort": 8081, "protocol": "tcp" }
      ],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "prod" },
        { "name": "MAIL_PROVIDER", "value": "aws-ses" },
        { "name": "AWS_SES_REGION", "value": "us-east-1" },
        { "name": "AWS_SES_FROM_EMAIL", "value": "support@yourdomain.com" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/user-service",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

```bash
aws ecs register-task-definition --cli-input-json file://task-definition.json
```

---

## 8. Deployment Guide: AWS EKS (Elastic Kubernetes Service)

### 8.1. Making EKS Simple: How IRSA Works
On Kubernetes, you don't want to attach permissions to the whole EC2 worker node because every container on the node would get those permissions.

Instead, AWS provides **IRSA (IAM Roles for Service Accounts)**. Think of it as a 3-part bridge:

```
[ Kubernetes Pod ] 
       │ (uses)
       ▼
[ ServiceAccount with annotation: eks.amazonaws.com/role-arn ]
       │ (exchanges projected token via OIDC)
       ▼
[ AWS IAM Role: UserServiceEksRole (has ses:SendEmail policy) ]
```

1. You create an **AWS IAM Role** with SES permissions and trust your EKS cluster's OpenID Connect (OIDC) provider.
2. You create a **Kubernetes ServiceAccount** with a single annotation pointing to that IAM Role ARN.
3. In your **Deployment manifest**, you set `serviceAccountName: user-service-sa`.

When the pod starts, EKS automatically injects a secure token into the pod. The AWS SDK reads this token automatically, exchanges it for temporary AWS credentials, and sends the email! **Zero access keys in your Kubernetes manifests!**

### 8.2. Simplified EKS Architecture Flow

```mermaid
flowchart LR
    subgraph EKS_Cluster ["Amazon EKS Cluster"]
        subgraph AppNamespace ["Kubernetes Namespace - default"]
            Pod["user-service Pod"]
            SA["ServiceAccount - user-service-sa"]
        end
        OIDC_Provider["EKS OIDC Provider"]
    end

    subgraph AWS_IAM ["AWS IAM Service"]
        IAM_Role["IAM Role - UserServiceEksRole"]
        STS["AWS STS - AssumeRoleWithWebIdentity"]
    end

    SES["AWS SES v2"]

    Pod -->|Uses Identity| SA
    SA -.->|Annotated with Role ARN| IAM_Role
    Pod -->|Sends OIDC JWT token| STS
    OIDC_Provider -.->|Validates Token| STS
    STS -->|Returns Short-Lived AWS Credentials| Pod
    Pod -->|Sends OTP Email| SES
```

### 8.3. Step-by-Step EKS Deployment

#### Step 1: Enable OIDC on your EKS Cluster (One-Time Setup)
```bash
eksctl utils associate-iam-oidc-provider --cluster easybuy-cluster --region us-east-1 --approve
```

#### Step 2: Create IAM Role for EKS
Get your cluster OIDC provider URL:
```bash
OIDC_PROVIDER=$(aws eks describe-cluster --name easybuy-cluster --region us-east-1 --query "cluster.identity.oidc.issuer" --output text | sed -e "s/^https:\/\///")
```

Create the trust policy (`eks-trust.json`):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/<OIDC_PROVIDER>"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "<OIDC_PROVIDER>:sub": "system:serviceaccount:default:user-service-sa",
          "<OIDC_PROVIDER>:aud": "sts.amazonaws.com"
        }
      }
    }
  ]
}
```

Create the role and attach the SES policy:
```bash
aws iam create-role --role-name UserServiceEksRole --assume-role-policy-document file://eks-trust.json
aws iam attach-role-policy --role-name UserServiceEksRole --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/EasyBuySesSendPolicy
```

#### Step 3: Kubernetes Manifests

Save the following as `user-service-k8s.yaml`:
```yaml
# 1. ServiceAccount with IAM Role Annotation
apiVersion: v1
kind: ServiceAccount
metadata:
  name: user-service-sa
  namespace: default
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::<ACCOUNT_ID>:role/UserServiceEksRole
---
# 2. Deployment referencing the ServiceAccount
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service-deployment
  namespace: default
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      serviceAccountName: user-service-sa # Connects pod to the IAM Role
      containers:
        - name: user-service
          image: <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/easybuy/user-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: MAIL_PROVIDER
              value: "aws-ses"
            - name: AWS_SES_REGION
              value: "us-east-1"
            - name: AWS_SES_FROM_EMAIL
              value: "support@yourdomain.com"
            # NOTE: No AWS_ACCESS_KEY_ID or AWS_SECRET_ACCESS_KEY needed!
            # EKS automatically injects the token file and role ARN.
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1024Mi"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 20
            periodSeconds: 10
```

Apply to the cluster:
```bash
kubectl apply -f user-service-k8s.yaml
```

---

## 9. Deployment on Other AWS Services (App Runner & Lambda)

### 9.1. AWS App Runner
AWS App Runner is a fully managed container service.
- **Instance Role:** Create an IAM Role with `EasyBuySesSendPolicy` and trust principal `tasks.apprunner.amazonaws.com`.
- In App Runner configuration, set **Instance role** to this role.
- Set Environment Variables:
  - `MAIL_PROVIDER=aws-ses`
  - `AWS_SES_REGION=us-east-1`
  - `AWS_SES_FROM_EMAIL=support@yourdomain.com`

### 9.2. AWS Lambda (Serverless)
If packaged with Spring Cloud Function or AWS Serverless Java Container:
- Attach `EasyBuySesSendPolicy` directly to the Lambda **Execution Role**.
- AWS SDK automatically discovers credentials from the execution environment.

---

## 10. Security Best Practices & Config Server Integration

### 10.1. Moving Secrets to Spring Cloud Config Server
When moving configuration to your Git repository connected to Config Server (`easybuy-config`):
1. **If using Static IAM Keys (temporary):**
   Encrypt the secret key using Spring Cloud Config CLI or encryption endpoint:
   ```bash
   curl http://localhost:8079/encrypt -d "your-aws-secret-key"
   ```
   Add to `user-service.properties` in git:
   ```properties
   mail.provider=aws-ses
   aws.ses.region=us-east-1
   aws.ses.access-key-id=AKIAIOSFODNN7EXAMPLE
   aws.ses.secret-access-key={cipher}a9f0e1...encrypted_data...
   aws.ses.from-email=support@yourdomain.com
   ```

2. **If using Cloud IAM Roles (ECS / EKS / EC2):**
   Simply omit the keys completely:
   ```properties
   mail.provider=aws-ses
   aws.ses.region=us-east-1
   aws.ses.from-email=support@yourdomain.com
   ```

### 10.2. Least Privilege IAM Policy
To restrict AWS SES sending exclusively from your verified domain/identity ARN, refine the IAM policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowSesSendEmailRestricted",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "arn:aws:ses:us-east-1:<ACCOUNT_ID>:identity/yourdomain.com"
    }
  ]
}
```

---

## Summary Checklist
- [x] AWS SDK v2 (`sesv2:2.25.70`) added to `user-service/pom.xml`.
- [x] Configuration classes created (`AwsSesProperties`, `AwsSesConfig`) with dual-credential resolution.
- [x] HTML template builder extracted into `EmailTemplateHelper` and shared across providers.
- [x] `AwsSesEmailService` implemented with `SesV2Client` and rich error handling.
- [x] `ResendEmailService` updated to use shared template.
- [x] `application.properties` updated with AWS SES placeholders and active provider toggle.
- [x] Unit tests added (`AwsSesEmailServiceTest`) — 13/13 tests passing.
- [x] Step-by-step deployment guide prepared for EC2, ECS, and EKS.
