# TariffNom System - Technical Architecture Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Overview](#architecture-overview)
3. [Component Diagram](#component-diagram)
4. [Layer Responsibilities](#layer-responsibilities)
5. [Service Catalog](#service-catalog)
6. [Design Principles](#design-principles)
7. [Technology Stack](#technology-stack)
8. [Data Flow](#data-flow)
9. [Security Architecture](#security-architecture)
10. [External Integrations](#external-integrations)

---

## System Overview

**TariffNom** is a comprehensive tariff calculation and analysis platform that simplifies the process of determining import tariffs and foreign fees. The system provides real-time tariff calculations, exchange rate analysis, and AI-driven purchase recommendations.

### Core Features
- **Tariff Calculation**: Calculate accurate import tariffs between any country pair
- **Exchange Rate Analysis**: Real-time currency trends and optimal purchase timing
- **Predictive Analysis**: AI-driven BUY/HOLD/WAIT recommendations based on news sentiment
- **User Management**: Secure authentication and user profiles
- **Search History**: Track and retrieve past calculations
- **Web Scraping**: Automated tariff data collection and updates

---

## Architecture Overview

TariffNom follows a **3-Tier Layered Architecture** pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│                    (React Frontend)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ HomePage │  │ Results  │  │ Exchange │  │Predictive│   │
│  │          │  │   Page   │  │  Rate    │  │ Analysis │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                    REST API (JSON)
                            │
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│                  (Spring Boot Backend)                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              CONTROLLER LAYER                         │  │
│  │  AuthController │ TariffController │ ExchangeRate... │  │
│  └──────────────────────────────────────────────────────┘  │
│                            │                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              SERVICE LAYER                            │  │
│  │  TariffCalculator │ ExchangeRate │ Predictive...    │  │
│  │  UserService │ ShippingService │ NewsAPI...         │  │
│  └──────────────────────────────────────────────────────┘  │
│                            │                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            REPOSITORY LAYER (JPA)                     │  │
│  │  TariffRateRepo │ ExchangeRateRepo │ NewsArticle... │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                      JDBC / JPA
                            │
┌─────────────────────────────────────────────────────────────┐
│                    DATA LAYER                                │
│                  (PostgreSQL Database)                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ TariffRate│  │ Exchange │  │   News   │  │   User   │   │
│  │          │  │   Rate   │  │ Articles │  │          │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Architecture Pattern: **Layered Architecture**

The system strictly follows layered architecture with clear separation:

1. **Presentation Layer** (Frontend) - React-based UI
2. **API Layer** (Controllers) - REST endpoints
3. **Business Logic Layer** (Services) - Core business rules
4. **Data Access Layer** (Repositories) - Database operations
5. **Data Layer** (PostgreSQL) - Persistent storage

---

## Component Diagram

```
                    ┌─────────────────────────────────┐
                    │   External Services/APIs        │
                    ├─────────────────────────────────┤
                    │ • OpenExchangeRates API         │
                    │ • News API                      │
                    │ • OpenAI API                    │
                    │ • REST Countries API            │
                    │ • Python Scraper Service        │
                    └───────────┬─────────────────────┘
                                │
                    ┌───────────▼─────────────────────┐
                    │     Spring Boot Backend          │
                    │  (Port 8080)                    │
                    ├──────────────────────────────────┤
                    │  ┌────────────────────────────┐ │
                    │  │   Controller Layer         │ │
                    │  │ • AuthController           │ │
                    │  │ • TariffController         │ │
                    │  │ • ExchangeRateController   │ │
                    │  │ • PredictiveAnalysisCtrl   │ │
                    │  │ • ScrapingController       │ │
                    │  └────────────────────────────┘ │
                    │             │                    │
                    │  ┌────────────────────────────┐ │
                    │  │   Service Layer            │ │
                    │  │ ┌─────────────────────┐   │ │
                    │  │ │ Basic Services      │   │ │
                    │  │ │ • UserService       │   │ │
                    │  │ │ • CountryService    │   │ │
                    │  │ │ • ProductService    │   │ │
                    │  │ │ • ShippingService   │   │ │
                    │  │ │ • ExchangeRateServ. │   │ │
                    │  │ │ • PredictiveAnalysis│   │ │
                    │  │ └─────────────────────┘   │ │
                    │  │ ┌─────────────────────┐   │ │
                    │  │ │ Tariff Logic        │   │ │
                    │  │ │ • TariffCalculator  │   │ │
                    │  │ │ • TariffRateService │   │ │
                    │  │ │ • ShippingCostServ. │   │ │
                    │  │ │ • TariffValidation  │   │ │
                    │  │ └─────────────────────┘   │ │
                    │  │ ┌─────────────────────┐   │ │
                    │  │ │ Data Services       │   │ │
                    │  │ │ • NewsAPIService    │   │ │
                    │  │ │ • SentimentAnalysis │   │ │
                    │  │ │ • WebScrapingServ.  │   │ │
                    │  │ │ • ConvertCodeServ.  │   │ │
                    │  │ │ • CurrencyCodeServ. │   │ │
                    │  │ └─────────────────────┘   │ │
                    │  └────────────────────────────┘ │
                    │             │                    │
                    │  ┌────────────────────────────┐ │
                    │  │   Repository Layer (JPA)   │ │
                    │  │ • UserRepository           │ │
                    │  │ • TariffRateRepository     │ │
                    │  │ • ExchangeRateRepository   │ │
                    │  │ • NewsArticleRepository    │ │
                    │  │ • SentimentAnalysisRepo    │ │
                    │  │ • CountryRepository        │ │
                    │  │ • ProductRepository        │ │
                    │  │ • ShippingRateRepository   │ │
                    │  └────────────────────────────┘ │
                    │             │                    │
                    │  ┌────────────────────────────┐ │
                    │  │   Security Layer           │ │
                    │  │ • JwtAuthFilter            │ │
                    │  │ • JwtService               │ │
                    │  │ • SecurityConfig           │ │
                    │  │ • UserDetailsService       │ │
                    │  └────────────────────────────┘ │
                    └───────────┬──────────────────────┘
                                │
                    ┌───────────▼──────────────────────┐
                    │   PostgreSQL Database            │
                    │   (Supabase)                     │
                    ├──────────────────────────────────┤
                    │ Tables:                          │
                    │ • user                           │
                    │ • tariff_rate                    │
                    │ • exchange_rate                  │
                    │ • news_article                   │
                    │ • sentiment_analysis             │
                    │ • country, product               │
                    │ • shipping_rate                  │
                    │ • search_history                 │
                    └──────────────────────────────────┘

            ┌───────────────────────────────────────────┐
            │        React Frontend                     │
            │        (Vite + Tailwind CSS)              │
            │        (Port 5173)                        │
            ├───────────────────────────────────────────┤
            │ Components:                               │
            │ • HomePage - Search/Input form            │
            │ • ResultsPage - Display calculations      │
            │ • ExchangeRateAnalysis - FX trends        │
            │ • PredictiveAnalysis - AI recommendations │
            │ • CountrySelect - Country picker          │
            │ • ThemeToggle - Light/Dark mode           │
            └───────────────────────────────────────────┘
```

---

## Layer Responsibilities

### 1. Presentation Layer (Frontend - React)

**Responsibility**: User interface and user experience

**Components:**
- **HomePage.jsx**: Main landing page with tariff calculation form
- **ResultsPage.jsx**: Display calculation results and analysis
- **ExchangeRateAnalysis.jsx**: Show exchange rate trends and recommendations
- **PredictiveAnalysis.jsx**: Display AI-driven purchase predictions
- **CountrySelect.jsx**: Reusable country selection component
- **ResultCard.jsx**: Display individual tariff results
- **ThemeToggle.jsx**: Light/dark mode switcher

**Technologies:**
- React 18.2.0
- Vite (build tool)
- Tailwind CSS (styling)
- Lucide React (icons)

---

### 2. API Layer (Controllers)

**Responsibility**: Handle HTTP requests, validate input, return responses

#### Controllers and Their Endpoints:

**AuthController**
```java
@RestController
@RequestMapping("/auth")
```
- `POST /auth/register` - User registration
- `POST /auth/login` - User authentication
- Returns JWT tokens for authenticated access

**TariffController**
```java
@RestController
@RequestMapping("/api/tariffs")
```
- `POST /api/tariffs/calculate` - Calculate tariff for product
- Orchestrates the main tariff calculation flow

**ExchangeRateController**
```java
@RestController
@RequestMapping("/api/exchange-rates")
```
- `POST /api/exchange-rates/analyze` - Analyze exchange rate trends
- `GET /api/exchange-rates/analyze` - Query parameter version
- `GET /api/exchange-rates/health` - Health check
- `GET /api/exchange-rates/api-reference` - API reference link

**PredictiveAnalysisController**
```java
@RestController
@RequestMapping("/api/predictive-analysis")
```
- `POST /api/predictive-analysis/predict` - Get BUY/HOLD/WAIT prediction
- `GET /api/predictive-analysis/predict` - Query parameter version
- `GET /api/predictive-analysis/debug/fetch-news` - Debug endpoint
- `GET /api/predictive-analysis/health` - Health check

**CountryController**
```java
@RestController
@RequestMapping("/api/countries")
```
- `GET /api/countries` - List all countries
- `GET /api/countries/{code}` - Get country details
- `POST /api/countries` - Add new country

**ProductController**
```java
@RestController
@RequestMapping("/api/products")
```
- `GET /api/products` - List all products
- `GET /api/products/{hsCode}` - Get product by HS code
- `POST /api/products` - Add new product

**ScrapingController**
```java
@RestController
@RequestMapping("/api/scraping")
```
- Triggers web scraping jobs for tariff data collection

**UserController**
```java
@RestController
@RequestMapping("/api/users")
```
- User profile management
- CRUD operations for users

**TariffRateController & ShippingRateController**
```java
@RestController
@RequestMapping("/api/rates/...")
```
- Manage tariff and shipping rate data

---

### 3. Service Layer (Business Logic)

**Responsibility**: Core business logic, algorithms, orchestration

#### Service Categories:

#### A. Basic Services (`service/basic/`)

**UserService**
- **Responsibility**: User account management
- **Methods**: 
  - `getAllUsers()` - Retrieve all users
  - `getUserById(Long id)` - Get user by ID
  - `createUser(UserRequestDTO)` - Create new user
  - `updateUser(Long id, UserRequestDTO)` - Update user
  - `deleteUser(Long id)` - Delete user
- **Dependencies**: UserRepository

**CountryService**
- **Responsibility**: Country data management and lookup
- **Methods**:
  - `getAllCountries()` - Get all countries
  - `getCountryByCode(String code)` - Lookup country
  - `createCountry(Country)` - Add new country
  - `validateCountryCode(String)` - Validate inputs
- **Dependencies**: CountryRepository

**ProductService**
- **Responsibility**: Product/HS code management
- **Methods**:
  - `createProduct(String hsCode, String desc, String category)`
  - `getProductByHsCode(String hsCode)`
  - `updateProduct(...)` - Update product details
  - `deleteProduct(String hsCode)`
- **Dependencies**: ProductRepository

**ShippingService**
- **Responsibility**: Shipping rate calculations
- **Methods**:
  - `getShippingRate(String mode, String importing, String exporting)`
  - Returns rates for AIR/SEA/LAND shipping modes
- **Dependencies**: ShippingRateRepository, CountryRepository

**TariffRateCRUDService**
- **Responsibility**: Basic CRUD for tariff rates
- **Methods**:
  - `createTariffRate(TariffRateDTO)`
  - `getTariffRateByDetails(hsCode, importing, exporting)`
  - `updateTariffRate(...)`, `deleteTariffRate(...)`
- **Dependencies**: TariffRateRepository, CountryRepository

**ExchangeRateService** ⭐ NEW
- **Responsibility**: Exchange rate analysis and trend prediction
- **Methods**:
  - `analyzeExchangeRates(request)` - Main analysis
  - `fetchAndStoreLiveRates(...)` - API integration
  - `performTrendAnalysis(...)` - Statistical analysis
  - `generateRecommendation(...)` - Purchase timing advice
- **Dependencies**: ExchangeRateRepository, CurrencyCodeService
- **External APIs**: OpenExchangeRates API

**PredictiveAnalysisService** ⭐ NEW
- **Responsibility**: AI-driven purchase predictions using news sentiment
- **Methods**:
  - `analyzePrediction(request)` - Main prediction logic
  - `generatePrediction(...)` - BUY/HOLD/WAIT algorithm
  - `calculateSentimentVolatility(...)` - Risk analysis
- **Dependencies**: NewsAPIService, SentimentAnalysisService, ExchangeRateService
- **Algorithm**: Multi-factor scoring (sentiment + exchange rate + volatility)

#### B. Tariff Logic Services (`service/tariffLogic/`)

**TariffCalculatorService**
- **Responsibility**: Orchestrates complete tariff calculation
- **Calculation Steps**:
  1. Build Customs Value (CIF or Transaction basis)
  2. Calculate Base Duty (MFN rate)
  3. Apply FTA preferential rates if eligible
  4. Add TRQ (Tariff Rate Quota) adjustments
  5. Apply additional duties (301/ADD/CVD/SG)
  6. Calculate VAT/GST on correct base
  7. Add shipping costs
  8. Sum total cost
- **Dependencies**: TariffRateService, ShippingCostService, CountryProfileRepository

**TariffRateService**
- **Responsibility**: Tariff rate retrieval and management
- **Methods**:
  - `calculateTariffAmount(request)` - Get tariff for HS code
  - Triggers web scraping if rate not in database
  - Caches results for performance
- **Dependencies**: TariffRateCRUDService, WebScrapingService, TariffCacheService

**ShippingCostService**
- **Responsibility**: Shipping cost calculations
- **Methods**:
  - `calculateShippingCost(mode, weight, importing, exporting)`
  - Supports AIR/SEA/LAND modes
  - Handles weight-based pricing
- **Dependencies**: ShippingService

**TariffValidationService**
- **Responsibility**: Input validation and default values
- **Methods**:
  - `validateTariffRequest(request)` - Validate inputs
  - Sets defaults for missing fields
  - Returns validation errors
- **Dependencies**: None

**TariffCacheService**
- **Responsibility**: In-memory caching for performance
- **Methods**:
  - `getCachedTariff(...)` - Retrieve from cache
  - `cacheTariff(...)` - Store in cache
  - Cache eviction policies
- **Dependencies**: None (in-memory)

#### C. Data Services (`service/data/`)

**NewsAPIService** ⭐ NEW
- **Responsibility**: Fetch news from News API
- **Methods**:
  - `fetchTradeNews(int daysBack)` - Get recent trade news
  - `parseNewsArticles(response)` - Parse JSON response
  - `testAPIConnection()` - Verify API connectivity
- **Keywords**: tariff, imports, exports, currency, trade policy
- **External API**: NewsAPI.org
- **Dependencies**: NewsArticleRepository

**SentimentAnalysisService** ⭐ NEW
- **Responsibility**: Sentiment scoring and aggregation
- **Methods**:
  - `analyzeSentiment(text)` - Keyword-based scoring
  - `processArticleSentiments(articles)` - Batch processing
  - `calculateWeeklySentiment(...)` - Weekly aggregates
  - `getSentimentHistory(weeks)` - Historical data
- **Algorithm**: Keyword matching with weighted scores (-1 to +1)
- **Dependencies**: NewsArticleRepository, SentimentAnalysisRepository

**WebScrapingService**
- **Responsibility**: Scrape tariff data from external sources
- **Methods**:
  - Calls Python microservice for scraping
  - Parses scraped data
  - Stores in database
- **External API**: Python Scraper Service (port 5001)
- **Dependencies**: ProductRepository, CountryRepository

**ConvertCodeService**
- **Responsibility**: Convert between country code formats
- **Methods**:
  - `convertToISO3(String twoDigitCode)` - Alpha-2 to Alpha-3
  - Caches conversions for performance
- **External API**: REST Countries API
- **Dependencies**: CountryService

**CurrencyCodeService** ⭐ NEW
- **Responsibility**: Map country codes to currency codes
- **Methods**:
  - `getCurrencyCode(String countryCode)` - Get ISO 4217 code
  - `hasCurrencyMapping(String)` - Check if mapping exists
- **Mapping**: 50+ countries to currencies (USD, EUR, GBP, CNY, etc.)
- **Dependencies**: None (static mapping)

---

### 4. Repository Layer (Data Access)

**Responsibility**: Database operations using Spring Data JPA

**All repositories extend** `JpaRepository<Entity, ID>` and provide:
- Standard CRUD operations
- Custom query methods
- Native queries for complex operations

#### Key Repositories:

**UserRepository**
```java
public interface UserRepository extends JpaRepository<User, Long>
```
- `findByUsername(String)`, `findByEmail(String)`
- User authentication queries

**TariffRateRepository**
```java
public interface TariffRateRepository extends JpaRepository<TariffRate, Long>
```
- `findByHsCodeAndImportingCountryCodeAndExportingCountryCode(...)`
- Complex tariff lookups

**ExchangeRateRepository** ⭐ NEW
```java
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long>
```
- `findLatestByFromCurrencyAndToCurrency(...)`
- `findByFromCurrencyAndToCurrencyAndRateDateBetween(...)`
- Historical rate queries

**NewsArticleRepository** ⭐ NEW
```java
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long>
```
- `findByPublishedAtBetween(...)`
- `getAverageSentiment(...)` - Aggregate queries
- Sentiment polarity counts

**SentimentAnalysisRepository** ⭐ NEW
```java
public interface SentimentAnalysisRepository extends JpaRepository<SentimentAnalysis, Long>
```
- `findLatest()` - Most recent sentiment
- `findByDateRange(...)` - Historical aggregates

---

### 5. Security Layer

**Responsibility**: Authentication and authorization

**Components:**

**JwtService**
- **Purpose**: JWT token generation and validation
- **Methods**:
  - `generateToken(userId, username, email, role)` - Create JWT
  - `parseToken(String token)` - Validate and parse
  - `isTokenValid(String token)` - Check validity
  - `getUsername(String token)` - Extract claims
- **Algorithm**: HMAC SHA-256
- **Expiration**: Configurable (default 15 minutes)

**JwtAuthenticationFilter**
- **Purpose**: Intercept requests and validate JWT tokens
- **Process**:
  1. Extract JWT from Authorization header
  2. Validate token signature
  3. Load user details
  4. Set authentication in SecurityContext
- **Extends**: OncePerRequestFilter

**SecurityConfig**
- **Purpose**: Configure Spring Security
- **Features**:
  - CORS configuration
  - CSRF disabled (stateless API)
  - JWT-based authentication
  - Public endpoints configuration
  - Password encoding (BCrypt)

**UserDetailsServiceImpl**
- **Purpose**: Load user details for authentication
- **Methods**:
  - `loadUserByUsername(String)` - Spring Security integration
  - Checks user active status
  - Builds granted authorities from roles

---

## Service Catalog

### Complete Service Responsibilities

| Service | Package | Responsibility | Key Methods |
|---------|---------|----------------|-------------|
| **TariffCalculatorService** | tariffLogic | Orchestrates full tariff calculation | `calculate(request)` |
| **TariffRateService** | tariffLogic | Manages tariff rates, triggers scraping | `calculateTariffAmount(request)` |
| **TariffValidationService** | tariffLogic | Validates inputs, sets defaults | `validateTariffRequest(request)` |
| **ShippingCostService** | tariffLogic | Calculates shipping costs | `calculateShippingCost(...)` |
| **TariffCacheService** | tariffLogic | In-memory caching for performance | `getCachedTariff(...)` |
| **ExchangeRateService** | basic | Exchange rate analysis & trends | `analyzeExchangeRates(request)` |
| **PredictiveAnalysisService** | basic | AI predictions (BUY/HOLD/WAIT) | `analyzePrediction(request)` |
| **UserService** | basic | User account management | `createUser()`, `updateUser()` |
| **CountryService** | basic | Country data management | `getAllCountries()`, `getCountryByCode()` |
| **ProductService** | basic | Product/HS code management | `createProduct()`, `getProductByHsCode()` |
| **ShippingService** | basic | Shipping rate retrieval | `getShippingRate(mode, ...)` |
| **TariffRateCRUDService** | basic | CRUD for tariff rates | `createTariffRate()`, `getTariffRateByDetails()` |
| **NewsAPIService** | data | Fetch news from News API | `fetchTradeNews(days)` |
| **SentimentAnalysisService** | data | Sentiment scoring & aggregation | `analyzeSentiment()`, `calculateWeeklySentiment()` |
| **WebScrapingService** | data | Scrape tariff data | Calls Python scraper service |
| **ConvertCodeService** | data | Country code conversion | `convertToISO3(code)` |
| **CurrencyCodeService** | data | Country to currency mapping | `getCurrencyCode(country)` |
| **JwtService** | security | JWT token management | `generateToken()`, `isTokenValid()` |

---

## Design Principles

### 1. **Separation of Concerns**
Each layer has a single, well-defined responsibility:
- Controllers: HTTP handling only
- Services: Business logic only
- Repositories: Data access only
- Models: Data representation only

### 2. **Dependency Injection**
All components use **constructor injection** with `@RequiredArgsConstructor` (Lombok):

```java
@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateRepository repository;
    private final CurrencyCodeService currencyService;
    // Dependencies injected automatically
}
```

### 3. **Single Responsibility Principle (SRP)**
Each service has one clear purpose:
- `TariffCalculatorService` → Calculate tariffs
- `ExchangeRateService` → Analyze exchange rates
- `NewsAPIService` → Fetch news
- `SentimentAnalysisService` → Analyze sentiment

### 4. **Open/Closed Principle**
Services are open for extension but closed for modification:
- Easy to add new prediction algorithms
- Easy to add new data sources
- Doesn't break existing functionality

### 5. **Interface Segregation**
Repositories provide focused, purpose-specific methods:
```java
// Specific methods instead of generic ones
findLatestByFromCurrencyAndToCurrency(...)
findByDateRange(...)
```

### 6. **Dependency Inversion**
High-level modules depend on abstractions (interfaces):
- Services depend on Repository **interfaces**, not implementations
- JPA provides the implementations automatically

### 7. **Modularity**
System is divided into logical modules:
- **Basic**: Core CRUD operations
- **TariffLogic**: Tariff-specific calculations
- **Data**: External data integration
- **Security**: Authentication/authorization

### 8. **DTO Pattern**
Data Transfer Objects separate internal models from API contracts:
- **Request DTOs**: Input validation
- **Response DTOs**: Clean API responses
- **Basic DTOs**: Internal data transfer

### 9. **Repository Pattern**
All database access goes through repository interfaces:
- Abstraction over data source
- Easy to switch databases
- Testability (can mock repositories)

### 10. **Service Orchestration**
Complex operations use service orchestration:
- `TariffCalculatorService` orchestrates: validation → rate lookup → calculation → shipping → total
- `PredictiveAnalysisService` orchestrates: news fetch → sentiment → exchange rate → prediction

---

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 21
- **ORM**: Spring Data JPA with Hibernate
- **Database**: PostgreSQL 17.6 (Supabase)
- **Security**: Spring Security + JWT
- **API Documentation**: SpringDoc OpenAPI 3.0
- **Build Tool**: Maven
- **Web Scraping**: JSoup + Python microservice
- **JSON Processing**: Jackson
- **Utilities**: Lombok, java-dotenv

### Frontend
- **Framework**: React 18.2.0
- **Build Tool**: Vite 4.3.9
- **Styling**: Tailwind CSS 3.3.2
- **Icons**: Lucide React
- **HTTP Client**: Fetch API

### External APIs
- **OpenExchangeRates**: Currency exchange rates
- **News API**: Trade news articles
- **OpenAI API**: AI-powered features
- **REST Countries API**: Country code conversion

### Infrastructure
- **Database**: Supabase PostgreSQL
- **Connection Pool**: HikariCP
- **Containerization**: Docker + Docker Compose

---

## Data Flow

### Flow 1: Tariff Calculation

```
User Input (Frontend)
    │
    ▼
POST /api/tariffs/calculate
    │
    ▼
TariffController
    │
    ▼
TariffCalculatorService ──┐
    │                     │
    ├─► TariffValidationService (validate inputs)
    │                     │
    ├─► TariffRateService (get tariff rate)
    │        │            │
    │        ├─► TariffRateRepository (check DB)
    │        │            │
    │        └─► WebScrapingService (if not in DB)
    │                     │
    ├─► ShippingCostService (calculate shipping)
    │        │            │
    │        └─► ShippingService (get rates)
    │                     │
    ├─► CountryProfileRepository (get VAT rates)
    │                     │
    └─► Calculate totals  │
         │               │
         ▼               │
    TariffCalculationResultDTO
         │
         ▼
    Response to Frontend
         │
         ▼
    ResultsPage displays results
```

### Flow 2: Exchange Rate Analysis

```
User clicks "Analyze Exchange Rates"
    │
    ▼
GET /api/exchange-rates/analyze
    │
    ▼
ExchangeRateController
    │
    ▼
ExchangeRateService
    │
    ├─► Resolve country codes
    │
    ├─► Get currency codes (CurrencyCodeService)
    │
    ├─► Try fetch live rates
    │    │
    │    └─► OpenExchangeRates API (HTTPS)
    │         │
    │         └─► Store in ExchangeRateRepository
    │
    ├─► Get historical rates (past 6 months)
    │    │
    │    └─► ExchangeRateRepository query
    │
    ├─► Perform trend analysis
    │    │
    │    ├─► Calculate min, max, average
    │    ├─► Determine trend (increasing/decreasing/stable)
    │    └─► Generate recommendation
    │
    └─► Build response
         │
         ▼
    ExchangeRateAnalysisResponse
         │
         ▼
    Frontend displays trends & recommendation
```

### Flow 3: Predictive Analysis (News Sentiment-Based)

```
User clicks "Get Prediction"
    │
    ▼
GET /api/predictive-analysis/predict
    │
    ▼
PredictiveAnalysisController
    │
    ▼
PredictiveAnalysisService
    │
    ├─► If enableNewsAnalysis = true:
    │    │
    │    ├─► NewsAPIService.fetchTradeNews()
    │    │    │
    │    │    └─► News API (HTTPS)
    │    │         │
    │    │         └─► Returns articles
    │    │
    │    ├─► SentimentAnalysisService.processArticleSentiments()
    │    │    │
    │    │    ├─► Analyze each article (keyword matching)
    │    │    └─► Store in NewsArticleRepository
    │    │
    │    └─► SentimentAnalysisService.calculateWeeklySentiment()
    │         │
    │         ├─► Aggregate scores
    │         ├─► Determine trend
    │         └─► Store in SentimentAnalysisRepository
    │
    ├─► Get latest sentiment (SentimentAnalysisRepository)
    │
    ├─► Get exchange rate (ExchangeRateRepository)
    │
    ├─► Generate prediction
    │    │
    │    ├─► Multi-factor scoring:
    │    │    • Current sentiment (40% weight)
    │    │    • Sentiment trend (30% weight)
    │    │    • Volatility (20% weight)
    │    │    • Consistency (10% weight)
    │    │
    │    └─► Determine: BUY / HOLD / WAIT
    │
    ├─► Get supporting headlines (2 samples)
    │
    └─► Build response
         │
         ▼
    PredictiveAnalysisResponse
         │
         ▼
    Frontend displays recommendation
```

### Flow 4: User Authentication

```
User enters credentials
    │
    ▼
POST /auth/login
    │
    ▼
AuthController
    │
    ▼
AuthenticationManager (Spring Security)
    │
    ├─► UserDetailsServiceImpl
    │    │
    │    └─► UserRepository.findByUsername()
    │         │
    │         └─► Load user from DB
    │
    ├─► Password verification (BCrypt)
    │
    └─► If valid:
         │
         ▼
    JwtService.generateToken()
         │
         ▼
    Return JWT token to frontend
         │
         ▼
    Frontend stores token in localStorage
         │
         ▼
    Future requests include:
    Authorization: Bearer <JWT>
         │
         ▼
    JwtAuthenticationFilter validates token
```

---

## Security Architecture

### Authentication Flow

```
┌──────────────┐
│   Client     │
│  (Browser)   │
└──────┬───────┘
       │
       │ 1. POST /auth/login
       │    {username, password}
       │
       ▼
┌──────────────────────────────┐
│   AuthController             │
│                              │
│  ┌────────────────────────┐ │
│  │ AuthenticationManager  │ │
│  │   validates creds      │ │
│  └────────────────────────┘ │
└──────┬───────────────────────┘
       │
       │ 2. If valid
       │
       ▼
┌──────────────────────────────┐
│   JwtService                 │
│   Generates JWT token        │
│   Signs with secret key      │
└──────┬───────────────────────┘
       │
       │ 3. Return JWT
       │
       ▼
┌──────────────┐
│   Client     │
│ Stores token │
│ in localStorage
└──────┬───────┘
       │
       │ 4. Subsequent requests
       │    Authorization: Bearer <JWT>
       │
       ▼
┌──────────────────────────────┐
│ JwtAuthenticationFilter      │
│                              │
│ 1. Extract token from header │
│ 2. Validate signature        │
│ 3. Check expiration          │
│ 4. Load user details         │
│ 5. Set SecurityContext       │
└──────┬───────────────────────┘
       │
       │ 5. Proceed to controller
       │
       ▼
┌──────────────────────────────┐
│   Protected Endpoint         │
│   (User is authenticated)    │
└──────────────────────────────┘
```

### Security Features

**Authentication**
- JWT-based stateless authentication
- BCrypt password hashing
- Token expiration (15 minutes default)
- Role-based access control (RBAC)

**Authorization**
- Public endpoints: `/auth/**`, `/api/countries/**`, `/api/exchange-rates/**`, etc.
- Protected endpoints: `/api/users/**`, admin functions
- Swagger UI: Public access

**Security Best Practices**
- Passwords never logged
- JWT secret in environment variable
- HTTPS enforced in production
- CORS configured properly
- SQL injection prevented (JPA parameterized queries)
- Input validation on all endpoints

---

## External Integrations

### 1. OpenExchangeRates API
```
┌──────────────────────────────┐
│  ExchangeRateService         │
└──────────┬───────────────────┘
           │
           │ HTTPS GET request
           │ https://openexchangerates.org/api/latest.json
           │ ?app_id={API_KEY}&base={FROM}&symbols={TO}
           │
           ▼
┌──────────────────────────────┐
│  OpenExchangeRates API       │
│  • 1,000 req/month (free)    │
│  • USD base only (free tier) │
│  • 170+ currencies           │
└──────────┬───────────────────┘
           │
           │ JSON response
           │ {"rates": {"EUR": 0.85, ...}}
           │
           ▼
┌──────────────────────────────┐
│  Parse & Store in DB         │
│  ExchangeRateRepository      │
└──────────────────────────────┘
```

**Purpose**: Real-time exchange rate data  
**Fallback**: Database (last known rates)  
**Caching**: 24 hours

### 2. News API
```
┌──────────────────────────────┐
│  NewsAPIService              │
└──────────┬───────────────────┘
           │
           │ HTTPS GET request
           │ https://newsapi.org/v2/everything
           │ ?q=trade&apiKey={KEY}
           │
           ▼
┌──────────────────────────────┐
│  News API                    │
│  • 100 req/day (free)        │
│  • 80,000+ sources           │
│  • 7 days historical (free)  │
└──────────┬───────────────────┘
           │
           │ JSON response
           │ {"articles": [{title, description, ...}]}
           │
           ▼
┌──────────────────────────────┐
│  SentimentAnalysisService    │
│  • Keyword-based scoring     │
│  • -1 to +1 scale            │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  Store Articles & Sentiment  │
│  NewsArticleRepository       │
│  SentimentAnalysisRepository │
└──────────────────────────────┘
```

**Purpose**: Trade news for sentiment analysis  
**Fallback**: Database (last stored sentiment)  
**Keywords**: tariff, imports, exports, currency, trade policy

### 3. Python Scraper Microservice
```
┌──────────────────────────────┐
│  WebScrapingService (Java)   │
└──────────┬───────────────────┘
           │
           │ HTTP POST
           │ http://localhost:5001/scrape
           │ {"importing": "US", "exporting": "CN"}
           │
           ▼
┌──────────────────────────────┐
│  Python Scraper Service      │
│  • Flask application         │
│  • BeautifulSoup parsing     │
│  • Scrapes govt websites     │
└──────────┬───────────────────┘
           │
           │ JSON response
           │ {"tariffData": [...]}
           │
           ▼
┌──────────────────────────────┐
│  Store in TariffRateRepo     │
└──────────────────────────────┘
```

**Purpose**: Automated tariff data collection  
**Technology**: Python Flask + BeautifulSoup  
**Port**: 5001

### 4. OpenAI API
```
┌──────────────────────────────┐
│  AI-Enhanced Services        │
└──────────┬───────────────────┘
           │
           │ HTTPS POST
           │ https://api.openai.com/v1/chat/completions
           │
           ▼
┌──────────────────────────────┐
│  OpenAI GPT                  │
│  • Natural language queries  │
│  • Smart recommendations     │
└──────────────────────────────┘
```

**Purpose**: AI-powered assistance  
**Usage**: Enhanced user experience

---

## Database Schema

### Core Entities

```sql
-- Users and Authentication
user
├── id (PK)
├── username
├── email
├── password (hashed)
├── role (USER/ADMIN)
└── is_active

-- Country Data
country
├── country_code (PK)
├── country_name
└── iso3_code

country_profile
├── id (PK)
├── country_code (FK)
└── vat_rate

-- Product Data
product
├── hs_code (PK)
├── description
└── category

-- Tariff Data
tariff_rate
├── id (PK)
├── hs_code (FK)
├── importing_country_code (FK)
├── exporting_country_code (FK)
├── ad_valorem_rate
└── duty_type

-- Shipping Data
shipping_rate
├── id (PK)
├── importing_country (FK)
├── exporting_country (FK)
├── air_rate
├── sea_rate
└── land_rate

-- Exchange Rate Data ⭐ NEW
exchange_rate
├── id (PK)
├── from_currency
├── to_currency
├── rate
├── rate_date
├── created_at
└── updated_at

-- News & Sentiment Data ⭐ NEW
news_article
├── id (PK)
├── title
├── description
├── url
├── source
├── published_at
├── sentiment_score
├── keywords
└── created_at

sentiment_analysis
├── id (PK)
├── week_start_date
├── week_end_date
├── average_sentiment
├── article_count
├── positive_count
├── negative_count
├── neutral_count
└── trend

-- User Activity
search_history
├── id (PK)
├── user_id (FK)
├── search_query
└── created_at
```

### Relationships

```
User ──1:N──> SearchHistory
Country ──1:N──> TariffRate
Product ──1:N──> TariffRate
Country ──1:1──> CountryProfile
Country ──1:N──> ShippingRate
```

---

## Design Patterns Used

### 1. **Repository Pattern**
All database access through repository interfaces

### 2. **DTO Pattern**
Separate internal models from API contracts

### 3. **Service Layer Pattern**
Business logic isolated in service classes

### 4. **Dependency Injection Pattern**
Constructor-based DI with Spring

### 5. **Builder Pattern**
Used in DTOs with Lombok `@Builder`

### 6. **Strategy Pattern**
Different calculation strategies (valuation basis, duty types)

### 7. **Facade Pattern**
`TariffCalculatorService` facades multiple services

### 8. **Observer Pattern**
Spring events for logging and monitoring

### 9. **Factory Pattern**
JWT token creation in `JwtService`

### 10. **Filter Pattern**
`JwtAuthenticationFilter` for request filtering

---

## System Characteristics

### Scalability
- **Stateless services** - Can scale horizontally
- **Connection pooling** - HikariCP (10 max connections)
- **Caching** - In-memory caching for frequently accessed data
- **Database indexing** - Optimized queries
- **Async processing** - Background jobs for scraping

### Performance
- **Response Time**: <200ms for cached data, <1s for API calls
- **Database**: Indexed queries, batch operations
- **API Rate Limits**: Managed with caching and fallbacks
- **Connection Pool**: Configured for optimal throughput

### Reliability
- **Graceful Degradation**: Fallback to database when APIs fail
- **Error Handling**: Comprehensive try-catch blocks
- **Logging**: SLF4J with different log levels
- **Validation**: Input validation at controller level
- **Transaction Management**: `@Transactional` for data consistency

### Maintainability
- **Clear structure**: Organized by layer and purpose
- **Documentation**: JavaDoc comments throughout
- **Testing**: Unit and integration tests
- **Code quality**: Lombok reduces boilerplate
- **Modularity**: Easy to add new features

### Security
- **Authentication**: JWT-based
- **Authorization**: Role-based access control
- **Encryption**: BCrypt for passwords
- **API Keys**: Stored in environment variables
- **CORS**: Configured properly
- **SQL Injection**: Prevented by JPA

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Docker Compose                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌────────────────────┐    ┌─────────────────────────┐ │
│  │  Frontend          │    │  Backend                │ │
│  │  (Nginx/Node)      │    │  (Spring Boot)          │ │
│  │  Port: 5173/80     │    │  Port: 8080             │ │
│  └────────────────────┘    └─────────────────────────┘ │
│                                                          │
│  ┌────────────────────┐    ┌─────────────────────────┐ │
│  │  Python Scraper    │    │  PostgreSQL             │ │
│  │  (Flask)           │    │  (Supabase)             │ │
│  │  Port: 5001        │    │  Port: 5432             │ │
│  └────────────────────┘    └─────────────────────────┘ │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## File Structure

```
CS203G4T2-Project/
├── frontend/                    # React Frontend
│   ├── components/
│   │   ├── HomePage.jsx
│   │   ├── ResultsPage.jsx
│   │   ├── ExchangeRateAnalysis.jsx ⭐
│   │   ├── PredictiveAnalysis.jsx ⭐
│   │   ├── CountrySelect.jsx
│   │   └── ThemeToggle.jsx
│   ├── utils/
│   │   └── themeColours.js
│   └── package.json
│
├── src/main/java/com/cs203/tariffg4t2/
│   ├── controller/              # REST API Endpoints
│   │   ├── AuthController.java
│   │   ├── TariffController.java
│   │   ├── ExchangeRateController.java ⭐
│   │   ├── PredictiveAnalysisController.java ⭐
│   │   └── ...
│   │
│   ├── service/                 # Business Logic
│   │   ├── basic/               # Basic CRUD services
│   │   ├── tariffLogic/         # Tariff calculations
│   │   └── data/                # External data integration
│   │
│   ├── repository/              # Data Access
│   │   └── basic/               # JPA repositories
│   │
│   ├── model/                   # Domain Models
│   │   ├── basic/               # Core entities
│   │   ├── enums/               # Enumerations
│   │   └── web/                 # Web-specific models
│   │
│   ├── dto/                     # Data Transfer Objects
│   │   ├── request/             # API request DTOs
│   │   ├── response/            # API response DTOs
│   │   └── basic/               # Shared DTOs
│   │
│   ├── security/                # Security Components
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   │
│   └── config/                  # Configuration
│       ├── CorsConfig.java
│       └── OpenApiConfig.java
│
├── src/main/resources/
│   └── application.properties   # Configuration
│
├── src/test/java/               # Tests
│   ├── service/                 # Unit tests
│   └── integration/             # Integration tests
│
├── tariff-scraper/              # Python Microservice
│   ├── main.py
│   └── rates.py
│
├── pom.xml                      # Maven dependencies
├── docker-compose.yml           # Container orchestration
└── README.md                    # Project documentation
```

---

This comprehensive documentation covers your system architecture. Would you like me to create:
1. UML class diagrams?
2. Sequence diagrams for specific flows?
3. More detailed component documentation?

Let me know what else you need!

