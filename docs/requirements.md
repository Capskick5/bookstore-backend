# Requirements Document

## Introduction

The Bookstore RAG Platform is an online bookstore web application augmented with a Retrieval-Augmented Generation (RAG) AI chatbot. Customers can browse and search a book catalog, manage a shopping cart, place and track orders, and write reviews. An AI chatbot answers questions about books, the catalog, and store policies using retrieval over a curated knowledge base, and provides book recommendations. Administrators manage books, categories, orders, users, and view dashboard statistics. Access to features is governed by role-based authorization.

This document is organized so that the scope can be divided into six parallel workstreams for a six-person team. Each requirement is tagged with a `Workstream` label. The workstreams are:

- **WS1 — Auth & Accounts**: registration, login, JWT, profile, role-based access control.
- **WS2 — Catalog & Reviews**: book catalog, search, filtering, book detail, reviews and ratings.
- **WS3 — Cart & Orders**: shopping cart, checkout, mock payment, order placement and history.
- **WS4 — Admin Panel**: book/category/order/user management and statistics dashboard.
- **WS5 — AI / RAG**: chatbot, retrieval, ingestion/indexing pipeline, recommendations, conversation history.
- **WS6 — Frontend Shell & DevOps**: React application shell, routing, deployment, CI/CD, observability.

### Technology Stack and Constraints

- **Frontend**: React, deployed on Vercel.
- **Backend**: Spring Boot with JWT authentication, Swagger/OpenAPI documentation, and role-based authorization (ADMIN, CUSTOMER).
- **AI**: Provider-agnostic LLM integration defaulting to OpenAI (GPT-4o and GPT-4o-mini) for both development and demonstration. Retrieval uses Qdrant as a separate service/container accessed through the Qdrant client. Embeddings default to OpenAI `text-embedding-3-small` through a configurable provider.
- **Database**: PostgreSQL is the relational database for application data. PostgreSQL is no longer the vector store; Qdrant is the vector store, run as a separate service/container.
- **Deployment**: Frontend on Vercel; Backend and AI services on Railway or Render; Qdrant runs as a separate service/container; CI/CD via GitHub Actions.
- **Local Development**: A docker-compose configuration runs the Backend, PostgreSQL, and Qdrant locally with a single command, using the same container images that are used in deployment.
- **Repository**: Monorepo with `/fe` (React) and Backend (Spring Boot at root, Python AI integration in `/rag`).

## Glossary

- **System**: The complete Bookstore RAG Platform, including frontend, backend, and AI services.
- **Frontend**: The React single-page application deployed on Vercel.
- **Backend**: The Spring Boot application exposing REST APIs.
- **Auth_Service**: The backend component responsible for registration, login, token issuance, and token validation.
- **Catalog_Service**: The backend component responsible for book and category retrieval, search, and filtering.
- **Review_Service**: The backend component responsible for book reviews and ratings.
- **Cart_Service**: The backend component responsible for shopping cart state.
- **Order_Service**: The backend component responsible for order placement, payment processing, and order history.
- **Payment_Gateway**: The mock payment integration used to simulate payment authorization.
- **Admin_Service**: The backend component responsible for administrative management of books, categories, orders, and users.
- **AI_Service**: The backend component responsible for the RAG chatbot, retrieval, recommendations, and conversation history.
- **LLM_Provider**: The configurable large language model provider (OpenAI GPT-4o and GPT-4o-mini by default; additional providers supported through the provider interface).
- **Embedding_Provider**: The configurable component that converts text into vector embeddings (OpenAI `text-embedding-3-small` by default).
- **Vector_Store**: A Qdrant vector database collection storing embeddings and payload metadata used for retrieval.
- **Ingestion_Pipeline**: The component that reads knowledge sources, generates embeddings, and writes them to the Vector_Store.
- **Knowledge_Base**: The collection of source documents (book descriptions, FAQs, store policies) used for retrieval.
- **CI_Pipeline**: The GitHub Actions workflow that builds, tests, and deploys the System.
- **JWT**: JSON Web Token used as the bearer credential for authenticated requests.
- **Access_Token**: A short-lived JWT proving the identity and role of a User.
- **Refresh_Token**: A longer-lived token used to obtain a new Access_Token.
- **User**: Any registered account holder.
- **Customer**: A User with the CUSTOMER role.
- **Admin**: A User with the ADMIN role.
- **Role**: An authorization label assigned to a User; one of ADMIN or CUSTOMER.
- **Book**: A catalog item with attributes including title, author, category, price, description, and stock quantity.
- **Category**: A classification grouping for Books.
- **Cart**: The collection of Book line items selected by a Customer prior to checkout.
- **Order**: A confirmed purchase containing line items, totals, and a status.
- **Order_Status**: The lifecycle state of an Order; one of PENDING, PAID, SHIPPED, DELIVERED, or CANCELLED.
- **Review**: A Customer-authored rating and text comment for a Book.
- **Conversation**: An ordered sequence of chatbot messages associated with a User.
- **Migration_Tool**: The versioned database schema migration tool (Flyway or Liquibase) that applies ordered migration scripts to PostgreSQL.
- **Seed_Dataset**: A reproducible set of initial data, including sample Books, Categories, an ADMIN User, and Knowledge_Base documents, loaded to provide a known starting state.
- **Error_Response**: A standardized JSON error body returned by the Backend containing the fields timestamp, status, error code, message, and path.
- **Sample_Dataset_Size**: The defined number of records (1000 Books) used as the baseline for stating list-endpoint response-time bounds.

## Requirements

---

## Workstream 1 — Auth & Accounts

### Requirement 1: User Registration

**Workstream:** WS1
**User Story:** As a visitor, I want to register an account, so that I can place orders and use personalized features.

#### Acceptance Criteria

1. WHEN a visitor submits a registration request with a unique email and a password meeting the password policy, THE Auth_Service SHALL create a User with the CUSTOMER role and return a success response with HTTP status 201.
2. THE Auth_Service SHALL store each password as a salted hash using the BCrypt algorithm.
3. IF a registration request supplies an email that already belongs to an existing User, THEN THE Auth_Service SHALL reject the request with HTTP status 409 and a message stating the email is already registered.
4. IF a registration request supplies a password shorter than 8 characters or lacking at least one letter and one digit, THEN THE Auth_Service SHALL reject the request with HTTP status 400 and a message describing the password policy.
5. IF a registration request supplies an email that does not match the format `local@domain.tld`, THEN THE Auth_Service SHALL reject the request with HTTP status 400 and a validation message.

