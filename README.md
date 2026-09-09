# vshoba-boutique

## Deploying to Render

1. Push this repo to GitHub (if not already).
2. On Render: **New + → Web Service** → connect this repo.
3. Build command: `./mvnw clean package -DskipTests`
4. Start command: `java -jar target/*.jar`
5. On Render: **New + → PostgreSQL** → create free database.
6. In the Web Service's **Environment** tab, set:
   - `DATABASE_URL` (from the Render Postgres dashboard, formatted as `jdbc:postgresql://<host>:<port>/<dbname>`)
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `SPRING_PROFILES_ACTIVE=prod`
   - `RAZORPAY_KEY` / `RAZORPAY_SECRET` (test mode values initially)
   - `JWT_SECRET` (generate one with `openssl rand -base64 48`)
7. **Deploy.** 
   - Note: free tier sleeps after 15 min inactivity (cold start on next request).
   - The free Postgres database expires after 90 days.