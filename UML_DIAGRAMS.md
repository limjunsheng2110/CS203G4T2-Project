# TariffNom System - UML Diagrams

## How to Use These Diagrams

The diagrams below are in **Mermaid** syntax. You can:
1. Copy and paste into https://mermaid.live/ to view/edit
2. Use Mermaid plugins in VS Code
3. Convert to PNG/SVG for documentation

---

## 1. System Component Diagram

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[React Frontend<br/>Port 5173]
    end
    
    subgraph "API Gateway Layer"
        Controllers[REST Controllers<br/>Spring Boot Port 8080]
    end
    
    subgraph "Business Logic Layer"
        BasicServices[Basic Services<br/>CRUD Operations]
        TariffServices[Tariff Logic Services<br/>Calculations]
        DataServices[Data Services<br/>External Integration]
        Security[Security Services<br/>JWT Auth]
    end
    
    subgraph "Data Access Layer"
        Repositories[JPA Repositories<br/>Spring Data]
    end
    
    subgraph "Data Storage"
        DB[(PostgreSQL<br/>Supabase)]
    end
    
    subgraph "External Services"
        OpenExchange[OpenExchangeRates API]
        NewsAPI[News API]
        PythonScraper[Python Scraper<br/>Port 5001]
        OpenAI[OpenAI API]
    end
    
    UI -->|HTTP/JSON| Controllers
    Controllers --> BasicServices
    Controllers --> TariffServices
    Controllers --> DataServices
    Controllers --> Security
    
    BasicServices --> Repositories
    TariffServices --> Repositories
    DataServices --> Repositories
    Security --> Repositories
    
    Repositories -->|JDBC/JPA| DB
    
    DataServices -->|HTTPS| OpenExchange
    DataServices -->|HTTPS| NewsAPI
    DataServices -->|HTTP| PythonScraper
    DataServices -->|HTTPS| OpenAI
```

---

## 2. Tariff Calculation Class Diagram

```mermaid
classDiagram
    class TariffController {
        -TariffCalculatorService calculatorService
        +calculate(request) TariffCalculationResultDTO
    }
    
    class TariffCalculatorService {
        -TariffRateService tariffRateService
        -ShippingCostService shippingCostService
        -TariffValidationService validationService
        +calculate(request) TariffCalculationResultDTO
        -resolveValuationBasis() ValuationBasis
        -calculateBaseDuty() BigDecimal
        -calculateVAT() BigDecimal
    }
    
    class TariffRateService {
        -TariffRateCRUDService crudService
        -WebScrapingService scrapingService
        -TariffCacheService cacheService
        +calculateTariffAmount(request) BigDecimal
        -saveScrapedDataToRepository()
    }
    
    class TariffValidationService {
        +validateTariffRequest(request) List~String~
        -setDefaultValues()
    }
    
    class ShippingCostService {
        -ShippingService shippingService
        +calculateShippingCost(mode, weight, ...) BigDecimal
    }
    
    class TariffRateRepository {
        <<interface>>
        +findByHsCodeAndCountries(...) Optional~TariffRate~
        +save(tariffRate) TariffRate
    }
    
    class TariffRate {
        -Long id
        -String hsCode
        -String importingCountryCode
        -String exportingCountryCode
        -BigDecimal adValoremRate
        -DutyType dutyType
    }
    
    TariffController --> TariffCalculatorService
    TariffCalculatorService --> TariffRateService
    TariffCalculatorService --> ShippingCostService
    TariffCalculatorService --> TariffValidationService
    TariffRateService --> TariffRateRepository
    TariffRateRepository --> TariffRate
