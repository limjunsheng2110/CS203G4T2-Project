# Controller Test Cases Documentation

## AuthController (20 tests)

### Registration (9 tests)
- `testRegister_Success` → 201 Created with JWT token
- `testRegister_DuplicateUsername` → 400 Bad Request
- `testRegister_DuplicateEmail` → 400 Bad Request
- `testRegister_InvalidEmail` → 400 Bad Request
- `testRegister_MissingUsername` → 400 Bad Request
- `testRegister_MissingPassword` → 400 Bad Request
- `testRegister_EmptyRequestBody` → 400 Bad Request
- `testRegister_WithAdminRole` → 201 Created
- `testRegister_PasswordEncoding` → Verify password encoder called

### Login (6 tests)
- `testLogin_Success` → 200 OK with JWT token
- `testLogin_InvalidCredentials` → 403 Forbidden
- `testLogin_UserNotFound` → 500 Internal Server Error
- `testLogin_MissingUsername` → 400 Bad Request
- `testLogin_MissingPassword` → 400 Bad Request
- `testLogin_EmptyRequestBody` → 400 Bad Request

### Token Validation (5 tests)
- `testValidateToken_Success` → 200 OK
- `testValidateToken_InvalidToken` → 401 Unauthorized
- `testValidateToken_MissingAuthorizationHeader` → 401 Unauthorized
- `testValidateToken_MissingBearerPrefix` → 401 Unauthorized
- `testValidateToken_EmptyToken` → 401 Unauthorized

---

## UserController (32 tests)

### Get All Users (3 tests)
- `testGetAllUsers_Success` → 200 OK with user list
- `testGetAllUsers_EmptyList` → 200 OK with empty array
- `testGetAllUsers_ServiceException` → 500 Internal Server Error

### Get User By ID (3 tests)
- `testGetUserById_Success` → 200 OK with user details
- `testGetUserById_NotFound` → 404 Not Found
- `testGetUserById_ServiceException` → 500 Internal Server Error

### Get User By Username (3 tests)
- `testGetUserByUsername_Success` → 200 OK with user details
- `testGetUserByUsername_NotFound` → 404 Not Found
- `testGetUserByUsername_ServiceException` → 500 Internal Server Error

### Get User By Email (3 tests)
- `testGetUserByEmail_Success` → 200 OK with user details
- `testGetUserByEmail_NotFound` → 404 Not Found
- `testGetUserByEmail_ServiceException` → 500 Internal Server Error

### Create User (4 tests)
- `testCreateUser_Success` → 201 Created
- `testCreateUser_DuplicateUsername` → 400 Bad Request
- `testCreateUser_InvalidEmail` → 400 Bad Request
- `testCreateUser_MissingRequiredFields` → 400 Bad Request

### Update User (3 tests)
- `testUpdateUser_Success` → 200 OK with updated user
- `testUpdateUser_NotFound` → 400 Bad Request
- `testUpdateUser_DuplicateEmail` → 400 Bad Request

### Deactivate User (3 tests)
- `testDeactivateUser_Success` → 200 OK
- `testDeactivateUser_NotFound` → 400 Bad Request
- `testDeactivateUser_AlreadyInactive` → 400 Bad Request

### Delete User (3 tests)
- `testDeleteUser_Success` → 200 OK
- `testDeleteUser_NotFound` → 400 Bad Request
- `testDeleteUser_ServiceException` → 400 Bad Request

### Get Active Users (3 tests)
- `testGetActiveUsers_Success` → 200 OK with active users
- `testGetActiveUsers_EmptyList` → 200 OK with empty array
- `testGetActiveUsers_ServiceException` → 500 Internal Server Error

### Get Users By Role (4 tests)
- `testGetUsersByRole_Success` → 200 OK with admin users
- `testGetUsersByRole_UserRole` → 200 OK with regular users
- `testGetUsersByRole_NoUsersWithRole` → 200 OK with empty array
- `testGetUsersByRole_ServiceException` → 500 Internal Server Error

---

## ProductController (19 tests)

### Get All Products (3 tests)
- `testGetAllProducts_Success` → 201 Created with product list
- `testGetAllProducts_EmptyList` → 201 Created with empty array
- `testGetAllProducts_ServiceException` → 500 Internal Server Error

### Get Product By HS Code (3 tests)
- `testGetProductByHsCode_Success` → 200 OK with product details
- `testGetProductByHsCode_NotFound` → 404 Not Found
- `testGetProductByHsCode_ServiceException` → 500 Internal Server Error

