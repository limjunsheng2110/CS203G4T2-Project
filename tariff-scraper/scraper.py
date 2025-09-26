import requests
from bs4 import BeautifulSoup
import os
from urllib.parse import urljoin
from openai import OpenAI
from typing import List, Dict

# Load OpenAI key from environment
openai_api_key = os.environ.get("OPENAI_API_KEY")
client = OpenAI(api_key=openai_api_key)

def extract_links(target_url: str) -> List[str]:
    """
    Extract relevant links from a WITS page that likely lead to tariff data.
    """
    response = requests.get(target_url)
    soup = BeautifulSoup(response.text, 'html.parser')

    all_links = soup.find_all('a', href=True)
    found_urls = set()

    for tag in all_links:
        href = tag['href']
        if 'tariff' in href.lower() or 'train' in href.lower():
            full_url = urljoin(target_url, href)
            found_urls.add(full_url)

    flat_url_list = list(found_urls)
    url_text = "\n".join(flat_url_list)

    prompt = f"""
You are a tariff researcher. From the list of URLs below, choose ONLY the 20 most informative and relevant links that are likely to contain tariff values for products.
Don't choose links that are general information, home pages, pages that don't contain any values, or unrelated to tariffs, even if they contain the word 'tariff'.

Return ONLY the URLs (no explanation).

Here is the list:
{url_text}
"""

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )

    selected_urls_text = response.choices[0].message.content
    selected_urls = selected_urls_text.strip().split("\n")[:20]

    return selected_urls

def extract_tariff_data_from_url(url: str) -> List[Dict[str, str]]:
    """
    Given a URL, fetch the page and extract tariff data using GPT.
    """
    page_response = requests.get(url)
    page_content = page_response.text

    extraction_prompt = f"""
You are an expert in analyzing international tariff databases.

From the following webpage content, extract tariff information **only if actual numeric tariff values or % are present.**

Use this exact format per entry (one block per product/category):

Type: (e.g. product category such as 'Livestock - beef', 'Electronics - mobile phones')
Year: (extract from content or infer from context or URL)
Imported from(country): (if not found, infer from the URL if possible)
Exported from(country): (if not found, infer from the URL if possible)
Tariff rate: (must be numeric, e.g., '0.00%', '7.5%')

Only output fields if tariff rate is numeric. Skip entries without valid rates.

Webpage URL: {url}
Webpage content:
{page_content}
"""

    extraction_response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": extraction_prompt}],
        temperature=0
    )

    extracted_text = extraction_response.choices[0].message.content

    # Parse extracted text into structured entries
    entries = []
    for line in extracted_text.strip().split("\n\n"):
        entry = {}
        for field in line.split("\n"):
            if ":" in field:
                key, value = field.split(":", 1)
                entry[key.strip()] = value.strip()
        if entry and "Tariff rate" in entry:
            entries.append(entry)

    return entries

def run_tariff_scraper(target_url: str) -> List[Dict[str, str]]:
    """
    Main function to run the full pipeline: extract links → extract data.
    """
    selected_urls = extract_links(target_url)

    all_data = []
    for url in selected_urls:
        print(f"Processing: {url}")
        data = extract_tariff_data_from_url(url)
        all_data.extend(data)

    return all_data
