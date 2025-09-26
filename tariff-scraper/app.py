from flask import Flask, jsonify
from scraper import run_tariff_scraper

app = Flask(__name__)

@app.route("/")
def home():
    return jsonify({"message": "Tariff Scraper Microservice is running"}), 200

# Your scraping endpoint
@app.route("/scrape", methods=["GET"])
def scrape():
    target_url = "https://wits.worldbank.org/CountryProfile/en/Country/WLD/Year/2022/TradeFlow/EXPIMP/Partner/All/Product/01-05_Animal"
    results = run_tariff_scraper(target_url)
    return jsonify(results), 200

if __name__ == "__main__":
    app.run(port=5001)