# Docker Deployment Guide

## Architecture Overview

This application consists of three main services orchestrated using Docker Compose:

```
┌─────────────────────────────────────────────────────┐
│                     Client                          │
│                   (Port 80)                         │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│              Nginx Reverse Proxy                    │
│                   (Port 80)                         │
│  Routes:                                            │
│  - /api/      → Backend                            │
│  - /auth/     → Backend                            │
│  - /scrape/   → Python Scraper                     │
│  - /swagger-ui/ → Backend Swagger                  │
└──────────┬────────────────────────┬─────────────────┘
           │                        │
           ▼                        ▼
┌──────────────────────┐  ┌─────────────────────────┐
│  Java Backend        │  │  Python Scraper         │
│  (Spring Boot)       │  │  (Flask)                │
│  Port: 8080          │  │  Port: 5001             │
│                      │  │                         │
│  - REST APIs         │  │  - Tariff Scraping      │
│  - Authentication    │  │  - OpenAI Integration   │
│  - Tariff Calc       │  │  - Rate Processing      │
└──────────────────────┘  └─────────────────────────┘
```

## Services

### 1. **tariff-backend** (Java Spring Boot)
- **Port**: 8080 (internal), exposed via nginx
- **Purpose**: Main application backend with REST APIs
- **Features**:
  - User authentication & authorization
  - Tariff calculations
  - Country & product management
  - Exchange rate analysis
  - Integration with external APIs
- **Health Check**: `/api/health`

### 2. **tariff-scraper** (Python Flask)
- **Port**: 5001 (internal), exposed via nginx at `/scrape/`
- **Purpose**: Tariff data scraping and processing
- **Features**:
  - Web scraping for tariff data
  - OpenAI integration for data extraction
  - Rate processing
- **Health Check**: `/health`

### 3. **tariff-nginx** (Nginx Reverse Proxy)
- **Port**: 80 (public)
- **Purpose**: Route incoming requests to appropriate services
- **Routes**:
  - `/api/*` → Backend (port 8080)
  - `/auth/*` → Backend (port 8080)
  - `/scrape/*` → Python Scraper (port 5001)
  - `/swagger-ui/*` → Backend Swagger UI
  - `/health` → Nginx health check

## Prerequisites

- Docker installed (version 20.10+)
- Docker Compose installed (version 2.0+)
- Minimum 2GB RAM available
- Ports 80, 8080, 5001 available

## Environment Variables

Create a `.env` file in the project root with the following variables:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/your-database
SPRING_DATASOURCE_USERNAME=your-db-username
SPRING_DATASOURCE_PASSWORD=your-db-password

# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-min-256-bits
JWT_EXPIRATION=86400000

# External API Keys
EXCHANGERATE_API_KEY=your-exchangerate-api-key
OPENAI_API_KEY=your-openai-api-key

# Spring Profiles
SPRING_PROFILES_ACTIVE=prod

# Flask Environment
FLASK_ENV=production
```

## Deployment Instructions

### Local Development

1. **Clone the repository**:
   ```bash
   git clone <your-repo-url>
   cd CS203G4T2-Project
   ```

2. **Create environment file**:
   ```bash
   cp .env.example .env
   # Edit .env with your actual values
   ```

3. **Build and start all services**:
   ```bash
   docker-compose up -d --build
   ```

4. **Check service status**:
   ```bash
   docker-compose ps
   ```

5. **View logs**:
   ```bash
   # All services
   docker-compose logs -f
   
   # Specific service
   docker-compose logs -f tariff-backend
   docker-compose logs -f tariff-scraper
   docker-compose logs -f tariff-nginx
   ```

6. **Access the application**:
   - Main API: http://localhost/api/
   - Authentication: http://localhost/auth/
   - Scraper: http://localhost/scrape/
   - Swagger UI: http://localhost/swagger-ui/
   - Health Check: http://localhost/health

### AWS EC2 Deployment

1. **Connect to your EC2 instance**:
   ```bash
   ssh -i your-key.pem ec2-user@your-ec2-ip
   ```

2. **Install Docker and Docker Compose** (if not already installed):
   ```bash
   # Install Docker
   sudo yum update -y
   sudo yum install docker -y
   sudo systemctl start docker
   sudo systemctl enable docker
   sudo usermod -a -G docker ec2-user
   
   # Install Docker Compose
   sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   
   # Log out and back in for group changes to take effect
   exit
   ```

3. **Clone repository and set up**:
   ```bash
   git clone <your-repo-url>
   cd CS203G4T2-Project
   ```

4. **Create and configure .env file**:
   ```bash
   nano .env
   # Add your environment variables
   ```

5. **Build and deploy**:
   ```bash
   docker-compose up -d --build
   ```

6. **Configure security group**:
   - Open port 80 (HTTP) in AWS EC2 security group
   - Optionally open port 443 (HTTPS) for SSL

7. **Access via public IP**:
   - http://your-ec2-public-ip/api/
   - http://your-ec2-public-ip/swagger-ui/

## Docker Compose Commands

### Starting Services
```bash
# Start all services
docker-compose up -d