### Requirement 2: User Login and Token Issuance

**Workstream:** WS1
**User Story:** As a registered user, I want to log in, so that I can access protected features.

#### Acceptance Criteria

1. WHEN a User submits valid credentials, THE Auth_Service SHALL return an Access_Token and a Refresh_Token with HTTP status 200.
2. THE Auth_Service SHALL set the Access_Token expiry to 15 minutes from issuance.
3. THE Auth_Service SHALL set the Refresh_Token expiry to 7 days from issuance.
4. THE Auth_Service SHALL include the User identifier and Role as claims in the Access_Token.
5. IF a User submits credentials that do not match a stored account, THEN THE Auth_Service SHALL reject the request with HTTP status 401 and a message stating the credentials are invalid.
6. WHEN a User submits a valid Refresh_Token to the refresh endpoint, THE Auth_Service SHALL return a new Access_Token with HTTP status 200.
7. IF a request to the refresh endpoint supplies an expired or invalid Refresh_Token, THEN THE Auth_Service SHALL reject the request with HTTP status 401.
8. THE Auth_Service SHALL run a daily scheduled cleanup job that deletes all expired refresh tokens from the database (refresh_tokens table) to prevent infinite data growth.

### Requirement 3: Token Validation and Role-Based Access Control

**Workstream:** WS1
**User Story:** As a platform owner, I want every protected endpoint to enforce authentication and role checks, so that resources are accessed only by authorized users.

#### Acceptance Criteria

1. WHEN a request to a protected endpoint includes a valid, unexpired Access_Token, THE Backend SHALL process the request according to the requesting User's Role.
2. IF a request to a protected endpoint omits the Access_Token or includes an expired or malformed Access_Token, THEN THE Backend SHALL reject the request with HTTP status 401.
3. IF a Customer requests an endpoint restricted to the ADMIN role, THEN THE Backend SHALL reject the request with HTTP status 403.
4. THE Backend SHALL restrict all administrative endpoints to Users holding the ADMIN role.
5. THE Backend SHALL permit catalog browsing and search endpoints without an Access_Token.
6. THE Backend SHALL enforce the standard Error_Response JSON schema for authentication failures (HTTP 401) and authorization failures (HTTP 403) by registering a custom `AuthenticationEntryPoint` and a custom `AccessDeniedHandler` in Spring Security. Default empty or raw responses from Spring Security filters SHALL NOT be returned.

### Requirement 4: Profile Management

**Workstream:** WS1
**User Story:** As a customer, I want to view and update my profile, so that my account information stays current.

#### Acceptance Criteria

1. WHEN an authenticated User requests the profile endpoint, THE Auth_Service SHALL return the User's name, email, Role, points (current balance), and tier (SILVER, GOLD, or PLATINUM) with HTTP status 200.
2. WHEN an authenticated User submits a profile update with a valid name or shipping address, THE Auth_Service SHALL persist the change and return the updated profile with HTTP status 200.
3. WHEN an authenticated User submits a password change with a correct current password and a new password meeting the password policy, THE Auth_Service SHALL update the stored password hash, revoke all refresh tokens of the User to invalidate all other active sessions, and return HTTP status 200.
4. IF a password change request supplies an incorrect current password, THEN THE Auth_Service SHALL reject the request with HTTP status 400 and a message stating the current password is incorrect.
5. THE Auth_Service SHALL determine the User's tier dynamically based on a persistent `users.lifetime_points` column in the database: SILVER (< 1000 points), GOLD (1000 - 4999 points), or PLATINUM (>= 5000 points). In this phase, the tier is cosmetic-only (display-only) and does not affect order discounts. When points are credited to the user for a delivered order (Requirement 31.1), the same amount of points SHALL also be added to `lifetime_points` atomically. Points deducted due to cancellation or refund of a delivered order (Requirement 31.2) SHALL also be deducted from `lifetime_points` atomically (capped at 0). Points spent/redeemed for checkout discounts (Requirement 31.3) SHALL NOT decrease `lifetime_points`.

---

## Workstream 2 — Catalog & Reviews

### Requirement 5: Browse Book Catalog

**Workstream:** WS2
**User Story:** As a visitor, I want to browse the book catalog, so that I can discover books to purchase.

#### Acceptance Criteria

1. WHEN a visitor requests the catalog listing, THE Catalog_Service SHALL return a paginated list of Books with HTTP status 200.
2. THE Catalog_Service SHALL support a default page size of 10 Books and a maximum page size of 50 Books. Response SHALL include total count, current page number, and total page count in each catalog response.
3. WHEN a visitor requests a catalog page beyond the available range, THE Catalog_Service SHALL return an empty Book list with HTTP status 200 and the correct total count.
4. THE Catalog_Service SHALL include for each listed Book the identifier, title, author, category, price, cover image reference, and average rating.
5. THE Catalog_Service SHALL only return Books whose active status is true (active == true) in the public catalog listing, search results, and filters for visitors/customers. Books with active status false (inactive) SHALL be hidden from public browsing.

### Requirement 6: Search and Filter Books

**Workstream:** WS2
**User Story:** As a customer, I want to search and filter books, so that I can find books matching my interests.

#### Acceptance Criteria

1. WHEN a visitor submits a search query containing a keyword, THE Catalog_Service SHALL return Books whose title or author contains the keyword, matched case-insensitively, with HTTP status 200.
2. WHERE a category filter is supplied, THE Catalog_Service SHALL return only Books belonging to the specified Category.
3. WHERE an author filter is supplied, THE Catalog_Service SHALL return only Books whose author matches the specified author.
4. WHERE a minimum price and a maximum price are supplied, THE Catalog_Service SHALL return only Books whose price is greater than or equal to the minimum price and less than or equal to the maximum price.
5. IF a price filter supplies a minimum price greater than the maximum price, THEN THE Catalog_Service SHALL reject the request with HTTP status 400 and a validation message.
6. THE Catalog_Service SHALL support sorting by price ascending (`price_asc`), price descending (`price_desc`), average rating descending (`rating_desc`), and creation date descending (`created_at_desc`).
7. WHEN multiple filters are supplied in a single request, THE Catalog_Service SHALL return only Books satisfying all supplied filters.
8. WHEN a search or filter request matches no Books, THE Catalog_Service SHALL return an empty list with HTTP status 200.