```

---

## 3. Exchange Rate Analysis Class Diagram

```mermaid
classDiagram
    class ExchangeRateController {
        -ExchangeRateService service
        +analyzeExchangeRates(request) Response
        +healthCheck() Response
    }
    
    class ExchangeRateService {
        -ExchangeRateRepository repository
        -CurrencyCodeService currencyService
        +analyzeExchangeRates(request) ExchangeRateAnalysisResponse
        +fetchAndStoreLiveRates(...) void
        -performTrendAnalysis(...) TrendAnalysisResult
        -determineTrend(...) String
        -generateRecommendation(...) RecommendationResult
    }
    
    class CurrencyCodeService {
        -Map~String, String~ COUNTRY_TO_CURRENCY
        +getCurrencyCode(countryCode) String
        +hasCurrencyMapping(countryCode) boolean
    }
    
    class ExchangeRateRepository {
        <<interface>>
        +findLatestByFromCurrencyAndToCurrency(...) Optional~ExchangeRate~
        +findByDateRangeBetween(...) List~ExchangeRate~
        +save(rate) ExchangeRate
    }
    
    class ExchangeRate {
        -Long id
        -String fromCurrency
        -String toCurrency
        -BigDecimal rate
        -LocalDate rateDate
        -LocalDateTime createdAt
    }
    
    ExchangeRateController --> ExchangeRateService
    ExchangeRateService --> CurrencyCodeService
    ExchangeRateService --> ExchangeRateRepository
    ExchangeRateRepository --> ExchangeRate
```

---

## 4. Predictive Analysis Class Diagram

```mermaid
classDiagram
    class PredictiveAnalysisController {
        -PredictiveAnalysisService service
        +getPrediction(request) Response
        +debugFetchNews() Response
    }
    
    class PredictiveAnalysisService {
        -NewsAPIService newsAPIService
        -SentimentAnalysisService sentimentService
        -ExchangeRateService exchangeRateService
        +analyzePrediction(request) PredictiveAnalysisResponse
        -generatePrediction(...) PredictionResult
        -calculateSentimentVolatility(...) double
    }
    
    class NewsAPIService {
        -NewsArticleRepository repository
        -RestTemplate restTemplate
        -String apiKey
        +fetchTradeNews(daysBack) List~NewsArticle~
        -parseNewsArticles(json) List~NewsArticle~
        +testAPIConnection() boolean
    }
    
    class SentimentAnalysisService {
        -Map~String, Double~ POSITIVE_KEYWORDS
        -Map~String, Double~ NEGATIVE_KEYWORDS
        +analyzeSentiment(text) Double
        +processArticleSentiments(articles) void
        +calculateWeeklySentiment(...) SentimentAnalysis
        -determineTrend(...) String
    }
    
    class NewsArticle {
        -Long id
        -String title
        -String description
        -String url
        -Double sentimentScore
        -LocalDateTime publishedAt
    }
    
    class SentimentAnalysis {
        -Long id
        -LocalDate weekStartDate
        -LocalDate weekEndDate
        -Double averageSentiment
        -Integer articleCount
        -String trend
    }
    
    PredictiveAnalysisController --> PredictiveAnalysisService
    PredictiveAnalysisService --> NewsAPIService
    PredictiveAnalysisService --> SentimentAnalysisService
    PredictiveAnalysisService --> ExchangeRateService
    NewsAPIService --> NewsArticle
    SentimentAnalysisService --> SentimentAnalysis
```

---

## 5. Security Architecture Diagram

```mermaid
classDiagram
    class SecurityConfig {
        -UserDetailsService userDetailsService
        -JwtAuthenticationFilter jwtFilter
        +securityFilterChain() SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager() AuthenticationManager
    }
    
    class JwtAuthenticationFilter {
        -JwtService jwtService
        -UserDetailsService userDetailsService
        +doFilterInternal(request, response, chain)
    }
    
    class JwtService {
        -Key key
        -long expirationMs
        +generateToken(userId, username, ...) String
        +isTokenValid(token) boolean
        +getUsername(token) String
        +parseToken(token) Claims
    }
    
    class UserDetailsServiceImpl {
        -UserRepository userRepository
        +loadUserByUsername(username) UserDetails
    }
    
    class AuthController {
        -AuthenticationManager authManager
        -UserService userService
        -JwtService jwtService
        +login(request) AuthResponse
        +register(request) AuthResponse
    }
    
    class User {
        -Long id
        -String username
        -String password
        -String email
        -Role role
        -Boolean isActive
    }
    
    SecurityConfig --> JwtAuthenticationFilter
    SecurityConfig --> JwtService
    JwtAuthenticationFilter --> JwtService
    JwtAuthenticationFilter --> UserDetailsServiceImpl
    UserDetailsServiceImpl --> User
    AuthController --> JwtService
    AuthController --> UserDetailsServiceImpl
