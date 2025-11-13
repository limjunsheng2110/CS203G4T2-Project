# News API Analysis Fix - Country-Specific Headlines

## Problem
The news API analysis had two major issues:
1. **Generic News Fetching**: The API was fetching general trade news without filtering by the specific country pair being analyzed (e.g., US-China)
2. **Unrelated Headlines**: Supporting news headlines displayed were random trade articles, not related to the selected countries

## Solution Implemented

### 1. Country-Specific News Fetching (`NewsAPIService.java`)

**Added new method with country filtering:**
```java
public List<NewsArticle> fetchTradeNews(int daysBack, String country1, String country2)
```

**Key Changes:**
- Modified the News API query to include country names when provided
- Query format: `(USA OR China) AND (trade OR tariff OR import OR export OR customs OR economy)`
- This ensures news articles are specifically about the country pair being analyzed
- Added `language=en` parameter to get English articles only
- Maintains backward compatibility with existing `fetchTradeNews(int daysBack)` method

**Example Query for US-China:**
```
Query: (USA OR China) AND (trade OR tariff OR import OR export OR customs OR economy)
```

### 2. Country-Filtered Supporting Headlines (`PredictiveAnalysisService.java`)

**Updated `getSupportingHeadlines()` method:**
- Now takes country1 and country2 as parameters
- Filters articles by checking if country names appear in title or description
- Case-insensitive matching (e.g., "china", "China", "CHINA" all match)
- Falls back to recent general articles if no country-specific articles found
- Increased default count from 2 to 5 headlines for better context

**Filtering Logic:**
```java
// Filter articles that mention either country in title or description
relevantArticles = articles.stream()
    .filter(article -> {
        String content = (title + " " + description).toLowerCase();
        return content.contains(country1.toLowerCase()) || 
               content.contains(country2.toLowerCase());
    })
```

### 3. Updated Prediction Analysis Flow

**Modified `analyzePrediction()` method:**
- Passes country names to `fetchTradeNews()` for targeted news fetching
- Passes country names to `getSupportingHeadlines()` for headline filtering
- Updated success message to show: `"News sentiment updated from live News API for USA and China trade"`

## Benefits

1. **Relevance**: News articles and headlines are now specifically about the country pair being analyzed
2. **Accuracy**: Sentiment analysis is based on country-relevant news, not generic trade articles
3. **User Experience**: Users see headlines that actually relate to their US-China (or other pair) analysis
4. **Better Insights**: Recommendations are based on news specific to the trading relationship

## Testing

To test the fix:

1. **Start the backend server**
2. **Go to the Predictive Analysis page**
3. **Select countries** (e.g., Importing: USA, Exporting: China)
4. **Enable News Analysis** toggle
5. **Click "Get Prediction"**

You should now see:
- Message showing country-specific news was fetched
- Supporting headlines that mention USA, China, or both
- Headlines about US-China trade relations, tariffs, etc.

## Example Output

**Before Fix:**
- Headlines: Random trade articles about India, EU, etc.
- No relevance to selected country pair

**After Fix:**
- Headlines: "US-China trade tensions escalate..."
- Headlines: "China responds to new US tariffs..."
- Headlines: "Trade deal prospects between USA and China..."

## Notes

- The News API query uses OR logic for countries, so articles mentioning either country will be included
- The AND logic with trade keywords ensures all articles are trade-related
- If no country-specific articles exist in the database, it falls back to recent general trade news
- Logging was added to track how many relevant headlines are found for debugging

## Files Modified

1. `src/main/java/com/cs203/tariffg4t2/service/data/NewsAPIService.java`
   - Added country-specific query building
   - Added overloaded method with country parameters

2. `src/main/java/com/cs203/tariffg4t2/service/basic/PredictiveAnalysisService.java`
   - Updated to pass countries to news fetching
   - Updated headline filtering to check country relevance
   - Improved logging for debugging

## Backward Compatibility

The changes maintain backward compatibility:
- Original `fetchTradeNews(int daysBack)` method still works
- Calls new method with null countries for general news
- Existing API endpoints continue to function