### Requirement 7: Book Detail

**Workstream:** WS2
**User Story:** As a customer, I want to view a book's detail page, so that I can decide whether to purchase it.

#### Acceptance Criteria

1. WHEN a visitor requests a Book by its identifier, THE Catalog_Service SHALL return the Book's title, author, category, price, description, stock quantity, average rating, and review count with HTTP status 200.
2. IF a visitor requests a Book identifier that does not exist, THEN THE Catalog_Service SHALL respond with HTTP status 404 and a message stating the Book was not found, and SHALL exclude any Book attributes from the response body.
3. WHEN a Book with a stock quantity of zero is requested, THE Catalog_Service SHALL return the Book detail including a stock quantity of 0.

### Requirement 8: Book Reviews and Ratings

**Workstream:** WS2
**User Story:** As a customer, I want to rate and review books, so that I can share my opinion with other customers.

#### Acceptance Criteria

1. WHEN an authenticated Customer submits a Review with an integer rating from 1 to 5 and a text comment for a Book, THE Review_Service SHALL persist the Review and return HTTP status 201.
2. IF a Review submission supplies a rating outside the range 1 to 5, THEN THE Review_Service SHALL reject the request with HTTP status 400 and a validation message.
3. IF a Customer submits a second Review for a Book the Customer has already reviewed, THEN THE Review_Service SHALL reject the request with HTTP status 409 and a message stating a Review already exists.
4. WHEN a visitor requests the Reviews for a Book, THE Review_Service SHALL return a paginated list of Reviews including reviewer name, rating, comment, and creation date with HTTP status 200.
5. WHEN a Review is created, updated, or deleted for a Book, THE Review_Service SHALL atomically recompute and store the Book's average rating using a single SQL UPDATE operation: `UPDATE books SET rating_avg = (SELECT COALESCE(AVG(rating), 0) FROM reviews WHERE book_id = :bookId) WHERE id = :bookId`.
6. WHEN an authenticated Customer or an ADMIN deletes a Review, THE Review_Service SHALL remove the Review and return HTTP status 200.
7. IF a User other than the author of the Review or an ADMIN attempts to delete a Review, THEN THE Review_Service SHALL reject the request with HTTP status 403.

---

## Workstream 3 — Cart & Orders

### Requirement 9: Shopping Cart Management

**Workstream:** WS3
**User Story:** As a customer, I want to manage a shopping cart, so that I can collect books before checking out.

#### Acceptance Criteria

1. WHEN an authenticated Customer adds a Book with a positive integer quantity to the Cart, THE Cart_Service SHALL add or increase the corresponding line item and return the updated Cart with HTTP status 200.
2. IF a Customer adds a Book quantity that exceeds the Book's available stock quantity, THEN THE Cart_Service SHALL reject the request with HTTP status 400 and a message stating insufficient stock.
3. WHEN an authenticated Customer updates a Cart line item to a positive integer quantity, THE Cart_Service SHALL set the line item to that quantity and return the updated Cart with HTTP status 200.
4. WHEN an authenticated Customer removes a line item from the Cart, THE Cart_Service SHALL delete the line item and return the updated Cart with HTTP status 200.
5. THE Cart_Service SHALL compute the Cart subtotal dynamically using real-time book prices (by joining the `books` table) as the sum over all line items of the current unit price multiplied by quantity.
6. WHEN an authenticated Customer requests the Cart, THE Cart_Service SHALL return all current line items, current real-time prices, and the subtotal with HTTP status 200.
7. WHEN a User logs in, THE Cart_Service SHALL automatically merge items from their guest session cart into their database-backed user cart. If an item already exists in both, the quantities shall be summed up to the book's available stock limit. If any guest cart item has `active == false`, it SHALL be automatically removed and excluded from the merge. During the merge, the unit prices of the merged items MUST be updated to the current real-time catalog prices, and the user's cart subtotal recomputed immediately. Any stale guest cart prices SHALL be discarded.

### Requirement 10: Checkout and Order Placement

**Workstream:** WS3
**User Story:** As a customer, I want to check out my cart, so that I can place an order.

#### Acceptance Criteria

