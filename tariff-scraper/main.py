# main.py

import os
from flask import Flask
from flask import request
from flask import jsonify
# Remove template rendering for now to fix crashes
# from flask import render_template
# Import the function from the new module
try:
    from rates import get_tariff_rates
except ImportError:
    # Fallback if rates module isn't available
    def get_tariff_rates(import_code, export_code):
        return {
            "status": "error",
            "message": "Scraping module not available",
            "import_code": import_code,
            "export_code": export_code
        }

# ==================================
# 🌐 FLASK APPLICATION
# ==================================

app = Flask(__name__)

# --- Health Check Route (for Docker health checks) ---
@app.route("/health", methods=["GET"])
def health_check():
    """Health check endpoint for Docker and load balancer monitoring."""
    return jsonify({
        "status": "healthy",
        "service": "tariff-scraper",
        "version": "1.0"
    }), 200

# --- Home Route: Simple JSON response instead of template ---
@app.route("/", methods=["GET"])
def home():
    """Returns API information instead of rendering template."""
    return jsonify({
        "service": "Tariff Scraper API",
        "endpoints": {
            "health": "/health",
            "scrape": "/scrape (POST)"
        },
        "status": "running"
    }), 200

# --- Scrape Route: Handles form submission and calls the scraper ---
@app.route("/scrape", methods=["POST"])
def scrape():
    # 1. Handle incoming parameters from the form POST request
    import_code = request.form.get('import_code') or request.json.get('import_code') if request.json else None
    export_code = request.form.get('export_code') or request.json.get('export_code') if request.json else None

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
            "message": f"Scraping failed: {str(e)}"
        }), 500

# ==================================
# 🚀 RUN THE APPLICATION
# ==================================

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    app.run(host="0.0.0.0", port=port, debug=False)
