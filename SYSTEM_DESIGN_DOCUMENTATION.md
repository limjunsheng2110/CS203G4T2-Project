# TariffNom System Design Documentation

**Project**: CS203 G4T2 Tariff Calculation Platform  
**Team**: Merlin Gan, Lim Junsheng, Sim Qi Xun, Tiew Chun Yong Ethan, Sim Kay Wee  
**Document Version**: 1.0  
**Last Updated**: November 10, 2025

---

## Executive Summary

TariffNom is a comprehensive tariff calculation and analysis platform designed to simplify international trade cost determination. The system provides real-time tariff calculations, exchange rate analysis, and AI-driven purchase recommendations through a modern web interface.

**Key Capabilities:**
- Accurate tariff calculations between any country pair
- Real-time exchange rate trend analysis (6-month historical)
- AI-driven purchase timing recommendations (BUY/HOLD/WAIT)
- News sentiment analysis for market intelligence
- Automated data collection through web scraping
- Secure user authentication and history tracking

---

## 1. Architecture Overview

### 1.1 Architectural Pattern: **3-Tier Layered Architecture**

TariffNom implements a classic layered architecture pattern with strict separation of concerns across three primary tiers:

**Tier 1: Presentation Layer (Frontend)**
- Technology: React 18.2 with Vite and Tailwind CSS
- Responsibility: User interface and user experience
- Communication: RESTful JSON API calls to backend

**Tier 2: Application Layer (Backend)**
- Technology: Spring Boot 3.5.5 with Java 21
- Responsibility: Business logic, API endpoints, data processing
- Sub-layers: Controller → Service → Repository

**Tier 3: Data Layer**
- Technology: PostgreSQL 17.6 (hosted on Supabase)
- Responsibility: Persistent data storage
- Access: Spring Data JPA with Hibernate ORM

### 1.2 Architectural Style

The system follows **RESTful microservices** principles:
- Stateless server architecture
- Resource-based URLs
- Standard HTTP methods (GET, POST, PUT, DELETE)
- JSON data interchange format
- JWT token-based authentication

---

## 2. System Components

### 2.1 Frontend Components (React)

**Technology Stack:**
- React 18.2.0 (Component-based UI library)
- Vite 4.3.9 (Build tool and dev server)
- Tailwind CSS 3.3.2 (Utility-first CSS framework)
- Lucide React (Icon library)

**Key Components:**

| Component | Purpose | Features |
|-----------|---------|----------|
| **HomePage** | Landing page with input form | Country selection, HS code input, shipping mode, product value |
| **ResultsPage** | Display calculation results | Tariff breakdown, cost summary, analysis sections |
| **ExchangeRateAnalysis** | Currency trend analysis | 6-month trends, min/max rates, purchase recommendations |
| **PredictiveAnalysis** | AI-driven predictions | BUY/HOLD/WAIT recommendations, news sentiment, confidence scores |
| **CountrySelect** | Reusable country picker | Dropdown with search, supports multiple formats |
| **ThemeToggle** | Light/dark mode switcher | Persistent theme preference |
| **ResultCard** | Individual result display | Formatted cost breakdown, visual indicators |

**Design Principles:**
- Component reusability
- Props-based data flow
- Responsive design (mobile-first)
- Accessibility compliance
- Performance optimization (lazy loading, memoization)

---

### 2.2 Backend Components (Spring Boot)

#### Layer 1: Controller Layer (API Endpoints)

**Purpose:** Handle HTTP requests, validate inputs, marshal responses

**Controllers:**

| Controller | Base Path | Responsibility |
|------------|-----------|----------------|
| **AuthController** | `/auth` | User registration and login, JWT token generation |
| **TariffController** | `/api/tariffs` | Tariff calculation orchestration |
| **ExchangeRateController** | `/api/exchange-rates` | Exchange rate analysis and trends |
| **PredictiveAnalysisController** | `/api/predictive-analysis` | AI predictions based on news sentiment |
| **CountryController** | `/api/countries` | Country data CRUD operations |
| **ProductController** | `/api/products` | Product/HS code management |
| **UserController** | `/api/users` | User profile management |
| **ScrapingController** | `/api/scraping` | Web scraping job management |
| **TariffRateController** | `/api/rates/tariff` | Tariff rate CRUD |
| **ShippingRateController** | `/api/rates/shipping` | Shipping rate CRUD |

**Design Characteristics:**
- Thin controllers (minimal logic)
- Input validation using `@Valid` annotations
- Exception handling with proper HTTP status codes
- CORS enabled for frontend integration
- Swagger/OpenAPI documentation

#### Layer 2: Service Layer (Business Logic)

**Purpose:** Implement business rules, orchestrate operations, handle complex logic

**Service Organization:**

**A. Basic Services** (`service/basic/`)
- General-purpose CRUD operations
- User management, country management, product management
- Examples: `UserService`, `CountryService`, `ProductService`, `ShippingService`

**B. Tariff Logic Services** (`service/tariffLogic/`)
- Tariff-specific calculations and rules
- Complex business logic for duty calculation
- Examples: `TariffCalculatorService`, `TariffRateService`, `ShippingCostService`

**C. Data Services** (`service/data/`)
- External API integrations
- Data transformation and enrichment
- Examples: `NewsAPIService`, `SentimentAnalysisService`, `WebScrapingService`, `CurrencyCodeService`

**Key Services and Responsibilities:**

