# Meeting Notes – Customer Interaction 1

**Project:** Payment Processing System  
**Meeting Type:** Requirements Gathering with customer  
**Attendees:** customer, Development Team

---

# 1. Meeting Objective

The objective of the meeting was to gather business requirements for the Payment Processing System, clarify the expected functionality, understand business priorities, and identify the scope for the first sprint.

---

# 2. Functional Requirements

The application should simulate a complete payment processing system that supports multiple payment methods.

### Supported Payment Methods
- UPI
- Net Banking
- Credit Cards

The design should be flexible enough to support additional payment methods in the future.

---

# 3. System Actors

The system will involve three primary actors:

- **Payer** – Initiates the payment.
- **Payee** – Receives the payment.
- **Intermediate (Bank/Payment Gateway)** – Processes the transaction and collects transaction fees.

The first version of the application is intended as an internal operational dashboard rather than a customer-facing platform.

---

# 4. Dashboard Requirements

The dashboard should provide visibility into payment operations by allowing users to:

- View all transactions
- Track payment status
- View transaction history
- Monitor transaction statistics
- Analyze payment processing costs
- Support future business analytics

Authentication is not required for the first phase.

---

# 5. Transaction Validation

The payment validation process should include configurable business rules instead of hardcoded values.

Suggested validation checks include:

- Transaction amount limits
- Customer-specific limits
- Country-based limits
- Sender validation
- Receiver validation
- Dynamic transaction limits based on customer behaviour

---

# 6. Fraud Detection

Fraud detection is considered an important enhancement for the project.

Initially, a rule-based risk scoring system should be implemented using factors such as:

- High transaction amounts
- Transactions during unusual hours
- Transactions from unexpected locations
- Sudden increase in transaction value
- Frequent transactions within a short period
- Historical customer behaviour

Machine Learning can be considered in future iterations once sufficient historical data is available.

---

# 7. Data Storage Requirements

The system should maintain comprehensive transaction records for compliance and analytics.

The following information should be stored for every transaction:

- Payment ID
- Amount
- Currency
- Sender details
- Receiver details
- Payment method
- Transaction status
- Status history
- Timestamp
- Location
- Risk score
- Error information (if applicable)

Historical transaction data will be used for analytics, fraud detection, and future model training.

---

# 8. Compliance & Security

The customer emphasized the importance of regulatory compliance.

Requirements include:

- Research applicable local compliance regulations.
- Follow PCI-DSS guidelines where applicable.
- Encrypt sensitive payment information.
- Never store card numbers in plain text.

---

# 9. Fraud Detection Success Criteria

The objective is **not** to eliminate all fraud by blocking every transaction.

Instead, the system should balance:

- Preventing fraudulent transactions
- Minimizing false positives
- Maintaining a good customer experience

Large-value transactions should be treated with stricter validation than low-value transactions.

---

# 10. Future Enhancements

Potential future improvements discussed include:

- Machine Learning-based fraud detection
- Adaptive transaction limits
- Customer feedback on flagged transactions
- Risk analytics dashboard
- Customer segmentation
- Historical trend analysis

---

# 11. Sprint 1 Deliverables

The first sprint should focus on delivering a working prototype that includes:

- Payment creation
- Payment validation
- Payment processing workflow
- Transaction dashboard
- Payment history
- Basic risk scoring
- Validation framework
- Audit trail

The customer prefers reviewing a working implementation before requesting additional features.

---

# Decisions Made

- Support multiple payment methods (UPI, Net Banking, Credit Cards).
- Implement three system actors: Payer, Payee, and Intermediate.
- Build an internal operational dashboard.
- Store complete transaction history.
- Include configurable validation rules.
- Implement basic rule-based fraud detection.
- Record a risk score for every transaction.
- Ensure compliance with PCI-DSS principles.
- Focus on delivering a functional MVP in Sprint 1.

---

# Action Items

| Task | Owner |
|------|-------|
| Design payment lifecycle | Development Team |
| Implement REST APIs | Development Team |
| Design database schema | Development Team |
| Build dashboard UI | Development Team |
| Implement validation rules | Development Team |
| Develop basic fraud risk scoring | Development Team |
| Research PCI-DSS and compliance requirements | Development Team |
| Maintain transaction audit history | Development Team |
| Upload meeting notes to repository | Development Team |