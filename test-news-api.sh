#!/bin/bash

echo "================================================"
echo "News API Connection Test"
echo "================================================"
echo ""

# Load environment variables
source set-env.sh

echo "1. Checking if API key is set..."
if [ -z "$NEWSAPI_API_KEY" ]; then
    echo "❌ NEWSAPI_API_KEY is NOT set!"
    echo "Please add it to set-env.sh"
    exit 1
else
    echo "✅ NEWSAPI_API_KEY is set: ${NEWSAPI_API_KEY:0:10}..."
fi

echo ""
echo "2. Testing News API connection..."
RESPONSE=$(curl -s "https://newsapi.org/v2/everything?q=tariff&language=en&pageSize=5&apiKey=$NEWSAPI_API_KEY")

STATUS=$(echo $RESPONSE | grep -o '"status":"[^"]*"' | cut -d':' -f2 | tr -d '"')

if [ "$STATUS" = "ok" ]; then
    echo "✅ News API connection successful!"
    TOTAL=$(echo $RESPONSE | grep -o '"totalResults":[0-9]*' | cut -d':' -f2)
    echo "   Found $TOTAL articles about 'tariff'"
    echo ""
    echo "   Sample headlines:"
    echo $RESPONSE | grep -o '"title":"[^"]*"' | head -3 | cut -d':' -f2- | tr -d '"' | sed 's/^/   - /'
else
    echo "❌ News API connection failed!"
    echo "   Status: $STATUS"
    echo "   Response: $RESPONSE"
fi

echo ""
echo "3. Testing backend predictive analysis endpoint..."
echo "   (Make sure Spring Boot is running on port 8080)"
echo ""

BACKEND_RESPONSE=$(curl -s "http://localhost:8080/api/predictive-analysis/health")
if [ $? -eq 0 ] && [ ! -z "$BACKEND_RESPONSE" ]; then
    echo "✅ Backend is running!"
    echo "   Response: $BACKEND_RESPONSE"
else
    echo "❌ Backend is not responding"
    echo "   Make sure Spring Boot is running: ./mvnw spring-boot:run"
fi

echo ""
echo "================================================"
echo "Next Steps:"
echo "================================================"
if [ "$STATUS" = "ok" ]; then
    echo "1. Make sure Spring Boot was started AFTER running 'source set-env.sh'"
    echo "2. Call the prediction endpoint with enableNewsAnalysis=true"
    echo "3. Check Spring Boot console logs for 'Fetching trade news' messages"
else
    echo "1. Check your News API key at: https://newsapi.org/account"
    echo "2. Make sure you haven't exceeded rate limits (100/day free tier)"
    echo "3. Update NEWSAPI_API_KEY in set-env.sh if needed"
fi
echo ""

