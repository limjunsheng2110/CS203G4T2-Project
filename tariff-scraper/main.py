# main.py

import os
from flask import Flask
from flask import request
from flask import jsonify
from flask import render_template
# Import the function from the new module
from rates import get_tariff_rates

# ==================================
# 🌐 FLASK APPLICATION
# ==================================

app = Flask(__name__)

# --- Home Route: Renders the input form (index.html) ---
@app.route("/", methods=["GET"])
def home():
    """Renders the country code input form."""
    # NOTE: You will need to create a 'templates/index.html' file
    return render_template("index.html")

# --- Scrape Route: Handles form submission and calls the scraper ---
@app.route("/scrape", methods=["POST"])
def scrape():
    # 1. Handle incoming parameters from the form POST request
    import_code = request.form.get('import_code')
    export_code = request.form.get('export_code')

    if not import_code or not export_code:
        return jsonify({
            "status": "error",
            "message": "Missing required parameters. Please provide 'import_code' and 'export_code'."
        }), 400

    print(f"\n--- New Request: Importing Country: {import_code}, Exporting Country: {export_code} ---")

    try:
        # 2. Call the dedicated scraper function from the imported module
        results_data = get_tariff_rates(import_code, export_code)

        print("--- Chapter 1 Scraping Request Complete ---")
        return jsonify({
            "status": "success",
            **results_data # Unpack the dictionary returned by get_tariff_rates
        }), 200

    except Exception as e:
        print(f"Critical error in /scrape endpoint: {e}")
        return jsonify({
            "status": "error",
            "message": f"Chapter 1 scraping failed for {import_code} vs {export_code}: {str(e)}"
        }), 500

# --- Health Route: Check if the service is running ---
@app.route("/health", methods=["GET"])
def health():
    """Health check endpoint for monitoring."""
    return jsonify({
        "status": "healthy",
        "service": "Chapter 1 Tariff Scraper",
        "timestamp": __import__('time').time()
    }), 200

if __name__ == "__main__":
    if not os.environ.get("OPENAI_API_KEY"):
        print("\n--- WARNING: OPENAI_API_KEY is not set. AI-based scraping will fail! ---")

    print(f"Starting Flask app on port 5001...")
    # Setting debug=True is useful during development
    app.run(host='0.0.0.0', port=5001, debug=True)