### Add Product (5 tests)
- `testAddProduct_Success` → 201 Created
- `testAddProduct_WithoutCategory` → 201 Created
- `testAddProduct_EmptyHsCode` → 400 Bad Request
- `testAddProduct_EmptyDescription` → 400 Bad Request
- `testAddProduct_DuplicateHsCode` → 400 Bad Request

### Update Product (4 tests)
- `testUpdateProduct_Success` → 200 OK with updated product
- `testUpdateProduct_OnlyDescription` → 200 OK
- `testUpdateProduct_OnlyCategory` → 200 OK
- `testUpdateProduct_ProductNotFound` → 404 Not Found

### Delete Product (4 tests)
- `testDeleteProduct_Success` → 200 OK
- `testDeleteProduct_NotFound` → 404 Not Found
- `testDeleteProduct_EmptyHsCode` → 404 Not Found
- `testDeleteProduct_ServiceException` → 500 Internal Server Error

---

## TariffController (9 tests)

### Calculate Tariff (9 tests)
- `testCalculateTariff_Success` → 200 OK with calculation results
- `testCalculateTariff_WithMinimalFields` → 200 OK
- `testCalculateTariff_InvalidCountryCode` → 400 Bad Request
- `testCalculateTariff_InvalidHsCode` → 400 Bad Request
- `testCalculateTariff_NegativeProductValue` → 400 Bad Request
- `testCalculateTariff_WithAdditionalDuties` → 200 OK with additional duties
- `testCalculateTariff_WithFreightAndInsurance` → 200 OK with customs value
- `testCalculateTariff_MissingMandatoryFields` → 400 Bad Request
- `testCalculateTariff_LargeProductValue` → 200 OK

---

## CountryController (15 tests)

### Get All Countries (2 tests)
- `testGetAllCountries_Success` → 200 OK with country list
- `testGetAllCountries_EmptyList` → 200 OK with empty array

### Get Country By Code (3 tests)
- `testGetCountryByCode_Success` → 200 OK with country details
- `testGetCountryByCode_NotFound` → 404 Not Found
- `testGetCountryByCode_InvalidCode` → 400 Bad Request

### Add Country (4 tests)
- `testAddCountry_Success` → 201 Created
- `testAddCountry_DuplicateCode` → 400 Bad Request
- `testAddCountry_InvalidCode` → 400 Bad Request
- `testAddCountry_MissingName` → 400 Bad Request

### Update Country (3 tests)
- `testUpdateCountry_Success` → 200 OK with updated country
- `testUpdateCountry_NotFound` → 404 Not Found
- `testUpdateCountry_PartialUpdate` → 200 OK

### Delete Country (3 tests)
- `testDeleteCountry_Success` → 200 OK
- `testDeleteCountry_NotFound` → 404 Not Found
- `testDeleteCountry_HasDependencies` → 400 Bad Request

---

## TariffCalculatorService (20 tests)

### Basic Calculation (2 tests)
- `testCalculate_Success_CIFValuation` → Correct calculation with CIF valuation
- `testCalculate_Success_TransactionValuation` → Correct calculation with Transaction valuation

### Additional Duties (4 tests)
- `testCalculate_WithSection301Duty` → Section 301 duty applied correctly
- `testCalculate_WithAntiDumpingDuty` → Anti-dumping duty applied correctly
- `testCalculate_WithMultipleAdditionalDuties` → Multiple duties stacked correctly
- `testCalculate_NoAdditionalDuties` → No additional duties when not applicable

### VAT/GST (3 tests)
- `testCalculate_VatIncludesDuties` → VAT calculated on customs value + duties
- `testCalculate_VatExcludesDuties` → VAT calculated on customs value only
- `testCalculate_WithVatOverride` → VAT override applied correctly

### Validation (2 tests)
- `testCalculate_ValidationErrors` → Throws exception on validation errors
- `testCalculate_WithMissingFieldsButDefaulted` → Handles defaulted fields

### Edge Cases (6 tests)
- `testCalculate_ZeroProductValue` → Handles zero product value
- `testCalculate_VeryLargeProductValue` → Handles very large values
- `testCalculate_NoCountryProfile` → Defaults when no country profile exists
- `testCalculate_NoTariffRateDate` → Handles missing tariff rate date
- `testCalculate_NullFreightAndInsurance` → Treats null as zero
- `testCalculate_TotalCostCalculation` → Verifies total cost formula