1. WHEN an authenticated Customer submits a checkout request with a non-empty Cart, a valid shipping address, and optionally a voucher code, THE Order_Service SHALL validate the checkout request. At checkout, the system SHALL capture the snapshot unit price of each book from the `books` table and write it to `order_items.unit_price`.
2. IF a Customer submits a checkout request with an empty Cart, THEN THE Order_Service SHALL reject the request with HTTP status 400 and a message stating the Cart is empty.
3. IF checkout is requested with any Book whose `active` status is false (inactive) OR whose quantity exceeds available stock, THE Order_Service SHALL reject checkout with HTTP status 400 or 409 and create no order.
4. WHEN checkout validation passes, THE Order_Service SHALL perform an atomic stock reservation using a single SQL UPDATE statement for each book: `UPDATE books SET stock = stock - :qty WHERE id = :id AND stock >= :qty`. If any update affects 0 rows, THE Order_Service SHALL rollback the transaction and return HTTP status 409 with an out-of-stock message.
5. THE Order_Service SHALL create the Order in `PENDING` status, record the snapshot values, create a `voucher_redemptions` record in a pending state, set the 15-minute stock hold expiration timestamp, generate a secure payment URL for the configured payment gateway (e.g. VNPAY or PayOS) passing an explicit transaction expiration parameter (e.g. `vnp_ExpireDate` for VNPAY) matching the 15-minute order stock hold duration, and return the URL with HTTP status 201.
6. THE Order_Service SHALL expose a generic webhook endpoint `/api/payment/webhook/{provider}` (e.g. `/vnpay`, `/payos`) to receive payment notifications. This endpoint must be whitelisted (permitAll) from authentication and CSRF protection in Spring Security, and its CORS configuration must allow external POST requests from the payment gateway providers.
7. WHEN a successful payment callback is received, THE Order_Service SHALL verify the webhook signature (checksum) using the provider-specific hash key from environment variables, verify the payment amount matches the order total, check that the order is in `PENDING` status (to prevent duplicate handling), update the Order status to `PAID`, increment the book's `sold_count` atomically (`UPDATE books SET sold_count = sold_count + :qty WHERE id = :id`), clear the Customer's Cart, and transition the `voucher_redemptions` record to active. To prevent race conditions between concurrent user cancellations, scheduler timeouts, and webhook executions, all status transition operations must acquire a pessimistic write lock (e.g., `SELECT FOR UPDATE` or `@Lock(LockModeType.PESSIMISTIC_WRITE)`) on the order record first.
8. THE Order_Service SHALL enforce voucher application rules at checkout: no stacking (maximum 1 voucher per order), the order subtotal must be >= `vouchers.min_order`, the voucher must be active and `starts_at` <= current time <= `ends_at`. The discount amount is calculated based on voucher type: FIXED (fixed amount), PERCENT (percentage discount capped by `max_discount`), or SHIP (free shipping up to 30000 VND). The voucher discount amount CANNOT exceed the order subtotal (minimum net order total is 0 VND before shipping fee). To prevent voucher usage limit races, the voucher's used count must be incremented atomically: `UPDATE vouchers SET used_count = used_count + 1 WHERE id = :id AND (usage_limit IS NULL OR used_count < usage_limit) AND active = true`. If 0 rows are affected, the checkout fails with HTTP 409.
9. THE Order_Service SHALL calculate the order total using the following formula:
   - `Discounted Subtotal = Max(0, Subtotal - Discount_Amount)` (where `Discount_Amount` is either the applied voucher discount OR the loyalty points discount, as combining them is prohibited).
   - `Shipping Fee` is 30,000 VND flat rate.
   - IF `Discounted Subtotal >= 300,000 VND` OR if a `SHIP` voucher is applied (reducing shipping fee by up to 30,000 VND), then `Shipping Fee = 0 VND` (or flat rate minus ship discount, capped at 0).
   - `Net Total = Discounted Subtotal + Shipping Fee`. All values must be non-negative integers.
10. IF a payment notification reports a failed or cancelled payment, OR if the 15-minute stock hold expires before a successful payment is confirmed, THE Order_Service SHALL transition the Order status to `CANCELLED`, restore the book stock atomically (`UPDATE books SET stock = stock + :qty WHERE id = :id`), release the voucher redemption by deleting the `voucher_redemptions` record and decrementing `vouchers.used_count` atomically (`UPDATE vouchers SET used_count = used_count - 1 WHERE id = :id`), and release any held loyalty points.
11. IF a successful payment webhook arrives *after* the order has been cancelled by the 15-minute timeout scheduler, THE Order_Service SHALL verify the provider's actual transaction authorization timestamp. If the payment was authorized *before* the cancellation occurred, the system SHALL restore the order to `PAID`, resolve inventory (or flag it for manual Admin intervention if stock was already sold out). If payment occurred *after* cancellation, the system SHALL flag the order for manual refund.
12. THE Order_Service SHALL record on each Order the line items, unit prices at time of purchase, total amount, shipping address, voucher code, points used, points earned, and creation timestamp.

### Requirement 11: Order History and Status

**Workstream:** WS3
**User Story:** As a customer, I want to view my order history and status, so that I can track my purchases.

#### Acceptance Criteria

1. WHEN an authenticated Customer requests the order history, THE Order_Service SHALL return a paginated list of the Customer's Orders ordered by creation timestamp descending, with HTTP status 200.
2. WHEN an authenticated Customer requests an Order by its identifier, THE Order_Service SHALL return the Order's line items, total amount, shipping address, Order_Status, and creation timestamp with HTTP status 200.
3. IF a Customer requests an Order that belongs to a different User, THEN THE Order_Service SHALL reject the request with HTTP status 403.
4. WHEN an authenticated Customer cancels an Order whose Order_Status is PENDING or PAID, THE Order_Service SHALL set the Order_Status to CANCELLED, restore each Book's stock quantity atomically (`UPDATE books SET stock = stock + :qty WHERE id = :id`), release the voucher redemption (decrement `vouchers.used_count` and delete `voucher_redemptions`), refund any points used for discount, and return HTTP status 200.
5. IF a Customer attempts to cancel an Order whose Order_Status is SHIPPED, DELIVERED, or CANCELLED, THEN THE Order_Service SHALL reject the request with HTTP status 409 and a message stating the Order can no longer be cancelled.
6. WHEN an Order is refunded or cancelled by an Admin after delivery (DELIVERED state), THE Order_Service SHALL deduct any earned points from the User's balance by writing a negative point transaction (allowing the balance to decrease, capped or negative as per transaction delta), delete the voucher redemption, and refund the points used for discount.
7. The endpoints for retrieving orders (history and details) SHALL support pagination with a default page size of 10 and a maximum of 50.

---

## Workstream 4 — Admin Panel

### Requirement 12: Manage Books

**Workstream:** WS4
**User Story:** As an admin, I want to create, update, and delete books, so that the catalog stays accurate.

#### Acceptance Criteria

1. WHEN an Admin submits a new Book with a title, author, Category, non-negative price, description, and non-negative stock quantity, THE Admin_Service SHALL create the Book and return it with HTTP status 201.
2. WHEN an Admin updates an existing Book with valid attributes, THE Admin_Service SHALL persist the changes and return the updated Book with HTTP status 200.
3. WHEN an Admin deletes a Book that is not referenced by any Order, THE Admin_Service SHALL remove the Book, delete its entries from all user `cart_items` to prevent FK violation, and return HTTP status 200.
4. IF an Admin attempts to delete a Book referenced by at least one Order, THE Admin_Service SHALL reject the request with HTTP status 409. Instead, to remove the book from the catalog, the Admin MUST set the Book's `active` status to false (soft-delete), which automatically hides the Book from public catalog listing, search, recommendations, and shopping carts.
5. IF an Admin submits a Book with a negative price or a negative stock quantity, THEN THE Admin_Service SHALL reject the request with HTTP status 400 and a validation message.
6. IF an Admin submits a Book with both a price of zero and a stock quantity of zero, THEN THE Admin_Service SHALL reject the request with HTTP status 400 and a message requiring either a positive price or a positive stock quantity.
7. WHEN an Admin creates, updates, or deletes a Book, or sets it to inactive, THE Admin_Service SHALL emit an asynchronous `BookChangedEvent` to trigger the RAG Ingestion_Pipeline reindexing.

