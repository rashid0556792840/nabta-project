# Nabta Backend — Windows 11 Setup

This phase is the Java backend and database foundation. It is a REST API, not yet the customer Android UI. The next phases will add the Ionic/Capacitor frontend and Android wrapper against these endpoints.

## Required software
- JDK 21 or later (Java 21 is the project's compilation target).
- Apache Maven 3.6.3 or later.
- MySQL Server 8.0 and MySQL Workbench 8.0.
- VS Code (Java Extension Pack is helpful).

## 1. Create the database
1. Open MySQL Workbench and connect to your local MySQL server.
2. Open `database/01_create_database.sql` and execute it.
3. Check that the `nabta_db` schema appears in the Schemas panel.
4. Tables are created/updated by Hibernate when the backend starts for the first time. Do not manually create the same tables a second time.

## 2. Configure environment variables (PowerShell)
Open PowerShell in this folder and set these values for the current terminal session (change the database password):
```powershell
$env:NABTA_DB_URL="jdbc:mysql://localhost:3306/nabta_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:NABTA_DB_USERNAME="root"
$env:NABTA_DB_PASSWORD="YOUR_MYSQL_PASSWORD"
$env:NABTA_JWT_SECRET="ReplaceWithARandomSecretThatIsAtLeast32CharactersLong"
$env:NABTA_ADMIN_NAME="Nabta Owner"
$env:NABTA_ADMIN_EMAIL="owner@nabta.local"
$env:NABTA_ADMIN_PASSWORD="Choose-A-Strong-Admin-Password-123!"
$env:NABTA_CORS_ORIGINS="http://localhost:8100,http://localhost:5173,http://localhost:8080,http://localhost,capacitor://localhost,ionic://localhost"
```
The administrator is created automatically on startup only if `NABTA_ADMIN_EMAIL` and `NABTA_ADMIN_PASSWORD` are set and that email does not already exist. Password must be at least 12 characters. Never use the example values in production.

## 3. Run the backend
From this folder:
```powershell
mvn clean spring-boot:run
```
Wait for Spring Boot to report that it has started. Then open `http://localhost:8080/api/health`. It should return `{"status":"UP","application":"Nabta Backend"}`.

To build a runnable JAR:
```powershell
mvn clean package
java -jar target/nabta-backend-1.0.0.jar
```

## 4. Quick test
Register a customer with Postman or VS Code REST Client:
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{"name":"Test Customer","email":"customer@example.com","password":"CustomerPass123!"}
```
Login as admin:
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{"email":"owner@nabta.local","password":"Choose-A-Strong-Admin-Password-123!"}
```
Copy `token` from the response and use it as `Authorization: Bearer <token>` when calling `/api/admin/...`. You must create categories before products.

Optional demo catalog: after the backend starts once, you may run `database/demo_seed.sql` in Workbench. This is sample database content, not hardcoded production data.

## 5. Notes and important limits
- The Android app cannot use `localhost` to reach a backend running on the PC. For a physical device on the same Wi-Fi, use the PC's local IPv4 address (for example `http://192.168.1.20:8080`) and allow the port through Windows Firewall for development. Production must use a deployed HTTPS backend.
- Do not expose MySQL to the internet.
- The default JWT fallback exists for local development only. Set `NABTA_JWT_SECRET` to a strong random secret before use and keep it private.
- Product deletion is a soft deactivation, preserving order history.
- File upload, FCM push delivery, WhatsApp configuration, deployment, and bilingual customer screens are planned for later phases and are not implemented by this backend phase.
- This source package was not compiled in the generation environment because Maven was not installed and internet access to Maven Central was unavailable. Run `mvn clean package` locally; resolve any actual build output before connecting the frontend.
