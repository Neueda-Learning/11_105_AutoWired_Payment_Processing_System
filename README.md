# 11_105_AutoWired_Payment_Processing_System

A full-stack Payments Processing system. It manages the complete lifecycle of a payment — creation, validation, risk scoring, transmission, and completion — while maintaining a full status-change audit trail.

## Tech Stack

* **Backend:** Java 17, Spring Boot 4, Spring Web MVC, Bean Validation, springdoc-openapi (Swagger UI)
* **Frontend:** React + TypeScript (Vite)
* **Storage:** In-memory repositories (no external DB required for this stage)

## Project Structure

```
server/   # Spring Boot REST API
client/   # React + TypeScript frontend
docs/     # Requirements and design docs
```

## Payment Lifecycle

```
CREATED → VALIDATED → SENT → COMPLETED
                  ↓
              FAILED (can occur at any stage)
```