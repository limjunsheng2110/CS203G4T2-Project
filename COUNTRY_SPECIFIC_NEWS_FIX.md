# Country-Specific News Filtering Fix

## Problem
The sentiment analysis was showing neutral scores (0.00, 0.02) because:
1. News API was fetching generic trade articles not specific to the country pair
2. The query used `OR` logic, returning articles mentioning EITHER country (e.g., articles only about USA or only about China)
3. No post-filtering to verify articles actually discussed the trade relationship between the two countries

**Result:** Sentiment was calculated from irrelevant news, leading to neutral/meaningless scores.

## Solution - Two-Layer Filtering

### Layer 1: Strict News API Query (NewsAPIService.java)

**Changed Query Logic:**
```java
// OLD (Too broad - OR logic):
query = "(USA OR China) AND (trade OR tariff OR import OR export)"

// NEW (Strict - AND logic):
query = "(USA AND China) AND (trade OR tariff OR import OR export OR \"trade war\" OR \"trade deal\" OR \"trade relations\")"
```

**What This Does:**
- **Requires BOTH countries** to be mentioned in the article
- Only fetches articles about the actual trade relationship
- Added more specific trade terms: "trade war", "trade deal", "trade relations"
- Added `language=en` parameter for English articles only

### Layer 2: Backend Post-Filtering (PredictiveAnalysisService.java)

**Added `filterArticlesByCountries()` method:**
```java
private List<NewsArticle> filterArticlesByCountries(List<NewsArticle> articles, String country1, String country2) {
    return articles.stream()
        .filter(article -> {
            String content = (title + " " + description).toLowerCase();
            // Check if BOTH countries are mentioned
            boolean mentionsCountry1 = content.contains(country1.toLowerCase());
            boolean mentionsCountry2 = content.contains(country2.toLowerCase());
            return mentionsCountry1 && mentionsCountry2;
        })
        .collect(Collectors.toList());
}
```

**What This Does:**
- Double-checks that articles mention BOTH countries
- Searches in both title AND description
- Case-insensitive matching
- Logs which articles pass the filter for debugging

### Updated Analysis Flow

**Before:**
1. Fetch news (generic trade articles)
2. Analyze sentiment (on irrelevant articles)
3. Get neutral scores

**After:**
1. Fetch news with **STRICT query** (country1 AND country2)
2. **Filter articles** to verify both countries mentioned
3. Throw error if no relevant articles found
4. Analyze sentiment **ONLY on relevant articles**
5. Get meaningful sentiment scores

## Example Queries

### For US-China Analysis:
```
News API Query: (USA AND China) AND (trade OR tariff OR import OR export OR "trade war" OR "trade deal" OR "trade relations")
```

### For UK-EU Analysis:
```
News API Query: (UK AND EU) AND (trade OR tariff OR import OR export OR "trade war" OR "trade deal" OR "trade relations")
```

## Logging Added

The system now logs:
- Number of articles fetched from API
- Number of articles after filtering
- Which articles mention both countries (debug level)
- Warning if no relevant articles found

**Example Log Output:**
```
Fetching STRICT country-pair trade news for: USA and China
Fetched 50 articles from News API
Filtered 50 articles down to 12 country-relevant articles
Successfully fetched and analyzed 12 relevant news articles for USA and China
```

## Error Handling

If NO articles mention both countries:
```
Error: "No relevant articles found for the selected country pair"
Message: "No articles found mentioning both USA and China. Fetched 50 articles but none were relevant."
Falls back to database sentiment data
```

## Expected Improvements

### Before Fix:
- **Sentiment:** 0.00 to 0.02 (neutral - meaningless)
- **Articles:** Mix of random trade news
- **Headlines:** Unrelated to country pair

### After Fix:
- **Sentiment:** Actual meaningful scores based on country-specific news
- **Articles:** Only articles discussing US-China trade relations
- **Headlines:** Directly relevant to the selected countries

## Example Relevant Headlines (US-China):
✅ "US-China trade tensions escalate over new tariffs"
✅ "China responds to US export restrictions"
✅ "Trade deal negotiations between USA and China stall"

## Example Filtered Out:
❌ "India increases tariffs on steel imports" (only mentions India)
❌ "EU trade policy changes" (doesn't mention either country)
❌ "Global supply chain disruptions" (too generic)

## Testing

To test the fix:

1. **Clear existing data** (optional - to see fresh results)
2. **Select country pair** (e.g., USA and China)
3. **Enable News Analysis**
4. **Click "Get Prediction"**

**What to Look For:**
- Message shows number of relevant articles analyzed
- Sentiment scores should be more varied (not always 0.00)
- Supporting headlines should mention both countries
- More negative sentiment if there are trade tensions
- More positive sentiment if there are trade deals

## Technical Notes

### Why Two Layers?
1. **News API filtering** - Reduces API data transfer and cost
2. **Backend filtering** - Guarantees accuracy even if API returns unexpected results
3. **Fail-safe approach** - Ensures quality even with API limitations

### Case-Insensitive Matching
The filter handles:
- "USA", "usa", "United States"
- "China", "china", "CHINA"
- Works with any capitalization

### Performance
- Filtering is done in-memory (fast)
- Only relevant articles are saved to database
- Reduces storage of irrelevant news

## Files Modified

1. **NewsAPIService.java**
   - Changed query to use AND instead of OR
   - Added more specific trade keywords
   - Added language filter

2. **PredictiveAnalysisService.java**
   - Added `filterArticlesByCountries()` method
   - Updated analysis flow to filter before sentiment analysis
   - Added logging for article counts
   - Enhanced error messages

## Configuration

No configuration changes needed. The system automatically:
- Uses strict filtering when countries are provided
- Falls back to general query when countries are null
- Maintains backward compatibility

## Monitoring

Check the backend logs for:
```
Fetching STRICT country-pair trade news for: [country1] and [country2]
Filtered X articles down to Y country-relevant articles
```

If Y is consistently 0, check:
1. Country names are spelled correctly
2. News API has recent articles about that country pair
3. The countries have active trade news