```

---

## 6. Sequence Diagram: Tariff Calculation Flow

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant TariffController
    participant TariffCalculatorService
    participant TariffRateService
    participant TariffRateRepository
    participant WebScrapingService
    participant Database
    
    User->>Frontend: Fill form & click Calculate
    Frontend->>TariffController: POST /api/tariffs/calculate
    activate TariffController
    
    TariffController->>TariffCalculatorService: calculate(request)
    activate TariffCalculatorService
    
    TariffCalculatorService->>TariffRateService: calculateTariffAmount()
    activate TariffRateService
    
    TariffRateService->>TariffRateRepository: findByHsCodeAndCountries()
    activate TariffRateRepository
    TariffRateRepository-->>TariffRateService: Optional<TariffRate>
    deactivate TariffRateRepository
    
    alt Rate Not Found in DB
        TariffRateService->>WebScrapingService: scrape(country pair)
        activate WebScrapingService
        WebScrapingService->>Database: Save scraped data
        WebScrapingService-->>TariffRateService: Scraped rates
        deactivate WebScrapingService
    end
    
    TariffRateService-->>TariffCalculatorService: tariffAmount
    deactivate TariffRateService
    
    TariffCalculatorService->>TariffCalculatorService: Calculate VAT
    TariffCalculatorService->>TariffCalculatorService: Calculate Shipping
    TariffCalculatorService->>TariffCalculatorService: Sum Total Cost
    
    TariffCalculatorService-->>TariffController: TariffCalculationResultDTO
    deactivate TariffCalculatorService
    
    TariffController-->>Frontend: JSON Response
    deactivate TariffController
    
    Frontend->>User: Display Results
```

---

## 7. Sequence Diagram: Exchange Rate Analysis Flow

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant ExchangeRateController
    participant ExchangeRateService
    participant OpenExchangeRatesAPI
    participant ExchangeRateRepository
    participant Database
    
    User->>Frontend: Click "Analyze Exchange Rates"
    Frontend->>ExchangeRateController: GET /api/exchange-rates/analyze
    activate ExchangeRateController
    
    ExchangeRateController->>ExchangeRateService: analyzeExchangeRates(request)
    activate ExchangeRateService
    
    ExchangeRateService->>ExchangeRateService: Resolve country codes
    ExchangeRateService->>ExchangeRateService: Get currency codes
    
    ExchangeRateService->>OpenExchangeRatesAPI: Fetch live rates
    activate OpenExchangeRatesAPI
    
    alt API Success
        OpenExchangeRatesAPI-->>ExchangeRateService: Current rates
        ExchangeRateService->>Database: Store rates
    else API Failure
        OpenExchangeRatesAPI-->>ExchangeRateService: Error
        ExchangeRateService->>ExchangeRateService: Use fallback mode
    end
    deactivate OpenExchangeRatesAPI
    
    ExchangeRateService->>ExchangeRateRepository: Get historical rates (6 months)
    activate ExchangeRateRepository
    ExchangeRateRepository->>Database: Query past 6 months
    Database-->>ExchangeRateRepository: Historical data
    ExchangeRateRepository-->>ExchangeRateService: List<ExchangeRate>
    deactivate ExchangeRateRepository
    
    ExchangeRateService->>ExchangeRateService: Perform trend analysis
    Note over ExchangeRateService: Calculate min, max, average<br/>Determine trend direction<br/>Generate recommendation
    
    ExchangeRateService-->>ExchangeRateController: ExchangeRateAnalysisResponse
    deactivate ExchangeRateService
    
    ExchangeRateController-->>Frontend: JSON Response
    deactivate ExchangeRateController
    
    Frontend->>User: Display trends & recommendation
