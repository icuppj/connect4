import { useEffect, useState } from "react";
import "./App.css";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";
const ROWS = 6;
const COLS = 7;

export default function App() {
  const [game, setGame] = useState(null);
  const [hoveredCol, setHoveredCol] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function startNewGame() {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${API_BASE}/api/games`, { method: "POST" });
      if (!res.ok) throw new Error("Failed to create game");
      const data = await res.json();
      setGame(data);
    } catch (err) {
      setError(err.message || "Could not reach backend API.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    startNewGame();
  }, []);

  async function handleColumnClick(col) {
    if (!game || game.status !== "IN_PROGRESS" || loading) return;

    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${API_BASE}/api/games/${game.id}/moves`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ player: game.currentPlayer, col }),
      });

      const data = await res.json();
      if (!res.ok) {
        setError(data.error || "Invalid move");
        return;
      }

      setGame(data);
    } catch {
      setError("Network error while submitting move.");
    } finally {
      setLoading(false);
    }
  }

  function renderStatus() {
    if (!game) return "Starting game...";
    if (game.status === "WON") {
      return `Player ${game.winner === "P1" ? "1 (Red)" : "2 (Yellow)"} wins!`;
    }
    if (game.status === "DRAW") {
      return "It's a draw!";
    }
    return `Current Turn: Player ${game.currentPlayer === "P1" ? "1 (Red)" : "2 (Yellow)"}`;
  }

  const boardChars = game?.board?.split("") || Array(ROWS * COLS).fill(".");

  return (
    <div className="app">
      <header className="header">
        <h1>Connect 4</h1>
        <p className={`status ${game?.status?.toLowerCase() || ""}`}>
          {renderStatus()}
        </p>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <div className="board-wrapper">
        <div className="board" onMouseLeave={() => setHoveredCol(null)}>
          {Array.from({ length: ROWS }).map((_, r) =>
            Array.from({ length: COLS }).map((__, c) => {
              const cell = boardChars[r * COLS + c];
              const isHovered = hoveredCol === c && game?.status === "IN_PROGRESS";
              const discClass =
                cell === "1" ? "p1" : cell === "2" ? "p2" : "empty";

              return (
                <button
                  key={`${r}-${c}`}
                  className={`cell ${isHovered ? "col-hover" : ""}`}
                  onMouseEnter={() => setHoveredCol(c)}
                  onClick={() => handleColumnClick(c)}
                  disabled={loading || game?.status !== "IN_PROGRESS"}
                  aria-label={`Row ${r + 1}, Column ${c + 1}`}
                >
                  <span className={`disc ${discClass}`} />
                </button>
              );
            })
          )}
        </div>
      </div>

      <div className="controls">
        <button className="new-game-btn" onClick={startNewGame} disabled={loading}>
          New Game
        </button>
        {game && <span className="game-id">Game #{game.id}</span>}
      </div>
    </div>
  );
}