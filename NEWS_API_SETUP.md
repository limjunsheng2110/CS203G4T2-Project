# News API Setup Guide

## Problem
The message "Live News API unavailable. Using last stored sentiment data from database" means the News API key is not configured.

## Solution - Set Up News API Key

### Step 1: Get a News API Key

1. Go to **https://newsapi.org/**
2. Click **"Get API Key"** (it's FREE for development)
3. Sign up with your email
4. Copy your API key (looks like: `1234567890abcdef1234567890abcdef`)

### Step 2: Configure the API Key

#### **Option A: Using Environment Variable (Recommended)**

**Windows (PowerShell):**
```powershell
$env:NEWSAPI_API_KEY="your_api_key_here"
```

**Windows (Command Prompt):**
```cmd
set NEWSAPI_API_KEY=your_api_key_here
```

**For Permanent Setup (Windows):**
1. Open **System Properties** → **Environment Variables**
2. Add a new **User Variable**:
   - Name: `NEWSAPI_API_KEY`
   - Value: `your_api_key_here`
3. Restart your IDE/terminal

**Mac/Linux:**
```bash
export NEWSAPI_API_KEY="your_api_key_here"
```

Add to `~/.bashrc` or `~/.zshrc` for permanent setup.

#### **Option B: Using env.ps1 File**

Edit your `env.ps1` file and add:
```powershell
$env:NEWSAPI_API_KEY = "your_api_key_here"
```

Then run before starting the backend:
```powershell
.\env.ps1
```

### Step 3: Restart Your Backend

After setting the environment variable, restart your Spring Boot application for the changes to take effect.

### Step 4: Test the Configuration

1. Open the Predictive Analysis page
2. Click the **"Debug Tools"** section
3. Enable **"Run live News API connectivity test"**
4. Click **"Run Diagnostics"**

You should see:
- ✅ **API key detected**
- ✅ **Connectivity test passed**

### Step 5: Try Again

1. Select your country pair (e.g., USA and China)
2. Enable **News Analysis**
3. Click **"Get Prediction"**

Now you should see:
```
Live News Data
News sentiment updated from live News API for USA and China trade (analyzed X relevant articles)
```

## Improved Error Messages

The system now shows specific error messages:

### If API Key is Missing:
```
Live News API unavailable: News API key is not configured. Please set NEWSAPI_API_KEY environment variable.
```

### If No Articles Found:
```
Live News API unavailable: News API returned 0 articles. The query '(USA AND China) AND (trade...)' found no matching news in the past 7 days.
```

### If Articles Don't Mention Both Countries:
```
Live News API unavailable: Fetched 50 articles but none mentioned both USA and China. Try using full country names (e.g., 'United States' instead of 'US').
```

## Common Country Name Variations

Try these if you get "no relevant articles":

### United States:
- "United States"
- "US"
- "USA"
- "America"

### China:
- "China"
- "Chinese"

### United Kingdom:
- "United Kingdom"
- "UK"
- "Britain"
- "British"

### European Union:
- "European Union"
- "EU"

## Troubleshooting

### Issue: "API key not configured"
**Solution:** Set the `NEWSAPI_API_KEY` environment variable and restart the backend.

### Issue: "News API returned 0 articles"
**Possible Causes:**
1. The country pair doesn't have recent trade news in the past 7 days
2. News API is down or rate-limited
3. Country names don't match what's in the news

**Solutions:**
- Try a different, more newsworthy country pair (e.g., USA and China)
- Try different country name variations
- Check https://newsapi.org/ to verify the service is working

### Issue: "Fetched articles but none mentioned both countries"
**Solution:** Use full country names instead of abbreviations:
- ✅ "United States" instead of "US"
- ✅ "China" instead of "CN"

### Issue: Still showing "Fallback Data"
**Check:**
1. Backend was restarted after setting the environment variable
2. Environment variable is correctly set (check with `echo $env:NEWSAPI_API_KEY` on Windows or `echo $NEWSAPI_API_KEY` on Mac/Linux)
3. No typos in the API key
4. API key is valid (test at https://newsapi.org/account)

## News API Limits

**Free Tier:**
- 100 requests per day
- News from past 30 days
- Good enough for development!

**Developer Tier ($449/month):**
- 250,000 requests per month
- More historical data
- Higher reliability

For your project, the free tier is sufficient.

## Testing Different Country Pairs

### High Activity Pairs (Likely to have news):
- ✅ United States + China
- ✅ United States + European Union
- ✅ United Kingdom + European Union
- ✅ United States + Mexico

### Low Activity Pairs (May have no recent news):
- ❌ Singapore + New Zealand
- ❌ Sweden + Poland
- ❌ Chile + Argentina

Choose newsworthy country pairs for testing!

## Verification Checklist

- [ ] Got API key from newsapi.org
- [ ] Set NEWSAPI_API_KEY environment variable
- [ ] Restarted backend
- [ ] Run diagnostics showing "API key detected"
- [ ] Selected newsworthy country pair
- [ ] Enabled news analysis
- [ ] Got "Live News Data" message
- [ ] Seeing non-zero sentiment scores

If all checked, your News API is working correctly! 🎉