```

---

## 8. Sequence Diagram: Predictive Analysis Flow

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant PredictiveController
    participant PredictiveService
    participant NewsAPIService
    participant SentimentService
    participant ExchangeRateService
    participant NewsAPI
    participant Database
    
    User->>Frontend: Click "Get Prediction"
    Frontend->>PredictiveController: GET /predict?enableNewsAnalysis=true
    activate PredictiveController
    
    PredictiveController->>PredictiveService: analyzePrediction(request)
    activate PredictiveService
    
    alt Enable News Analysis = true
        PredictiveService->>NewsAPIService: fetchTradeNews(7 days)
        activate NewsAPIService
        
        NewsAPIService->>NewsAPI: GET /everything?q=trade...
        activate NewsAPI
        NewsAPI-->>NewsAPIService: JSON (articles)
        deactivate NewsAPI
        
        NewsAPIService->>NewsAPIService: Parse articles
        NewsAPIService->>Database: Check duplicates
        NewsAPIService-->>PredictiveService: List<NewsArticle>
        deactivate NewsAPIService
        
        PredictiveService->>SentimentService: processArticleSentiments(articles)
        activate SentimentService
        
        loop For each article
            SentimentService->>SentimentService: Analyze sentiment (keyword matching)
        end
        
        SentimentService->>Database: Save articles with scores
        SentimentService->>SentimentService: calculateWeeklySentiment()
        SentimentService->>Database: Save weekly aggregate
        SentimentService-->>PredictiveService: Done
        deactivate SentimentService
    end
    
    PredictiveService->>Database: Get latest sentiment
    Database-->>PredictiveService: SentimentAnalysis
    
    PredictiveService->>ExchangeRateService: Get current rate
    ExchangeRateService->>Database: Query latest rate
    Database-->>ExchangeRateService: ExchangeRate
    ExchangeRateService-->>PredictiveService: Rate data
    
    PredictiveService->>PredictiveService: Generate prediction
    Note over PredictiveService: Multi-factor scoring:<br/>• Sentiment (40%)<br/>• Trend (30%)<br/>• Volatility (20%)<br/>• Consistency (10%)
    
    PredictiveService->>PredictiveService: Determine BUY/HOLD/WAIT
    PredictiveService->>Database: Get supporting headlines
    Database-->>PredictiveService: Recent articles
    
    PredictiveService-->>PredictiveController: PredictiveAnalysisResponse
    deactivate PredictiveService
    
    PredictiveController-->>Frontend: JSON Response
    deactivate PredictiveController
    
    Frontend->>User: Display recommendation
```

---

## 9. Authentication Sequence Diagram

```mermaid
sequenceDiagram
    actor User
    participant Frontend
    participant AuthController
    participant AuthenticationManager
    participant UserDetailsService
    participant UserRepository
    participant JwtService
    participant Database
    
    User->>Frontend: Enter credentials
    Frontend->>AuthController: POST /auth/login
    activate AuthController
    
    AuthController->>AuthenticationManager: authenticate(credentials)
    activate AuthenticationManager
    
    AuthenticationManager->>UserDetailsService: loadUserByUsername(username)
    activate UserDetailsService
    
    UserDetailsService->>UserRepository: findByUsername(username)
    activate UserRepository
    UserRepository->>Database: SELECT * FROM user WHERE username=?
    Database-->>UserRepository: User record
    UserRepository-->>UserDetailsService: Optional<User>
    deactivate UserRepository
    
    UserDetailsService->>UserDetailsService: Build UserDetails
    UserDetailsService->>UserDetailsService: Check isActive
    UserDetailsService-->>AuthenticationManager: UserDetails
    deactivate UserDetailsService
    
    AuthenticationManager->>AuthenticationManager: Verify password (BCrypt)
    
    alt Password Valid
        AuthenticationManager-->>AuthController: Authentication Success
        deactivate AuthenticationManager
        
        AuthController->>JwtService: generateToken(userId, username, email, role)
        activate JwtService
        JwtService->>JwtService: Create JWT with claims
        JwtService->>JwtService: Sign with secret key
        JwtService-->>AuthController: JWT Token
        deactivate JwtService
        
        AuthController-->>Frontend: AuthResponse (token, userInfo)
        Frontend->>Frontend: Store token in localStorage
        Frontend->>User: Login successful
    else Password Invalid
        AuthenticationManager-->>AuthController: Authentication Failed
        AuthController-->>Frontend: 401 Unauthorized
        Frontend->>User: Show error
    end
    
    deactivate AuthController
```