**TariffCalculatorService** 
- **Responsibility**: Orchestrates complete tariff calculation pipeline
- **Process**:
  1. Validate input (via `TariffValidationService`)
  2. Determine valuation basis (CIF vs Transaction value)
  3. Calculate customs value
  4. Retrieve tariff rate (via `TariffRateService`)
  5. Calculate base duty (ad valorem, specific, compound, or mixed)
  6. Apply preferential rates from trade agreements (FTA)
  7. Handle Tariff Rate Quotas (TRQ)
  8. Add additional duties (Section 301, ADD, CVD, Safeguard)
  9. Calculate VAT/GST on appropriate base
  10. Add shipping costs (via `ShippingCostService`)
  11. Sum total landed cost
- **Dependencies**: TariffRateService, ShippingCostService, TariffValidationService, CountryProfileRepository, AdditionalDutyMapRepository

**ExchangeRateService** ⭐
- **Responsibility**: Analyze exchange rate trends and provide purchase timing recommendations
- **Key Features**:
  - Fetches live rates from OpenExchangeRates API
  - Stores historical data (6 months retention)
  - Performs statistical trend analysis (moving averages)
  - Identifies optimal purchase dates
  - Graceful fallback to database when API unavailable
- **Algorithm**: Compares first-half vs second-half averages to detect trends
- **Output**: Current rate, average, min/max rates, recommended purchase date
- **Dependencies**: ExchangeRateRepository, CurrencyCodeService, OpenExchangeRates API

**PredictiveAnalysisService** ⭐
- **Responsibility**: Generate AI-driven BUY/HOLD/WAIT recommendations using news sentiment
- **Multi-Factor Prediction Algorithm**:
  - **Factor 1 (40%)**: Current sentiment polarity (-1 to +1)
  - **Factor 2 (30%)**: Sentiment trend direction (improving/declining/stable)
  - **Factor 3 (20%)**: Market volatility (standard deviation)
  - **Factor 4 (10%)**: Historical consistency
- **Decision Logic**:
  - BUY: Positive sentiment + improving trend + low volatility
  - WAIT: Negative sentiment + declining trend + high volatility
  - HOLD: Neutral sentiment + stable trend
- **Output**: Recommendation, confidence score (0-1), rationale, supporting headlines
- **Dependencies**: NewsAPIService, SentimentAnalysisService, ExchangeRateService

**NewsAPIService**
- **Responsibility**: Fetch trade and economic news articles
- **Process**:
  - Calls News API with keywords: "tariff", "imports", "exports", "currency", "trade policy"
  - Retrieves articles from past 7 days
  - Filters English language only
  - Parses JSON response
  - Checks for duplicates (by URL)
  - Stores new articles in database
- **API Provider**: NewsAPI.org
- **Rate Limits**: 100 requests/day (free tier)

**SentimentAnalysisService**
- **Responsibility**: Analyze sentiment of news articles
- **Algorithm**: Keyword-based scoring with weighted values
  - **Positive keywords** (+0.4 to +0.7): growth, boost, expansion, improve, cooperation, partnership, deal, opportunity, recovery
  - **Negative keywords** (-0.4 to -0.8): tariff, war, dispute, conflict, decline, threat, tension, sanction, crisis, uncertainty
  - **Scoring**: Sum of matched keyword weights / number of matches
  - **Normalization**: Clamped between -1.0 (very negative) and +1.0 (very positive)
- **Aggregation**: Weekly averages with trend detection
- **Output**: Sentiment score, trend direction, article counts by polarity

#### Layer 3: Repository Layer (Data Access)

**Purpose:** Abstract database operations using Spring Data JPA

**Design:**
- All repositories extend `JpaRepository<Entity, ID>`
- Custom query methods using Spring Data naming conventions
- `@Query` annotations for complex queries
- Automatic transaction management

**Key Repositories:**

| Repository | Entity | Key Methods |
|------------|--------|-------------|
| **UserRepository** | User | `findByUsername()`, `findByEmail()` |
| **TariffRateRepository** | TariffRate | `findByHsCodeAndImportingCountryCodeAndExportingCountryCode()` |
| **ExchangeRateRepository** | ExchangeRate | `findLatestByFromCurrencyAndToCurrency()`, `findByDateRangeBetween()` |
| **NewsArticleRepository** | NewsArticle | `findByPublishedAtBetween()`, `getAverageSentiment()`, `countPositiveArticles()` |
| **SentimentAnalysisRepository** | SentimentAnalysis | `findLatest()`, `findByDateRange()` |
| **CountryRepository** | Country | `findByCountryCode()`, `findByCountryNameIgnoreCase()` |
| **ProductRepository** | Product | `findById()` (HS code is ID) |
| **ShippingRateRepository** | ShippingRate | `findByImportingAndExportingCountry()` |

---

### 2.3 Security Components

**JwtService**
- Generates and validates JWT tokens
- Uses HMAC-SHA256 signing algorithm
- Configurable expiration time (default 15 minutes)
- Extracts user claims (userId, username, email, role)

**JwtAuthenticationFilter**
- Intercepts every HTTP request
- Extracts JWT from Authorization header (Bearer scheme)
- Validates token signature and expiration
- Loads user details and sets Spring Security context
- Allows request to proceed to controllers

**SecurityConfig**
- Configures Spring Security framework
- Defines public vs protected endpoints
- Disables CSRF (stateless API)
- Enables CORS for frontend
- Configures password encoder (BCrypt)
- Sets up authentication provider chain

