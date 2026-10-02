
  # SmartAttend

  SmartAttend is a college attendance management system built with React/Vite, Spring Boot 3, Java 17, Maven, Spring Security, JPA, Flyway, and H2/MySQL.

  ## Project layout

  ```text
  frontend/   React + Vite JavaScript/JSX application
  backend/    Spring Boot + Maven API and database layer
  docs/       Architecture documentation
  ```

  ## Frontend

  ```bash
  cd frontend
  npm install
  npm run dev
  ```

  The frontend uses the backend authentication API. Set `VITE_API_BASE_URL` in `frontend/.env` when the backend is not running at `http://localhost:8080/api`.

  Authentication is real: register at `/register`, sign in at `/login`, and the session is restored with `GET /api/auth/me`. The current browser client stores the JWT in `localStorage` because the API is stateless; use an httpOnly secure cookie before production deployment.

  Frontend source files use `.jsx` for React components and `.js` for JavaScript/configuration files.

  ## Local backend

  Prerequisites: Java 17+ and Maven 3.9+.

  Run the backend from `backend/`:

  ```bash
  mvn spring-boot:run
  ```

  Local development uses an H2 file database in `backend/data/` and creates or updates its schema automatically. The database directory is ignored by Git.

  For a deployed MySQL environment, provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`, then run with `--spring.profiles.active=mysql`. That profile applies Flyway migrations from `backend/src/main/resources/db/migration` and validates the resulting schema. New databases receive all migrations. For a database already created from the old V1/V2 schema while Flyway was disabled, baseline it at version 2 once before starting the profile so Flyway applies V3 and V4 without recreating existing tables. Migrations are forward-only so attendance and account history remain intact.

  ### Secure initial administrator

  Admin accounts cannot be created through public registration. For a fresh installation, start the backend once with these environment variables set; the bootstrap creates the account only if that email does not already exist:

  ```powershell
  $env:APP_BOOTSTRAP_ADMIN_ENABLED = "true"
  $env:SUPER_ADMIN_NAME = "System Administrator"
  $env:SUPER_ADMIN_EMAIL = "admin@your-school.edu"
  $env:SUPER_ADMIN_PASSWORD = "use-a-unique-password-of-at-least-12-characters"
  mvn spring-boot:run
  ```

  The password is BCrypt-hashed. After first setup, unset the bootstrap variables. Never commit these secrets.

  ### Admin and faculty workflows

  - Admin user management supports server-side search, role filtering, pagination, profile edits, suspension, reactivation, and soft deletion. Suspension and deletion increment the account token version; old JWTs stop working immediately.
  - Admin academic setup manages grade levels, per-grade streams, and active subjects mapped to a grade/stream.
  - Faculty choose active catalog subjects and use **Confirm & Save** to lock their selection. Only an administrator can unlock or replace a locked selection.
  - Student registration requires an active grade and stream. Faculty registration creates only a faculty profile; public registration cannot create an administrator.

  The schema includes user and token lifecycle fields, grade and stream catalog tables, subject-to-stream mappings, faculty subject locks, section-level faculty/subject assignments, explicit present/absent/excused attendance records, leave requests, notifications, configurable attendance thresholds, and optional geolocation data. See [docs/ER-diagram.md](docs/ER-diagram.md).

  Run persistence tests with `mvn test` from `backend/`. Tests use an isolated H2 database and do not require MySQL.

  ### Implemented API slice

  - `POST /api/auth/login` validates a BCrypt password and returns a stateless JWT.
  - `POST /api/sessions/start` and `POST /api/sessions/{id}/stop` are faculty-owned operations.
  - `GET /api/sessions/{id}/qr` returns a signed QR token valid for 20 seconds.
  - `POST /api/attendance/mark` validates the open session, QR signature/expiry, student enrollment, and duplicate constraints on the server.

  Send the token as `Authorization: Bearer <token>`. The default JWT secret is for local development only; set `JWT_SECRET` to a random value of at least 32 bytes outside local development.

  The face provider and fingerprint provider are explicit integration boundaries and reject requests until their external service or hardware is configured.

  See [SmartAttend-Design.md](SmartAttend-Design.md) for the end-to-end design and integration rules, and [docs/ER-diagram.md](docs/ER-diagram.md) for the database relationships.
