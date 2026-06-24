# Database Entity Design - Metro & Bus Ticket PWA

Following the specification provided, the following entities have been created to support the ticketing system.

## Authentication & Authorization Entities

### Account
- **Table**: `accounts`
- **Key Fields**: id (UUID), email, password, fullName, phoneNumber, address, personalId
- **Status**: isActive, isEmailVerified, mustChangePassword
- **Relationships**: Many-to-Many with Role

### Role
- **Table**: `roles`
- **Key Fields**: id (auto-increment), name, description
- **Predefined Roles**:
  - PASSENGER
  - APP_ADMIN
- **Relationships**: Many-to-Many with Permission

### Permission
- **Table**: `permissions`
- **Key Fields**: id (auto-increment), name, description
- **Relationships**: Many-to-Many with Role

## Core Transit & Ticketing Entities

### Station
- **Table**: `stations`
- **Key Fields**: id (UUID), stationCode, name, stationType, isActive
- **Purpose**: Transit stop/station management

### TicketType
- **Table**: `ticket_types`
- **Key Fields**: id (UUID), name, durationDays, price, description
- **Purpose**: Different ticket types (daily, weekly, monthly passes)

### Ticket
- **Table**: `tickets`
- **Key Fields**: id (UUID), ticketCode, isActive, validFrom, validUntil, remainingUses
- **Relationships**: 
  - Many-to-One with Account
  - Many-to-One with TicketType
  - Many-to-One with PhysicalCard (optional)

### PhysicalCard
- **Table**: `physical_cards`
- **Key Fields**: id (UUID), cardUid, status, issuedAt, expiredAt
- **Purpose**: Physical transit cards (NFC/RFID)
- **Relationships**: Many-to-One with Account

### TravelHistory
- **Table**: `travel_history`
- **Key Fields**: id (auto-increment), checkinTime, checkoutTime, transportId
- **Relationships**:
  - Many-to-One with Account
  - Many-to-One with PhysicalCard
  - Many-to-One with Ticket
  - Many-to-One with Station (checkin & checkout)
- **Purpose**: Track passenger journeys

## Order & Payment Entities

### Order
- **Table**: `orders`
- **Key Fields**: id (auto-increment), orderCode, totalAmount, status
- **Status**: PENDING, COMPLETED, FAILED, CANCELLED
- **Relationships**: Many-to-One with Account

### OrderItem
- **Table**: `order_items`
- **Key Fields**: id (auto-increment), itemType, quantity, unitPrice, subtotal
- **Item Types**: TICKET, PHYSICAL_CARD
- **Relationships**: 
  - Many-to-One with Order
  - Many-to-One with TicketType
  - Many-to-One with PhysicalCard

### Payment
- **Table**: `payments`
- **Key Fields**: id (auto-increment), transactionCode, amount, status
- **Payment Methods**: CARD, WALLET, UPI, BANK_TRANSFER, CASH
- **Status**: PENDING, COMPLETED, FAILED, REFUNDED
- **Relationships**: Many-to-One with Order

## Wallet & User Account Entities

### Wallet
- **Table**: `wallets`
- **Key Fields**: id (UUID), balance, totalTopup, totalSpent, status
- **Relationships**: One-to-One with Account
- **Purpose**: Digital wallet for prepaid travel

### WalletTransaction
- **Table**: `wallet_transactions`
- **Key Fields**: id (UUID), amount, type, reason, status
- **Transaction Types**: CREDIT, DEBIT
- **Reasons**: TOPUP, PURCHASE, REFUND, BONUS, ADJUSTMENT
- **Relationships**: Many-to-One with Wallet

### OtpCode
- **Table**: `otp_codes`
- **Key Fields**: id (UUID), code, type, isUsed, expiresAt
- **OTP Types**: EMAIL_VERIFICATION, PASSWORD_RESET, LOGIN
- **Purpose**: One-time passwords for verification
- **Relationships**: Many-to-One with Account

### Notification
- **Table**: `notifications`
- **Key Fields**: id (UUID), title, message, type, status, isRead
- **Notification Types**: EMAIL, SMS, PUSH, IN_APP
- **Relationships**: Many-to-One with Account
- **Purpose**: User notifications and alerts

## Database Relationships Summary

### Many-to-Many
- accounts ↔ roles
- roles ↔ permissions

### One-to-One
- account ↔ wallet

### One-to-Many (Reverse relationships)
- account → tickets, physical_cards, otp_codes, notifications, travel_history, orders, wallet
- role → account_roles
- permission → role_permissions
- ticket_type → tickets, order_items
- physical_card → tickets, travel_history, order_items
- station → travel_history (checkin/checkout)
- order → order_items, payments
- wallet → wallet_transactions

## Features Supported

✅ User registration & authentication  
✅ Role-based access control (PASSENGER, APP_ADMIN)  
✅ Email verification via OTP  
✅ Password reset via OTP  
✅ Digital wallet management  
✅ Ticket & pass card management  
✅ Order management  
✅ Payment processing  
✅ Travel history tracking  
✅ User notifications  
✅ Audit timestamps (created_at, updated_at)

## Next Steps

1. Create JPA Repositories for each entity
2. Implement Service layer with business logic
3. Create REST Controllers for API endpoints
4. Add DTOs for request/response payloads
5. Implement authorization/permission checks
6. Add database migration scripts (Flyway/Liquibase)