**UserDetailsServiceImpl**
- Implements Spring Security's `UserDetailsService`
- Loads user from database
- Checks user active status
- Builds granted authorities from user roles
- Integrates with Spring Security authentication

---

### 2.4 Configuration Components

**CorsConfig**
- Configures Cross-Origin Resource Sharing
- Allows frontend (port 5173) to access backend (port 8080)
- Permits all HTTP methods
- Allows credentials and custom headers

**OpenApiConfig**
- Configures Swagger/OpenAPI documentation
- Defines API metadata (title, version, description)
- Organizes endpoints by tags
- Accessible at `/swagger-ui/index.html`

---

## 3. Design Principles

### 3.1 SOLID Principles

**S - Single Responsibility Principle**
- Each service has one clear responsibility
- Example: `ExchangeRateService` only handles exchange rates, not tariffs

**O - Open/Closed Principle**
- Services open for extension through new implementations
- Closed for modification through interface-based design
- Example: Easy to add new prediction algorithms without changing existing code

**L - Liskov Substitution Principle**
- Repository interfaces can be swapped with implementations
- Services depend on abstractions, not concrete classes

**I - Interface Segregation Principle**
- Repositories provide focused, specific methods
- No "god" interfaces with too many methods
- Example: Separate repositories for each entity type

**D - Dependency Inversion Principle**
- High-level modules depend on abstractions (interfaces)
- Dependency injection through Spring
- Example: Services depend on Repository interfaces, not implementations

### 3.2 Additional Design Principles

**Separation of Concerns**
- Clear boundaries between layers
- Controllers don't contain business logic
- Services don't handle HTTP concerns
- Repositories don't contain business rules

**Don't Repeat Yourself (DRY)**
- Shared logic extracted to utility services
- Reusable components in frontend
- Common validation logic centralized

**Dependency Injection**
- Constructor-based injection (Spring Boot)
- Promotes testability
- Reduces coupling

**Fail-Safe Defaults**
- Graceful degradation when external APIs fail
- Database fallback mechanisms
- Default values for optional parameters

**Logging and Monitoring**
- SLF4J logging throughout
- Different log levels (INFO, WARN, ERROR, DEBUG)
- Performance metrics tracking

---

## 4. Technology Decisions

### 4.1 Backend Technology Choices

**Spring Boot 3.5.5**
- **Why**: Industry-standard Java framework
- **Benefits**: Auto-configuration, embedded server, production-ready features
- **Features Used**: Web MVC, Data JPA, Security, Validation

**PostgreSQL 17.6**
- **Why**: Robust relational database with JSON support
- **Benefits**: ACID compliance, advanced indexing, scalability
- **Hosting**: Supabase (managed PostgreSQL with built-in features)

**Spring Security + JWT**
- **Why**: Stateless authentication for REST APIs
- **Benefits**: Scalable, no server-side sessions, works with SPA
- **Algorithm**: HMAC-SHA256, BCrypt password hashing

**Spring Data JPA**
- **Why**: Reduces boilerplate data access code
- **Benefits**: Automatic query generation, type-safe queries, caching

**Lombok**
- **Why**: Reduce Java verbosity
- **Benefits**: Auto-generate getters/setters/constructors, cleaner code

### 4.2 Frontend Technology Choices

**React 18.2**
- **Why**: Component-based architecture, large ecosystem
- **Benefits**: Virtual DOM performance, reusable components, hooks

**Vite**
- **Why**: Fast build tool and dev server
- **Benefits**: Hot module replacement, optimized production builds

**Tailwind CSS**
- **Why**: Utility-first CSS framework
- **Benefits**: Rapid development, consistent design, responsive utilities

### 4.3 External Service Choices

**OpenExchangeRates**
- **Why**: Reliable, well-documented exchange rate API
- **Free Tier**: 1,000 requests/month, hourly updates, 170+ currencies
- **Alternative**: Could use exchangerate-api.com or fixer.io

**News API**
- **Why**: Comprehensive news aggregation from 80,000+ sources
- **Free Tier**: 100 requests/day, 7 days historical data
- **Alternative**: Could use Bing News API or NewsData.io

**Python Flask Microservice**
- **Why**: Python's BeautifulSoup excellent for web scraping
- **Benefits**: Separation of concerns, language-specific strengths

---

## 5. Service Responsibilities (Detailed)

### 5.1 Core Tariff Services

#### TariffCalculatorService
**Type:** Orchestration Service  
**Package:** `com.cs203.tariffg4t2.service.tariffLogic`

**Responsibilities:**
1. Coordinate the complete tariff calculation workflow
2. Apply business rules for duty calculation
3. Handle different valuation bases (CIF, Transaction Value)
4. Calculate duties using various types (ad valorem, specific, compound, mixed)
5. Apply trade agreement preferential rates
6. Handle Tariff Rate Quotas (TRQ)
7. Stack additional duties (Section 301, Anti-Dumping, Countervailing, Safeguard)
8. Calculate VAT/GST on appropriate base values
9. Integrate shipping costs
10. Return comprehensive cost breakdown

**Key Methods:**
```java
public TariffCalculationResultDTO calculate(TariffCalculationRequestDTO request)
```

**Algorithm:**
```
CustomsValue = CIF or Transaction Value
BaseDuty = CustomsValue × TariffRate
DutyBase = CustomsValue + BaseDuty
AdditionalDuties = DutyBase × Additional Rates (stacked)
TaxBase = DutyBase + AdditionalDuties
VAT = TaxBase × VAT_Rate
TotalCost = TaxBase + VAT + ShippingCost
```