---

## CountryService (35 tests)

### Get All Countries (2 tests)
- `testGetAllCountriesFromDatabase_Success` → Returns list of countries
- `testGetAllCountriesFromDatabase_EmptyList` → Returns empty list

### Get Country By Code (3 tests)
- `testGetCountryByCode_Success` → Returns country by code
- `testGetCountryByCode_CaseInsensitive` → Handles case insensitive lookup
- `testGetCountryByCode_NotFound` → Throws exception when not found

### Create Country (7 tests)
- `testCreateCountry_Success` → Creates new country
- `testCreateCountry_CodeToUpperCase` → Converts code to uppercase
- `testCreateCountry_DuplicateCode` → Throws exception on duplicate
- `testCreateCountry_NullCode` → Throws exception on null code
- `testCreateCountry_EmptyCode` → Throws exception on empty code
- `testCreateCountry_NullName` → Throws exception on null name
- `testCreateCountry_EmptyName` → Throws exception on empty name

### Update Country (6 tests)
- `testUpdateCountry_Success` → Updates country successfully
- `testUpdateCountry_NotFound` → Returns null when not found
- `testUpdateCountry_NullCode` → Throws exception on null code
- `testUpdateCountry_EmptyCode` → Throws exception on empty code
- `testUpdateCountry_NullName` → Skips update when name is null
- `testUpdateCountry_EmptyName` → Skips update when name is empty

### Delete Country (4 tests)
- `testDeleteCountryByCode_Success` → Deletes country successfully
- `testDeleteCountryByCode_NotFound` → Returns false when not found
- `testDeleteCountryByCode_NullCode` → Throws exception on null code
- `testDeleteCountryByCode_EmptyCode` → Throws exception on empty code

### Get 3-Digit Country Code (4 tests)
- `testGet3DigitCountryCode_Success` → Returns country with ISO3 code
- `testGet3DigitCountryCode_NotFound` → Returns null when not found
- `testGet3DigitCountryCode_NullCode` → Throws exception on null code
- `testGet3DigitCountryCode_EmptyCode` → Throws exception on empty code

### Convert Country Name to ISO3 (6 tests)
- `testConvertCountryNameToIso3_FromCache` → Uses cached conversion
- `testConvertCountryNameToIso3_FromDatabase` → Looks up from database
- `testConvertCountryNameToIso3_NotFound` → Returns null when not found
- `testConvertCountryNameToIso3_NullInput` → Returns null on null input
- `testConvertCountryNameToIso3_EmptyInput` → Returns null on empty input
- `testConvertCountryNameToIso3_WhitespaceOnly` → Returns null on whitespace

### Convert Country Name to ISO2 (7 tests)
- `testConvertCountryNameToIso2_FromCache` → Uses cached conversion
- `testConvertCountryNameToIso2_AlreadyISO2` → Returns input if already ISO2
- `testConvertCountryNameToIso2_FromISO3` → Converts ISO3 to ISO2
- `testConvertCountryNameToIso2_FromDatabase` → Looks up from database
- `testConvertCountryNameToIso2_NotFound` → Returns null when not found
- `testConvertCountryNameToIso2_NullInput` → Returns null on null input
- `testConvertCountryNameToIso2_EmptyInput` → Returns null on empty input

### Get Countries Count (2 tests)
- `testGetCountriesCount_Success` → Returns correct count
- `testGetCountriesCount_Zero` → Returns zero when empty

### Clear All Countries (1 test)
- `testClearAllCountries_Success` → Deletes all countries

---

## ProductService (18 tests)

### Get All Products (3 tests)
- `testGetAllProducts_Success` → Returns list of products
- `testGetAllProducts_EmptyList` → Returns empty list
- `testGetAllProducts_RepositoryException` → Handles database errors

### Get Product By HS Code (4 tests)
- `testGetProductByHsCode_Success` → Returns product by HS code
- `testGetProductByHsCode_NotFound` → Throws exception when not found
- `testGetProductByHsCode_CaseSensitivity` → Handles case sensitivity
- `testGetProductByHsCode_RepositoryException` → Handles database errors

### Save Product (2 tests)
- `testSaveProduct_Success` → Saves product successfully
- `testSaveProduct_NullProduct` → Throws exception on null product

