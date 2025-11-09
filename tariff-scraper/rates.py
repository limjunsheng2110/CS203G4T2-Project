#Rates.py

import requests
import os
import sys
from typing import List, Dict
from urllib.parse import quote_plus
from openai import OpenAI

# --- OpenAI Dependency Setup ---
try:
    # Load OpenAI key from environment
    openai_api_key = os.environ.get("OPENAI_API_KEY")
    if not openai_api_key:
        print("Warning: OPENAI_API_KEY environment variable not set. OpenAI functions will fail.")
    client = OpenAI(api_key=openai_api_key)
except ImportError:
    print("Error: Required 'openai' library is missing. Run: pip install openai")
    sys.exit(1)


# ==================================
# 📚 SCRAPING FUNCTIONS (Chapter 1 Focus)
# ==================================

def extract_tariff_data_from_url(url: str, import_code: str, export_code: str) -> List[Dict[str, str]]:
    """
    Given a single URL, fetch the page and extract tariff data using GPT.
    Only extracts Chapter 1 tariff data (HS codes starting with 01).
    """
    print(f"-> Extracting Chapter 1 (HS 01) data from: {url}")
    print(f"-> Expected: {export_code} exporting TO {import_code} importing")

    if not openai_api_key:
         raise Exception("OpenAI API Key is missing. Cannot perform data extraction.")

    try:
        page_response = requests.get(url, timeout=15)
    if not openai_api_key or client is None:
    except requests.RequestException as e:
        raise Exception(f"Failed to fetch data from URL: {url} - {e}")

    page_content = page_response.text

    # --- GPT Extraction Prompt ---
    extraction_prompt = f"""
You are an expert in analyzing international tariff databases.

From the following webpage content, extract tariff information **ONLY for Chapter 1 products (HS codes starting with 01)**.
Chapter 1 covers: Live animals (horses, cattle, sheep, goats, swine, poultry, etc.)

IMPORTANT: Based on the URL structure, this page shows tariffs that {import_code} (importing country) applies on goods coming FROM {export_code} (exporting country).

Extract information **only if both conditions are met**:
1. HS code starts with "01" (Chapter 1 - Live Animals)
2. Actual numeric tariff values or percentages are present

Use this exact format per entry (one block per product):


Exporting Country: {export_code}
Importing Country: {import_code}
Product Name: (specific live animal product name)
HS Code: (must start with "01", e.g., "0101", "0102", "010121")
Tariff Rate: (must be numeric, e.g., '0.00%', '7.5%', '15.2%')
Date: (extract the effective date, year, or reporting period if available, e.g., "2024", "2023-12-31", "Jan 2024")

CRITICAL INSTRUCTIONS:
- Exporting Country must ALWAYS be: {export_code}
- Importing Country must ALWAYS be: {import_code}
- Only include entries where HS code starts with "01"
- Skip all entries that don't have Chapter 1 HS codes
- Only include entries with valid numeric tariff rates
- Look for any date, year, or time period information associated with the tariff data
- If no specific date is found, use the most recent year mentioned on the page

Webpage URL: {url}
Webpage content:
{page_content}
"""
    # -----------------------------

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

        # Simple validation for Chapter 1 HS code
        if (entry and
            "HS Code" in entry and
            "Tariff Rate" in entry and
            entry["HS Code"].replace(".", "").replace(" ", "").startswith("01")):

            # Force correct country assignments regardless of what GPT extracted
            entry["Exporting Country"] = export_code
            entry["Importing Country"] = import_code
            entries.append(entry)

    print(f"-> Found {len(entries)} Chapter 1 tariff entries")
    return entries

def run_tariff_scraper(target_url: str, import_code: str, export_code: str) -> List[Dict[str, str]]:
    """
    Main function to run the single-page pipeline.
    It takes the target_url and directly extracts the data from it.
    """
    data = extract_tariff_data_from_url(target_url, import_code, export_code)
    return data

def get_tariff_rates(import_code: str, export_code: str) -> Dict:
    """
    Constructs the URL and scrapes the tariff rates.
    """
    BASE_URL_TEMPLATE = "https://wits.worldbank.org/tariff/trains/en/country/{import_code}/partner/{export_code}/product/all"

    import_code_safe = quote_plus(import_code.upper())
    export_code_safe = quote_plus(export_code.upper())

    target_url = BASE_URL_TEMPLATE.format(import_code=import_code_safe, export_code=export_code_safe)

    print(f"-> Scraping tariffs: {export_code} exports TO {import_code} imports")
    results = run_tariff_scraper(target_url, import_code, export_code)

    # Format the results for the API response
    formatted_results = []
    for item in results:
        # Double-check and force correct country assignment
        formatted_item = {
            "exportingCountry": export_code,  # Force correct exporting country
            "importingCountry": import_code,  # Force correct importing country
            "productName": item.get("Product Name", ""),
            "hsCode": item.get("HS Code", ""),
            "tariffRate": item.get("Tariff Rate", ""),
            "date": item.get("Date", "2024")  # Include date with default fallback
        }
        formatted_results.append(formatted_item)

    return {
        "source_url": target_url,
        "chapter": "Chapter 1 - Live Animals",
        "results_count": len(formatted_results),
        "data": formatted_results
    }