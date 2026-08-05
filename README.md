# AutoWired Payment Processing System

A comprehensive, enterprise-grade payment processing platform with multi-factor authentication, real-time fraud detection, dynamic fee management, and multi-currency support. Built with modern technologies and security best practices.

## 🚀 Features

### Customer Portal
- **User Registration & Authentication** - Secure PIN-based registration with hashed storage
- **Multi-Factor Authentication** - PIN and OTP-based payment verification
- **Account Management** - Link multiple bank accounts and payment methods
- **Payment Methods** - Support for Credit Card, UPI, Net Banking
- **Multi-Currency Support** - USD, EUR, GBP, INR, JPY, AUD, CAD with real-time conversion
- **Transaction History** - Complete payment tracking with status updates
- **Daily Spending Limits** - Configurable per-user transaction limits

### Bank Admin Dashboard
- **Real-Time Fraud Monitoring** - Advanced risk scoring with multiple detection algorithms
- **Flagged Transactions Review** - High-risk payment investigation and management
- **Dynamic Fee Rules** - Configurable percentage/flat fees with caps and date ranges
- **User Management** - Onboarding, verification, and user administration
- **Analytics & Reporting** - Platform-wide transaction insights
- **Currency Rate Management** - Update exchange rates in real-time

### Security & Compliance
- **SHA-256 Hashing** - All PINs and OTPs securely hashed
- **Multi-Factor Authentication** - Mandatory for all transactions
- **Risk Scoring Engine** - Detects high-value, odd-hour, velocity, and spike patterns
- **Idempotency Keys** - Prevents duplicate payment processing
- **Audit Trail** - Complete payment status history tracking
- **Rate Limiting** - OTP resend limits to prevent abuse

## 🛠️ Tech Stack

### Backend
- **Java 17** - Modern Java with latest features
- **Spring Boot 4.0.7** - Production-ready application framework
- **Spring Web MVC** - RESTful API architecture
- **Spring Data JDBC** - Database access layer
- **MySQL** - Relational database for production data
- **Bean Validation** - Input validation and constraints
- **springdoc-openapi** - Interactive API documentation (Swagger UI)
- **JUnit 5 & Mockito** - Comprehensive test coverage (69 tests)

### Frontend
- **React 18** - Modern UI library
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool and dev server
- **React Router** - Client-side routing
- **Axios** - HTTP client for API calls
- **Tailwind CSS** - Utility-first styling

## 📁 Project Structure

```
.
├── server/                          # Spring Boot backend
│   ├── src/main/java/
│   │   └── com/payment/server/
│   │       ├── config/              # Configuration classes
│   │       ├── controller/          # REST API endpoints
│   │       ├── dto/                 # Data transfer objects
│   │       ├── exception/           # Custom exceptions
│   │       ├── model/               # Domain models
│   │       ├── repository/          # Data access layer
│   │       └── service/             # Business logic
│   │           ├── AuthenticationService.java
│   │           ├── PaymentService.java
│   │           ├── RiskScoringService.java
│   │           ├── FeeCalculationService.java
│   │           ├── UserService.java
│   │           └── ...
│   └── src/test/java/               # Unit & integration tests
│       └── com/payment/server/service/
│           ├── AuthenticationServiceTest.java (10 tests)
│           ├── FeeCalculationServiceTest.java (9 tests)
│           ├── RiskScoringServiceTest.java (9 tests)
│           ├── UserServiceTest.java (15 tests)
│           ├── CurrencyConversionServiceTest.java (17 tests)
│           ├── PaymentServiceTest.java (5 tests)
│           └── PaymentValidationServiceTest.java (3 tests)
│
├── client/                          # React frontend
│   ├── src/
│   │   ├── components/              # Reusable UI components
│   │   ├── pages/                   # Page components
│   │   │   ├── Landing.tsx          # Landing page
│   │   │   ├── bank/                # Admin dashboard
│   │   │   └── user/                # Customer portal
│   │   ├── services/                # API integration
│   │   └── types/                   # TypeScript definitions
│   └── public/                      # Static assets
│
└── docs/                            # Documentation
```

## 💳 Payment Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                     Payment Flow                            │
└─────────────────────────────────────────────────────────────┘

1. PENDING_AUTHENTICATION
   ↓ (Initiate payment with auth method)
   
2. CREATED
   ↓ (Authenticate with PIN/OTP)
   
3. PENDING_VALIDATION
   ↓ (Validate amount, currency, accounts, payment method)
   
4. VALIDATED
   ↓ (Calculate risk score & fees)
   
5. PENDING_SENDING
   ↓ (Process transaction)
   
6. SENT
   ↓ (Complete transfer)
   
7. COMPLETED ✓

   ↓ (Any stage can fail)
   
   FAILED ✗
