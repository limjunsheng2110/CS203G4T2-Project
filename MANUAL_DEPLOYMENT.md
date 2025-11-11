# Manual Deployment Guide for EC2

## SSH into your EC2 instance first:
```bash
ssh -i your-key.pem ec2-user@52.65.58.208
```

## Then run these commands to manually redeploy:

### Step 1: Stop all running containers
```bash
docker stop tariff-nginx tariff-backend tariff-scraper
docker rm tariff-nginx tariff-backend tariff-scraper
```

### Step 2: Remove old images to force fresh pull
```bash
docker rmi kayweesim/tariff-backend:latest
docker rmi kayweesim/tariff-scraper:latest
```

### Step 3: Login to Docker Hub
```bash
docker login
# Enter your Docker Hub username: kayweesim
# Enter your Docker Hub password: [your password]
```

### Step 4: Pull latest images (with --no-cache to bypass Docker's cache)
```bash
docker pull --no-cache kayweesim/tariff-backend:latest
docker pull --no-cache kayweesim/tariff-scraper:latest
docker pull nginx:alpine
```

### Step 5: Start Python Scraper
```bash
docker run -d \
  --name tariff-scraper \
  --network tariff-network \
  -p 5001:5001 \
  --restart unless-stopped \
  -e OPENAI_API_KEY="your-openai-key-here" \
  kayweesim/tariff-scraper:latest
```

### Step 6: Start Spring Boot Backend
```bash
docker run -d \
  --name tariff-backend \
  --network tariff-network \
  -p 8080:8080 \
  --restart unless-stopped \
  -e SPRING_DATASOURCE_URL="your-db-url" \
  -e SPRING_DATASOURCE_USERNAME="your-db-username" \
  -e SPRING_DATASOURCE_PASSWORD="your-db-password" \
  -e JWT_SECRET="your-jwt-secret" \
  -e JWT_EXPIRATION_MS="900000" \
  -e OPENAI_API_KEY="your-openai-key" \
  -e OPENEXCHANGERATES_API_KEY="your-exchange-rate-key" \
  -e PYTHON_SCRAPER_URL="http://tariff-scraper:5001/scrape" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  kayweesim/tariff-backend:latest
```

### Step 7: Start Nginx
```bash
docker run -d \
  --name tariff-nginx \
  --network tariff-network \
  -p 80:80 \
  -v /tmp/nginx.conf:/etc/nginx/nginx.conf:ro \
  --restart unless-stopped \
  nginx:alpine
```

### Step 8: Verify deployment
```bash
# Check if all containers are running
docker ps

# Check backend logs to verify new code
docker logs tariff-backend --tail 50

# Test the API endpoint
curl http://localhost:8080/api/tariff/calculate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-token" \
  -d '{
    "importingCountry": "SG",
    "exportingCountry": "US",
    "hsCode": "0101.21.00",
    "productValue": 1000,
    "weight": 10,
    "shippingMode": "AIR"
  }'
```

### Step 9: Check if shippingRatePerKg is in the response
Look for the `shippingRatePerKg` field in the API response. It should show the per-kg rate.

---

## Quick One-Liner Script (Copy and paste this entire block):

```bash
# Stop and remove containers
docker stop tariff-nginx tariff-backend tariff-scraper 2>/dev/null
docker rm tariff-nginx tariff-backend tariff-scraper 2>/dev/null

# Remove old images
docker rmi kayweesim/tariff-backend:latest 2>/dev/null
docker rmi kayweesim/tariff-scraper:latest 2>/dev/null

# Pull fresh images
docker pull kayweesim/tariff-backend:latest
docker pull kayweesim/tariff-scraper:latest

# Start scraper
docker run -d --name tariff-scraper --network tariff-network -p 5001:5001 --restart unless-stopped -e OPENAI_API_KEY="YOUR_KEY_HERE" kayweesim/tariff-scraper:latest

# Start backend (REPLACE THE ENVIRONMENT VARIABLES WITH YOUR ACTUAL VALUES)
docker run -d --name tariff-backend --network tariff-network -p 8080:8080 --restart unless-stopped \
  -e SPRING_DATASOURCE_URL="YOUR_DB_URL" \
  -e SPRING_DATASOURCE_USERNAME="YOUR_DB_USER" \
  -e SPRING_DATASOURCE_PASSWORD="YOUR_DB_PASS" \
  -e JWT_SECRET="YOUR_JWT_SECRET" \
  -e OPENAI_API_KEY="YOUR_OPENAI_KEY" \
  -e OPENEXCHANGERATES_API_KEY="YOUR_EXCHANGE_KEY" \
  -e PYTHON_SCRAPER_URL="http://tariff-scraper:5001/scrape" \
  kayweesim/tariff-backend:latest

# Wait for backend to start
sleep 30

# Start nginx
docker run -d --name tariff-nginx --network tariff-network -p 80:80 -v /tmp/nginx.conf:/etc/nginx/nginx.conf:ro --restart unless-stopped nginx:alpine

# Check status
docker ps
echo "Deployment complete! Check logs with: docker logs tariff-backend"
```

---

## Troubleshooting:

### If you see "network tariff-network not found":
```bash
docker network create tariff-network
```

### If nginx.conf is missing:
```bash
# You need to copy your nginx.conf to EC2 first
# From your local machine:
scp -i your-key.pem nginx.conf ec2-user@52.65.58.208:/tmp/
```

### To check if the new code is deployed:
```bash
# Check when the image was pulled
docker images | grep tariff-backend

# Check the backend logs for startup messages
docker logs tariff-backend | grep -i "started"

# Test the new endpoint directly
docker exec tariff-backend curl -X POST http://localhost:8080/api/tariff/calculate \
  -H "Content-Type: application/json" \
  -d '{"importingCountry":"SG","exportingCountry":"US","hsCode":"0101210000","productValue":1000,"weight":10,"heads":1,"shippingMode":"AIR"}'
```

### To force Docker to not use any cache:
```bash
# Clear all Docker cache
docker system prune -a --volumes -f
```

**WARNING**: This will delete ALL unused images, containers, and volumes. Only do this if you're sure!

---

## Environment Variables You Need:

Make sure to replace these placeholders with your actual values from GitHub Secrets:
- `SPRING_DATASOURCE_URL` - Your RDS database URL
- `SPRING_DATASOURCE_USERNAME` - Your database username
- `SPRING_DATASOURCE_PASSWORD` - Your database password
- `JWT_SECRET` - Your JWT secret key
- `OPENAI_API_KEY` - Your OpenAI API key
- `OPENEXCHANGERATES_API_KEY` - Your Exchange Rates API key

You can find these values in your GitHub repository → Settings → Secrets and variables → Actions