### Requirement 13: Manage Categories

**Workstream:** WS4
**User Story:** As an admin, I want to manage categories, so that books can be organized.

#### Acceptance Criteria

1. WHEN an Admin submits a new Category with a name unique among Categories, THE Admin_Service SHALL create the Category and return it with HTTP status 201.
2. IF an Admin submits a Category name that duplicates an existing Category, THEN THE Admin_Service SHALL reject the request with HTTP status 409.
3. WHEN an Admin renames a Category, THE Admin_Service SHALL persist the change and return the updated Category with HTTP status 200.
4. IF an Admin attempts to delete a Category referenced by at least one Book, THEN THE Admin_Service SHALL reject the request with HTTP status 409 and a message stating the Category is in use.

### Requirement 14: Manage Orders

**Workstream:** WS4
**User Story:** As an admin, I want to view and update orders, so that I can fulfill customer purchases.

#### Acceptance Criteria

1. WHEN an Admin requests the orders listing, THE Admin_Service SHALL return a paginated list of all Orders with HTTP status 200.
2. WHERE an Order_Status filter is supplied, THE Admin_Service SHALL return only Orders matching the specified Order_Status.
3. WHEN an Admin updates an Order_Status following the allowed transitions PAID to SHIPPED and SHIPPED to DELIVERED, THE Admin_Service SHALL persist the new Order_Status and return the updated Order with HTTP status 200.
4. IF an Admin requests an Order_Status transition that is not among the allowed transitions, THEN THE Admin_Service SHALL reject the request with HTTP status 409 and a message describing the allowed transitions.

### Requirement 15: Manage Users

**Workstream:** WS4
**User Story:** As an admin, I want to manage user accounts, so that I can administer access.

#### Acceptance Criteria

1. WHEN an Admin requests the users listing, THE Admin_Service SHALL return a paginated list of Users including identifier, name, email, and Role with HTTP status 200.
2. WHEN an Admin changes a User's Role to ADMIN or CUSTOMER, THE Admin_Service SHALL persist the new Role and return the updated User with HTTP status 200.
3. WHEN an Admin disables a User account, THE Admin_Service SHALL mark the account disabled, immediately revoke all of the User's active refresh tokens, and THE Auth_Service SHALL thereafter reject login and token refresh requests for that account with HTTP status 403. In addition, the JWT token filter SHALL verify the User's enabled status from the database or cache on every incoming request to reject active access tokens for disabled users.
4. IF an Admin attempts to change the Role of or disable the Admin's own account, THEN THE Admin_Service SHALL reject the request with HTTP status 409 and a message stating self-modification of role or status is not permitted.

### Requirement 16: Statistics Dashboard

**Workstream:** WS4
**User Story:** As an admin, I want to view basic statistics, so that I can monitor store performance.

#### Acceptance Criteria

1. WHEN an Admin requests the dashboard statistics, THE Admin_Service SHALL return the total number of Orders, total revenue from Orders whose Order_Status is not CANCELLED, total number of registered Users, and total number of Books with HTTP status 200.
2. THE Admin_Service SHALL support optional `startDate` and `endDate` query parameters (in `YYYY-MM-DD` format). If provided, statistics and revenue SHALL be filtered by this range. If not provided, the default filter range SHALL be the last 30 days.
3. THE Admin_Service SHALL compute total revenue as the sum of total amounts over all Orders whose Order_Status is not CANCELLED and fall within the filter range.
4. WHEN an Admin requests top-selling Books, THE Admin_Service SHALL return the five Books with the highest total ordered quantity in descending order with HTTP status 200.

---

## Workstream 5 — AI / RAG

### Requirement 17: Knowledge Base Ingestion and Indexing

**Workstream:** WS5
**User Story:** As a platform owner, I want a pipeline that indexes store knowledge into a vector store, so that the chatbot can retrieve relevant context.

#### Acceptance Criteria

1. WHEN the Ingestion_Pipeline runs against a knowledge source containing book descriptions, FAQs, and store policies, THE Ingestion_Pipeline SHALL split each source document into text chunks, request an embedding for each chunk from the Embedding_Provider, and write each chunk together with its embedding and source metadata to the Vector_Store. The Ingestion_Pipeline SHALL use a target chunk size of 300 tokens and a chunk overlap of 100 tokens. The Ingestion_Pipeline SHALL parse book documents (PDF/EPUB) using standard pre-built libraries (such as `pypdf`/`pymupdf` for PDF, and `ebooklib` for EPUB) with a text-only fallback. Any complex embedded elements (like mathematical equations, complex layout formatting, vector drawings, or images) SHALL be bypassed, extracting only plain text to avoid ingestion time traps.
2. THE Ingestion_Pipeline SHALL record on each Vector_Store entry the source type, source identifier, and chunk index.
3. WHEN the Ingestion_Pipeline reprocesses a source document that was previously indexed, THE Ingestion_Pipeline SHALL replace the prior Vector_Store entries for that source identifier so that no duplicate entries for the same source remain.
4. IF the Embedding_Provider returns an error for a chunk, THEN THE Ingestion_Pipeline SHALL record the failure for that chunk, continue processing the remaining chunks, and report the count of failed chunks on completion.
5. WHEN an Admin triggers a reindex request, THE Ingestion_Pipeline SHALL run against all current knowledge sources asynchronously, immediately returning HTTP status 202 (Accepted) with a task identifier. The Admin dashboard SHALL expose a field showing the "Last Indexed" timestamp and task status.
6. WHEN a Book is deleted or set to inactive (active == false), THE Ingestion_Pipeline SHALL receive an asynchronous request to delete all Vector_Store entries associated with that Book's identifier, and SHALL remove those entries completely within 5 seconds to prevent the chatbot from retrieving or recommending the book. To avoid sequential table scans during deletions, a Payload Index must be created on the book's identifier (or source metadata field) in the vector database (Qdrant).