```

### Payment Status States
- `PENDING_AUTHENTICATION` - Awaiting PIN/OTP verification
- `CREATED` - Authenticated, awaiting validation
- `PENDING_VALIDATION` - Validation in progress
- `VALIDATED` - Passed all validation checks
- `PENDING_SENDING` - Transaction processing
- `SENT` - Payment transmitted
- `COMPLETED` - Successfully completed
- `FAILED` - Payment failed (with reason)

## 🔒 Risk Scoring Algorithm

The system evaluates each payment using multiple risk factors:

| Risk Factor | Points | Threshold |
|-------------|--------|-----------|
| High Amount | +40 | ≥ ₹50,000 |
| Odd Hours | +20 | 12am-5am |
| High Velocity | +40 | ≥3 txns/hour |
| Spending Spike | +30 | ≥3x user average |

**Total Score:** Max 100 (capped)  
**Review Threshold:** 70+ points (flagged for manual review)

## 💰 Fee Calculation

- **Rule-Based System** - Configurable by payment method and amount range
- **Fee Types** - Percentage or flat fees
- **Caps** - Minimum and maximum fee limits
- **Time-Based** - Rules with effective date ranges
- **Default Fallback** - 1% fee (max ₹500) when no rule matches

## 🚦 Getting Started

### Prerequisites
- Java 17 or higher
- Node.js 18+ and npm
- MySQL 8.0+
- Maven 3.8+

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd server
   ```

2. **Configure database**
   ```bash
   # Update application.properties with your MySQL credentials
   spring.datasource.url=jdbc:mysql://localhost:3306/payment_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Run tests**
   ```bash
   ./mvnw test
   ```

5. **Access Swagger UI**
   ```
   http://localhost:8080/swagger-ui.html
   ```

### Frontend Setup

1. **Navigate to client directory**
   ```bash
   cd client
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start development server**
   ```bash
   npm run dev
   ```

4. **Access application**
   ```
   http://localhost:5173
   ```

### Build for Production

**Backend:**
```bash
cd server
./mvnw clean package
java -jar target/server-0.0.1-SNAPSHOT.jar
```

**Frontend:**
```bash
cd client
npm run build
# Output in dist/ directory
```

## 📡 API Documentation

Interactive API documentation available at:
```
http://localhost:8080/swagger-ui.html
```

### Key Endpoints

**Authentication:**
- `POST /api/auth/initiate` - Start payment with auth challenge
- `POST /api/auth/authenticate/{paymentId}` - Verify PIN/OTP
- `POST /api/auth/resend-otp/{paymentId}` - Resend OTP code

**Payments:**
- `GET /api/payments` - List all payments
- `GET /api/payments/{id}` - Get payment details
- `GET /api/payments/flagged` - Get high-risk payments
- `GET /api/payments/{id}/history` - Get status history

**Users:**
- `POST /api/users/register` - Register new user
- `GET /api/users/{id}` - Get user details
- `POST /api/users/{id}/bank-accounts` - Add bank account
- `POST /api/users/{id}/payment-methods` - Add payment method

**Admin:**
- `GET /api/admin/fee-rules` - List fee rules
- `POST /api/admin/fee-rules` - Create fee rule
- `PUT /api/admin/currency-rates` - Update exchange rates

## 🧪 Testing

The project includes comprehensive test coverage:

- **69 total tests** across all service layers
- **Unit tests** with Mockito for isolated testing
- **Integration tests** with Spring Boot Test
- **Test coverage** for all critical business logic

Run tests:
```bash
cd server
./mvnw test
```

## 🔐 Security Features

- **Password Hashing** - SHA-256 for PINs and OTPs
- **Token-Based Storage** - Credit card numbers never stored raw
- **Idempotency** - Duplicate payment prevention
- **Rate Limiting** - OTP resend limits (max 3)
- **Input Validation** - Bean validation on all inputs
- **SQL Injection Protection** - Parameterized queries
- **CORS Configuration** - Secure cross-origin requests

## 📊 Database Schema

Key entities:
- `users` - Platform users with PIN authentication
- `bank_accounts` - Linked bank accounts with balances
- `payment_methods` - Credit cards, UPI, net banking
- `payments` - Transaction records with full lifecycle
- `payment_status_history` - Complete audit trail
- `auth_challenges` - OTP/PIN verification records
- `transaction_fee_rules` - Dynamic fee configuration
- `customers` - Legacy customer records

## 🌍 Multi-Currency Support

- **Base Currency:** USD
- **Ledger Currency:** INR
- **Supported:** USD, EUR, GBP, INR, JPY, AUD, CAD
- **Real-time Conversion** - Configurable exchange rates
- **Admin Updates** - Dynamic rate management

## 📝 License

This project is part of a payment processing system demonstration.

## 👥 Contributing

Contributions are welcome! Please follow the existing code style and add tests for new features.

## 📞 Support

For issues or questions, please open an issue in the repository.