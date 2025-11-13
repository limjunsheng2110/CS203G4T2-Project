# JaCoCo Code Coverage Guide

## Overview
JaCoCo (Java Code Coverage) is now configured in your Spring Boot project to measure test coverage and generate reports.

## What Was Added

### 1. JaCoCo Plugin in pom.xml
- **Version**: 0.8.11 (latest stable)
- **Configuration**: Automatically generates coverage reports when tests run
- **Minimum Coverage**: 50% line coverage, 40% branch coverage
- **Exclusions**: Config and DTO packages (optional)

## How to Use JaCoCo

### Generate Coverage Report
Run your tests with Maven:
```bash
mvnw clean test
```

Or on Windows:
```bash
.\mvnw.cmd clean test
```

This will:
1. Run all your tests
2. Collect coverage data
3. Generate HTML, XML, and CSV reports

### Check Coverage and Enforce Thresholds
To run tests AND verify coverage meets minimum requirements:
```bash
mvnw clean verify
```

Or on Windows:
```bash
.\mvnw.cmd clean verify
```

This will:
1. Run all tests
2. Generate coverage reports
3. Check if coverage meets the minimum thresholds (50% line, 40% branch)
4. **FAIL the build** if coverage is below the threshold

### View Coverage Report
After running tests, open the HTML report:
```
target/site/jacoco/index.html
```

Open this file in your browser to see:
- **Overall coverage** percentage
- **Package-level** coverage breakdown
- **Class-level** coverage details
- **Line-by-line** coverage (green = covered, red = not covered)

### Generate Report Without Running Tests Again
If tests already ran:
```bash
mvnw jacoco:report
```

## Report Locations

After running tests, you'll find reports in:
- **HTML Report**: `target/site/jacoco/index.html` (human-readable)
- **XML Report**: `target/site/jacoco/jacoco.xml` (for CI/CD tools)
- **CSV Report**: `target/site/jacoco/jacoco.csv` (for data analysis)
- **Binary Data**: `target/jacoco.exec` (raw coverage data)

## Understanding Coverage Metrics

JaCoCo measures several types of coverage:

1. **Line Coverage**: Percentage of code lines executed (minimum: 50%)
2. **Branch Coverage**: Percentage of if/else branches taken (minimum: 40%)
3. **Instruction Coverage**: JVM bytecode instructions covered
4. **Complexity Coverage**: Cyclomatic complexity paths covered
5. **Method Coverage**: Methods invoked during tests
6. **Class Coverage**: Classes loaded during tests

## Customizing Coverage Requirements

### Change Minimum Coverage Threshold
Edit `pom.xml` in the JaCoCo plugin configuration:

```xml
<limit>
  <counter>LINE</counter>
  <value>COVEREDRATIO</value>
  <minimum>0.50</minimum>  <!-- Change from 50% to desired percentage -->
</limit>
```

### Exclude More Classes from Coverage
Currently excluded: config and dto packages. To add more:

```xml
<configuration>
  <excludes>
    <exclude>**/config/**</exclude>
    <exclude>**/dto/**</exclude>
    <exclude>**/entity/**</exclude>  <!-- Add this -->
    <exclude>**/Application.class</exclude>  <!-- Add this -->
  </excludes>
</configuration>
```

### Disable Coverage Check Temporarily
Comment out the `jacoco-check` execution in pom.xml:

```xml
<!-- <execution>
  <id>jacoco-check</id>
  ...
</execution> -->
```

## CI/CD Integration

### GitHub Actions
Add this to your `.github/workflows/maven.yml`:

```yaml
- name: Run tests with coverage
  run: mvn clean verify

- name: Upload coverage report
  uses: actions/upload-artifact@v3
  with:
    name: jacoco-report
    path: target/site/jacoco/

- name: Comment coverage on PR
  uses: madrapps/jacoco-report@v1.6
  with:
    paths: target/site/jacoco/jacoco.xml
    token: ${{ secrets.GITHUB_TOKEN }}
```

### Integration with Coverage Services
The XML report can be uploaded to:
- **Codecov**: `bash <(curl -s https://codecov.io/bash)`
- **Coveralls**: Using coveralls-maven-plugin
- **SonarQube**: Automatically detects jacoco.xml

## Quick Commands Reference

```bash
# Run tests and generate coverage report
.\mvnw.cmd clean test

# Run tests, generate report, and enforce coverage thresholds
.\mvnw.cmd clean verify

# Only generate report (if tests already ran)
.\mvnw.cmd jacoco:report

# Clean all generated files
.\mvnw.cmd clean

# Generate report and open in browser (Windows)
.\mvnw.cmd clean test && start target\site\jacoco\index.html

# Run specific test and see coverage
.\mvnw.cmd test -Dtest=TariffControllerTest
```

## Understanding the Build Output

### Successful Coverage Check
```
[INFO] --- jacoco-maven-plugin:0.8.11:check (jacoco-check) @ tariffg4t2 ---
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

### Failed Coverage Check
```
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.11:check
[ERROR] Rule violated for bundle tariffg4t2: lines covered ratio is 0.45, but expected minimum is 0.50
[ERROR] BUILD FAILURE
```

## Tips for Better Coverage

1. **Write unit tests** for services and utilities
2. **Write integration tests** for controllers
3. **Test edge cases** and error handling
4. **Mock external dependencies** (databases, APIs)
5. **Focus on business logic** coverage first
6. **Don't obsess over 100%** - aim for meaningful coverage
7. **Exclude generated code** (DTOs, entities, configs)

## Current Test Coverage

Your project already has tests for:
- ✅ AuthController
- ✅ CountryController
- ✅ ProductController
- ✅ TariffController
- ✅ UserController
- ✅ ExchangeRateService
- ✅ TariffCalculatorService
- ✅ TariffValidationService
- ✅ CountryService
- ✅ ProductService

Run `.\mvnw.cmd verify` to see if your coverage meets the 50% threshold!

## Troubleshooting

### Error: "parameters 'rules' for goal jacoco:check are missing or invalid"
**Solution**: Run `mvn verify` instead of `mvn jacoco:check`. The verify phase properly executes the configured check.

### No coverage report generated
- Ensure tests actually run (check for test failures)
- Verify `target/jacoco.exec` file exists
- Run `.\mvnw.cmd clean test` to start fresh

### Coverage seems too low
- Check if all test files are being discovered
- Verify test naming convention (*Test.java)
- Look at the HTML report to see what's not covered
- Check if you need to exclude more classes (config, dto, entities)

### Build fails on coverage check
- Temporarily disable by removing the `jacoco-check` execution
- Or lower the minimum threshold to 0.30 (30%)
- Or write more tests to improve coverage
- Or exclude more packages that don't need testing

### Tests pass but verify fails
This is normal! It means your tests work, but coverage is below the threshold. Options:
1. Write more tests
2. Lower the threshold
3. Exclude packages that don't need coverage

## Maven Lifecycle Phases

Understanding when JaCoCo runs:
- **compile**: Compiles code (JaCoCo not involved)
- **test**: Runs tests + generates coverage data + creates report
- **verify**: Runs tests + generates report + **checks thresholds**
- **package**: Creates JAR (after verify passes)
- **install**: Installs to local Maven repo

## Next Steps

1. Run `.\mvnw.cmd clean verify` to check your coverage
2. Open `target\site\jacoco\index.html` to view the report
3. If build fails, check which packages have low coverage
4. Write additional tests or adjust thresholds
5. Commit the updated pom.xml to your repository

Happy testing! 🎯
