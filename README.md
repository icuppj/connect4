# Connect 4 — Full Stack Assignment

A self-contained Connect 4 web application built with **Java**, **React**, **MySQL**, and **Docker**.

## Tech Stack
- **Backend:** Java 21 (built-in `HttpServer`, pure game engine, JDBC)
- **Frontend:** React + Vite (served via Nginx in Docker)
- **Database:** MySQL 8.4 (`games` and `moves` tables)
- **Infrastructure:** Docker & Docker Compose (plus Kubernetes manifests in `k8s/` if included)

---

## Quick Start (Single Command)

**Prerequisites:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

From the project root, run:

```bash
docker compose up --build
```

Once the containers are healthy:
- **Play the game:** http://localhost:3000
- **Backend API:** http://localhost:8080

To stop and remove the containers:

```bash
docker compose down
```

*(To reset the database volume as well, run `docker compose down -v`.)*

---

## API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/games` | Create a new 6×7 Connect 4 game |
| `GET` | `/api/games/{id}` | Fetch current board state, turn, and status |
| `POST` | `/api/games/{id}/moves` | Drop a piece (`{"player": "P1", "col": 3}`) |

---

## Main Decisions & Trade-Offs

- **Server-authoritative game engine:** All drop calculations, turn validation, and win/draw checks live in Java (`Connect4Engine`) rather than the browser. This guarantees valid state and clean MySQL persistence at the cost of a small HTTP round-trip per move.
- **Zero-framework Java HTTP server:** Used Java's built-in `com.sun.net.httpserver.HttpServer` and direct JDBC transactions (`SELECT ... FOR UPDATE`) instead of Spring Boot to keep startup fast, dependencies minimal, and Docker builds lightweight within a short hour window.
- **Docker Compose for local run:** Prioritized `docker compose up --build` for reliable one-command local evaluation, keeping the database schema auto-initialized via `/docker-entrypoint-initdb.d/schema.sql`.

---

## What I Would Do Next With More Time

- **Real-time multiplayer:** Add WebSockets so two players can play from separate browsers with live updates.
- **AI opponent:** Implement a bot for single-player mode.
- **Move history & undo:** Expose `GET /api/games/{id}/moves` in the UI to step through or replay past turns, and highlight the winning 4 discs.
---

## Time Breakdown

| Area | Time Spent |
| :--- | :--- |
| MySQL schema & Docker DB setup | ~40 min |
| Java game engine, JDBC repository & REST API | ~60 min |
| React UI & styling | ~40 min |
| Dockerfiles, Docker Compose & README | ~30 min |
| **Total** | **~2 hrs 50 min** |

---

## AI Usage

AI tools were used to assist with scaffolding, debugging Maven/Docker configuration, and drafting boilerplate. Full conversation/agent transcripts are included in this link https://share.gemini.google/TbWaALdRrzNB.
