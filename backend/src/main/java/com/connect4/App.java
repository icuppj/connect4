package com.connect4;

import com.connect4.db.GameRepository;
import com.connect4.game.Connect4Engine;
import com.connect4.game.MoveResult;
import com.connect4.game.Player;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    private static final Pattern PLAYER_PATTERN = Pattern.compile("\"player\"\\s*:\\s*\"(P1|P2)\"");
    private static final Pattern COL_PATTERN = Pattern.compile("\"col\"\\s*:\\s*(-?\\d+)");

    public static void main(String[] args) throws Exception {
        String url = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/connect4?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        );
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "rootpass");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        GameRepository repo = new GameRepository(url, user, password);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/games", exchange -> handleGames(exchange, repo));
        server.setExecutor(null);
        server.start();

        System.out.println("Connect 4 API listening on http://localhost:" + port);
    }

    private static void handleGames(HttpExchange exchange, GameRepository repo) throws IOException {
        addCorsHeaders(exchange);

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // POST /api/games -> create new game
            if ("POST".equalsIgnoreCase(method) && path.equals("/api/games")) {
                long gameId = repo.createGame();
                Connect4Engine game = repo.loadGame(gameId);
                sendJson(exchange, 201, toGameJson(gameId, game));
                return;
            }

            // GET /api/games/{id} -> fetch game state
            if ("GET".equalsIgnoreCase(method) && path.matches("/api/games/\\d+")) {
                long gameId = Long.parseLong(path.substring("/api/games/".length()));
                Connect4Engine game = repo.loadGame(gameId);
                sendJson(exchange, 200, toGameJson(gameId, game));
                return;
            }

            // POST /api/games/{id}/moves -> drop a piece
            if ("POST".equalsIgnoreCase(method) && path.matches("/api/games/\\d+/moves")) {
                String idPart = path.replace("/api/games/", "").replace("/moves", "");
                long gameId = Long.parseLong(idPart);

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Matcher playerMatcher = PLAYER_PATTERN.matcher(body);
                Matcher colMatcher = COL_PATTERN.matcher(body);

                if (!playerMatcher.find() || !colMatcher.find()) {
                    sendJson(exchange, 400, "{\"error\":\"Body must include player (P1 or P2) and col (0-6).\"}");
                    return;
                }

                Player player = Player.valueOf(playerMatcher.group(1));
                int col = Integer.parseInt(colMatcher.group(1));

                MoveResult result = repo.applyMove(gameId, player, col);
                String winnerJson = result.winner() != null ? "\"" + result.winner().name() + "\"" : "null";
                String nextJson = result.nextPlayer() != null ? "\"" + result.nextPlayer().name() + "\"" : "null";

                String json = String.format(
                    "{\"id\":%d,\"row\":%d,\"col\":%d,\"movedBy\":\"%s\",\"status\":\"%s\",\"currentPlayer\":%s,\"winner\":%s,\"board\":\"%s\"}",
                    gameId, result.row(), result.col(), result.movedBy().name(),
                    result.status().name(), nextJson, winnerJson, result.boardString()
                );
                sendJson(exchange, 200, json);
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private static String toGameJson(long id, Connect4Engine game) {
        String winnerJson = game.getWinner() != null ? "\"" + game.getWinner().name() + "\"" : "null";
        String currentJson = game.getStatus().name().equals("IN_PROGRESS")
            ? "\"" + game.getCurrentPlayer().name() + "\""
            : "null";

        return String.format(
            "{\"id\":%d,\"status\":\"%s\",\"currentPlayer\":%s,\"winner\":%s,\"board\":\"%s\"}",
            id, game.getStatus().name(), currentJson, winnerJson, game.toBoardString()
        );
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}