#### TariffRateService
**Type:** Data Retrieval Service  
**Package:** `com.cs203.tariffg4t2.service.tariffLogic`

**Responsibilities:**
1. Retrieve tariff rates for specific HS codes and country pairs
2. Check database first (cache lookup)
3. Trigger web scraping if rate not found
4. Save scraped data to repository
5. Handle multiple data sources
6. Cache frequently accessed rates

**Key Methods:**
```java
public BigDecimal calculateTariffAmount(TariffCalculationRequestDTO request)
```

**Data Flow:**
```
1. Check TariffRateRepository
2. If not found → Call WebScrapingService
3. Save scraped data
4. Return tariff amount
```

---

### 5.2 Exchange Rate Services

#### ExchangeRateService ⭐ NEW FEATURE
**Type:** Analysis Service  
**Package:** `com.cs203.tariffg4t2.service.basic`

**Responsibilities:**
1. Fetch live exchange rates from OpenExchangeRates API
2. Store rates in database for historical analysis
3. Analyze 6-month trends using statistical methods
4. Identify minimum and maximum rates with dates
5. Calculate moving averages
6. Determine trend direction (increasing/decreasing/stable)
7. Generate purchase timing recommendations
8. Provide fallback using database when API unavailable

**Key Methods:**
```java
public ExchangeRateAnalysisResponse analyzeExchangeRates(ExchangeRateAnalysisRequest request)
private void fetchAndStoreLiveRates(String fromCurrency, String toCurrency)
private TrendAnalysisResult performTrendAnalysis(List<ExchangeRate> rates)
private RecommendationResult generateRecommendation(List<ExchangeRate> rates, TrendAnalysisResult trend)
```

**Trend Detection Algorithm:**
```java
// Split historical data into two halves
firstHalfAverage = average(rates[0 to midpoint])
secondHalfAverage = average(rates[midpoint to end])

percentChange = ((secondHalf - firstHalf) / firstHalf) × 100

if (percentChange > 2.0%)  → "increasing"
if (percentChange < -2.0%) → "decreasing"
else                       → "stable"
```

**Recommendation Logic:**
- **Increasing trend**: "Consider purchasing sooner to avoid higher costs"
- **Decreasing trend**: "Consider waiting or purchasing soon as rates are falling"
- **Stable trend**: "Current rate is near average, stable conditions"

#### CurrencyCodeService
**Type:** Utility Service  
**Package:** `com.cs203.tariffg4t2.service.data`

**Responsibilities:**
1. Map country codes (ISO 3166-1) to currency codes (ISO 4217)
2. Support both alpha-2 and alpha-3 country codes
3. Handle regional currency groups (e.g., Eurozone countries → EUR)

**Coverage:**
- 50+ country-to-currency mappings
- Major trading nations and currencies
- Easy to extend with new mappings

---

### 5.3 Predictive Analysis Services

#### PredictiveAnalysisService ⭐ NEW FEATURE
**Type:** AI/ML Service  
**Package:** `com.cs203.tariffg4t2.service.basic`

**Responsibilities:**
1. Orchestrate news-based predictive analysis
2. Combine sentiment data with exchange rate trends
3. Apply multi-factor scoring algorithm
4. Generate actionable recommendations (BUY/HOLD/WAIT)
5. Calculate confidence scores
6. Provide supporting evidence (news headlines)
7. Handle API failures with database fallback

**Key Methods:**
```java
public PredictiveAnalysisResponse analyzePrediction(PredictiveAnalysisRequest request)
private PredictionResult generatePrediction(SentimentAnalysis sentiment, 
                                            ExchangeRate exchangeRate,
                                            List<SentimentAnalysis> history)
private double calculateSentimentVolatility(List<SentimentAnalysis> history)
```

**Multi-Factor Scoring Algorithm:**

```
Initialize scores: buyScore = 0, holdScore = 0, waitScore = 0

Factor 1: Current Sentiment
  if sentiment > 0.3  → buyScore += 0.4
  if sentiment < -0.3 → waitScore += 0.4
  else                → holdScore += 0.3

Factor 2: Sentiment Trend
  if "improving"  → buyScore += 0.3
  if "declining"  → waitScore += 0.3
  if "stable"     → holdScore += 0.2

Factor 3: Volatility
  if volatility < 0.2 → buyScore += 0.2, holdScore += 0.2
  if volatility > 0.3 → waitScore += 0.3

Factor 4: Consistency
  if all positive → buyScore += 0.1
  if all negative → waitScore += 0.1

Normalize: total = buyScore + holdScore + waitScore
  buyScore /= total
  holdScore /= total
  waitScore /= total

Recommendation = highest score
Confidence = that score
```

**Example Calculation:**
```
Sentiment: 0.45 (positive)       → buyScore += 0.4
Trend: "improving"               → buyScore += 0.3
Volatility: 0.15 (low)          → buyScore += 0.2
Consistently positive            → buyScore += 0.1
                                   ─────────────
Total buyScore = 1.0
Total holdScore = 0.0
Total waitScore = 0.0
Sum = 1.0

Result:
  Recommendation: BUY
  Confidence: 100% (1.0 / 1.0)
```

#### NewsAPIService
**Type:** Integration Service  
**Package:** `com.cs203.tariffg4t2.service.data`

**Responsibilities:**
1. Interface with News API (newsapi.org)
2. Construct API requests with proper parameters
3. Parse JSON responses
4. Extract article metadata (title, description, source, date, URL)
5. Identify matched keywords
6. Prevent duplicate article storage
7. Handle API errors gracefully

