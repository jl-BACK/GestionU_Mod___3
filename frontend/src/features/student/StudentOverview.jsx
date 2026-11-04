import { useState } from "react";
import { api, json } from "../../api.js";
import { MetricCard } from "../../components/ui/MetricCard.jsx";
import { SectionTitle } from "../../components/ui/SectionTitle.jsx";
import { Table } from "../../components/ui/Table.jsx";
import { Empty } from "../../components/ui/Empty.jsx";
import { money, date } from "../../utils/formatters.js";

export function StudentOverview({ data, reload, flash }) {
  const [reservationId, setReservationId] = useState("");
  const [hasDamage, setHasDamage] = useState(false);
  const [damageDescription, setDamageDescription] = useState("");
  const [busy, setBusy] = useState(false);

  const score = Number(data?.score ?? 50);

  async function handleCheckoutSubmit(e) {
    e.preventDefault();
    setBusy(true);

    try {
      const payload = {
        reservationId: reservationId.trim(),
        hasDamage,
        damageDescription: hasDamage ? damageDescription.trim() : null,
      };

      const result = await api("/checkouts", {
        method: "POST",
        body: json(payload),
      });

      flash(result.message || "Devolución registrada exitosamente.");
      setReservationId("");
      setDamageDescription("");
      setHasDamage(false);
      await reload();
    } catch (err) {
      flash(err.message, true);
    } finally {
      setBusy(false);
    }
  }

  const currentDateLabel = new Date().toLocaleDateString("es-CO", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  const activeSanctionsCount = data?.activeSanctions?.length || 0;
  const lateCheckoutsCount =
    data?.checkouts?.filter((x) => x.result === "CON_RETRASO").length || 0;
  const pendingChargesTotal = (data?.charges || [])
    .filter((x) => x.status === "PENDIENTE")
    .reduce((acc, curr) => acc + Number(curr.amount || 0), 0);

  const recentCheckoutRows = (data?.checkouts || []).slice(0, 8).map((item) => [
    item.reservationId,
    item.resourceName || item.resourceId,
    date(item.checkedOutAt),
    <span
      key="result"
      className={item.result === "A_TIEMPO" ? "status good" : "status warning"}
    >
      {item.result}
    </span>,
    item.delayMinutes ? `${item.delayMinutes} min` : "—",
  ]);

  const chargesRows = (data?.charges || []).map((charge, idx) => [
    charge.reason,
    charge.resourceName || charge.resourceId,
    money(charge.amount),
    <span
      key={idx}
      className={
        charge.status === "PENDIENTE" ? "status warning" : "status good"
      }
    >
      {charge.status}
    </span>,
  ]);

  return (
    <>
      <SectionTitle
        eyebrow="RESUMEN PERSONAL"
        title={`Hola, ${data?.name || "estudiante"}`}
        extra={<span className="date-pill">{currentDateLabel}</span>}
      />

      <div className="metrics">
        <MetricCard
          label="Puntaje de confianza"
          value={`${score} pts`}
          helper={
            score === 0
              ? "Cuenta bloqueada para nuevas reservas"
              : "Tu devolución suma al buen historial"
          }
          tone={score === 0 ? "red" : "blue"}
        />
        <MetricCard
          label="Sanciones activas"
          value={activeSanctionsCount}
          helper={score === 0 ? "Incluye bloqueo por score" : "Restricciones vigentes"}
          tone="amber"
        />
        <MetricCard
          label="Devoluciones tardías"
          value={lateCheckoutsCount}
          helper="Las devoluciones detienen descuentos"
          tone="green"
        />
        <MetricCard
          label="Cobros pendientes"
          value={money(pendingChargesTotal)}
          helper="El pago se realiza por fuera del sistema"
          tone="violet"
        />
      </div>

      <div className="content-grid">
        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">CUMPLIMIENTO</p>
              <h2>Restricciones vigentes</h2>
            </div>
            <span className="pill">{activeSanctionsCount}</span>
          </div>

          {data?.activeSanctions?.length ? (
            <div className="stack-list">
              {data.activeSanctions.map((sanc, idx) => (
                <div className="list-row" key={idx}>
                  <div className="list-icon amber">!</div>
                  <div className="grow">
                    <b>{sanc.reason}</b>
                    <small>
                      Desde {sanc.startsOn}
                      {sanc.endsOn
                        ? ` · Hasta ${sanc.endsOn}`
                        : " · Sin fecha de finalización"}
                    </small>
                  </div>
                  <span className="status warning">{sanc.scope}</span>
                </div>
              ))}
            </div>
          ) : (
            <Empty>No tienes restricciones activas.</Empty>
          )}
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">ACCIÓN RÁPIDA</p>
              <h2>Registrar devolución</h2>
            </div>
            <span className="panel-symbol">↙</span>
          </div>

          <form className="form-stack" onSubmit={handleCheckoutSubmit}>
            <label>
              ID de reserva
              <input
                value={reservationId}
                onChange={(e) => setReservationId(e.target.value)}
                placeholder="Identificador informado por M2"
                required
              />
            </label>

            <label className="check-line">
              <input
                type="checkbox"
                checked={hasDamage}
                onChange={(e) => setHasDamage(e.target.checked)}
              />
              Reportar una novedad técnica
            </label>

            {hasDamage && (
              <label>
                Describe el daño
                <textarea
                  rows={3}
                  value={damageDescription}
                  onChange={(e) => setDamageDescription(e.target.value)}
                  placeholder="Detalla los desperfectos encontrados..."
                  required
                />
              </label>
            )}

            <button className="primary" disabled={busy}>
              {busy ? "Registrando…" : "Registrar check-out"}
            </button>
            <p className="hint">
              Puedes devolver recursos aunque tengas una sanción o el score esté en cero.
            </p>
          </form>
        </section>
      </div>

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">ACTIVIDAD</p>
            <h2>Historial reciente</h2>
          </div>
        </div>
        <Table
          headers={["Reserva", "Recurso", "Fecha", "Resultado", "Retraso"]}
          rows={recentCheckoutRows}
          empty="Aún no tienes devoluciones registradas."
        />
      </section>

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">FINANZAS</p>
            <h2>Mis cobros</h2>
          </div>
        </div>
        <Table
          headers={["Concepto", "Recurso", "Monto", "Estado"]}
          rows={chargesRows}
          empty="No presentas cobros pendientes ni históricos."
        />
      </section>
    </>
  );
}
