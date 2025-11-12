# SonarQube Integration Guide

## Overview
SonarQube/SonarCloud is now integrated into your project for continuous code quality and security analysis. It works alongside JaCoCo to provide coverage metrics and identify code smells, bugs, and vulnerabilities.

## What Was Added

### 1. SonarQube Properties in pom.xml
- **Organization**: Your SonarCloud organization
- **Project Key**: CS203G4T2-Project
- **Coverage Integration**: Automatically uses JaCoCo XML reports
- **Host**: https://sonarcloud.io

### 2. sonar-project.properties
- Project configuration for local and CI/CD analysis
- Exclusions for DTOs, configs, and entities
- Custom rules configuration

### 3. GitHub Actions Integration
- Automatic scanning on every push/PR to main branch
- Runs after tests and before deployment
- Reports results to SonarCloud dashboard

## Setup Instructions

### Step 1: Create SonarCloud Account

1. Go to [https://sonarcloud.io](https://sonarcloud.io)
2. Click "Sign up" and choose "With GitHub"
3. Authorize SonarCloud to access your GitHub account

### Step 2: Import Your Project

1. Click the **"+"** button in the top-right
2. Select **"Analyze new project"**
3. Choose your GitHub organization
4. Select **CS203G4T2-Project** repository
5. Click **"Set Up"**

### Step 3: Get Your Organization Key

1. In SonarCloud, go to **My Account** → **Organizations**
2. Copy your **organization key** (e.g., `your-github-username`)
3. You'll need this for the next step

### Step 4: Generate SonarCloud Token

1. Go to **My Account** → **Security**
2. Under **Generate Tokens**, enter a name: `GitHub Actions`
3. Set expiration (recommend: 90 days or No expiration)
4. Click **Generate**
5. **Copy the token** (you won't see it again!)

### Step 5: Add GitHub Secrets

Add these secrets to your GitHub repository:

1. Go to your GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"** for each:

```
Name: SONAR_TOKEN
Value: [paste the token from Step 4]

Name: SONAR_ORGANIZATION
Value: [your organization key from Step 3]
```

### Step 6: Update pom.xml

Update the organization in your `pom.xml`:

```xml
<sonar.organization>your-actual-org-key</sonar.organization>
```

Replace `your-org` with your actual SonarCloud organization key.

### Step 7: Update sonar-project.properties

Update the organization key in `sonar-project.properties`:

```properties
sonar.organization=your-actual-org-key
```

## How to Use SonarQube

### Automatic Scanning (CI/CD)

Every time you push to `main` or create a PR:
1. GitHub Actions runs your tests
2. JaCoCo generates coverage reports
3. SonarQube scans your code
4. Results appear in SonarCloud dashboard
5. Quality Gate status shows in GitHub PR

### Manual Local Scanning

To run SonarQube analysis locally:

```bash
# First, run tests to generate coverage
.\mvnw.cmd clean verify

# Then run SonarQube analysis
.\mvnw.cmd sonar:sonar ^
  -Dsonar.projectKey=CS203G4T2-Project ^
  -Dsonar.organization=your-org-key ^
  -Dsonar.host.url=https://sonarcloud.io ^
  -Dsonar.token=your-sonar-token
```

On Linux/Mac:
```bash
mvn clean verify
mvn sonar:sonar \
  -Dsonar.projectKey=CS203G4T2-Project \
  -Dsonar.organization=your-org-key \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=your-sonar-token
```

## Understanding SonarQube Metrics

### Quality Gate
SonarQube uses a "Quality Gate" to determine if code meets quality standards:
- ✅ **Passed**: Code meets all criteria
- ❌ **Failed**: Code has issues that need fixing

### Key Metrics

1. **Bugs** 🐛
   - Potential runtime errors
   - Target: 0 bugs

2. **Vulnerabilities** 🔒
   - Security issues
   - Target: 0 vulnerabilities

3. **Code Smells** 👃
   - Maintainability issues
   - Target: Technical debt ratio < 5%

4. **Coverage** 📊
   - Test coverage percentage (from JaCoCo)
   - Target: > 80% (configurable)

5. **Duplications** 📋
   - Duplicated code blocks
   - Target: < 3%

6. **Security Hotspots** 🔥
   - Code that needs security review
   - Target: All reviewed

### Reliability Ratings
- **A**: No bugs
- **B**: At least 1 minor bug
- **C**: At least 1 major bug
- **D**: At least 1 critical bug
- **E**: At least 1 blocker bug

### Security Ratings
- **A**: No vulnerabilities
- **B**: At least 1 minor vulnerability
- **C**: At least 1 major vulnerability
- **D**: At least 1 critical vulnerability
- **E**: At least 1 blocker vulnerability

### Maintainability Ratings
- **A**: Technical debt ratio ≤ 5%
- **B**: Technical debt ratio between 6-10%
- **C**: Technical debt ratio between 11-20%
- **D**: Technical debt ratio between 21-50%
- **E**: Technical debt ratio > 50%

## Viewing Results

### SonarCloud Dashboard
1. Go to [https://sonarcloud.io](https://sonarcloud.io)
2. Click on your project: **CS203G4T2-Project**
3. View the dashboard with all metrics

### In GitHub Pull Requests
- Quality Gate status appears as a check
- Click "Details" to see full SonarCloud report
- Failed Quality Gate blocks merge (optional)

### In GitHub Actions
- Check the workflow run logs
- Look for the "SonarQube Scan" step
- See summary of issues found

## Customizing Quality Gate

### In SonarCloud UI
1. Go to **Project Settings** → **Quality Gate**
2. Create a custom quality gate or use default
3. Set thresholds for:
   - Coverage on new code
   - Duplicated lines
   - Maintainability rating
   - Reliability rating
   - Security rating

### Example Custom Quality Gate
```
Coverage on New Code: ≥ 80%
Duplicated Lines on New Code: ≤ 3%
Maintainability Rating: ≥ A
Reliability Rating: ≥ A
Security Rating: ≥ A
Security Hotspots Reviewed: 100%
```

## Excluding Files from Analysis

Already configured exclusions in `sonar-project.properties`:
- Configuration files (`**/config/**`)
- DTOs (`**/dto/**`)
- Entity classes (`**/entity/**`)
- Main application class (`**/*Application.java`)

To add more exclusions, edit `sonar-project.properties`:

```properties
sonar.exclusions=\
  **/dto/**,\
  **/config/**,\
  **/entity/**,\
  **/*Application.java,\
  **/generated/**
```

## Fixing Issues

### 1. Review Issues in SonarCloud
- Click on **Issues** tab
- Filter by type (Bug, Vulnerability, Code Smell)
- Click on an issue to see details and remediation advice

### 2. Prioritize
1. **Bugs** (highest priority)
2. **Vulnerabilities** (security issues)
3. **Code Smells** (maintainability)

### 3. Fix in Your Code
- SonarCloud provides specific line numbers
- Includes explanation and example fixes
- Some issues can be marked as "Won't Fix" if not applicable

### 4. Re-run Analysis
- Push your fixes to GitHub
- CI/CD automatically re-runs SonarQube
- Check if Quality Gate passes

## Common Issues and Fixes

### Issue: "Cognitive Complexity too high"
**Fix**: Break down complex methods into smaller ones
```java
// Before: Complex method with many nested conditions
public void complexMethod() {
    if (condition1) {
        if (condition2) {
            if (condition3) {
                // ...
            }
        }
    }
}

// After: Simplified with extracted methods
public void simpleMethod() {
    if (shouldProcess()) {
        processData();
    }
}
```

### Issue: "Unused private method"
**Fix**: Remove unused code or make it public if needed externally

### Issue: "Generic exceptions should not be thrown"
**Fix**: Use specific exception types
```java
// Before
throw new Exception("Error");

// After
throw new IllegalArgumentException("Invalid input");
```

### Issue: "Close this resource"
**Fix**: Use try-with-resources
```java
// Before
FileReader reader = new FileReader("file.txt");
// ... use reader

// After
try (FileReader reader = new FileReader("file.txt")) {
    // ... use reader
}
```

## Integration with JaCoCo

SonarQube automatically reads your JaCoCo coverage reports:
- Line coverage
- Branch coverage
- Coverage on new code
- Uncovered lines highlighted in SonarCloud

The workflow is:
1. Tests run → JaCoCo collects coverage
2. JaCoCo generates XML report
3. SonarQube reads the XML report
4. Coverage metrics displayed in SonarCloud

## Best Practices

1. **Fix issues before merging**: Don't accumulate technical debt
2. **Aim for A ratings**: Strive for high quality in all categories
3. **Review Security Hotspots**: Even if not vulnerabilities, review them
4. **Maintain high coverage**: Keep coverage above 80%
5. **Monitor trends**: Watch for increasing debt over time
6. **Use Quality Gate in PRs**: Prevent merging low-quality code
7. **Regular maintenance**: Schedule time to address code smells

## Badges for README

Add SonarCloud badges to your README:

```markdown
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=CS203G4T2-Project&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=CS203G4T2-Project)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=CS203G4T2-Project&metric=coverage)](https://sonarcloud.io/summary/new_code?id=CS203G4T2-Project)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=CS203G4T2-Project&metric=bugs)](https://sonarcloud.io/summary/new_code?id=CS203G4T2-Project)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=CS203G4T2-Project&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=CS203G4T2-Project)
```

## Troubleshooting

### Error: "No organization provided"
**Solution**: Add `SONAR_ORGANIZATION` secret to GitHub

### Error: "Invalid authentication credentials"
**Solution**: Regenerate SonarCloud token and update `SONAR_TOKEN` secret

### Error: "Project not found"
**Solution**: Ensure project is created in SonarCloud and projectKey matches

### Coverage not showing
**Solution**: 
1. Verify JaCoCo runs before SonarQube: `mvn verify` then `mvn sonar:sonar`
2. Check `target/site/jacoco/jacoco.xml` exists
3. Verify path in `sonar-project.properties`

### Build hangs during SonarQube scan
**Solution**: Check network connectivity to sonarcloud.io, or increase timeout in pom.xml

## Commands Cheat Sheet

```bash
# Run tests with coverage
.\mvnw.cmd clean verify

# Run SonarQube analysis
.\mvnw.cmd sonar:sonar

# Run both together
.\mvnw.cmd clean verify sonar:sonar

# Skip SonarQube in build
.\mvnw.cmd clean package -Dsonar.skip=true

# Run analysis for specific branch
.\mvnw.cmd sonar:sonar -Dsonar.branch.name=feature-branch
```

## Next Steps

1. ✅ Create SonarCloud account and import project
2. ✅ Add `SONAR_TOKEN` and `SONAR_ORGANIZATION` to GitHub Secrets
3. ✅ Update `pom.xml` and `sonar-project.properties` with your org key
4. ✅ Push changes to trigger first analysis
5. ✅ Review results in SonarCloud dashboard
6. ✅ Fix any critical issues
7. ✅ Add badges to your README
8. ✅ Configure Quality Gate for your needs

## Resources

- [SonarCloud Documentation](https://docs.sonarcloud.io/)
- [SonarQube Java Rules](https://rules.sonarsource.com/java/)
- [Quality Gate Documentation](https://docs.sonarcloud.io/improving/quality-gates/)
- [Maven Scanner Documentation](https://docs.sonarcloud.io/advanced-setup/ci-based-analysis/sonarscanner-for-maven/)

Happy code quality improvement! 🎯

