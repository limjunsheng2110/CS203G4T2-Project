# GitHub Secrets Configuration for Your Project

Add these secrets to your GitHub repository:
Settings → Secrets and Variables → Actions → New repository secret

## Required Secrets:

1. **DOCKER_USERNAME** = your Docker Hub username
2. **DOCKER_PASSWORD** = your Docker Hub password/token
3. **DEPLOY_HOST** = 52.65.58.208 (your EC2 IP)
4. **SSH_USERNAME** = ubuntu
5. **SSH_PRIVATE_KEY** = (contents of your tariffg4t2-keypair.pem file)

## Database Secrets:
6. **DB_URL** = your PostgreSQL RDS connection string
7. **DB_USERNAME** = your database username  
8. **DB_PASSWORD** = your database password

## API Keys:
9. **JWT_SECRET** = your JWT secret key
10. **OPENAI_KEY** = your OpenAI API key
11. **OPENEXCHANGERATES_API_KEY** = your exchange rates API key

## Example Values:
- DEPLOY_HOST: 52.65.58.208
- SSH_USERNAME: ubuntu
- SSH_PRIVATE_KEY: -----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...
...your full private key content...
-----END PRIVATE KEY-----