### Requirement 18: Retrieval Over the Vector Store

**Workstream:** WS5
**User Story:** As a customer, I want the chatbot to base answers on store knowledge, so that the answers are accurate and grounded.

#### Acceptance Criteria

1. WHEN the AI_Service receives a user question, THE AI_Service SHALL request an embedding for the question from the Embedding_Provider and retrieve from the Vector_Store the chunks whose embeddings are most similar to the question embedding.
2. THE AI_Service SHALL retrieve at most the top 5 most similar chunks for a single question.
3. WHEN retrieval returns at least one chunk meeting the minimum similarity threshold, THE AI_Service SHALL include the retrieved chunks and their source metadata in the prompt sent to the LLM_Provider.
4. IF retrieval returns no chunks meeting the minimum similarity threshold, THEN THE AI_Service SHALL omit all chunks and chunk metadata from the prompt and SHALL instruct the LLM_Provider to answer that the store knowledge base does not contain the requested information.

### Requirement 19: RAG Chatbot Conversation

**Workstream:** WS5
**User Story:** As a customer, I want to ask the chatbot questions about books and store policies, so that I can get help without browsing manually.

#### Acceptance Criteria

1. WHEN an authenticated User sends a chatbot message within a Conversation, THE AI_Service SHALL generate a response grounded in the retrieved chunks and return the response with HTTP status 200.
2. THE AI_Service SHALL include only the last 10 messages from the active conversation, ordered chronologically (ascending by `created_at`). The context window MUST exclude the system instructions prompt itself (which is always prepended as a separate message system role block). Only messages with role `user` and `assistant` are counted towards the 10-message limit.
3. THE AI_Service SHALL support multiple Conversations per User. The backend SHALL expose `GET /ai/conversations` (returns recent conversations paginated, default page size 10) and `DELETE /ai/conversations/{id}` (permits deleting a conversation by the owner or an ADMIN).
4. THE AI_Service SHALL persist each user message and each chatbot response as part of the Conversation associated with the User.
5. WHEN an authenticated User requests the Conversation history, THE AI_Service SHALL return the ordered messages of the User's Conversations with HTTP status 200.
6. IF a chatbot request is received without a valid Access_Token, THEN THE AI_Service SHALL reject the request with HTTP status 401.
7. IF a chatbot request is received with a valid Access_Token from a User whose Role is not permitted to use the chatbot, THEN THE AI_Service SHALL reject the request with HTTP status 403.
8. IF the LLM_Provider does not return a response within the configured timeout, THEN THE AI_Service SHALL return HTTP status 504 with a message stating the assistant is temporarily unavailable.
9. THE AI_Service SHALL enforce a rate limit of 20 chat requests per minute per User.
10. IF a User exceeds the chatbot rate limit, THE AI_Service SHALL reject the additional request with HTTP status 429 and a message stating the rate limit has been exceeded.

### Requirement 20: Book Recommendations

**Workstream:** WS5
**User Story:** As a customer, I want the chatbot to recommend books, so that I can discover relevant titles.

#### Acceptance Criteria

1. WHEN a User asks the chatbot for book recommendations described by a topic, genre, or interest, THE AI_Service SHALL retrieve Books from the Vector_Store whose indexed descriptions are most similar to the request, filter out books where `active == false` or `stock == 0` from the recommendations list, and return the remaining books.
2. THE AI_Service SHALL include for each recommended Book the title, author, and Book identifier so the Frontend can link to the Book detail.
3. THE AI_Service SHALL recommend only Books that exist in the current catalog.

### Requirement 21: Provider-Agnostic AI Configuration

**Workstream:** WS5
**User Story:** As a platform owner, I want the LLM and embedding providers to be configurable with OpenAI as the default, so that the demo runs on OpenAI while I retain the option to add another provider without code changes.

#### Acceptance Criteria

1. WHERE no LLM_Provider is explicitly configured, THE AI_Service SHALL default to OpenAI GPT-4o for chatbot generation and OpenAI GPT-4o-mini for lower-cost generation.
2. WHERE no Embedding_Provider is explicitly configured, THE AI_Service SHALL default to the OpenAI `text-embedding-3-small` embedding model.
3. WHERE the configured LLM_Provider is OpenAI, THE AI_Service SHALL route chatbot generation requests to OpenAI.
4. THE AI_Service SHALL select the LLM_Provider and the Embedding_Provider from runtime configuration without requiring a code change, and SHALL route each chatbot generation request to exactly one LLM_Provider.
5. IF the configured LLM_Provider or Embedding_Provider credential is missing at startup, THEN THE AI_Service SHALL fail startup with a log message identifying the missing credential.
6. THE AI_Service SHALL expose a single internal provider interface so that adding an alternate LLM_Provider requires only a new implementation of that interface.

---

## Workstream 6 — Frontend Shell & DevOps

### Requirement 22: React Application Shell and Routing

**Workstream:** WS6
**User Story:** As a customer, I want a responsive web interface, so that I can use the store from a browser.

#### Acceptance Criteria

1. THE Frontend SHALL provide routes for the catalog, Book detail, Cart, checkout, order history, profile, chatbot, and an admin section.
2. IF Frontend route initialization fails, THEN THE Frontend SHALL display an error page rather than load partial functionality.
3. WHILE no valid Access_Token is stored, THE Frontend SHALL redirect navigation to authenticated-only routes to the login route.
4. WHILE no valid Access_Token is stored, THE Frontend SHALL hide navigation links to the admin section regardless of any Role claimed by a stored token.
5. WHILE the stored Access_Token carries the CUSTOMER role, THE Frontend SHALL hide navigation links to the admin section.
6. WHEN the Backend returns HTTP status 401 to a Frontend request, THE Frontend SHALL clear the stored tokens and redirect to the login route.
7. WHEN a Frontend request is pending, THE Frontend SHALL display a loading indicator for the affected view.

