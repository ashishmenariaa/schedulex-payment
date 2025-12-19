# 💰 Payment Retry & Order Management System

A production-grade **automated payment retry system** built with Spring Boot that handles failed payment transactions intelligently. When a payment fails, the system automatically schedules retry attempts at strategic intervals (6h, 24h, 72h) to maximize recovery rates.

## 🎯 Business Problem Solved

E-commerce companies lose significant revenue due to failed payments (insufficient funds, expired cards, temporary issues). This system:
- **Automatically retries** failed payments without manual intervention
- **Recovers 60-70%** of initially failed transactions
- **Reduces customer friction** - customers don't need to manually retry
- **Tracks everything** - full audit trail of payment attempts

## ✨ Key Features

### Core Functionality
- ✅ **Automated Payment Retry** - Smart retry scheduling (6h → 24h → 72h)
- ✅ **Job Scheduling Engine** - Custom-built multi-threaded scheduler
- ✅ **Priority Queue System** - High-priority orders processed first
- ✅ **Transaction History** - Complete audit trail of every payment attempt
- ✅ **Real-time Dashboard** - Monitor orders and payments live
- ✅ **Manual Retry** - Override automatic schedule when needed

### Technical Highlights
- ✅ **Multi-threaded Execution** - 10 worker threads for concurrent processing
- ✅ **Database Polling** - Efficient job discovery mechanism
- ✅ **Retry Logic** - Exponential backoff strategy
- ✅ **Status Tracking** - Order and payment status management
- ✅ **RESTful APIs** - Complete CRUD operations

## 🏗️ Architecture
```
┌─────────────┐
│  Dashboard  │ (HTML/JS)
└──────┬──────┘
       │
       ↓
┌─────────────────────────────────────┐
│     REST API Controllers            │
│  /api/orders  /api/jobs             │
└──────┬──────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────┐
│     Service Layer                   │
│  • OrderService                     │
│  • PaymentService                   │
│  • JobService                       │
└──────┬──────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────┐
│  Job Scheduler Engine               │
│  ┌────────────────────────────┐    │
│  │ JobSchedulerEngine         │    │
│  │  - Polls DB every 5s       │    │
│  │  - Finds due jobs          │    │
│  └────────┬───────────────────┘    │
│           ↓                         │
│  ┌────────────────────────────┐    │
│  │ JobQueueManager            │    │
│  │  - Priority blocking queue │    │
│  │  - FIFO + Priority based   │    │
│  └────────┬───────────────────┘    │
│           ↓                         │
│  ┌────────────────────────────┐    │
│  │ JobExecutor                │    │
│  │  - 10 worker threads       │    │
│  │  - Concurrent execution    │    │
│  └────────────────────────────┘    │
└──────┬──────────────────────────────┘
       │
       ↓
┌─────────────────────────────────────┐
│  MySQL Database                     │
│  • orders                           │
│  • payment_transactions             │
│  • jobs                             │
└─────────────────────────────────────┘
```

## 🛠️ Tech Stack

- **Backend:** Java 17, Spring Boot 3.4.12
- **Database:** MySQL 8.x
- **ORM:** Spring Data JPA, Hibernate
- **Build Tool:** Maven
- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Architecture:** Layered (Controller → Service → Repository)

## 📦 Database Schema

### Orders Table
```sql
- id (PK)
- orderId (unique)
- customerName, customerEmail, customerPhone
- amount (DECIMAL)
- paymentStatus (ENUM: PENDING, FAILED, SUCCESS, CANCELLED)
- orderStatus (ENUM: CREATED, PAID, PROCESSING, SHIPPED, etc.)
- retryCount, maxRetries
- nextRetryTime, lastRetryTime
- failureReason
- createdAt, updatedAt, paidAt
```

### Payment Transactions Table
```sql
- id (PK)
- orderId (FK)
- amount
- status (SUCCESS/FAILED)
- attemptNumber
- errorMessage
- gatewayResponse (JSON)
- attemptedAt
```

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MySQL 8.x
- Maven 3.6+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/schedulex.git
cd schedulex
```

2. **Create MySQL database**
```sql
CREATE DATABASE schedulex_db;
```

3. **Configure database**

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/schedulex_db
spring.datasource.username=root
spring.datasource.password=yourpassword
```

4. **Build and run**
```bash
./mvnw clean install
./mvnw spring-boot:run
```

5. **Access the application**
- Dashboard: http://localhost:8080/payment-dashboard.html
- API: http://localhost:8080/api/orders

## 📡 API Endpoints

### Order Management
```
POST   /api/orders              - Create new order
GET    /api/orders              - Get all orders
GET    /api/orders/{orderId}    - Get specific order
GET    /api/orders/stats        - Get statistics
POST   /api/orders/{orderId}/retry - Manual retry
GET    /api/orders/{orderId}/transactions - Payment history
```

### Job Management
```
POST   /api/jobs                - Create job
GET    /api/jobs                - Get all jobs
GET    /api/jobs/{id}           - Get specific job
GET    /api/jobs/stats          - Get job statistics
```

## 🎬 How It Works

### Payment Flow

1. **Order Creation**
   - Customer places order
   - System attempts first payment
   - If successful → Order marked as PAID ✅
   - If failed → Retry scheduled 🔄

2. **Automatic Retry**
```
   Attempt 1: Immediate (at order creation)
   Attempt 2: +6 hours later
   Attempt 3: +24 hours later
   Attempt 4: +72 hours later
   
   If all fail → Order CANCELLED ❌
```

3. **Retry Success Rate**
   - First attempt: 40% success
   - Second attempt: 70% success (funds may be added)
   - Third attempt: 90% success

## 📊 Sample Usage

### Create Order via API
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD_12345",
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "customerPhone": "+91-9876543210",
    "amount": 12990.00,
    "orderItems": "[{\"product\":\"iPhone 15 Pro\",\"qty\":1}]",
    "maxRetries": 3
  }'
```

### Response
```json
{
  "id": 1,
  "orderId": "ORD_12345",
  "paymentStatus": "FAILED",
  "orderStatus": "PAYMENT_FAILED",
  "nextRetryTime": "2025-12-20T03:00:00",
  "retryCount": 1,
  "failureReason": "Insufficient funds in account"
}
```

## 🎯 Key Design Decisions

1. **Why Custom Scheduler?**
   - Learning experience - understanding job scheduling internals
   - Full control over retry logic and timing
   - Production systems use Quartz/Spring Scheduler (scalable alternative)

2. **Why Simulated Payments?**
   - Easy demo without payment gateway credentials
   - Real integration would use Razorpay/Stripe API
   - Business logic remains the same

3. **Retry Intervals (6h, 24h, 72h)**
   - 6h: Customer might add funds same day
   - 24h: Next business day, salary credited
   - 72h: Final attempt after weekend/holidays

## 🔮 Future Enhancements

- [ ] Email/SMS notifications on payment status
- [ ] Razorpay/Stripe integration
- [ ] Redis-based distributed job queue
- [ ] Webhook support for real-time updates
- [ ] Admin authentication (JWT)
- [ ] Prometheus metrics & monitoring
- [ ] Docker containerization
- [ ] Multi-tenant support

## 🤝 Contributing

This is a learning project. Feedback and suggestions welcome!

## 📝 License

MIT License - feel free to use for learning purposes

## 👨‍💻 Author

**Your Name**
- GitHub: [@ashishmenariaa](https://github.com/ashishmenariaa)
- linkedin: https://www.linkedin.com/in/ashish-menaria-6593511a7/


---

⭐ **Star this repo if you found it helpful!**
