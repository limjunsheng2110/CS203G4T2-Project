# ========================================
# 🔍 EC2 Deployment Verification Script
# ========================================
# Run this script to check your microservices deployment

Write-Host "🔍 Checking Tariff Microservices Deployment Status..." -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan

# Test all your endpoints
$endpoints = @(
    @{Name="Health Check"; Url="http://52.65.58.208/health"}
    @{Name="Backend API"; Url="http://52.65.58.208/api/countries"}
    @{Name="Authentication"; Url="http://52.65.58.208/auth/login"}
    @{Name="Swagger UI"; Url="http://52.65.58.208/swagger-ui/"}
    @{Name="Python Scraper"; Url="http://52.65.58.208/scrape/"}
)

foreach ($endpoint in $endpoints) {
    Write-Host "`n🧪 Testing: $($endpoint.Name)" -ForegroundColor Yellow
    try {
        $response = Invoke-WebRequest -Uri $endpoint.Url -Method GET -TimeoutSec 10 -ErrorAction Stop
        Write-Host "✅ Status: $($response.StatusCode) - $($endpoint.Name) is accessible" -ForegroundColor Green
    }
    catch {
        if ($_.Exception.Response.StatusCode -eq 404) {
            Write-Host "⚠️  Status: 404 - Service running but endpoint not found (this may be normal)" -ForegroundColor Yellow
        }
        elseif ($_.Exception.Response.StatusCode -eq 403) {
            Write-Host "🔒 Status: 403 - Service running but requires authentication" -ForegroundColor Yellow
        }
        else {
            Write-Host "❌ Status: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

Write-Host "`n🌐 Your microservices should be available at:" -ForegroundColor Cyan
Write-Host "• Backend API: http://52.65.58.208/api/" -ForegroundColor White
Write-Host "• Authentication: http://52.65.58.208/auth/" -ForegroundColor White
Write-Host "• Python Scraper: http://52.65.58.208/scrape/" -ForegroundColor White
Write-Host "• Swagger UI: http://52.65.58.208/swagger-ui/" -ForegroundColor White
Write-Host "• Health Check: http://52.65.58.208/health" -ForegroundColor White

Write-Host "`n💡 Note: Some endpoints may return 403/404 which is normal for protected/specific routes" -ForegroundColor Gray
