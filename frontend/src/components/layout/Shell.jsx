const STUDENT_TABS = [
  ["overview", "Mi cumplimiento"],
  ["checkout", "Registrar devolución"],
  ["damage", "Reportar daño"],
];

const ADMIN_TABS = [
  ["overview", "Panel de control"],
  ["absences", "Inasistencias"],
  ["charges", "Cobros y costos"],
  ["students", "Estudiantes"],
];

export function Shell({ user, tab, setTab, onLogout, children }) {
  const tabs =
    user.role === "PERSONAL_ADMINISTRATIVO" ? ADMIN_TABS : STUDENT_TABS;
  const currentTabLabel = tabs.find((t) => t[0] === tab)?.[1] || "";

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="side-brand">
          <span className="brand-mark small">U</span>
          <div>
            <b>Gestión Unimag</b>
            <small>Módulo 3</small>
          </div>
        </div>

        <div className="side-label">ESPACIO DE TRABAJO</div>

        <nav>
          {tabs.map(([id, label]) => (
            <button
              key={id}
              className={tab === id ? "nav-item active" : "nav-item"}
              onClick={() => setTab(id)}
            >
              <span className="nav-dot" />
              {label}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="user-chip">
            <div className="avatar">
              {user.code.slice(0, 1).toUpperCase()}
            </div>
            <div>
              <b>{user.code}</b>
              <small>
                {user.role === "PERSONAL_ADMINISTRATIVO"
                  ? "Administración"
                  : "Estudiante"}
              </small>
            </div>
          </div>
          <button className="logout" onClick={onLogout}>
            Cerrar sesión <span>↗</span>
          </button>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div>
            <span className="breadcrumb">Gestión Unimag</span>
            <span className="crumb-sep">/</span>
            <b>{currentTabLabel}</b>
          </div>
          <span className="connection">
            <i /> Sistema activo
          </span>
        </header>

        <div className="page-content">{children}</div>
      </main>
    </div>
  );
}