**API Endpoint Used:**
```
GET https://newsapi.org/v2/everything
  ?q=trade
  &from=2025-11-03
  &language=en
  &sortBy=publishedAt
  &pageSize=100
  &apiKey={API_KEY}
```

**Response Handling:**
- Checks status field for "ok"
- Extracts totalResults count
- Iterates through articles array
- Validates URL uniqueness
- Returns List<NewsArticle>

#### SentimentAnalysisService
**Type:** NLP/Analysis Service  
**Package:** `com.cs203.tariffg4t2.service.data`

**Responsibilities:**
1. Perform sentiment analysis on text
2. Use keyword matching algorithm
3. Process batches of articles
4. Calculate weekly sentiment aggregates
5. Determine sentiment trends over time
6. Classify sentiment polarity (positive/negative/neutral)
7. Calculate sentiment volatility (risk metric)

**Sentiment Keywords:**

Positive (+):
- agreement (0.5), boost (0.6), growth (0.7), increase (0.4)
- expansion (0.6), strengthen (0.5), improve (0.6), positive (0.5)
- cooperation (0.5), partnership (0.5), deal (0.4), opportunity (0.6), recovery (0.7)

Negative (-):
- tariff (-0.4), war (-0.8), dispute (-0.6), conflict (-0.7)
- decline (-0.6), fall (-0.5), drop (-0.5), threat (-0.7)
- tension (-0.6), sanction (-0.7), crisis (-0.8), uncertainty (-0.5), risk (-0.4)

**Classification Thresholds:**
- Positive: score > 0.2
- Negative: score < -0.2
- Neutral: -0.2 ≤ score ≤ 0.2

**Trend Detection:**
```java
currentWeekSentiment vs previousWeekSentiment

if (change > 0.1)  → "improving"
if (change < -0.1) → "declining"
else               → "stable"
```

---

### 5.4 Supporting Services

#### ShippingService
- Retrieves shipping rates for country pairs
- Supports three modes: AIR, SEA, LAND
- CRUD operations for shipping rates

#### CountryService
- Manages country reference data
- Validates country codes and names
- Supports alpha-2, alpha-3, and full name formats

#### ProductService
- Manages product/HS code data
- CRUD operations for products
- Category-based organization

#### UserService
- User account management
- Profile updates
- User listing and retrieval

#### WebScrapingService
- Integrates with Python scraper microservice
- Parses scraped tariff data
- Stores in database
- Handles scraping job queue

#### ConvertCodeService
- Converts country codes between formats
- Alpha-2 ↔ Alpha-3 conversion
- Caches conversions for performance
- Uses REST Countries API for lookups

---

## 6. Data Models

### 6.1 Core Domain Entities

**User**
```java
@Entity
@Table(name = "user")
class User {
    @Id Long id;
    String username;  // Unique
    String email;     // Unique  
    String password;  // BCrypt hashed
    Role role;        // USER, ADMIN
    Boolean isActive;
    LocalDateTime createdAt;
}
```

**TariffRate**
```java
@Entity
@Table(name = "tariff_rate")
class TariffRate {
    @Id Long id;
    String hsCode;
    String importingCountryCode;
    String exportingCountryCode;
    BigDecimal adValoremRate;
    DutyType dutyType;  // AD_VALOREM, SPECIFIC, COMPOUND, MIXED
    LocalDateTime updatedAt;
}
```

