# Docker Deployment Guide

## 🏗️ Architecture Overview

This application uses a microservices architecture with three main services orchestrated via Docker:

```
┌─────────────────────────────────────────────────────────┐
│                      NGINX (Port 80)                    │
│                   Reverse Proxy Layer                   │
└─────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Java Backend    │  │ Python Scraper  │  │ Frontend (opt.) │
│ (Port 8080)     │  │ (Port 5001)     │  │                 │
│ Spring Boot API │  │ Flask/FastAPI   │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

## 📦 Services

### 1. **tariff-backend** (Java Spring Boot)
- **Image**: `kayweesim/tariff-backend:latest`
- **Internal Port**: 8080
- **Container Name**: `tariff-backend`
- **Purpose**: Main REST API for tariff calculations, user management, authentication
- **Endpoints**: `/api/*`, `/auth/*`, `/swagger-ui/*`

### 2. **tariff-scraper** (Python)
- **Image**: `kayweesim/tariff-scraper:latest`
- **Internal Port**: 5001
- **Container Name**: `tariff-scraper`
- **Purpose**: Web scraping service for tariff rates using OpenAI
- **Endpoints**: `/scrape/*`
- **Health Check**: `/health` endpoint checked every 30s

### 3. **tariff-nginx** (NGINX Reverse Proxy)
- **Image**: `nginx:alpine`
- **External Port**: 80 (exposed to internet)
- **Container Name**: `tariff-nginx`
- **Purpose**: Routes requests to appropriate backend services, handles CORS

## 🔌 Port Mapping

| Service | Internal Port | External Port | Access |
|---------|--------------|---------------|---------|
| NGINX | 80 | 80 | ✅ Public |
| Backend | 8080 | - | 🔒 Internal only (via NGINX) |
| Scraper | 5001 | - | 🔒 Internal only (via NGINX) |

## 🌐 NGINX Routing

NGINX acts as a reverse proxy and routes requests to the appropriate service:

| Path | Routed To | Service |
|------|-----------|---------|
| `/api/*` | `tariff-backend:8080` | Java Backend |
| `/auth/*` | `tariff-backend:8080` | Java Backend |
| `/swagger-ui/*` | `tariff-backend:8080` | API Documentation |
| `/scrape/*` | `tariff-scraper:5001` | Python Scraper |
| `/health` | NGINX response | Health check |

### NGINX Configuration Notes
- **CORS Headers**: Automatically added to all responses for cross-origin requests
- **Preflight Handling**: OPTIONS requests return 204 No Content
- **Upstream Load Balancing**: Can be extended for multiple instances

## 🚀 Deployment Commands

### Building Images

```bash
# Build Backend
docker build -t kayweesim/tariff-backend:latest .

# Build Scraper
cd tariff-scraper
docker build -t kayweesim/tariff-scraper:latest .
```

### Running on EC2

```bash
# Pull latest images
docker pull kayweesim/tariff-backend:latest
docker pull kayweesim/tariff-scraper:latest
docker pull nginx:alpine

# Run Backend (with environment variables)
docker run -d \
  --name tariff-backend \
  --network bridge \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/tariffdb \
  -e SPRING_DATASOURCE_USERNAME=your-username \
  -e SPRING_DATASOURCE_PASSWORD=your-password \
  -e JWT_SECRET=your-jwt-secret \
  kayweesim/tariff-backend:latest

# Run Scraper (with environment variables)
docker run -d \
  --name tariff-scraper \
  --network bridge \
  -e OPENAI_API_KEY=your-openai-api-key \
  kayweesim/tariff-scraper:latest

# Run NGINX (mount config file)
docker run -d \
  --name tariff-nginx \
  --network bridge \
  -p 80:80 \
  -v /path/to/nginx.conf:/etc/nginx/nginx.conf:ro \
  nginx:alpine
```

### Using Docker Compose (Recommended)

Create a `docker-compose.prod.yml`:

```yaml
version: '3.9'

services:
  tariff-backend:
    image: kayweesim/tariff-backend:latest
    container_name: tariff-backend
    environment:
      - SPRING_DATASOURCE_URL=${DB_URL}
      - SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  tariff-scraper:
    image: kayweesim/tariff-scraper:latest
    container_name: tariff-scraper
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5001/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  tariff-nginx:
    image: nginx:alpine
    container_name: tariff-nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - tariff-backend
      - tariff-scraper
    restart: unless-stopped
```

Deploy with:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🔧 Environment Variables

### Backend (tariff-backend)
Required environment variables:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=username
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=your-secret-key
```

### Scraper (tariff-scraper)
Required environment variables:
```env
OPENAI_API_KEY=sk-your-api-key-here
```

### Setting Environment Variables on EC2

```bash
# Create .env file
cat > .env << EOF
DB_URL=jdbc:postgresql://your-db-host:5432/tariffdb
DB_USERNAME=your-username
DB_PASSWORD=your-password
JWT_SECRET=your-jwt-secret
OPENAI_API_KEY=sk-your-api-key
EOF

# Use with docker run
docker run -d --env-file .env kayweesim/tariff-backend:latest
```

## 🐛 Troubleshooting

### Container Keeps Restarting

```bash
# Check container logs
docker logs tariff-backend
docker logs tariff-scraper
docker logs tariff-nginx

# Common issues:
# 1. Missing environment variables (OPENAI_API_KEY, DB credentials)
# 2. NGINX config syntax errors
# 3. Database connection failures
# 4. Port conflicts
```

### NGINX Configuration Errors

**Error**: `"add_header" directive is not allowed here`

**Solution**: `add_header` must be inside `server`, `location`, or `http` blocks, NOT at the top level. See the fixed `nginx.conf`.

### Python Scraper Errors

**Error**: `OpenAIError: The api_key client option must be set`

**Solution**: Set the `OPENAI_API_KEY` environment variable:
```bash
docker run -d -e OPENAI_API_KEY=sk-your-key tariff-scraper
```

### Checking Service Health

```bash
# Check if containers are running
docker ps

# Check specific service health
curl http://localhost/health
curl http://localhost:8080/actuator/health  # Backend
curl http://localhost:5001/health           # Scraper

# On EC2 (replace with your EC2 public IP)
curl http://your-ec2-ip/health
```

## 📊 Monitoring

### View Logs in Real-Time

```bash
# All services
docker-compose logs -f

# Specific service
docker logs -f tariff-backend
docker logs -f tariff-scraper
docker logs -f tariff-nginx
```

### Check Resource Usage

```bash
docker stats
```

### Restart Services

```bash
# Restart specific service
docker restart tariff-backend
docker restart tariff-scraper
docker restart tariff-nginx

# Restart all services
docker-compose restart
```

## 🔄 Update/Redeploy

```bash
# Pull latest images
docker pull kayweesim/tariff-backend:latest
docker pull kayweesim/tariff-scraper:latest

# Stop and remove old containers
docker stop tariff-backend tariff-scraper tariff-nginx
docker rm tariff-backend tariff-scraper tariff-nginx

# Restart with new images
docker-compose -f docker-compose.prod.yml up -d

# Or rebuild and redeploy
docker-compose -f docker-compose.prod.yml up -d --build
```

## 🔐 Security Considerations

1. **Environment Variables**: Never commit `.env` files to Git
2. **Secrets Management**: Use AWS Secrets Manager or similar for production
3. **HTTPS**: Configure SSL/TLS certificates (Let's Encrypt recommended)
4. **Firewall**: Only expose port 80/443, keep 8080 and 5001 internal
5. **Database**: Ensure database is not publicly accessible

## 📝 Network Communication

Services communicate internally via Docker's bridge network:
- NGINX can reach backend at `http://tariff-backend:8080`
- NGINX can reach scraper at `http://tariff-scraper:5001`
- External traffic only hits NGINX on port 80

## ✅ Verification Checklist

After deployment, verify:
- [ ] All containers are running: `docker ps`
- [ ] No restart loops: Check STATUS column
- [ ] Backend health: `curl http://your-ip/api/health`
- [ ] Scraper health: `curl http://your-ip/scrape/health`
- [ ] NGINX health: `curl http://your-ip/health`
- [ ] Swagger UI accessible: `http://your-ip/swagger-ui/`
- [ ] No errors in logs: `docker logs [container-name]`

## 🆘 Quick Fixes

### Reset Everything
```bash
# Nuclear option: stop and remove everything
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
docker-compose -f docker-compose.prod.yml up -d
```

### Check NGINX Config Syntax
```bash
docker exec tariff-nginx nginx -t
```

### Enter Container for Debugging
```bash
docker exec -it tariff-backend sh
docker exec -it tariff-scraper sh
docker exec -it tariff-nginx sh
```

