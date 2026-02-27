# SplitSync

**SplitSync** is a native Android expense-splitting application built entirely in Kotlin, designed for groups of friends who share costs across multiple currencies. The app supports real-time synchronization, offline-first operation, multi-currency conversion with live exchange rates, and peer-to-peer friend management — including NFC-based friend discovery.

> This project was developed as a portfolio piece to demonstrate proficiency in modern Android development practices, architecture patterns, and full-stack mobile engineering suitable for an internship-level position.

---

## Table of Contents

- [Key Features](#key-features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Technical Highlights](#technical-highlights)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [License](#license)

---

## Key Features

| Feature | Description |
|---|---|
| **Group Expense Splitting** | Create groups, add members, record expenses, and split costs equally among participants. |
| **Multi-Currency Support** | Expenses can be recorded in 170+ currencies with automatic conversion to each user's default currency using live exchange rates. |
| **Real-Time Sync** | All data (groups, expenses, payments, friends, profiles) synchronizes in real time across devices via Firestore listeners. |
| **Offline-First** | Full local persistence with Room. The app remains functional without network connectivity and reconciles changes when reconnected. |
| **Friend System** | Add friends by username, manage friend requests (pending, accepted), and view cross-context pairwise balances. |
| **NFC Friend Discovery** | Tap phones together to exchange friend invitations using Android Host Card Emulation (HCE) with short-lived Firestore tokens. QR code fallback included. |
| **Smart Settlement Plans** | Choose single-currency or multi-currency settle-up strategies. Multi-currency settlements lock FX rates at the time of execution and record per-currency breakdowns for auditability. |
| **Per-Currency & Converted Balances** | Balances are maintained in their native currencies. An approximate total in the user's default currency is shown alongside, with "last updated" indicators when cached rates are used. |
| **Profile & Account Management** | Edit display name, change username (with availability check), upload profile photo to Firebase Storage, manage notification preferences, and choose a default currency. |
| **In-App Notifications** | Receive notifications on invite acceptance and settlement completion, with unread badge counts. |

---

## Architecture

The project strictly follows **Clean Architecture** as recommended by Google's Guide to App Architecture, organized into three distinct layers:

```
┌──────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│   Jetpack Compose UI  ←→  ViewModels (StateFlow)         │
├──────────────────────────────────────────────────────────┤
│                     Domain Layer                         │
│   Use Cases  │  Repository Interfaces  │  Domain Models  │
├──────────────────────────────────────────────────────────┤
│                      Data Layer                          │
│  Room (local cache)  │  Firestore (remote)  │  REST API  │
│          Sync Managers  │  Repository Impls              │
└──────────────────────────────────────────────────────────┘
```

### Layer responsibilities

- **Domain** — pure Kotlin, no Android framework dependencies. Contains business models, repository interfaces, use case classes, and split-calculation rules. Every feature is expressed as an explicit use case (e.g., `BuildSettlementPlanUseCase`, `ConvertMoneyUseCase`, `ObservePairwiseDebtBucketsUseCase`).
- **Data** — implements repository contracts using Room for local persistence, Firestore for cloud persistence, and a REST-based exchange-rate client. A dedicated `SyncCoordinator` orchestrates multiple `SyncManager` instances that keep local and remote data consistent via Firestore snapshot listeners and optimistic writes.
- **Presentation** — 100% Jetpack Compose with unidirectional data flow. ViewModels expose `StateFlow` to Composables; navigation is handled by Compose Navigation with typed routes.

---

## Technology Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Dependency Injection | Koin |
| Local Database | Room (with KSP annotation processing) |
| Remote Backend | Firebase Authentication, Cloud Firestore, Firebase Storage |
| Exchange Rates | fawazahmed0/currency-api (jsDelivr CDN + Cloudflare Pages fallback) |
| Image Loading | Coil Compose |
| QR Code Generation | ZXing Core |
| NFC | Android Host Card Emulation (HCE), NDEF |
| Concurrency | Kotlin Coroutines + Flow |
| Build System | Gradle (Kotlin DSL) with Version Catalog |

---

## Technical Highlights

### Clean Architecture with Explicit Use Cases

Every user-facing action maps to a dedicated use case class. The project contains **34+ use cases** spanning authentication, group management, expense operations, balance computation, exchange-rate conversion, friend management, settlement planning, NFC tokenization, and notifications. This enforces single-responsibility and makes the codebase easy to test and extend.

### Offline-First Synchronization

A custom `SyncCoordinator` manages seven specialized sync managers:

- `GroupSyncManager` / `GroupMemberSyncManager` — group metadata and membership
- `ExpenseSyncManager` / `PaymentSyncManager` — financial transactions
- `FriendSyncManager` / `DirectThreadSyncManager` — social graph and direct expenses
- `UserProfileSyncManager` / `NotificationSyncManager` — profile data and in-app notifications

Each manager establishes Firestore snapshot listeners on sign-in and writes changes to Room. Dirty records are pushed back when connectivity resumes, achieving bidirectional sync with conflict reconciliation.

### Multi-Currency Conversion with Fallback

The exchange-rate subsystem demonstrates robust error handling:

1. **Cache-first** — rates are stored in a `FxRateEntity` Room table with TTL-based staleness checks.
2. **Dual-host remote** — primary fetch from jsDelivr CDN; automatic retry on the Cloudflare Pages fallback host if the primary fails.
3. **Graceful degradation** — when both remote sources are unreachable, cached rates are returned with a `source = CACHE` flag and `fetchedAt` timestamp, enabling the UI to display "last updated …" annotations.
4. **Currency metadata** — a `CurrencyMeta` registry maps 170+ currency codes to their minor-unit digit counts, ensuring correct rounding for currencies like JPY (0 decimals), KWD (3 decimals), and BTC (8 decimals).

### Settlement Plan Engine

The settle-up system supports two modes:

- **Single-currency** — settles one currency bucket exactly.
- **Multi-currency** — converts the user's aggregate cross-currency exposure into a single payment amount using locked FX rates. A greedy allocation algorithm distributes the payment across debt buckets in descending order of absolute value. The resulting `SettlementPlan` stores per-currency breakdowns and FX locks for full auditability.

Cross-context settlements from the Friend screen correctly write payment records back into the originating group or direct thread, ensuring balances update consistently everywhere.

### Pairwise Balance Extraction (Option B)

Friend-level balances are computed by extracting the pairwise financial effect of every shared expense — including multi-participant group expenses — between two users. The algorithm correctly handles three cases: payer is user A, payer is user B, payer is a third party (delta = 0). This allows the friend activity timeline to show a coherent "between us" financial history spanning both direct and group contexts.

### NFC Friend Discovery

Friend discovery via NFC is implemented using Android Host Card Emulation (HCE):

- The sharing device generates a short-lived Firestore token (`expiresAt = now + 2 min`) and advertises a URI payload via HCE APDU responses.
- The scanning device reads the APDU, parses the URI, validates the token against Firestore, and triggers the standard friend-request flow.
- A QR code fallback using ZXing ensures usability on devices without NFC support.

### Reactive UI with Unidirectional Data Flow

All ViewModels expose immutable `StateFlow<UiState>` objects. Composables collect state and dispatch events — never mutate state directly. Flows are combined, debounced, and mapped in the ViewModel layer, keeping Composables pure rendering functions.

---

## Project Structure

```
app/src/main/java/com/bangreedy/splitsync/
├── core/                        # Cross-cutting utilities
│   ├── currency/                # CurrencyMeta registry (170+ currencies)
│   ├── dispatchers/             # Coroutine dispatcher abstractions
│   ├── money/                   # Monetary formatting helpers
│   ├── result/                  # Sealed result wrappers
│   └── time/                    # Date/time utilities
├── data/                        # Data layer
│   ├── local/                   # Room entities, DAOs, database
│   ├── mapper/                  # Entity ↔ domain model mappers
│   ├── remote/                  # Firestore data sources, exchange-rate API client
│   ├── repository/              # Repository implementations
│   └── sync/                    # Sync managers and coordinator
├── di/                          # Koin dependency injection modules
├── domain/                      # Domain layer (pure Kotlin)
│   ├── model/                   # Domain models
│   ├── repository/              # Repository interfaces
│   ├── rules/                   # Business rules (equal-split calculator)
│   └── usecase/                 # Use case classes (34+)
├── presentation/                # Presentation layer
│   ├── account/                 # Profile & settings screens
│   ├── activity/                # Activity feed
│   ├── addexpense/              # Add expense flow
│   ├── auth/                    # Authentication screens
│   ├── common/                  # Shared components (NFC, currency picker, FX display)
│   ├── friends/                 # Friends list, friend details, NFC friend screen
│   ├── groupdetails/            # Group details, ledger, member management
│   ├── groups/                  # Groups list screen
│   ├── invites/                 # Invite management
│   ├── navigation/              # Compose Navigation graph
│   ├── notifications/           # Notifications screen
│   ├── profile/                 # Profile display components
│   ├── settleup/                # Settlement screens
│   └── ui/                      # Theme and design tokens
└── SplitSyncApp.kt              # Application class (Koin initialization)
```

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2) or newer
- JDK 11+
- A Firebase project with Authentication, Cloud Firestore, and Firebase Storage enabled

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/SplitSync.git
   ```
2. Place your `google-services.json` file in `app/src/debug/`.
3. Open the project in Android Studio and sync Gradle.
4. Run on an emulator or physical device (API 24+).

### Firebase Configuration

- Enable **Email/Password** authentication in the Firebase Console.
- Create the following Firestore collections (auto-created on first use): `users`, `groups`, `directThreads`.
- Enable **Firebase Storage** for profile photo uploads.
- Deploy Firestore security rules to restrict document access by authenticated UID.

---

## License

This project is provided for portfolio and educational purposes.