**ExchangeRate** ⭐
```java
@Entity
@Table(name = "exchange_rate")
class ExchangeRate {
    @Id Long id;
    String fromCurrency;  // ISO 4217 (e.g., USD, CNY)
    String toCurrency;
    BigDecimal rate;
    LocalDate rateDate;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

**NewsArticle** ⭐
```java
@Entity
@Table(name = "news_article")
class NewsArticle {
    @Id Long id;
    String title;
    String description;
    String url;           // Unique
    String source;
    LocalDateTime publishedAt;
    Double sentimentScore;  // -1.0 to +1.0
    String keywords;
    String countryCode;
    LocalDateTime createdAt;
}
```

**SentimentAnalysis** ⭐
```java
@Entity
@Table(name = "sentiment_analysis")
class SentimentAnalysis {
    @Id Long id;
    LocalDate weekStartDate;
    LocalDate weekEndDate;
    Double averageSentiment;  // -1.0 to +1.0
    Integer articleCount;
    Integer positiveCount;
    Integer negativeCount;
    Integer neutralCount;
    String trend;  // "improving", "declining", "stable"
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

### 6.2 Supporting Entities

**Country**
- countryCode (PK), countryName, iso3Code

**Product**
- hsCode (PK), description, category

**ShippingRate**
- id (PK), importingCountry, exportingCountry, airRate, seaRate, landRate

**CountryProfile**
- id (PK), countryCode (FK), vatRate

**SearchHistory**
- id (PK), userId (FK), searchQuery, createdAt

---

## 7. API Design

### 7.1 RESTful API Principles

**Resource-Based URLs:**
- `/api/tariffs` - Tariff calculations
- `/api/exchange-rates` - Exchange rate analysis
- `/api/predictive-analysis` - Predictions
- `/api/users` - User management
- `/api/countries` - Country data
- `/api/products` - Product data

**HTTP Methods:**
- GET - Retrieve resources
- POST - Create resources or trigger operations
- PUT - Update resources
- DELETE - Remove resources

**Status Codes:**
- 200 OK - Success
- 201 Created - Resource created
- 400 Bad Request - Validation error
- 401 Unauthorized - Authentication required
- 404 Not Found - Resource doesn't exist
- 500 Internal Server Error - Server error

**Request/Response Format:**
- Content-Type: application/json
- Accept: application/json
- UTF-8 encoding

### 7.2 API Documentation

**Tool:** SpringDoc OpenAPI 3.0 (Swagger UI)

**Access:** `http://localhost:8080/swagger-ui/index.html`

**Features:**
- Interactive API testing
- Request/response examples
- Schema documentation
- Try-it-out functionality
- Organized by tags

---

## 8. Security Architecture

### 8.1 Authentication Flow

```
1. User submits credentials (username + password)
2. AuthController receives request
3. Spring Security AuthenticationManager validates
4. UserDetailsServiceImpl loads user from database
5. BCrypt verifies password hash
6. If valid, JwtService generates JWT token
7. Token returned to client
8. Client stores token in localStorage
9. Future requests include: Authorization: Bearer <JWT>
10. JwtAuthenticationFilter validates each request
11. Sets SecurityContext for the request
12. Request proceeds to controller
```

### 8.2 Authorization Model

**Public Endpoints (No Authentication Required):**
- `/auth/**` - Login and registration
- `/api/countries/**` - Country data
- `/api/products/**` - Product data
- `/api/tariffs/**` - Tariff calculations
- `/api/exchange-rates/**` - Exchange rate analysis
- `/api/predictive-analysis/**` - Predictions
- `/swagger-ui/**` - API documentation

**Protected Endpoints (Authentication Required):**
- `/api/users/**` - User management (admin only)
- Future admin functions

**Role-Based Access:**
- **USER**: Standard access to all features
- **ADMIN**: User management, system configuration

### 8.3 Security Features

**Password Security:**
- BCrypt hashing (cost factor: 10)
- Salt automatically generated per password
- Passwords never logged or displayed

**JWT Security:**
- HMAC-SHA256 signing
- Secret key from environment variable
- 15-minute expiration (configurable)
- Includes claims: userId, username, email, role

**API Security:**
- All external API keys in environment variables
- Keys never committed to git
- Keys masked in logs
- HTTPS for all external communications

**SQL Injection Prevention:**
- JPA parameterized queries
- No raw SQL string concatenation
- Input validation on all endpoints

**CORS Security:**
- Configured allowed origins
- Credentials support enabled
- Specific headers allowed

---

## 9. Data Flow Examples

### 9.1 Complete Tariff Calculation Flow

```
User Input:
  - Importing Country: United States (US)
  - Exporting Country: China (CN)
  - HS Code: 8471.30.01 (Computers)
  - Product Value: $10,000
  - Shipping Mode: AIR
  - Weight: 50 kg

Step 1: Frontend sends POST /api/tariffs/calculate
  Request: {
    importingCountry: "US",
    exportingCountry: "CN",
    hsCode: "8471.30.01",
    productValue: 10000,
    shippingMode: "AIR",
    totalWeight: 50
  }

Step 2: TariffController receives request

Step 3: TariffCalculatorService.calculate()
  
  3a: TariffValidationService validates input
      ✓ All required fields present
      ✓ Country codes valid
      ✓ HS code format correct
  
  3b: TariffRateService.calculateTariffAmount()
      → Check TariffRateRepository for HS=8471.30.01, US→CN
      → If not found, trigger WebScrapingService
      → Return: adValoremRate = 15.5%
  
  3c: Calculate customs value (CIF basis)
      customsValue = productValue = $10,000
  
  3d: Calculate base duty
      baseDuty = $10,000 × 15.5% = $1,550
  
  3e: Check for trade agreements
      → No active FTA between US-CN
      → Use MFN rate
  
  3f: Calculate additional duties
      → Section 301: 25% on dutyBase
      → dutyBase = $10,000 + $1,550 = $11,550
      → Section301 = $11,550 × 25% = $2,887.50
  
  3g: Calculate VAT/GST
      → US has no federal VAT
      → VAT = $0
  
  3h: ShippingCostService.calculateShippingCost()
      → Mode: AIR, Weight: 50kg, CN→US
      → ShippingRate = $2.50/kg
      → Shipping = 50 × $2.50 = $125
  
  3i: Sum total cost
      totalCost = customsValue + baseDuty + additionalDuties + VAT + shipping
                = $10,000 + $1,550 + $2,887.50 + $0 + $125
                = $14,562.50

Step 4: Return TariffCalculationResultDTO
  Response: {
    totalCost: 14562.50,
    customsValue: 10000,
    baseDuty: 1550,
    additionalDuties: 2887.50,
    vatOrGst: 0,
    shippingCost: 125,
    adValoremRate: 15.5,
    exportingCountry: "CN",
    importingCountry: "US",
    hsCode: "8471.30.01"
  }

Step 5: Frontend displays results
```

### 9.2 Exchange Rate Analysis Flow

```
User Action: Click "Analyze Exchange Rates"

Step 1: Frontend calls GET /api/exchange-rates/analyze
  ?importingCountry=US&exportingCountry=CN

Step 2: ExchangeRateService.analyzeExchangeRates()
  
  2a: Resolve country codes
      US → US (alpha-2)
      CN → CN (alpha-2)
  
  2b: Get currency codes
      US → USD
      CN → CNY
  
  2c: Try fetch live rates
      → Call OpenExchangeRates API
      → URL: /latest.json?app_id=...&base=CNY&symbols=USD
      → Response: {"rates": {"USD": 0.1385}}
      → Store in database
      → liveDataAvailable = true
  
  2d: Get historical rates (past 6 months)
      → Query: findByFromCurrencyAndToCurrencyAndRateDateBetween()
      → Returns: 180 data points (daily rates)
  
  2e: Perform trend analysis
      → Calculate average: 0.1378
      → Find min: 0.1365 on Sept 15
      → Find max: 0.1392 on Oct 20
      → Determine trend: "stable" (change < 2%)
  
  2f: Generate recommendation
      → recommendedDate = Sept 15 (min rate date)
      → rationale = "Exchange rate is relatively stable..."

Step 3: Return ExchangeRateAnalysisResponse
  {
    currentRate: 0.1385,
    averageRate: 0.1378,
    minRate: 0.1365,
    minRateDate: "2025-09-15",
    maxRate: 0.1392,
    maxRateDate: "2025-10-20",
    recommendedPurchaseDate: "2025-09-15",
    trendAnalysis: "stable",
    liveDataAvailable: true,
    dataSource: "live_api"
  }

Step 4: Frontend displays visualization
```

### 9.3 Predictive Analysis Flow

```
User Action: Toggle "Enable News Analysis" ON, Click "Get Prediction"

Step 1: Frontend calls GET /api/predictive-analysis/predict
  ?importingCountry=US&exportingCountry=CN&enableNewsAnalysis=true

Step 2: PredictiveAnalysisService.analyzePrediction()
  
  2a: Resolve currencies
      US → USD, CN → CNY
  
  2b: Fetch news (if enabled)
      → NewsAPIService.fetchTradeNews(7 days)
      → API call to News API
      → Returns: 47 articles about "trade"
  
  2c: Analyze sentiment
      → For each article:
          title + description → analyze keywords
          Example: "Trade cooperation boosts economy"
            → "cooperation" (+0.5), "boosts" (+0.6)
            → sentiment = (0.5 + 0.6) / 2 = 0.55
      → Save articles with scores to database
  
  2d: Calculate weekly aggregate
      → Average of 47 articles: 0.32
      → Count: 28 positive, 12 negative, 7 neutral
      → Compare with last week: 0.28 → +0.04 → "improving"
      → Store in SentimentAnalysis table
  
  2e: Get exchange rate
      → Query latest CNY→USD rate: 0.1385
      → Determine trend: "stable"
  
  2f: Generate prediction
      Multi-factor scoring:
        Current sentiment (0.32) → neutral → holdScore += 0.3
        Sentiment trend (improving) → buyScore += 0.3
        Volatility (0.15, low) → buyScore += 0.2
        Not consistent → no bonus
      
      Scores: buy=0.5, hold=0.3, wait=0.0
      Normalized: buy=62.5%, hold=37.5%, wait=0%
      
      Recommendation: BUY
      Confidence: 0.625 (62.5%)
  
  2g: Get supporting headlines
      → Select top 2 most recent articles
      → Include titles, sources, sentiment scores

Step 3: Return PredictiveAnalysisResponse
  {
    recommendation: "BUY",
    confidenceScore: 0.625,
    rationale: "Market sentiment is positive and improving...",
    currentSentiment: 0.32,
    sentimentTrend: "improving",
    articlesAnalyzed: 47,
    currentExchangeRate: 0.1385,
    exchangeRateTrend: "stable",
    supportingHeadlines: [...],
    liveNewsAvailable: true
  }

Step 4: Frontend displays color-coded recommendation
  - Green card with ↗ icon
  - "BUY" in large text
  - Confidence bar at 62.5%
  - Rationale explanation
  - 2 clickable news headlines
  - 4-week sentiment chart
```

---

## 10. External Integrations

### 10.1 OpenExchangeRates API

**Purpose:** Real-time foreign exchange rates

**Integration Details:**
- **Endpoint**: `https://openexchangerates.org/api/latest.json`
- **Authentication**: App ID in query parameter
- **Rate Limit**: 1,000 requests/month (free tier)
- **Update Frequency**: Hourly
- **Limitations**: Free tier only supports USD as base currency

**Request Example:**
```
GET https://openexchangerates.org/api/latest.json
  ?app_id=2bf6c2d0b4984dcca22925622c77a84e
  &base=USD
  &symbols=CNY,EUR,GBP
```

**Response Format:**
```json
{
  "timestamp": 1699632000,
  "base": "USD",
  "rates": {
    "CNY": 7.23,
    "EUR": 0.85,
    "GBP": 0.79
  }
}
```

**Caching Strategy:**
- Store rates in database
- Cache for 24 hours before refreshing
- Fallback to database when API unavailable

### 10.2 News API

**Purpose:** Trade and economic news articles

**Integration Details:**
- **Endpoint**: `https://newsapi.org/v2/everything`
- **Authentication**: API key in query parameter
- **Rate Limit**: 100 requests/day (free tier)
- **Data Range**: 7 days historical (free tier)
- **Sources**: 80,000+ news sources

**Request Example:**
```
GET https://newsapi.org/v2/everything
  ?q=trade
  &from=2025-11-03
  &language=en
  &sortBy=publishedAt
  &pageSize=100
  &apiKey=9121b99e28f8442888a5a49f9e667997
```

**Response Format:**
```json
{
  "status": "ok",
  "totalResults": 4728,
  "articles": [
    {
      "source": {"name": "Reuters"},
      "title": "Trade talks resume...",
      "description": "...",
      "url": "https://...",
      "publishedAt": "2025-11-10T08:30:00Z"
    }
  ]
}
```

**Caching Strategy:**
- Store articles in database
- Weekly sentiment aggregates
- Fallback to last known sentiment

### 10.3 Python Scraper Microservice

**Purpose:** Web scraping for tariff data

**Integration Details:**
- **Endpoint**: `http://localhost:5001/scrape`
- **Protocol**: HTTP POST
- **Technology**: Python Flask + BeautifulSoup

**Request:**
```json
POST http://localhost:5001/scrape
{
  "importingCountry": "US",
  "exportingCountry": "CN",
  "hsCode": "8471.30.01"
}
```

**Response:**
```json
{
  "tariffData": [{
    "hsCode": "8471.30.01",
    "adValoremRate": 15.5,
    "dutyType": "AD_VALOREM"
  }]
}
```

---

## 11. Design Patterns Implementation

### 11.1 Repository Pattern
**Where**: All data access  
**Purpose**: Abstract database operations  
**Benefit**: Easy to switch data sources, testability

### 11.2 Service Layer Pattern
**Where**: Business logic layer  
**Purpose**: Centralize business rules  
**Benefit**: Reusable, maintainable logic

### 11.3 DTO Pattern
**Where**: API boundaries  
**Purpose**: Decouple internal models from API contracts  
**Benefit**: API stability, validation, versioning

### 11.4 Dependency Injection
**Where**: All components  
**Purpose**: Loose coupling  
**Benefit**: Testability, flexibility, maintainability

### 11.5 Builder Pattern
**Where**: DTOs and complex objects  
**Purpose**: Fluent object construction  
**Benefit**: Readable code, optional parameters

### 11.6 Strategy Pattern
**Where**: Calculation algorithms (valuation basis, duty types)  
**Purpose**: Interchangeable algorithms  
**Benefit**: Easy to add new calculation methods

### 11.7 Facade Pattern
**Where**: TariffCalculatorService  
**Purpose**: Simplify complex subsystem  
**Benefit**: Single entry point for tariff calculation

### 11.8 Template Method Pattern
**Where**: Validation, calculation flows  
**Purpose**: Define algorithm skeleton  
**Benefit**: Consistent processing flow

---

## 12. Performance Optimizations

### 12.1 Database Optimizations
- **Indexes**: All foreign keys and frequently queried fields
- **Connection Pooling**: HikariCP (max 10 connections)
- **Batch Operations**: Hibernate batch inserts (size 20)
- **Query Optimization**: Fetch only required fields
- **Caching**: Second-level cache for country/product data

### 12.2 Application Optimizations
- **In-Memory Caching**: TariffCacheService for recent calculations
- **Lazy Loading**: Load related entities only when needed
- **Async Processing**: Background jobs for scraping
- **API Rate Limiting**: Client-side throttling to respect API limits

### 12.3 Network Optimizations
- **HTTP/2**: Modern protocol support
- **Gzip Compression**: Reduce payload sizes
- **CDN**: Static assets served from CDN in production
- **Connection Keep-Alive**: Reuse HTTP connections

---

## 13. Error Handling Strategy

### 13.1 Exception Hierarchy

```
RuntimeException
├── IllegalArgumentException (400 Bad Request)
├── IllegalStateException (500 Internal Server Error)
└── Custom Exceptions:
    ├── TariffCalculationException
    ├── ExchangeRateException
    └── SentimentAnalysisException
```

### 13.2 Error Response Format

```json
{
  "error": "Human-readable error message",
  "details": "Technical details for debugging",
  "timestamp": "2025-11-10T18:45:00"
}
```

### 13.3 Fallback Mechanisms

**Exchange Rate Service:**
- Primary: OpenExchangeRates API
- Fallback: Database (last known rates)
- User notification: "Using last stored data"

**Predictive Analysis:**
- Primary: News API (live sentiment)
- Fallback: Database (last sentiment analysis)
- User notification: "Live News API unavailable"

**Web Scraping:**
- Primary: Python scraper service
- Fallback: Return zero/null (graceful degradation)
- User notification: Included in response

---

## 14. Testing Strategy

### 14.1 Unit Tests
- **Scope**: Individual service methods
- **Framework**: JUnit 5, Mockito
- **Coverage**: Service layer business logic
- **Mocking**: Repositories, external services

### 14.2 Integration Tests
- **Scope**: Full API endpoints
- **Framework**: Spring Boot Test, MockMvc
- **Coverage**: Controller → Service → Repository
- **Database**: H2 in-memory or test PostgreSQL instance

### 14.3 Test Examples
- `ExchangeRateServiceTest`: 8 test cases
- `ExchangeRateIntegrationTest`: 8 end-to-end tests
- Coverage: Input validation, trend analysis, API fallback, error handling

---

## Summary

The TariffNom system demonstrates:
✅ **Clean architecture** with clear layer separation  
✅ **SOLID principles** throughout the codebase  
✅ **Comprehensive service design** with focused responsibilities  
✅ **Robust error handling** with graceful degradation  
✅ **External API integration** with fallback mechanisms  
✅ **Security best practices** (JWT, BCrypt, input validation)  
✅ **Performance optimization** (caching, connection pooling, indexing)  
✅ **Maintainable code** (DI, DTOs, documentation)  

**Total System Components:**
- 10 Controllers
- 17 Services
- 11 Repositories
- 14 Domain Models
- 100+ API endpoints (including Swagger)

---

**End of Technical Architecture Documentation**

