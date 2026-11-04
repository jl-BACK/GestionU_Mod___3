import { useCallback, useEffect, useState } from "react";
import { api, json } from "../../api.js";
import { SectionTitle } from "../../components/ui/SectionTitle.jsx";
import { Table } from "../../components/ui/Table.jsx";
import { PromptModal } from "../../components/ui/PromptModal.jsx";
import { date } from "../../utils/formatters.js";

export function AdminAbsences({ flash, reload }) {
  const [eligible, setEligible] = useState([]);
  const [recent, setRecent] = useState([]);
  const [busy, setBusy] = useState(false);

  const [modal, setModal] = useState({
    isOpen: false,
    title: "",
    description: "",
    placeholder: "",
    confirmLabel: "Confirmar",
    isDestructive: false,
    required: false,
    onConfirm: () => {},
  });

  const closeModal = () => setModal((prev) => ({ ...prev, isOpen: false }));

  const loadData = useCallback(async () => {
    const [eligibleList, recentList] = await Promise.all([
      api("/absences/eligible"),
      api("/absences"),
    ]);
    setEligible(eligibleList);
    setRecent(recentList);
  }, []);

  useEffect(() => {
    loadData().catch((err) => flash(err.message, true));
  }, [loadData, flash]);

  function handleOpenConfirmModal(reservationId) {
    setModal({
      isOpen: true,
      title: "Confirmar ausencia a reserva",
      description: `Reserva: ${reservationId}. Puedes ingresar una nota opcional de constatación.`,
      placeholder: "Ej. Se verificó el aula tras 30 min y el titular no acudió...",
      confirmLabel: "Confirmar ausencia",
      isDestructive: false,
      required: false,
      onConfirm: async (note) => {
        closeModal();
        setBusy(true);
        try {
          await api("/absences", {
            method: "POST",
            body: json({ reservationId, note: note || null }),
          });
          flash("Confirmación guardada; la sanción espera el acuse de M2.");
          await loadData();
          await reload();
        } catch (err) {
          flash(err.message, true);
        } finally {
          setBusy(false);
        }
      },
    });
  }

  function handleOpenAnnulModal(absenceId) {
    setModal({
      isOpen: true,
      title: "Anular confirmación de inasistencia",
      description: "El motivo es obligatorio para el registro de auditoría.",
      placeholder: "Indica el motivo justificado de la anulación...",
      confirmLabel: "Anular inasistencia",
      isDestructive: true,
      required: true,
      onConfirm: async (reason) => {
        closeModal();
        try {
          await api(`/absences/${absenceId}/annul`, {
            method: "POST",
            body: json({ reason }),
          });
          flash("Inasistencia anulada y auditada correctamente.");
          await loadData();
          await reload();
        } catch (err) {
          flash(err.message, true);
        }
      },
    });
  }

  async function handleRetry(absenceId) {
    try {
      await api(`/absences/${absenceId}/retry`, { method: "POST" });
      flash("Inasistencia reenviada a M2.");
      await loadData();
      await reload();
    } catch (err) {
      flash(err.message, true);
    }
  }

  const eligibleRows = eligible.map((item) => [
    item.spaceName || item.resourceName || item.spaceId || item.resourceId,
    <div key={item.reservationId}>
      <b>{item.holderName || item.holderCode}</b>
      <small className="table-sub">{item.holderCode}</small>
    </div>,
    date(item.startsAt),
    <button
      key="action"
      className="small-action"
      disabled={busy}
      onClick={() => handleOpenConfirmModal(item.reservationId)}
    >
      Confirmar ausencia
    </button>,
  ]);

  const recentRows = recent.map((item) => {
    let statusClass = "status warning";
    if (item.status === "ACEPTADA") statusClass = "status good";
    if (item.status === "RECHAZADA_M2") statusClass = "status danger";

    let actionElement = "—";
    if (
      item.status === "RECHAZADA_M2" &&
      item.rejectReason?.toUpperCase().includes("TOO_EARLY")
    ) {
      actionElement = (
        <div className="actions">
          <button className="text-action" onClick={() => handleRetry(item.id)}>
            Reintentar
          </button>
          <button
            className="text-action danger-text"
            onClick={() => handleOpenAnnulModal(item.id)}
          >
            Anular
          </button>
        </div>
      );
    } else if (["ACEPTADA", "PENDIENTE_ACUSE", "RECHAZADA_M2"].includes(item.status)) {
      actionElement = (
        <button
          className="text-action danger-text"
          onClick={() => handleOpenAnnulModal(item.id)}
        >
          Anular
        </button>
      );
    }

    return [
      item.reservationId,
      item.studentCode,
      <span key="status" className={statusClass}>
        {item.status}
      </span>,
      item.rejectReason || "—",
      actionElement,
    ];
  });

  return (
    <>
      <SectionTitle
        eyebrow="GESTIÓN DE ESPACIOS"
        title="Confirmar inasistencias"
      />

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">ELEGIBLES</p>
            <h2>Reservas de espacios vencidas</h2>
          </div>
          <span className="pill">{eligible.length}</span>
        </div>
        <Table
          headers={["Espacio", "Estudiante", "Inicio", "Acción"]}
          rows={eligibleRows}
          empty="No hay reservas elegibles para confirmar."
        />
      </section>

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">REGISTRO</p>
            <h2>Confirmaciones recientes</h2>
          </div>
        </div>
        <Table
          headers={["Reserva", "Estudiante", "Estado", "Motivo", "Acción"]}
          rows={recentRows}
          empty="Aún no hay confirmaciones registradas."
        />
      </section>

      <PromptModal
        isOpen={modal.isOpen}
        title={modal.title}
        description={modal.description}
        placeholder={modal.placeholder}
        confirmLabel={modal.confirmLabel}
        isDestructive={modal.isDestructive}
        required={modal.required}
        onConfirm={modal.onConfirm}
        onCancel={closeModal}
      />
    </>
  );
}
