import requests
import os
import sys
from typing import List, Dict
from bs4 import BeautifulSoup
from urllib.parse import quote_plus # Used for safely encoding URL parts

# The Flask and request/response handling logic
from flask import Flask, request, jsonify, render_template

# --- OpenAI Dependency Setup ---
# NOTE: Ensure you have 'openai', 'requests', 'beautifulsoup4', and 'Flask' installed
try:
    from openai import OpenAI
    # Load OpenAI key from environment
    openai_api_key = os.environ.get("OPENAI_API_KEY")
    if not openai_api_key:
        print("Warning: OPENAI_API_KEY environment variable not set. OpenAI functions will fail.")
    client = OpenAI(api_key=openai_api_key)
except ImportError:
    print("Error: Required libraries are missing. Run: pip install openai flask requests beautifulsoup4")
    sys.exit(1)


# ==================================
# 📚 SCRAPING FUNCTIONS (Chapter 1 Focus)
# ==================================

def extract_tariff_data_from_url(url: str) -> List[Dict[str, str]]:
    """
    Given a single URL, fetch the page and extract tariff data using GPT.
    Only extracts Chapter 1 tariff data (HS codes starting with 01).
    """
    print(f"-> Extracting Chapter 1 (HS 01) data from: {url}")

    try:
        page_response = requests.get(url, timeout=15)
        page_response.raise_for_status() # Raise HTTPError for bad responses (4xx or 5xx)
    except requests.RequestException as e:
        raise Exception(f"Failed to fetch data from URL: {url} - {e}")

    page_content = page_response.text

    # --- GPT PROMPT REMAINS THE SAME ---
    extraction_prompt = f"""
You are an expert in analyzing international tariff databases.

From the following webpage content, extract tariff information **ONLY for Chapter 1 products (HS codes starting with 01)**.
Chapter 1 covers: Live animals (horses, cattle, sheep, goats, swine, poultry, etc.)

Extract information **only if both conditions are met**:
1. HS code starts with "01" (Chapter 1 - Live Animals)
2. Actual numeric tariff values or percentages are present

Use this exact format per entry (one block per product):

Exporting Country: (country that exports/sells the product)
Importing Country: (country that imports/buys the product and applies the tariff)
Product Name: (specific live animal product name)
HS Code: (must start with "01", e.g., "0101", "0102", "010121")
Tariff Rate: (must be numeric, e.g., '0.00%', '7.5%', '15.2%')

IMPORTANT:
- Only include entries where HS code starts with "01"
- Skip all entries that don't have Chapter 1 HS codes
- Only include entries with valid numeric tariff rates
- Exporting Country is the source/seller, Importing Country is the destination/buyer

Webpage URL: {url}
Webpage content:
{page_content}
"""
    # ------------------------------------

    if not openai_api_key:
         raise Exception("OpenAI API Key is missing. Cannot perform data extraction.")

    try:
        extraction_response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": extraction_prompt}],
            temperature=0
        )
    except Exception as e:
        raise Exception(f"OpenAI API call failed: {e}")

    extracted_text = extraction_response.choices[0].message.content

    # Parse extracted text into structured entries
    entries = []
    blocks = extracted_text.strip().split("\n\n")
    for block in blocks:
        if not block.strip():
            continue

        entry = {}
        for field in block.strip().split("\n"):
            if ":" in field:
                key, value = field.split(":", 1)
                entry[key.strip()] = value.strip()

        if (entry and
            "HS Code" in entry and
            "Tariff Rate" in entry and
            entry["HS Code"].replace(".", "").replace(" ", "").startswith("01")):
            entries.append(entry)

    print(f"-> Found {len(entries)} Chapter 1 tariff entries")
    return entries


def run_tariff_scraper(target_url: str) -> List[Dict[str, str]]:
    """
    Main function to run the single-page pipeline.
    It takes the target_url and directly extracts the data from it.
    """
    data = extract_tariff_data_from_url(target_url)
    return data


# ==================================
# 🌐 FLASK APPLICATION (With Form Input)
# ==================================

app = Flask(__name__)

# --- NEW ROUTE for the input form ---
@app.route("/", methods=["GET"])
def home():
    """Renders the country code input form."""
    return render_template("index.html")
# ------------------------------------

@app.route("/scrape", methods=["GET", "POST"]) # Enable POST to receive form data
def scrape():
    # Base URL template
    BASE_URL_TEMPLATE = "https://wits.worldbank.org/tariff/trains/en/country/{import_code}/partner/{export_code}/product/all"

    # 1. Handle incoming parameters (from form POST or direct GET query)
    import_code = request.form.get('import_code') if request.method == 'POST' else request.args.get('import_code')
    export_code = request.form.get('export_code') if request.method == 'POST' else request.args.get('export_code')

    if not import_code or not export_code:
        # If accessing directly via GET, fall back to target_url parameter (legacy support)
        target_url = request.args.get('target_url')
        if not target_url:
            return jsonify({
                "error": "Missing required parameters.",
                "message": "Please provide 'import_code' and 'export_code' via POST or 'target_url' via GET."
            }), 400
    else:
        # 2. Build the target URL from country codes
        import_code = quote_plus(import_code.upper())
        export_code = quote_plus(export_code.upper())

        target_url = BASE_URL_TEMPLATE.format(import_code=import_code, export_code=export_code)

    print(f"\n--- New Request: Scraping URL: {target_url} ---")

    try:
        # 3. Run the scraper
        results = run_tariff_scraper(target_url)
        print(f"Total Chapter 1 entries scraped: {len(results)}")

        # 4. Format the results
        formatted_results = []
        for item in results:
            formatted_item = {
                "exportingCountry": item.get("Exporting Country", ""),
                "importingCountry": item.get("Importing Country", ""),
                "productName": item.get("Product Name", ""),
                "hsCode": item.get("HS Code", ""),
                "tariffRate": item.get("Tariff Rate", "")
            }
            formatted_results.append(formatted_item)

        print("--- Chapter 1 Scraping Request Complete ---")
        return jsonify({
            "status": "success",
            "source_url": target_url,
            "chapter": "Chapter 1 - Live Animals",
            "results_count": len(formatted_results),
            "data": formatted_results
        }), 200

    except Exception as e:
        print(f"Critical error in /scrape endpoint: {e}")
        return jsonify({"error": f"Chapter 1 scraping failed for URL {target_url}: {str(e)}"}), 500

if __name__ == "__main__":
    if not os.environ.get("OPENAI_API_KEY"):
        print("\n--- WARNING: OPENAI_API_KEY is not set. AI-based scraping will fail! ---")

    print(f"Starting Flask app on port 5001...")
    app.run(host='0.0.0.0', port=5001, debug=True)