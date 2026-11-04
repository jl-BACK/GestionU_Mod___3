import { useState } from "react";
import { BASE } from "../../api.js";
import { MetricCard } from "../../components/ui/MetricCard.jsx";
import { SectionTitle } from "../../components/ui/SectionTitle.jsx";
import { Table } from "../../components/ui/Table.jsx";
import { money, date } from "../../utils/formatters.js";

export function AdminOverview({ data, rankings, reload, flash }) {
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [resourceType, setResourceType] = useState("");
  const [faculty, setFaculty] = useState("");

  async function handleFilterSubmit(e) {
    e.preventDefault();
    try {
      await reload(from, to, resourceType, faculty);
    } catch (err) {
      flash(err.message, true);
    }
  }

  async function handleDownloadCsv() {
    try {
      const params = new URLSearchParams();
      if (from) params.set("from", from);
      if (to) params.set("to", to);
      if (resourceType) params.set("resourceType", resourceType);
      if (faculty) params.set("faculty", faculty);

      const token = localStorage.getItem("gestion-token");
      const response = await fetch(`${BASE}/admin/analytics/export?${params.toString()}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.message || "No fue posible exportar los datos.");
      }

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "gestion-unimag-analytics.csv";
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      flash(err.message, true);
    }
  }

  const damageRankingRows = (rankings?.damage || []).map((x) => [
    x.resource,
    x.count,
  ]);

  const absenceRankingRows = (rankings?.absences || []).map((x) => [
    x.space,
    x.count,
  ]);

  return (
    <>
      <SectionTitle
        eyebrow="ANALÍTICA DE CUMPLIMIENTO"
        title="Panel de control"
        extra={
          <button className="secondary" onClick={handleDownloadCsv}>
            Descargar CSV ↓
          </button>
        }
      />

      <form className="filter-row" onSubmit={handleFilterSubmit}>
        <label>
          Desde
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
          />
        </label>
        <label>
          Hasta
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
          />
        </label>
        <label>
          Tipo de recurso
          <input
            placeholder="Todos"
            value={resourceType}
            onChange={(e) => setResourceType(e.target.value)}
          />
        </label>
        <label>
          Facultad
          <input
            placeholder="Todas"
            value={faculty}
            onChange={(e) => setFaculty(e.target.value)}
          />
        </label>
        <button className="secondary">Aplicar filtros</button>
      </form>

      <p className="hint update-hint">
        Periodo {data?.from || "—"} a {data?.to || "—"} · Actualizado {date(data?.updatedAt)}
      </p>

      <div className="metrics">
        <MetricCard
          label="Estudiantes registrados"
          value={data?.students ?? 0}
          helper="Personas con historial M3"
        />
        <MetricCard
          label="Bloqueados"
          value={data?.blockedStudents ?? 0}
          helper="Score igual a cero"
          tone="red"
        />
        <MetricCard
          label="Devoluciones a tiempo"
          value={`${data?.onTimePercent ?? 0}%`}
          helper={`${data?.onTimeReturns ?? 0} check-outs`}
          tone="green"
        />
        <MetricCard
          label="Devoluciones tardías"
          value={`${data?.latePercent ?? 0}%`}
          helper={`${data?.lateReturns ?? 0} check-outs`}
          tone="amber"
        />
        <MetricCard
          label="Inasistencias"
          value={data?.sanctionsApplied ?? 0}
          helper="Aceptadas por M2"
          tone="blue"
        />
        <MetricCard
          label="Moras con descuentos"
          value={data?.overdueLoansWithDeductions ?? 0}
          helper={`${data?.lostLoans ?? 0} préstamos perdidos`}
          tone="violet"
        />
        <MetricCard
          label="Reportes técnicos"
          value={data?.damageReports ?? 0}
          helper="En el periodo"
          tone="amber"
        />
        <MetricCard
          label="Cobros pendientes"
          value={data?.pendingCharges ?? 0}
          helper={money(data?.pendingAmountCOP)}
          tone="violet"
        />
      </div>

      <div className="content-grid">
        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">MANTENIMIENTO</p>
              <h2>Recursos con más reportes</h2>
            </div>
          </div>
          <Table
            headers={["Recurso o tipo", "Reportes"]}
            rows={damageRankingRows}
            empty="No hay reportes en el periodo seleccionado."
          />
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">USO DE ESPACIOS</p>
              <h2>Espacios con más inasistencias</h2>
            </div>
          </div>
          <Table
            headers={["Espacio", "Inasistencias"]}
            rows={absenceRankingRows}
            empty="No hay inasistencias en el periodo seleccionado."
          />
        </section>
      </div>
    </>
  );
}
