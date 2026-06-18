# Metro & Bus Ticket PWA - API Specification
## Use Cases & Corresponding APIs

---

## 1. AUTHENTICATION & ACCOUNT MANAGEMENT

### 1.1 User Registration (Step 1: Submit Registration Details)

**Use Case:** New user submits registration details with email verification requirement

**Main Flow:**
1. User enters registration form with email, password, first name, and last name
2. System validates input (email format, password strength, required fields)
3. System checks if email already exists in database
4. If email is new, system generates 6-digit OTP code
5. System sends OTP to user's email address
6. System creates temporary session record with pending verification status
7. System returns sessionId and confirms OTP was sent
8. Frontend displays OTP input screen with countdown timer (2 minutes expiry)

**API Endpoint:** `POST /api/auth/register`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (202 Accepted):**
```json
{
  "status": "pending_verification",
  "email": "user@example.com",
  "message": "OTP code has been sent to your email",
  "sessionId": "reg_session_abc123",
  "otpExpiresIn": 120
}
```

---

### 1.1b User Registration (Step 2: Verify OTP)
**Use Case:** User verifies their email through OTP code sent to their email address

**Main Flow:**
1. User receives OTP code in email inbox
2. User enters 6-digit OTP code in the frontend form
3. User submits verification request with sessionId, email, and OTP code
4. System validates OTP code (checks if code matches, hasn't expired, and attempts remaining)
5. If valid, system creates new user account with verified email status
6. System deletes temporary session record and OTP data for security
7. System returns confirmation with userId and account creation timestamp
8. System marks user as email-verified, allowing them to proceed to login

**API Endpoint:** `POST /api/auth/verify-otp`

**Request:**
```json
{
  "sessionId": "reg_session_abc123",
  "email": "user@example.com",
  "otpCode": "123456"
}
```

**Response (200 OK):**
```json
{
  "status": "verified",
  "email": "user@example.com",
  "message": "Email verified successfully. Account registration completed.",
  "userId": "user123",
  "accountCreatedAt": "2026-06-07T11:16:16Z"
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": {
    "code": "INVALID_OTP",
    "message": "Invalid or expired OTP code",
    "attemptsRemaining": 2,
    "timestamp": "2026-06-07T11:16:16Z"
  }
}
```

---

### 1.1c Resend OTP Code
**Use Case:** User requests a new OTP code if the previous one expired or was not received

**Main Flow:**
1. User clicks "Didn't receive OTP?" or "Resend OTP" button on verification screen
2. System validates that sessionId is still valid and not expired
3. System validates that user hasn't exceeded resend attempt limit (3 attempts)
4. If validation passes, system generates new 6-digit OTP code
5. System invalidates previous OTP code in database
6. System sends new OTP to user's email address
7. System resets countdown timer to 2 minutes
8. System returns success message with remaining resend attempts

**API Endpoint:** `POST /api/auth/resend-otp`

**Request:**
```json
{
  "sessionId": "reg_session_abc123",
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "status": "otp_sent",
  "email": "user@example.com",
  "message": "New OTP code has been sent to your email",
  "otpExpiresIn": 120,
  "attemptsRemaining": 3
}
```

---

### 1.2 User Login

**Use Case:** Registered user logs in to their account

**Main Flow:**
1. User enters email and password in login form
2. System validates that both fields are provided
3. System checks if email exists in user database and email is verified
4. If email exists, system retrieves password hash from database
5. System compares submitted password with stored hash using bcrypt algorithm
6. If password matches, system generates JWT access token (expires in 1 hour)
7. System generates refresh token for long-term session management (expires in 7 days)
8. System logs login activity with timestamp and IP address
9. System returns access token, refresh token, and expiration time
10. Frontend stores tokens in secure storage (HttpOnly cookies or secure localStorage)

**API Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "userId": "user123",
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}
```

---

### 1.3 User Logout

**Use Case:** User logs out from the application

**Main Flow:**
1. User clicks logout button in application menu
2. Frontend extracts userId from authentication context or token
3. Frontend sends logout request to backend
4. System validates that userId is valid and authenticated
5. System blacklists or invalidates user's refresh token in database
6. System logs logout activity with timestamp
7. System optionally clears any active sessions or temporary data for this user
8. System returns success confirmation
9. Frontend clears stored tokens from secure storage
10. Frontend redirects user to login/home page

**API Endpoint:** `POST /api/auth/logout`

**Request:**
```json
{
  "userId": "user123"
}
```

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

---

### 1.4 Update Account Information

**Use Case:** User updates their profile details (phone, address, personal id)

**Main Flow:**
1. User navigates to account settings/profile page
2. System retrieves and displays current profile information
3. User modifies desired fields (phone, address, personal id)
4. User clicks save/submit button
5. System validates all input fields (phone format, required fields)
6. System updates user profile in database with modified information
7. System logs profile update activity with timestamp and changed fields
8. System returns updated user profile information
9. Frontend displays confirmation message to user

**API Endpoint:** `PUT /api/users/{userId}`

**Request:**
```json
{
  "phoneNumber": "+84987654321",
  "address": "123 Main St, City, Country",
  "personalId": "ID123456789"
}
```

**Response (200 OK):**
```json
{
  "userId": "user123",
  "phoneNumber": "+84987654321",
  "address": "123 Main St, City, Country",
  "personalId": "ID123456789",
  "updatedAt": "2026-06-07T12:00:00Z"
}
```

---

### 1.5 Get User Profile

**Use Case:** User retrieves their current profile information

**Main Flow:**
1. User navigates to account or profile section
2. System verifies user is authenticated via access token
3. System retrieves userId from authentication context
4. System queries user database to fetch all profile data (email, name, phone, etc.)
5. System also retrieves user metadata (account creation date, last updated date)
6. System validates that user has permission to view this profile
7. System returns complete profile information
8. Frontend displays profile information in user-friendly format

**API Endpoint:** `GET /api/users/{userId}`

**Response (200 OK):**
```json
{
  "userId": "user123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+84912345678",
  "address": "123 Main St, City, Country",
  "personalId": "ID123456789",
  "createdAt": "2026-06-07T11:16:16Z",
  "updatedAt": "2026-06-07T12:00:00Z"
}
```

---

## 2. PHYSICAL CARD MANAGEMENT

### 2.1 View Available Card Packages

**Use Case:** User views available physical card options with ticket packages

**Main Flow:**
1. User navigates to "Buy Card" or "Physical Cards" section
2. System retrieves all active card packages from database
3. System includes package details (name, ticket count, validity period, price)
4. System checks current user's location/region if different packages apply regionally
5. System calculates promotional discounts if any are active
6. System returns sorted list of available packages (by popularity or price)
7. Frontend displays packages in grid or list format with price and features
8. User can click on a package to see detailed information (terms, conditions, benefits)

**API Endpoint:** `GET /api/cards/packages`

**Response (200 OK):**
```json
{
  "packages": [
    {
      "packageId": "pkg001",
      "cardName": "Student Pass",
      "validityDays": 30,
      "price": 150000,
      "currency": "VND",
      "description": "20 trips valid for 30 days"
    },
    {
      "packageId": "pkg002",
      "cardName": "Commuter Pass",
      "validityDays": 60,
      "price": 300000,
      "currency": "VND",
      "description": "50 trips valid for 60 days"
    }
  ]
}
```

---

### 2.2 Purchase Physical Card with Package

**Use Case:** User buys a physical card with ticket package attached

**Main Flow:**
1. User selects desired card package from available options
2. User specifies delivery address (or uses saved address from profile)
3. User chooses delivery method (standard/express) if available
4. System calculates total amount (package price + delivery fee + taxes)
5. System displays order summary for user confirmation
6. User selects payment method (credit card, wallet, bank transfer)
7. System initiates payment processing through payment gateway
8. After successful payment, system creates order record in database
9. System sends order confirmation email with order ID, tracking details, and expected delivery date
10. System sends card production request to card management party with order details
11. System updates order status to "processing".
12. System returns order confirmation to frontend
13. User can view order status anytime through order tracking feature

**API Endpoint:** `POST /api/cards/purchase`

**Request:**
```json
{
  "userId": "user123",
  "packageId": "pkg001",
  "paymentMethod": "credit_card",
  "deliveryAddress": "123 Main Street, District 1, HCMC"
}
```

**Response (201 Created):**
```json
{
  "orderId": "order123",
  "userId": "user123",
  "cardId": "card_abc123",
  "packageId": "pkg001",
  "status": "in delivery",
  "deliveryAddress": "123 Main Street, District 1, HCMC",
  "estimatedDelivery": "2026-06-10",
  "createdAt": "2026-06-07T11:16:16Z"
}
```

---

<!-- ### 2.3 Get Physical Card Status

**Use Case:** User checks the delivery status of their physical card

**Main Flow:**
1. User navigates to "My Cards" or "Orders" section
2. User selects a specific card order to view details
3. System retrieves card and order information from database
4. System displays current status (pending_production, shipped, in_transit, delivered)
5. If card has been shipped, system displays tracking number and carrier information
6. System checks with shipping provider for real-time tracking updates
7. System displays remaining tickets on card and expiry date
8. If card is already delivered and not activated, system shows activation instructions
9. System returns complete card status and shipping information
10. Frontend displays status timeline showing order progress

**API Endpoint:** `GET /api/cards/{cardId}/status`

**Response (200 OK):**
```json
{
  "cardId": "card_abc123",
  "cardNumber": "1234567890",
  "packageId": "pkg001",
  "status": "shipped",
  "trackingNumber": "TRK123456789",
  "ticketsRemaining": 20,
  "validUntil": "2026-07-07",
  "shippedAt": "2026-06-08",
  "estimatedDelivery": "2026-06-10"
}
``` -->

---

## 3. ONLINE TICKET PURCHASE

### 3.1 Get Available Ticket Types

**Use Case:** User views available ticket types for online purchase

**Main Flow:**
1. User navigates to "Buy Ticket" section
2. System displays available ticket types (e.g., single trip, day pass, weekly pass)
3. User selects a ticket type
4. System retrieves pricing information (base fare, any promotional discounts)
**API Endpoint:** `GET /api/tickets/type`

**Response (200 OK):**
```json
{
  "ticketTypes": [
    {
      "type": "single_trip",
      "description": "One-way trip between two stations",
      "price": 15000,
      "currency": "VND"
    },
    {
      "type": "day_pass",
      "description": "Unlimited trips for one day",
      "price": 50000,
      "currency": "VND"
    },
    {
      "type": "weekly_pass",
      "description": "Unlimited trips for one week",
      "price": 200000,
      "currency": "VND"
    }
  ]
}
```

---

### 3.2 Buy Ticket

**Use Case:** User purchases an online ticket for a specific type.

**Main Flow:**
1. User selects desired ticket type from available options
2. System displays ticket type details and price confirmation
3. User enters passenger details
4. System displays total amount
5. User selects payment method
6. System processes payment through payment gateway
7. If payment successful, system send ticket request to ticketing management system
8. Upper system generates unique QR codes for each ticket
9. System sends confirmation email with ticket details and QR codes
15. System assigns tickets to user account with "active" status
16. System returns ticket confirmation with QR codes to frontend
17. User can view/download tickets or save to digital wallet

**API Endpoint:** `POST /api/tickets/purchase`

**Request:**
```json
{
  "userId": "user123",
  "ticketType": "route001",
  "paymentMethod": "digital_wallet"
}
```

**Response (201 Created):**
```json
{
  "ticketId": "ticket_xyz789",
  "orderId": "order456",
  "userId": "user123",
  "ticketType": "route001",
  "origin": "Central Station",
  "destination": "Airport Terminal",
  "totalPrice": 15000,
  "currency": "VND",
  "status": "confirmed",
  "qrCode": "QR_DATA_STRING_HERE",
  "confirmationNumber": "CONF123456",
  "purchasedAt": "2026-06-07T11:16:16Z"
}
```

---

### 3.4 View Online Tickets

**Use Case:** User views their purchased online tickets

**Main Flow:**
1. User navigates to "My Tickets" section
2. System retrieves all tickets associated with user account
3. System filters and categorizes tickets by status (upcoming, completed, cancelled, expired)
4. System displays most recent/upcoming tickets first
5. System applies pagination if many tickets exist (limit tickets per page)
6. For each ticket, system shows: route, departure time, status, QR code, confirmation number
7. User can optionally filter by date range or route
8. System returns paginated list of tickets matching filters
9. Frontend displays tickets in list or card format
10. User can click on individual ticket to see full details, download, or view QR code

**API Endpoint:** `GET /api/users/{userId}/tickets`

**Query Parameters:**
- `status` - Filter by status (upcoming, completed, cancelled)
- `limit` - Number of results
- `offset` - Pagination offset

**Response (200 OK):**
```json
{
  "tickets": [
    {
      "ticketId": "ticket_xyz789",
      "routeId": "route001",
      "origin": "Central Station",
      "destination": "Airport Terminal",
      "departureTime": "2026-06-07T14:30:00Z",
      "arrivalTime": "2026-06-07T15:15:00Z",
      "status": "active",
      "qrCode": "QR_DATA_STRING_HERE",
      "confirmationNumber": "CONF123456",
      "purchasedAt": "2026-06-07T11:16:16Z"
    }
  ],
  "total": 1,
  "limit": 10,
  "offset": 0
}
```

---


## 4. QR CODE & CHECK-IN/CHECK-OUT

### 4.1 Get QR Code for Ticket

**Use Case:** User retrieves QR code of their ticket for check-in/check-out at gates

**Main Flow:**
1. User is viewing a ticket (from "My Tickets" or email)
2. User clicks "View QR Code" or "Show QR" button
3. System retrieves ticket details from database
4. System verifies ticket is still valid (not expired, not cancelled, within travel date)
5. System checks that current time is within travel window (e.g., departure ±2 hours for international)
6. System generates or retrieves pre-generated QR code image
7. System includes encoded data: ticketId, userId, origin, destination, departure time
8. System returns QR code as image (PNG/SVG format) plus raw data string
9. Frontend displays large QR code for easy scanning at gates
10. User can refresh QR code if needed (generates new code with fresh timestamp)

**API Endpoint:** `GET /api/tickets/{ticketId}/qrcode`

**Response (200 OK):**
```json
{
  "ticketId": "ticket_xyz789",
  "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA...",
  "qrCodeData": "QR_DATA_STRING_HERE",
  "ticketStatus": "valid",
  "validUntil": "2026-06-07T15:15:00Z",
  "origin": "Central Station",
  "destination": "Airport Terminal"
}
```

---

### 4.2 Validate QR Code (Check-In)

**Use Case:** Gate scanner validates QR code for passenger check-in

**Main Flow:**
1. Passenger arrives at station/gate entrance
2. Gate staff/scanner device scans QR code from passenger's phone or printed ticket
3. Scanner application sends QR data to backend validation service
4. System decodes QR code to extract ticketId
5. System retrieves ticket details from database
6. System validates:
   - Ticket exists and belongs to valid user
   - Ticket is in "active" or "valid" status (not cancelled/expired)
   - Travel date matches today's date
   - Current time is within check-in window (typically 1-2 hours before departure)
   - Ticket hasn't already been checked in (prevent duplicate scanning)
7. If all validations pass, system marks ticket status as "checked_in"
8. System records check-in timestamp and station ID
9. System retrieves passenger name and route details
10. System returns validation success with passenger/ticket info
11. Gate system displays green indicator (✓) and opens gate access
12. System logs access record for security audit

**API Endpoint:** `POST /api/qrcode/validate-checkin`

**Request:**
```json
{
  "qrCodeData": "QR_DATA_STRING_HERE",
  "stationId": "station_central_001",
  "timestamp": "2026-06-07T14:28:00Z"
}
```

**Response (200 OK):**
```json
{
  "ticketId": "ticket_xyz789",
  "isValid": true,
  "passengerName": "John Doe",
  "origin": "Central Station",
  "destination": "Airport Terminal",
  "checkedInAt": "2026-06-07T14:28:00Z",
  "status": "checked_in"
}
```

---

### 4.3 Validate QR Code (Check-Out)

**Use Case:** Gate scanner validates QR code for passenger check-out

**Main Flow:**
1. Passenger arrives at destination station/gate exit
2. Gate staff/scanner device scans QR code from passenger's phone or ticket
3. Scanner application sends QR data to backend validation service
4. System decodes QR code to extract ticketId
5. System retrieves ticket details from database
6. System validates:
   - Ticket exists and belongs to valid user
   - Ticket is in "checked_in" status (must have checked in first)
   - Current location matches destination on ticket
   - Current time is within reasonable check-out window (after arrival time ±30 mins)
   - Ticket hasn't already been checked out (prevent duplicate scanning)
7. If all validations pass, system marks ticket status as "completed"
8. System records check-out timestamp and exit station ID
9. System calculates final travel information (actual duration, distance traveled)
10. For card-based tickets, system deducts one ticket from card balance
11. For physical cards, system creates travel record with tap-in/tap-out times
12. System returns validation success with travel completion details
13. Gate system displays green indicator and opens gate/barrier
14. System creates and stores travel history record for user

**API Endpoint:** `POST /api/qrcode/validate-checkout`

**Request:**
```json
{
  "qrCodeData": "QR_DATA_STRING_HERE",
  "stationId": "station_airport_002",
  "timestamp": "2026-06-07T15:10:00Z"
}
```

**Response (200 OK):**
```json
{
  "ticketId": "ticket_xyz789",
  "isValid": true,
  "passengerName": "John Doe",
  "origin": "Central Station",
  "destination": "Airport Terminal",
  "checkedOutAt": "2026-06-07T15:10:00Z",
  "status": "completed",
  "fareDeducted": 15000
}
```

---

## 5. PAYMENT MANAGEMENT

### 5.1 Get Payment Methods

**Use Case:** User views available payment methods

**Main Flow:**
1. User navigates to payment or checkout section
2. System retrieves all active payment methods configured in the system
3. System includes payment method details (type, name, acceptance info)
4. System checks which payment methods are currently enabled/operational
5. System organizes methods by category (cards, digital wallets, bank transfer)
6. System may show payment method icons/logos for visual identification
7. System returns sorted list of available payment methods
8. Frontend displays payment options clearly for user selection
9. User can select preferred method based on availability and convenience

**API Endpoint:** `GET /api/payments/methods`

**Response (200 OK):**
```json
{
  "paymentMethods": [
    {
      "methodId": "method001",
      "type": "credit_card",
      "name": "Visa/Mastercard",
      "enabled": true
    },
    {
      "methodId": "method002",
      "type": "digital_wallet",
      "name": "Momo/Zalopay",
      "enabled": true
    },
    {
      "methodId": "method003",
      "type": "bank_transfer",
      "name": "Bank Transfer",
      "enabled": true
    }
  ]
}
```

---

### 5.2 Add Payment Method

**Use Case:** User adds a new payment method to their account

**Main Flow:**
1. User navigates to account settings or payment methods management
2. User clicks "Add Payment Method" or "Add Card"
3. System displays payment method input form (credit card, digital wallet details, bank account)
4. User enters payment details securely (tokenized input prevents direct exposure)
5. System validates input:
   - Card number format (Luhn algorithm for credit cards)
   - Expiry date not expired
   - CVV format correct
   - Other required fields present
6. System sends card details to PCI-compliant payment processor
7. Payment processor tokenizes and returns secure token to application
8. System stores only token and masked details (last 4 digits) in database
9. System marks payment method as "active" and optionally as default
10. System sets up automatic billing if applicable
11. System logs payment method addition for security audit
12. System returns confirmation with masked payment details
13. Frontend displays success message and new payment method in list

**API Endpoint:** `POST /api/payments/methods`

**Request:**
```json
{
  "userId": "user123",
  "type": "credit_card",
  "cardNumber": "4532015112830366",
  "cardholderName": "John Doe",
  "expiryDate": "12/28",
  "cvv": "123",
  "isDefault": true
}
```

**Response (201 Created):**
```json
{
  "paymentMethodId": "pm_abc123",
  "type": "credit_card",
  "lastFourDigits": "0366",
  "cardholderName": "John Doe",
  "isDefault": true,
  "addedAt": "2026-06-07T11:16:16Z"
}
```

---

### 5.3 Get Payment History

**Use Case:** User views their payment transaction history

**Main Flow:**
1. User navigates to "Transaction History" or "Payments" section
2. User optionally selects date range filter (e.g., last 30 days, custom range)
3. System retrieves all payment transactions for user account from database
4. System filters by date range if provided
5. System includes transaction details: amount, type, description, payment method, status, date
6. System calculates running totals (total spent in period, breakdown by transaction type)
7. System applies pagination (limit results to 10-50 per page)
8. System sorts transactions by date (most recent first)
9. System returns paginated transaction list
10. Frontend displays transactions in table/list format with details
11. User can click on transaction to see extended details (invoice, receipt, etc.)
12. User can export transaction history as PDF or CSV if needed

**API Endpoint:** `GET /api/users/{userId}/payments`

**Query Parameters:**
- `startDate` - Start date (YYYY-MM-DD)
- `endDate` - End date (YYYY-MM-DD)
- `limit` - Number of results
- `offset` - Pagination offset

**Response (200 OK):**
```json
{
  "payments": [
    {
      "transactionId": "txn_001",
      "amount": 15000,
      "currency": "VND",
      "type": "ticket_purchase",
      "description": "Online Ticket - Central Station to Airport Terminal",
      "paymentMethod": "credit_card",
      "status": "completed",
      "timestamp": "2026-06-07T11:16:16Z"
    },
    {
      "transactionId": "txn_002",
      "amount": 150000,
      "currency": "VND",
      "type": "card_purchase",
      "description": "Physical Card - Student Pass",
      "paymentMethod": "digital_wallet",
      "status": "completed",
      "timestamp": "2026-06-05T14:30:00Z"
    }
  ],
  "total": 2,
  "totalSpent": 165000,
  "limit": 10,
  "offset": 0
}
```

---

## 6. TICKET & TRAVEL HISTORY

## 6. TICKET & TRAVEL HISTORY

### 6.1 Get Ticket History

**Use Case:** User retrieves their ticket purchase and usage history

**Main Flow:**
1. User navigates to "Ticket History" or "My Tickets Archive"
2. User optionally applies filters: date range, ticket type (online/card), status
3. System retrieves all tickets associated with user account from database
4. System applies filters if provided
5. System includes ticket details: route, purchase date, travel date, price, status
6. System sorts tickets by purchase date (most recent first)
7. System applies pagination (limit results to 10-50 per page)
8. System calculates summary statistics: total tickets, total spent, date range coverage
9. System returns paginated ticket history list
10. Frontend displays tickets in list or grid format
11. User can view detailed information for each ticket (route map, passenger info, etc.)
12. User can download ticket as PDF or receipt if needed
13. User can potentially rebook/repurchase similar route if they traveled it before

**API Endpoint:** `GET /api/users/{userId}/tickets/history`

**Query Parameters:**
- `startDate` - Start date (YYYY-MM-DD)
- `endDate` - End date (YYYY-MM-DD)
- `ticketType` - Filter by type (online, card)
- `limit` - Number of results
- `offset` - Pagination offset

**Response (200 OK):**
```json
{
  "ticketHistory": [
    {
      "ticketId": "ticket_xyz789",
      "ticketType": "online",
      "origin": "Central Station",
      "destination": "Airport Terminal",
      "purchaseDate": "2026-06-07T11:16:16Z",
      "departureTime": "2026-06-07T14:30:00Z",
      "arrivalTime": "2026-06-07T15:15:00Z",
      "price": 15000,
      "status": "used"
    },
    {
      "ticketId": "ticket_abc456",
      "ticketType": "card",
      "cardId": "card_abc123",
      "origin": "District 1 Station",
      "destination": "District 3 Station",
      "purchaseDate": "2026-06-05T10:00:00Z",
      "departureTime": "2026-06-05T16:00:00Z",
      "arrivalTime": "2026-06-05T16:20:00Z",
      "price": 0,
      "status": "used"
    }
  ],
  "total": 2,
  "limit": 10,
  "offset": 0
}
```

---

### 6.2 Get Travel History (Tap In/Tap Out Records)

**Use Case:** User retrieves detailed travel history showing check-in and check-out at stations

**Main Flow:**
1. User navigates to "Travel History" or "Journey Records"
2. User optionally applies filters: date range, origin/destination stations
3. System retrieves all completed travel records for user from database
4. System retrieves associated check-in and check-out data for each trip
5. System includes travel details: origin, destination, check-in time, check-out time, duration, fare
6. System includes vehicle information: type (metro/bus), vehicle number, line/route number
7. System includes trip status and any exceptions (if trip incomplete or unusual patterns)
8. System applies filters if provided
9. System sorts travels by check-in date (most recent first)
10. System applies pagination (limit results to 10-50 per page)
11. System calculates summary: total trips, total distance, total time spent traveling
12. System returns paginated travel history list
13. Frontend displays travels in timeline or list format with route maps
14. User can view trip details: full route, stops, transfer information if applicable

**API Endpoint:** `GET /api/users/{userId}/travels`

**Query Parameters:**
- `startDate` - Start date (YYYY-MM-DD)
- `endDate` - End date (YYYY-MM-DD)
- `limit` - Number of results
- `offset` - Pagination offset

**Response (200 OK):**
```json
{
  "travelHistory": [
    {
      "travelId": "travel_123",
      "ticketId": "ticket_xyz789",
      "origin": "Central Station",
      "destination": "Airport Terminal",
      "checkInTime": "2026-06-07T14:28:00Z",
      "checkOutTime": "2026-06-07T15:10:00Z",
      "duration": 42,
      "fare": 15000,
      "transportType": "metro",
      "vehicleNumber": "L1-001",
      "status": "completed"
    },
    {
      "travelId": "travel_124",
      "ticketId": "ticket_abc456",
      "origin": "District 1 Station",
      "destination": "District 3 Station",
      "checkInTime": "2026-06-05T16:00:00Z",
      "checkOutTime": "2026-06-05T16:20:00Z",
      "duration": 20,
      "fare": 7500,
      "transportType": "bus",
      "vehicleNumber": "BUS-102",
      "status": "completed"
    }
  ],
  "total": 2,
  "totalDistance": "25 km",
  "totalTrips": 2,
  "limit": 10,
  "offset": 0
}
```

---

### 6.3 Get Travel Statistics

**Use Case:** User views their travel statistics and usage patterns

**Main Flow:**
1. User navigates to "Travel Statistics" or "My Analytics" section
2. User selects time period for analysis: week, month, year, or custom range
3. System retrieves all travel records within selected period
4. System calculates statistics:
   - Total number of trips taken
   - Total distance traveled (sum of all trips)
   - Total money spent (sum of all fares)
   - Average trips per day/week
   - Most frequently used route (origin-destination pair)
   - Preferred transport type (metro vs bus usage breakdown)
   - Peak travel times (busiest hour/day of week)
   - Peak travel days (Monday-Sunday usage patterns)
5. System identifies travel trends: increasing/decreasing usage, seasonal patterns
6. System calculates environmental impact if available (CO2 saved using public transport)
7. System returns comprehensive statistics object
8. Frontend displays statistics in various formats: charts, graphs, tables
9. Frontend shows visualizations: pie charts (transport types), bar charts (daily usage), heat maps (peak times)
10. User can compare statistics across different periods

**API Endpoint:** `GET /api/users/{userId}/travels/statistics`

**Query Parameters:**
- `period` - Time period (week, month, year)

**Response (200 OK):**
```json
{
  "statistics": {
    "period": "month",
    "totalTrips": 45,
    "totalDistance": "156 km",
    "totalSpent": 345000,
    "currency": "VND",
    "averageTripsPerDay": 1.5,
    "mostUsedRoute": {
      "origin": "Central Station",
      "destination": "Airport Terminal",
      "tripCount": 12
    },
    "preferredTransportType": "metro",
    "peakTravelTime": "08:00-09:00"
  }
}
```

---

## API Authentication & Security

### Authentication Header
All protected endpoints require:
```
Authorization: Bearer <accessToken>
```

### Refresh Token
**API Endpoint:** `POST /api/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 3600
}
```

---

## Error Handling

### Standard Error Response Format
```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Description of the error",
    "timestamp": "2026-06-07T11:16:16Z",
    "details": {}
  }
}
```

### Common HTTP Status Codes
- **200 OK** - Successful request
- **201 Created** - Resource successfully created
- **400 Bad Request** - Invalid parameters
- **401 Unauthorized** - Missing or invalid authentication
- **403 Forbidden** - User does not have permission
- **404 Not Found** - Resource not found
- **409 Conflict** - Resource conflict (e.g., duplicate card)
- **500 Internal Server Error** - Server error

---

## Summary of Use Cases & APIs

| # | Use Case | HTTP Method | Endpoint |
|---|----------|-------------|----------|
| 1.1 | User Registration (Submit Details) | POST | `/api/auth/register` |
| 1.1b | User Registration (Verify OTP) | POST | `/api/auth/verify-otp` |
| 1.1c | Resend OTP Code | POST | `/api/auth/resend-otp` |
| 1.2 | User Login | POST | `/api/auth/login` |
| 1.3 | User Logout | POST | `/api/auth/logout` |
| 1.4 | Update Account Info | PUT | `/api/users/{userId}` |
| 1.5 | Get User Profile | GET | `/api/users/{userId}` |
| 2.1 | View Card Packages | GET | `/api/cards/packages` |
| 2.2 | Purchase Physical Card | POST | `/api/cards/purchase` |
| 2.3 | Get Card Status | GET | `/api/cards/{cardId}/status` |
| 2.4 | Activate Physical Card | POST | `/api/cards/{cardId}/activate` |
| 3.1 | Get Available Routes | GET | `/api/routes` |
| 3.2 | Book Online Ticket | POST | `/api/tickets/purchase` |
| 3.3 | Get Ticket Types | GET | `/api/tickets/types` |
| 3.4 | View Online Tickets | GET | `/api/users/{userId}/tickets` |
| 3.5 | Cancel Online Ticket | DELETE | `/api/tickets/{ticketId}` |
| 4.1 | Get QR Code | GET | `/api/tickets/{ticketId}/qrcode` |
| 4.2 | Validate QR (Check-In) | POST | `/api/qrcode/validate-checkin` |
| 4.3 | Validate QR (Check-Out) | POST | `/api/qrcode/validate-checkout` |
| 5.1 | Get Payment Methods | GET | `/api/payments/methods` |
| 5.2 | Add Payment Method | POST | `/api/payments/methods` |
| 5.3 | Get Payment History | GET | `/api/users/{userId}/payments` |
| 6.1 | Get Ticket History | GET | `/api/users/{userId}/tickets/history` |
| 6.2 | Get Travel History | GET | `/api/users/{userId}/travels` |
| 6.3 | Get Travel Statistics | GET | `/api/users/{userId}/travels/statistics` |