---

## 10. Data Model ER Diagram

```mermaid
erDiagram
    USER ||--o{ SEARCH_HISTORY : creates
    USER {
        Long id PK
        String username UK
        String email UK
        String password
        String role
        Boolean is_active
    }
    
    COUNTRY ||--o{ TARIFF_RATE : "has rates for"
    COUNTRY ||--|| COUNTRY_PROFILE : "has profile"
    COUNTRY {
        String country_code PK
        String country_name
        String iso3_code
    }
    
    PRODUCT ||--o{ TARIFF_RATE : "has rates"
    PRODUCT {
        String hs_code PK
        String description
        String category
    }
    
    TARIFF_RATE {
        Long id PK
        String hs_code FK
        String importing_country_code FK
        String exporting_country_code FK
        BigDecimal ad_valorem_rate
        String duty_type
    }
    
    COUNTRY_PROFILE {
        Long id PK
        String country_code FK
        BigDecimal vat_rate
    }
    
    SHIPPING_RATE {
        Long id PK
        String importing_country FK
        String exporting_country FK
        BigDecimal air_rate
        BigDecimal sea_rate
        BigDecimal land_rate
    }
    
    EXCHANGE_RATE {
        Long id PK
        String from_currency
        String to_currency
        BigDecimal rate
        Date rate_date
    }
    
    NEWS_ARTICLE {
        Long id PK
        String title
        String description
        String url UK
        String source
        DateTime published_at
        Double sentiment_score
        String keywords
    }
    
    SENTIMENT_ANALYSIS {
        Long id PK
        Date week_start_date
        Date week_end_date
        Double average_sentiment
        Integer article_count
        String trend
    }
    
    SEARCH_HISTORY {
        Long id PK
        Long user_id FK
        String search_query
        DateTime created_at
    }
```

---

## 11. Service Dependency Diagram

```mermaid
graph TB
    subgraph Controllers
        TC[TariffController]
        EC[ExchangeRateController]
        PC[PredictiveAnalysisController]
        AC[AuthController]
    end
    
    subgraph "Service Layer"
        TCS[TariffCalculatorService]
        TRS[TariffRateService]
        SCS[ShippingCostService]
        TVS[TariffValidationService]
        
        ERS[ExchangeRateService]
        CCS[CurrencyCodeService]
        
        PAS[PredictiveAnalysisService]
        NAS[NewsAPIService]
        SAS[SentimentAnalysisService]
        
        US[UserService]
        CS[CountryService]
        PS[ProductService]
    end
    
    subgraph "Repository Layer"
        TRR[TariffRateRepo]
        ERR[ExchangeRateRepo]
        NAR[NewsArticleRepo]
        SAR[SentimentAnalysisRepo]
        UR[UserRepo]
    end
    
    TC --> TCS
    TCS --> TRS
    TCS --> SCS
    TCS --> TVS
    TRS --> TRR
    
    EC --> ERS
    ERS --> CCS
    ERS --> ERR
    
    PC --> PAS
    PAS --> NAS
    PAS --> SAS
    PAS --> ERS
    NAS --> NAR
    SAS --> NAR
    SAS --> SAR
    
    AC --> US
    US --> UR
```

---

## 12. Deployment Diagram

