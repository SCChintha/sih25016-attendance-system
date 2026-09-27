
  # SmartAttend

  SmartAttend is a college attendance management system. The current repository contains the existing React/Vite dashboard prototype and the Phase 2 Spring Boot database foundation.

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

  For a deployed MySQL environment, keep the `mysql` Spring profile and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`, then run with `--spring.profiles.active=mysql`. That profile uses Flyway migrations from `backend/src/main/resources/db/migration` and validates the existing schema.

  Run persistence tests with `mvn test` from `backend/`. Tests use an isolated H2 database and do not require MySQL.

  ### Implemented API slice

  - `POST /api/auth/login` validates a BCrypt password and returns a stateless JWT.
  - `POST /api/sessions/start` and `POST /api/sessions/{id}/stop` are faculty-owned operations.
  - `GET /api/sessions/{id}/qr` returns a signed QR token valid for 20 seconds.
  - `POST /api/attendance/mark` validates the open session, QR signature/expiry, student enrollment, and duplicate constraints on the server.

  Send the token as `Authorization: Bearer <token>`. The default JWT secret is for local development only; set `JWT_SECRET` to a random value of at least 32 bytes outside local development.

  The face provider and fingerprint provider are explicit integration boundaries and reject requests until their external service or hardware is configured. Offline synchronization, analytics, exports, and frontend API wiring remain subsequent phases.

  See [docs/ER-diagram.md](docs/ER-diagram.md) for the database relationships.
  