# Start with rebuild
docker-compose up -d --build

# Start specific service
docker-compose up -d tariff-backend
```

### Stopping Services
```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Stop specific service
docker-compose stop tariff-backend
```

### Monitoring
```bash
# View running containers
docker-compose ps

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f tariff-nginx

# View last 100 lines
docker-compose logs --tail=100

# Check resource usage
docker stats
```

### Troubleshooting
```bash
# Restart a service
docker-compose restart tariff-nginx

# Rebuild without cache
docker-compose build --no-cache

# Remove all stopped containers
docker-compose rm

# Execute command in running container
docker-compose exec tariff-backend sh
docker-compose exec tariff-scraper sh
```

## Health Checks

All services include health checks that automatically restart containers if they fail:

- **Backend**: Checks `/api/health` every 30s (40s start period)
- **Scraper**: Checks `/health` every 30s (5s start period)
- **Nginx**: Checks `/health` every 30s (5s start period)

## Networking

All services run on a custom bridge network called `tariff-network`. This allows:
- Service-to-service communication using container names
- Isolation from other Docker networks
- DNS resolution between containers

Example: Backend can reach scraper at `http://tariff-scraper:5001`

## Restart Policies

All services use `restart: unless-stopped`, which means:
- Containers restart automatically if they crash
- Containers start automatically when Docker daemon starts (after system reboot)
- Containers don't restart if manually stopped

## Persistent Deployment

To ensure your services remain running after closing your laptop/SSH:

1. **Use Docker Compose** (recommended):
   ```bash
   docker-compose up -d
   ```
   The `-d` flag runs containers in detached mode.

2. **Check they're running**:
   ```bash
   docker-compose ps
   ```

3. **Verify restart policy**:
   ```bash
   docker inspect tariff-backend | grep -A 5 "RestartPolicy"
   ```

4. **On EC2**: Services will continue running even after you disconnect from SSH.

5. **After laptop shutdown**: If deploying on EC2, services continue running on the server.

## Troubleshooting Common Issues

### Nginx keeps restarting
- **Cause**: Invalid nginx.conf syntax
- **Solution**: Check nginx logs: `docker-compose logs tariff-nginx`
- **Fixed**: The `add_header` directive is now correctly placed inside location blocks

### Python scraper fails with OpenAI error
- **Cause**: Missing `OPENAI_API_KEY` environment variable
- **Solution**: Add the key to your `.env` file and restart: `docker-compose restart tariff-scraper`

### Backend can't connect to database
- **Cause**: Wrong database credentials or unreachable host
- **Solution**: Verify `SPRING_DATASOURCE_URL`, `USERNAME`, and `PASSWORD` in `.env`

### Port already in use
- **Cause**: Another service using ports 80, 8080, or 5001
- **Solution**: 
  ```bash
  # Find process using port
  sudo lsof -i :80
  # Kill process or change port in docker-compose.yml
  ```

### Services can't communicate
- **Cause**: Not on same Docker network
- **Solution**: All services should be on `tariff-network` (already configured)

## Security Considerations

1. **Environment Variables**: Never commit `.env` file to version control
2. **JWT Secret**: Use a strong, random secret (min 256 bits)
3. **Database**: Use strong passwords and restrict network access
4. **HTTPS**: For production, add SSL/TLS (use Let's Encrypt with Certbot)
5. **API Keys**: Rotate regularly and restrict permissions

## Performance Optimization

1. **Resource Limits**: Add resource limits to docker-compose.yml:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '1.0'
         memory: 1G
   ```

2. **Logging**: Limit log size:
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

3. **Build Cache**: Use `docker-compose build` without `--no-cache` for faster rebuilds

## Monitoring & Logs

### View real-time logs
```bash
docker-compose logs -f --tail=100
```

### Export logs to file
```bash
docker-compose logs > deployment.log
```

### Check container health
```bash
docker-compose ps
docker inspect --format='{{json .State.Health}}' tariff-backend | jq
```

## Backup and Recovery

### Backup environment
```bash
docker-compose config > docker-compose-backup.yml
cp .env .env.backup
```

### Export images
```bash
docker save -o backend.tar kayweesim/tariff-backend:latest
docker save -o scraper.tar kayweesim/tariff-scraper:latest
```

## Updating Services

```bash
# Pull latest code
git pull origin main

# Rebuild and restart
docker-compose up -d --build

# Or rebuild specific service
docker-compose up -d --build tariff-backend
```

## CI/CD Integration

For automated deployment:
1. Build Docker images in CI pipeline
2. Push to Docker Hub / AWS ECR
3. SSH to EC2 and pull latest images
4. Run `docker-compose up -d --pull always`

## Support

For issues or questions:
1. Check logs: `docker-compose logs -f`
2. Verify environment variables in `.env`
3. Ensure all ports are available
4. Check AWS security group settings (if on EC2)
5. Review health check status: `docker-compose ps`