### Requirement 23: Frontend Authentication and Token Handling

**Workstream:** WS6
**User Story:** As a customer, I want the app to manage my session, so that I stay logged in across requests.

#### Acceptance Criteria

1. WHEN a User logs in successfully through the Frontend, THE Frontend SHALL store the Access_Token and Refresh_Token and attach the Access_Token as a bearer credential on subsequent Backend requests.
2. WHEN a Backend request fails with HTTP status 401 because of an expired Access_Token and a valid Refresh_Token is stored, THE Frontend SHALL request a new Access_Token using the Refresh_Token and retry the original request once.
3. WHEN a User logs out, THE Frontend SHALL remove the stored Access_Token and Refresh_Token, and SHALL then redirect to the catalog route; IF the redirect fails, THEN THE Frontend SHALL still complete token removal.

### Requirement 24: API Documentation

**Workstream:** WS6
**User Story:** As a developer, I want interactive API documentation, so that I can understand and test the backend endpoints.

#### Acceptance Criteria

1. THE Backend SHALL expose a Swagger/OpenAPI documentation endpoint describing all REST endpoints, their request schemas, and their response schemas.
2. THE Backend SHALL annotate each protected endpoint in the OpenAPI documentation with its required Role.
3. THE Backend SHALL document the bearer-token security scheme so that the documentation interface can issue authenticated requests.

### Requirement 25: CI/CD Pipeline

**Workstream:** WS6
**User Story:** As a developer, I want automated build, test, and deployment, so that changes ship reliably and contributions are reviewable.

#### Acceptance Criteria

1. WHEN a commit is pushed to a branch, THE CI_Pipeline SHALL build the backend root Spring Boot project and the `/rag` FastAPI Python project, and run their automated unit and integration test suites. (Note: The React frontend lives in a separate repository and is excluded from this CI/CD pipeline).
2. IF any build step or test in the CI_Pipeline fails, THEN THE CI_Pipeline SHALL mark the workflow run as failed and block merge to the main branch.
3. THE CI_Pipeline SHALL require the unit and integration test suites to pass as a precondition for merging a pull request to the main branch.
4. WHEN a commit is merged to the main branch and all checks pass, THE CI_Pipeline SHALL deploy the Backend to Railway or Render.
5. THE CI_Pipeline SHALL read deployment credentials and provider API keys from repository or environment secrets rather than from committed files.

### Requirement 26: Configuration, Secrets, and Input Validation

**Workstream:** WS6
**User Story:** As a platform owner, I want secure configuration and validated input, so that the deployed system protects data and resists malformed requests.

#### Acceptance Criteria

1. THE Backend SHALL read database credentials, JWT signing secrets, and provider API keys from environment variables.
2. IF a required environment variable is absent at startup, THEN THE Backend SHALL fail startup with a log message identifying the missing variable.
3. WHEN the Backend receives a request body that violates the documented field constraints, THE Backend SHALL reject the request with HTTP status 400 and a message identifying the invalid fields.
4. THE Backend SHALL restrict cross-origin requests to the origins configured in the environment variable `APP_CORS_ALLOWED_ORIGINS` (comma-separated list, defaulting to `http://localhost:3000` for development).
5. WHEN the Backend returns an error response from any endpoint, THE Backend SHALL return an Error_Response JSON body containing the fields timestamp, status, error code, message, and path.
6. THE Backend SHALL use the same Error_Response schema for client errors and server errors across all endpoints.

### Requirement 27: Observability and Logging

**Workstream:** WS6
**User Story:** As an operator, I want basic logs and health checks, so that I can monitor and troubleshoot the deployed system.

#### Acceptance Criteria

1. WHEN the Backend handles a request, THE Backend SHALL log the request method, path, response status, and duration.
2. THE Backend SHALL expose a health endpoint that returns HTTP status 200 while both the application is fully operational and its database connection is available.
3. IF the application has failed to start or the database connection is unavailable, THEN THE Backend health endpoint SHALL return HTTP status 503.
4. WHEN the Backend logs a request or error, THE Backend SHALL exclude passwords, tokens, and provider API keys from the log output.

### Requirement 28: Containerized Local and Deployment Parity

**Workstream:** WS6
**User Story:** As a developer, I want to run the whole stack locally with one command, so that my local environment matches deployment.

#### Acceptance Criteria

1. WHEN a developer runs the single documented docker-compose command, THE System SHALL start the Backend, PostgreSQL, and Qdrant as containers and expose the Backend on the documented local port.
2. THE System SHALL define the Backend, PostgreSQL, and Qdrant services so that the same container images used locally are used in deployment.
3. IF a required service container fails to become healthy within its configured startup window, THEN the docker-compose startup SHALL report the unhealthy service and the Backend SHALL not report itself healthy.
4. THE Backend SHALL connect to the Qdrant service through the Qdrant client using connection settings read from environment variables.

### Requirement 29: Database Migrations and Seed Data

**Workstream:** WS6
**User Story:** As a developer, I want versioned schema migrations and reproducible seed data, so that the database state is predictable across environments.

#### Acceptance Criteria

1. WHEN the Backend starts, THE Migration_Tool SHALL apply any pending ordered migration scripts to PostgreSQL before the Backend begins serving requests.
2. THE Backend SHALL manage all schema changes through the Migration_Tool rather than through automatic schema generation.
3. IF a migration script fails to apply, THEN THE Migration_Tool SHALL halt startup and THE Backend SHALL fail startup with a log message identifying the failed migration.
4. WHEN the Seed_Dataset is loaded against an empty database, THE System SHALL populate sample Books, Categories, an ADMIN User, and Knowledge_Base documents.
5. WHEN the Seed_Dataset is loaded more than once against the same database, THE System SHALL leave the seeded records in the same state as a single load without creating duplicate records.

### Requirement 30: List Pagination and Response-Time Bounds

**Workstream:** WS6
**User Story:** As a customer, I want list views to load quickly and page through results, so that browsing remains responsive as the catalog grows.

#### Acceptance Criteria

