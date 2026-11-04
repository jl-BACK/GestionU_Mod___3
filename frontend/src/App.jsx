import { useCallback, useEffect, useState } from "react";
import { api, json } from "./api.js";
import { Shell } from "./components/layout/Shell.jsx";
import { Login } from "./features/auth/Login.jsx";
import { StudentOverview } from "./features/student/StudentOverview.jsx";
import { StudentDamage } from "./features/student/StudentDamage.jsx";
import { AdminOverview } from "./features/admin/AdminOverview.jsx";
import { AdminAbsences } from "./features/admin/AdminAbsences.jsx";
import { AdminCharges } from "./features/admin/AdminCharges.jsx";
import { AdminStudents } from "./features/admin/AdminStudents.jsx";

export default function App() {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("gestion-user"));
    } catch {
      return null;
    }
  });

  const [tab, setTab] = useState("overview");
  const [data, setData] = useState(null);
  const [rankings, setRankings] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(null);

  const flash = useCallback((message, isError = false) => {
    setToast({ message, isError });
    window.setTimeout(() => setToast(null), 4500);
  }, []);

  const loadStudent = useCallback(async () => {
    const response = await api("/me/compliance");
    setData(response);
  }, []);

  const loadAdmin = useCallback(
    async (from = "", to = "", resourceType = "", faculty = "") => {
      const params = new URLSearchParams();
      if (from) params.set("from", from);
      if (to) params.set("to", to);
      if (resourceType) params.set("resourceType", resourceType);
      if (faculty) params.set("faculty", faculty);

      const query = params.toString() ? `?${params.toString()}` : "";
      const [overview, top] = await Promise.all([
        api(`/admin/analytics/overview${query}`),
        api(`/admin/analytics/rankings${query}`),
      ]);

      setData(overview);
      setRankings(top);
    },
    []
  );

  const reload = useCallback(
    async (from, to, resourceType, faculty) => {
      if (user?.role === "PERSONAL_ADMINISTRATIVO") {
        await loadAdmin(from, to, resourceType, faculty);
      } else {
        await loadStudent();
      }
    },
    [user, loadAdmin, loadStudent]
  );

  useEffect(() => {
    if (user) {
      reload().catch((err) => flash(err.message, true));
    }
  }, [user, reload, flash]);

  async function handleLogin(code, password) {
    setBusy(true);
    setError("");

    try {
      const result = await api("/auth/login", {
        method: "POST",
        body: json({ code, password }),
      });

      const nextUser = { code: result.code, role: result.role };
      localStorage.setItem("gestion-token", result.accessToken);
      localStorage.setItem("gestion-user", JSON.stringify(nextUser));

      setUser(nextUser);
      setTab("overview");
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  function handleLogout() {
    localStorage.removeItem("gestion-token");
    localStorage.removeItem("gestion-user");
    setUser(null);
    setData(null);
  }

  if (!user) {
    return <Login onLogin={handleLogin} busy={busy} error={error} />;
  }

  let content;
  if (user.role === "PERSONAL_ADMINISTRATIVO") {
    switch (tab) {
      case "absences":
        content = <AdminAbsences flash={flash} reload={reload} />;
        break;
      case "charges":
        content = <AdminCharges flash={flash} />;
        break;
      case "students":
        content = <AdminStudents flash={flash} />;
        break;
      default:
        content = (
          <AdminOverview
            data={data}
            rankings={rankings}
            reload={loadAdmin}
            flash={flash}
          />
        );
    }
  } else {
    switch (tab) {
      case "checkout":
        content = <StudentOverview data={data} reload={reload} flash={flash} />;
        break;
      case "damage":
        content = <StudentDamage reload={reload} flash={flash} />;
        break;
      default:
        content = <StudentOverview data={data} reload={reload} flash={flash} />;
    }
  }

  return (
    <Shell
      user={user}
      tab={tab}
      setTab={setTab}
      onLogout={handleLogout}
    >
      {content}
      {toast && (
        <div className={toast.isError ? "toast error-toast" : "toast"}>
          {toast.message}
        </div>
      )}
    </Shell>
  );
}
