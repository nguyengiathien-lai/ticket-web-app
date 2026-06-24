# 📋 Entity Classes Created - Database Schema

## ✅ Entities Created According to Specification

### **Core Authentication Entities** (3)
- ✅ `Account` - User accounts with email, password, verification flags
- ✅ `Role` - Predefined roles (PASSENGER, APP_ADMIN)
- ✅ `Permission` - Permission definitions

### **Transit & Ticket Entities** (5)
- ✅ `Station` - Transit stations/stops
- ✅ `TicketType` - Ticket/pass types (daily, weekly, monthly)
- ✅ `Ticket` - Individual tickets issued to users
- ✅ `PhysicalCard` - Physical NFC/RFID cards
- ✅ `TravelHistory` - Journey records with check-in/check-out

### **Order & Payment Entities** (3)
- ✅ `Order` - Customer orders
- ✅ `OrderItem` - Line items in orders
- ✅ `Payment` - Payment transactions

### **Wallet & User Features** (4)
- ✅ `Wallet` - Digital wallet for each account
- ✅ `WalletTransaction` - Transaction history
- ✅ `OtpCode` - OTP codes for verification/reset
- ✅ `Notification` - Email/SMS/Push/In-app notifications

---

## 🗄️ Database Tables Structure

| Entity | Table Name | Type | PK |
|--------|-----------|------|-----|
| Account | `accounts` | Core | UUID |
| Role | `roles` | RBAC | INT (auto) |
| Permission | `permissions` | RBAC | INT (auto) |
| Station | `stations` | Transit | UUID |
| TicketType | `ticket_types` | Transit | UUID |
| Ticket | `tickets` | Transit | UUID |
| PhysicalCard | `physical_cards` | Transit | UUID |
| TravelHistory | `travel_history` | Transit | BIGINT (auto) |
| Order | `orders` | Commerce | BIGINT (auto) |
| OrderItem | `order_items` | Commerce | BIGINT (auto) |
| Payment | `payments` | Commerce | BIGINT (auto) |
| Wallet | `wallets` | User | UUID |
| WalletTransaction | `wallet_transactions` | User | UUID |
| OtpCode | `otp_codes` | Auth | UUID |
| Notification | `notifications` | User | UUID |

---

## 🔗 Mapping Tables

| Mapping Table | Columns |
|---------------|---------|
| `account_roles` | account_id (FK), role_id (FK) |
| `role_permissions` | role_id (FK), permission_id (FK) |

---

## 📊 Key Features Implemented

### Authentication & Authorization
- ✅ Account registration (email, password, personal details)
- ✅ Email verification (is_email_verified flag)
- ✅ Password change requirement flag (mustChangePassword)
- ✅ Account status management (isActive)
- ✅ OTP-based verification (email, password reset, login)
- ✅ Role-Based Access Control (RBAC):
  - Roles: PASSENGER, APP_ADMIN (predefined)
  - Permissions: Extensible permission system
  - Account-Role mapping (Many-to-Many)
  - Role-Permission mapping (Many-to-Many)

### Ticketing System
- ✅ Multiple ticket types (daily, weekly, monthly passes)
- ✅ Ticket validity period tracking (validFrom, validUntil)
- ✅ Remaining uses tracking (remainingUses)
- ✅ Physical card support (NFC/RFID cards with UID)
- ✅ Travel history logging (check-in/check-out)
- ✅ Station tracking (origin/destination)

### E-Commerce
- ✅ Order management with order codes
- ✅ Multi-item orders (physical cards + tickets)
- ✅ Multiple payment methods (card, wallet, UPI, bank, cash)
- ✅ Payment status tracking (pending, completed, failed, refunded)
- ✅ Order status management

### Digital Wallet
- ✅ Wallet balance tracking
- ✅ Wallet transactions (credit/debit)
- ✅ Transaction reasons (topup, purchase, refund, bonus)
- ✅ Transaction reference tracking
- ✅ Wallet status management

### Notifications
- ✅ Multiple delivery channels (Email, SMS, Push, In-app)
- ✅ Notification status tracking
- ✅ Read/unread status
- ✅ Entity linking (order, ticket, payment)

---

## 🔒 Data Protection

All entities include:
- ✅ Audit timestamps (created_at, updated_at)
- ✅ Automatic timestamp management via @PrePersist/@PreUpdate
- ✅ Proper constraints (NOT NULL, UNIQUE, Foreign Keys)
- ✅ Sensible defaults (isActive, isEmailVerified, status)

---

## 🚀 Ready for Implementation

**Current Status**: ✅ Complete

All entities are now defined and ready for:
1. ✅ Hibernate auto-schema generation
2. ⏳ JPA Repository creation
3. ⏳ Service layer implementation
4. ⏳ REST Controller development
5. ⏳ DTOs & mapping
6. ⏳ Business logic & validation

---

## 📝 Notes

- User entity renamed to Account (per spec)
- BaseEntity still available for shared columns
- UUID used for user-facing entities (Account, Ticket, etc.)
- Auto-increment INT/BIGINT for internal lookups (Orders, Payments)
- BigDecimal used for monetary values (Order, Payment, Wallet)
- Comprehensive relationship mapping (1:1, 1:N, N:N)
- Status fields for state management across all entities
- Ready for integration with Spring Data JPA repositories

Next: Create repositories, services, and controllers!