### Find By HS Code (2 tests)
- `testFindByHsCode_Success` → Returns Optional with product
- `testFindByHsCode_NotFound` → Returns empty Optional

### Count Products (2 tests)
- `testCountProducts_Success` → Returns correct count
- `testCountProducts_Zero` → Returns zero when empty

### Delete Product (3 tests)
- `testDeleteProduct_Success` → Deletes product successfully
- `testDeleteById_Success` → Deletes by ID successfully
- `testDeleteAll_Success` → Deletes all products

### Exists Tests (2 tests)
- `testExistsById_True` → Returns true when exists
- `testExistsById_False` → Returns false when doesn't exist

---

## TariffValidationService (37 tests)

### Null Request (1 test)
- `testValidateTariffRequest_NullRequest` → Returns error for null request

### Valid Request (3 tests)
- `testValidateTariffRequest_Success` → Validates complete valid request
- `testIsValidRequest_True` → Returns true for valid request
- `testIsValidRequest_False` → Returns false for invalid request

### Missing Required Fields (5 tests)
- `testValidateTariffRequest_MissingImportingCountry` → Error when importing country is null
- `testValidateTariffRequest_BlankImportingCountry` → Error when importing country is blank
- `testValidateTariffRequest_MissingExportingCountry` → Error when exporting country is null
- `testValidateTariffRequest_MissingHsCode` → Error when HS code is null
- `testValidateTariffRequest_BlankHsCode` → Error when HS code is blank

### Defaulted Fields (11 tests)
- `testValidateTariffRequest_DefaultProductValue` → Defaults product value to $100
- `testValidateTariffRequest_NegativeProductValue` → Corrects negative to $100
- `testValidateTariffRequest_ZeroProductValue` → Corrects zero to $100
- `testValidateTariffRequest_DefaultRooEligible` → Defaults ROO to false
- `testValidateTariffRequest_DefaultShippingMode` → Defaults shipping mode to SEA
- `testValidateTariffRequest_DefaultFreight` → Defaults freight to $0
- `testValidateTariffRequest_DefaultInsurance` → Defaults insurance to $0
- `testValidateTariffRequest_DefaultHeadsAndWeight` → Defaults both to 1
- `testValidateTariffRequest_NegativeHeads` → Corrects negative heads to 1
- `testValidateTariffRequest_NegativeWeight` → Corrects negative weight to 1.0

### Country Validation (4 tests)
- `testValidateTariffRequest_InvalidImportingCountry` → Error for unknown importing country
- `testValidateTariffRequest_InvalidExportingCountry` → Error for unknown exporting country
- `testValidateTariffRequest_CountryByName` → Resolves country by name
- `testValidateTariffRequest_CountryCodeCaseInsensitive` → Handles lowercase country codes

### HS Code Validation (7 tests)
- `testValidateTariffRequest_ValidHsCode6Digits` → Accepts 6-digit HS code
- `testValidateTariffRequest_ValidHsCode10Digits` → Accepts 10-digit HS code
- `testValidateTariffRequest_HsCodeWithNonDigits` → Cleans non-digit characters
- `testValidateTariffRequest_InvalidHsCodeTooShort` → Error for HS code < 6 digits
- `testValidateTariffRequest_InvalidHsCodeTooLong` → Error for HS code > 10 digits
- `testValidateTariffRequest_HsCodeWithLetters` → Error for HS code with letters

### Resolve To Alpha2 (6 tests)
- `testResolveToAlpha2_ByCode` → Resolves country by code
- `testResolveToAlpha2_ByName` → Resolves country by name
- `testResolveToAlpha2_NotFound` → Returns empty for unknown country
- `testResolveToAlpha2_NullInput` → Returns empty for null input
- `testResolveToAlpha2_BlankInput` → Returns empty for blank input
- `testResolveToAlpha2_EmptyInput` → Returns empty for empty input

### Multiple Errors & Cache (2 tests)
- `testValidateTariffRequest_MultipleErrors` → Returns all validation errors
- `testResolveToAlpha2_MultipleCalls` → Multiple calls work correctly

---

## Summary

| Component | Tests | 
|-----------|-------|
| **Controllers** | |
| AuthController | 20 |
| UserController | 32 |
| ProductController | 19 |
| TariffController | 9 |
| CountryController | 15 |
| **Services** | |
| TariffCalculatorService | 20 |
| CountryService | 35 |
| ProductService | 18 |
| TariffValidationService | 37 |
| **TOTAL** | **205** |