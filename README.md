# OrderHub — Security Layer

Microservice security layer for an order and client management application. Implements a Gateway, Google OAuth2 authentication, JWT, and a single entry point for all services.

## Stack

- **Java 21**, **Spring Boot 3.2**, **Maven**
- **authorization-server** — OAuth2 Client (Google), JWT issuance, `/profile` endpoint
- **gateway** — Spring Cloud Gateway, routing, JWT validation, CORS
- External dependencies: **java-backend** (orders/clients API), **frontend** (React), **PostgreSQL**

## Repository structure

```
orderhub-security/
├── authorization-server/     # OAuth2 + JWT, port 9000
├── gateway/                  # Single entry point, port 8080
├── k8s/                      # GKE manifests (deployments, services, ingress)
├── .github/workflows/        # CI/CD: build images and deploy on push to master
├── docker-compose.yml        # Local run of the full stack
├── .env.example              # Example environment variables
└── pom.xml                   # Parent POM (modules: authorization-server, gateway)
```

## Implemented (assignment requirements)

1. **Gateway** — access to all microservices via a single base URL and port (8080). Routes: `/auth/**` → authorization-server, `/api/**` → java-backend, everything else → frontend.
2. **Google authentication** — the app is unavailable without signing in via Google. Implemented in a separate authorization-server microservice (Spring Security OAuth2 Client).
3. **API security** — the gateway validates JWT in the `Authorization` header; returns 401 for missing or invalid token. Forwards `X-User-Email` and `X-User-Name` headers to backends.
4. **Endpoint `/profile`** — returns the user’s name and data from the JWT (or from the OAuth2 session on first login). Implemented in authorization-server.
5. **Frontend integration** — the frontend calls `/auth/profile`; on 401 it shows the login screen. The Login button goes to authorization-server → Google → after login, redirects back to the frontend with the token in the URL; the frontend stores the token and sends it in the header on subsequent requests.
6. **Docker** — images for authorization-server and gateway (multi-stage build, JRE); docker-compose runs the full stack (auth-server, gateway, postgres, java-backend, frontend) with healthchecks and environment variables.
7. **Kubernetes (GKE)** — manifests in `k8s/`: namespace, postgres, deployments and services for all services, Ingress (GCE). Gateway uses profile `k8s` with URIs via service names.
8. **CI/CD** — GitHub Actions (`.github/workflows/build-and-deploy.yml`): on push to master/main — build images, push to GCR, update deployment images in GKE and rollout.

## Prerequisites

- **Docker** and **Docker Compose**
- **Google OAuth 2.0** credentials: [Google Cloud Console](https://console.cloud.google.com/) → your project → **APIs & Services** → **Credentials** → **Create Credentials** → **OAuth 2.0 Client ID** → Application type: **Web application** → add Authorized redirect URIs (see below)
- **java-backend** and **frontend** — separate projects (Spring Boot REST API and React app); their paths are used in `docker-compose.yml` for build context

## Running locally

1. Clone the repository (or download the code).
2. Copy `.env.example` to `.env` and set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` (from Google Cloud Console, OAuth 2.0 Client).
3. In Google Console, in your OAuth 2.0 client, add to **Authorized redirect URIs**:
   - `http://localhost:8080/auth/login/oauth2/code/google`
   - `http://localhost:8080/login/oauth2/code/google`
4. In `docker-compose.yml`, set build contexts for **java-backend** and **frontend** to the paths where your backend and frontend projects lie (see comments in the file). If you already have images, you can use `image:` instead of `build:`.
5. From the repository root:
   ```bash
   docker compose up -d
   ```
6. Open in a browser: **http://localhost:3000** — frontend; sign in via “Login with Google”.

Stop: `docker compose down`.

## Environment variables

- **authorization-server:** `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET`, `FRONTEND_LOGIN_CALLBACK`, `GOOGLE_REDIRECT_URI` (for Docker, set in docker-compose).
- **gateway:** `JWT_SECRET`, `AUTHORIZATION_SERVER_URI`, `JAVA_BACKEND_URI`, `FRONTEND_URI` (in Docker — service names).
- See `.env.example`.

## Cloud deployment (GKE)

The manifests in `k8s/` are intended for deployment on Google Kubernetes Engine. To apply them you need: a GKE cluster, secrets in the cluster (`kubectl create secret generic app-secrets -n orderhub ...`), images pushed to GCR and referenced in the manifests’ `image:` fields, and a domain in the Ingress. Details are in the manifest comments and in `k8s/secrets.yaml.example`.

---

## Why cloud deployment was not completed

Cloud deployment to Google Cloud Platform (GKE) was not completed because it was not possible to attach a billing account. After multiple unsuccessful attempts to add a payment method and enable billing in Google Cloud Console, the operation did not succeed (errors on the service side or region/card restrictions). Creating a GKE cluster requires an active billing account, so all steps related to cloud deployment (creating the cluster, pushing images to GCR, applying manifests in GKE) were left undone.

The full flow is implemented and tested locally: Gateway, Google authentication, JWT, `/profile`, frontend and backend run via Docker Compose. Kubernetes manifests and the CI/CD workflow are in place and can be used once billing is successfully enabled and a GKE cluster is created.
