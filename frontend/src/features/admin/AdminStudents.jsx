import { useState } from "react";
import { api, json } from "../../api.js";
import { MetricCard } from "../../components/ui/MetricCard.jsx";
import { SectionTitle } from "../../components/ui/SectionTitle.jsx";
import { Table } from "../../components/ui/Table.jsx";
import { PromptModal } from "../../components/ui/PromptModal.jsx";
import { money, date } from "../../utils/formatters.js";

export function AdminStudents({ flash }) {
  const [code, setCode] = useState("");
  const [data, setData] = useState(null);

  const [modal, setModal] = useState({
    isOpen: false,
    title: "",
    description: "",
    placeholder: "",
    confirmLabel: "Desbloquear",
    isDestructive: false,
    required: true,
    onConfirm: () => {},
  });

  const closeModal = () => setModal((prev) => ({ ...prev, isOpen: false }));

  async function handleSearch(e) {
    e.preventDefault();
    try {
      const studentData = await api(
        `/admin/students/${encodeURIComponent(code.trim())}`
      );
      setData(studentData);
    } catch (err) {
      flash(err.message, true);
    }
  }

  function handleOpenUnblockModal() {
    setModal({
      isOpen: true,
      title: `Desbloquear estudiante: ${data.name || data.studentCode}`,
      description:
        "El motivo es obligatorio y quedará registrado de forma permanente en la auditoría del sistema.",
      placeholder: "Indica el motivo justificado del desbloqueo...",
      confirmLabel: "Desbloquear usuario",
      isDestructive: false,
      required: true,
      onConfirm: async (reason) => {
        closeModal();
        try {
          await api(`/admin/students/${encodeURIComponent(code.trim())}/unblock`, {
            method: "POST",
            body: json({ reason }),
          });
          flash("Estudiante desbloqueado y auditoría registrada.");
          const updated = await api(
            `/admin/students/${encodeURIComponent(code.trim())}`
          );
          setData(updated);
        } catch (err) {
          flash(err.message, true);
        }
      },
    });
  }

  const scoreHistoryRows = (data?.scoreHistory || []).map((item) => [
    date(item.createdAt),
    item.type,
    item.reason,
    item.effectivePoints > 0
      ? `+${item.effectivePoints}`
      : item.effectivePoints,
    item.scoreAfter,
  ]);

  const chargesRows = (data?.charges || []).map((charge) => [
    charge.reason,
    money(charge.amount),
    charge.status,
    date(charge.createdAt),
  ]);

  return (
    <>
      <SectionTitle eyebrow="EXPEDIENTE" title="Consultar estudiante" />

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          placeholder="Código institucional (ej. 202610001)"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          required
        />
        <button className="primary">Consultar</button>
      </form>

      {data && (
        <>
          <div className="metrics">
            <MetricCard
              label="Estudiante"
              value={data.name}
              helper={data.studentCode}
            />
            <MetricCard
              label="Score"
              value={data.score}
              helper={data.blocked ? "Bloqueado (Score 0)" : "Habilitado"}
              tone={data.blocked ? "red" : "green"}
            />
            <MetricCard
              label="Sanciones"
              value={data.activeSanctions?.length || 0}
              helper="Vigentes"
              tone={data.activeSanctions?.length ? "amber" : "blue"}
            />
          </div>

          {data.blocked && (
            <div style={{ marginBottom: "20px" }}>
              <button className="primary" onClick={handleOpenUnblockModal}>
                Desbloquear con auditoría
              </button>
            </div>
          )}

          <section className="panel">
            <div className="panel-head">
              <h2>Historial de score de confianza</h2>
            </div>
            <Table
              headers={["Fecha", "Tipo", "Motivo", "Cambio", "Resultado"]}
              rows={scoreHistoryRows}
              empty="No hay movimientos en el historial de score."
            />
          </section>

          <section className="panel">
            <div className="panel-head">
              <h2>Cobros asignados</h2>
            </div>
            <Table
              headers={["Concepto", "Monto", "Estado", "Fecha"]}
              rows={chargesRows}
              empty="No hay cobros registrados para este estudiante."
            />
          </section>
        </>
      )}

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
