from scraper import run_tariff_scraper
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route("/")
def home():
    return jsonify({"message": "Tariff Scraper Microservice is running"}), 200

@app.route("/scrape", methods=["GET"])
def scrape():
    # Get target_url from query parameter sent by Spring Boot
    target_url = request.args.get('target_url')

    if not target_url:
        return jsonify({"error": "target_url parameter is required"}), 400

    try:
        results = run_tariff_scraper(target_url)

        # Convert to the format expected by Spring Boot (TariffDataDTO)
        formatted_results = []
        for item in results:
            formatted_item = {
                "type": item.get("Type", ""),
                "year": item.get("Year", ""),
                "importedFrom": item.get("Imported from(country)", ""),
                "exportedFrom": item.get("Exported from(country)", ""),
                "tariffRate": item.get("Tariff rate", "")
            }
            formatted_results.append(formatted_item)

        return jsonify(formatted_results), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(port=5001, debug=True)
