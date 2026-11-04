import { useState } from "react";

export function Login({ onLogin, busy, error }) {
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");

  function handleSubmit(e) {
    e.preventDefault();
    onLogin(code, password);
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="brand-mark">U</div>
        <p className="eyebrow">UNIVERSIDAD DEL MAGDALENA</p>
        <h1>Gestión Unimag</h1>
        <p className="muted">Control de uso, cumplimiento y recursos</p>

        <form onSubmit={handleSubmit}>
          <label>
            Código institucional
            <input
              autoComplete="username"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="Ej. 202610001"
              required
            />
          </label>

          <label>
            Contraseña
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>

          {error && <p className="error">{error}</p>}

          <button className="primary full" disabled={busy}>
            {busy ? "Ingresando…" : "Ingresar"}
          </button>
        </form>

        <p className="hint">
          Las cuentas demo se configuran en el archivo .env del entorno local.
        </p>
      </section>
    </main>
  );
}