```mermaid
graph TB
    subgraph "Client Device"
        Browser[Web Browser]
    end
    
    subgraph "Frontend Container"
        React[React Application<br/>Vite Dev Server<br/>Port 5173]
    end
    
    subgraph "Backend Container"
        SpringBoot[Spring Boot Application<br/>Embedded Tomcat<br/>Port 8080]
    end
    
    subgraph "Microservice Container"
        Python[Python Scraper<br/>Flask<br/>Port 5001]
    end
    
    subgraph "Database"
        Postgres[(PostgreSQL<br/>Supabase Cloud<br/>Port 6543)]
    end
    
    subgraph "External APIs"
        OER[OpenExchangeRates<br/>HTTPS]
        NA[News API<br/>HTTPS]
        OAI[OpenAI<br/>HTTPS]
    end
    
    Browser -->|HTTP| React
    React -->|REST API<br/>JSON| SpringBoot
    SpringBoot -->|HTTP| Python
    SpringBoot -->|JDBC/SSL| Postgres
    SpringBoot -->|HTTPS| OER
    SpringBoot -->|HTTPS| NA
    SpringBoot -->|HTTPS| OAI
```

---

## 13. Package Structure Diagram

```mermaid
graph LR
    subgraph com.cs203.tariffg4t2
        Controller[controller/]
        Service[service/]
        Repository[repository/]
        Model[model/]
        DTO[dto/]
        Security[security/]
        Config[config/]
    end
    
    Controller --> Service
    Service --> Repository
    Repository --> Model
    Controller --> DTO
    Service --> DTO
    Controller --> Security
    Service --> Model
    
    subgraph "Service Packages"
        Basic[basic/<br/>CRUD services]
        TariffLogic[tariffLogic/<br/>Calculations]
        Data[data/<br/>External APIs]
    end
    
    Service --> Basic
    Service --> TariffLogic
    Service --> Data
```

---

## 14. Class Diagram: Core Domain Models

```mermaid
classDiagram
    class User {
        +Long id
        +String username
        +String email
        +String password
        +Role role
        +Boolean isActive
        +LocalDateTime createdAt
    }
    
    class TariffRate {
        +Long id
        +String hsCode
        +String importingCountryCode
        +String exportingCountryCode
        +BigDecimal adValoremRate
        +DutyType dutyType
        +LocalDateTime updatedAt
    }
    
    class ExchangeRate {
        +Long id
        +String fromCurrency
        +String toCurrency
        +BigDecimal rate
        +LocalDate rateDate
        +LocalDateTime createdAt
    }
    
    class NewsArticle {
        +Long id
        +String title
        +String description
        +String url
        +String source
        +LocalDateTime publishedAt
        +Double sentimentScore
        +String keywords
    }
    
    class SentimentAnalysis {
        +Long id
        +LocalDate weekStartDate
        +LocalDate weekEndDate
        +Double averageSentiment
        +Integer articleCount
        +Integer positiveCount
        +Integer negativeCount
        +Integer neutralCount
        +String trend
    }
    
    class Country {
        +String countryCode
        +String countryName
        +String iso3Code
    }
    
    class Product {
        +String hsCode
        +String description
        +String category
    }
    
    class ShippingRate {
        +Long id
        +String importingCountry
        +String exportingCountry
        +BigDecimal airRate
        +BigDecimal seaRate
        +BigDecimal landRate
    }
    
    Country "1" -- "*" TariffRate : imports/exports
    Product "1" -- "*" TariffRate : has
    User "1" -- "*" SearchHistory : creates
```

---

## Notes

### Diagram Tools

**To view/edit these diagrams:**
1. **Online**: Copy to https://mermaid.live/
2. **VS Code**: Install "Mermaid Preview" extension
3. **Export**: Use mermaid-cli or online tools to generate PNG/SVG

### Customization

You can modify these diagrams to:
- Add more details
- Show specific flows
- Highlight your contributions
- Focus on particular features

### Documentation Best Practices

**For your report, include:**
1. Component diagram (shows overall system)
2. 1-2 sequence diagrams (shows key flows)
3. Class diagram (shows main entities)
4. Brief text explanation of each

This makes your architecture clear and professional! 🎯