1. THE Backend SHALL provide server-side pagination on all list endpoints, accepting a page number and page size and returning the total count, current page number, and total page count.
2. WHERE a page size is not supplied to a list endpoint, THE Backend SHALL apply a documented default page size.
3. IF a list endpoint receives a page size exceeding the documented maximum page size, THEN THE Backend SHALL reject the request with HTTP status 400 and a validation message.
4. WHEN a list endpoint is requested against a database holding the Sample_Dataset_Size, THE Backend SHALL return the requested page within 500 milliseconds measured at the Backend, excluding network transfer time.

## Workstream 5 (Additional) & Cross-Cutting Requirements

### Requirement 31: Loyalty Points Management

**Workstream:** WS1, WS3
**User Story:** As a customer, I want to earn and redeem loyalty points, so that I get rewarded for purchases.

#### Acceptance Criteria

1. WHEN an Order status changes to `DELIVERED`, THE Order_Service SHALL credit the customer with loyalty points equal to `order.total / 10000` spent (integer division, rounded down; e.g. 105,000 VND total paid spent = 10 points). Points earned are recorded in `point_transactions` with `reason='order_delivered'`.
2. WHEN a customer cancels an Order whose status was `PAID` or `DELIVERED`, THE Order_Service SHALL debit the customer's points by the amount previously credited, recording a negative `delta` in `point_transactions` with `reason='order_cancelled'`. A customer's `users.points` balance can decrease, capped at a minimum of 0 (no negative total balance allowed).
3. WHEN a customer applies loyalty points at checkout, THE Order_Service SHALL deduct `points_to_redeem` from the customer's `users.points` balance, apply a discount equal to `points_to_redeem * 100` VND (i.e. 1 point = 100 VND discount), up to a maximum of 20% of the order value, and record the transaction in `point_transactions` with `reason='points_redeemed'`.
4. A customer's `users.points` balance SHALL always equal the sum of all `delta` values in their `point_transactions` records. To prevent concurrent update race conditions, the points update operation in database MUST be executed using delta SQL updates or Pessimistic Locking (`SELECT FOR UPDATE` on the user row) inside the transaction.
5. A customer CANNOT combine loyalty points and a voucher in the same order. IF both are supplied at checkout, THE Order_Service SHALL reject the request with HTTP status 400 stating "Cannot combine points and voucher."
6. WHEN an authenticated customer requests `GET /me/points`, THE Auth_Service SHALL return the customer's current `points` balance and a paginated list of recent `point_transactions` (default page size 10, maximum 50).

### Requirement 32: Prompt Injection Defense

**Workstream:** WS5
**User Story:** As a platform owner, I want the chatbot to resist adversarial user prompts, so that it behaves reliably and safely.

#### Acceptance Criteria

1. THE AI_Service SHALL strictly segregate the System Prompt (defining bot behavior and boundaries) from User Messages in the payload sent to the LLM_Provider.
2. THE AI_Service SHALL sanitize all User Messages by matching and rejecting common injection keywords (e.g. "ignore prior instructions", "system prompt", "developer mode") with HTTP status 400.
3. THE AI_Service SHALL apply content filtering on chatbot outputs to intercept and suppress messages containing unauthorized commands, developer configuration details, or sensitive metadata.

### Requirement 33: Asynchronous RAG Ingestion and Task Status

**Workstream:** WS4, WS5
**User Story:** As an admin, I want book ingestion and reindexing to run asynchronously, so that my admin UI does not freeze during chunking and embedding.

#### Acceptance Criteria

1. WHEN an Admin creates, updates, deletes, or deactivates a book, THE Admin_Service SHALL emit a `BookChangedEvent` which is processed asynchronously. The backend API SHALL immediately return HTTP status 202 (Accepted).
2. THE Ingestion_Pipeline SHALL process the reindexing task in the background. If a failure occurs (e.g. OpenAI rate limits or Qdrant connection timeouts), the pipeline SHALL retry with exponential backoff up to 3 times before saving a failed task status. To prevent permanent desynchronization between PostgreSQL and Qdrant when background task retries fail, a nightly scheduled reconciliation cron job (running in Spring Boot) SHALL scan PostgreSQL catalog books against Qdrant vector metadata, deleting any orphan vectors from deleted/inactive books and queueing ingestion for missing active books.
3. THE Admin dashboard SHALL expose an endpoint `GET /admin/ai/reindex/status` returning a list of background reindexing tasks, their status (PENDING, RUNNING, COMPLETED, FAILED), and the "Last Indexed" timestamp.

---

## Workstream Allocation Summary

| Workstream | Owner Focus | Requirements |
|------------|-------------|--------------|
| WS1 — Auth & Accounts | Registration, login, JWT, RBAC, profile, addresses, tiers | 1, 2, 3, 4, 31 |
| WS2 — Catalog & Reviews | Catalog, search/filter, book detail, reviews | 5, 6, 7, 8 |
| WS3 — Cart & Orders | Cart, checkout, payment webhook, order history | 9, 10, 11 |
| WS4 — Admin Panel | Book/category/order/user management, dashboard | 12, 13, 14, 15, 16, 33 |
| WS5 — AI / RAG | Ingestion, retrieval, chatbot, recommendations, providers, security | 17, 18, 19, 20, 21, 32 |
| WS6 — Platform & DevOps | React shell, auth handling, Swagger, CI/CD, config, observability | 22, 23, 24, 25, 26, 27 |

### Cross-Workstream Integration Contracts

- **WS1 ↔ all**: The Access_Token claims (User identifier, Role) and the bearer security scheme are the shared contract for authentication and authorization across every protected endpoint.
- **WS2 ↔ WS3**: Book stock quantity and unit price are read at add-to-cart time and re-validated at checkout (Requirements 9, 10).
- **WS2/WS4 ↔ WS5**: Book description changes (Requirement 12.7) trigger Knowledge_Base reindexing (Requirement 17.3), keeping retrieval current.
- **WS3 ↔ WS4**: Order_Status transitions are owned by WS4 admin actions (Requirement 14) and read by WS3 customer views (Requirement 11).
- **WS5 ↔ WS6**: Recommended Books carry identifiers (Requirement 20.2) so the Frontend can deep-link to Book detail routes (Requirement 22).