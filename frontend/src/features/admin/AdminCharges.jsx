import { useCallback, useEffect, useState } from "react";
import { api, json } from "../../api.js";
import { SectionTitle } from "../../components/ui/SectionTitle.jsx";
import { Table } from "../../components/ui/Table.jsx";
import { PromptModal } from "../../components/ui/PromptModal.jsx";
import { money } from "../../utils/formatters.js";

export function AdminCharges({ flash }) {
  const [charges, setCharges] = useState([]);
  const [costs, setCosts] = useState([]);
  const [resourceType, setResourceType] = useState("");
  const [amount, setAmount] = useState("");

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
    const [chargesList, catalogList] = await Promise.all([
      api("/admin/charges"),
      api("/admin/cost-catalog"),
    ]);
    setCharges(chargesList);
    setCosts(catalogList);
  }, []);

  useEffect(() => {
    loadData().catch((err) => flash(err.message, true));
  }, [loadData, flash]);

  async function handleMarkAsPaid(chargeId) {
    try {
      await api(`/admin/charges/${chargeId}/paid`, {
        method: "POST",
        body: json({}),
      });
      flash("Cobro marcado como pagado exitosamente.");
      await loadData();
    } catch (err) {
      flash(err.message, true);
    }
  }

  function handleOpenAnnulModal(chargeId) {
    setModal({
      isOpen: true,
      title: "Anular cobro",
      description: "El motivo de anulación es obligatorio para el registro de auditoría.",
      placeholder: "Ej. Error en la constatación del daño / Garantía del fabricante...",
      confirmLabel: "Anular cobro",
      isDestructive: true,
      required: true,
      onConfirm: async (reason) => {
        closeModal();
        try {
          await api(`/admin/charges/${chargeId}/annul`, {
            method: "POST",
            body: json({ reason }),
          });
          flash("Cobro anulado con registro de auditoría.");
          await loadData();
        } catch (err) {
          flash(err.message, true);
        }
      },
    });
  }

  async function handleSaveCost(e) {
    e.preventDefault();
    try {
      await api("/admin/cost-catalog", {
        method: "POST",
        body: json({
          resourceType: resourceType.trim(),
          amount: Number(amount),
          active: true,
        }),
      });
      flash("Costo de catálogo actualizado.");
      setResourceType("");
      setAmount("");
      await loadData();
    } catch (err) {
      flash(err.message, true);
    }
  }

  const pendingChargesCount = charges.filter(
    (x) => x.status === "PENDIENTE"
  ).length;

  const chargesRows = charges.map((charge) => {
    let statusClass = "status muted-status";
    if (charge.status === "PENDIENTE") statusClass = "status warning";
    if (charge.status === "PAGADO") statusClass = "status good";

    return [
      charge.studentCode,
      charge.reason,
      money(charge.amount),
      <span key="status" className={statusClass}>
        {charge.status}
      </span>,
      charge.status === "PENDIENTE" ? (
        <div className="actions" key="actions">
          <button
            className="text-action"
            onClick={() => handleMarkAsPaid(charge.id)}
          >
            Pagado
          </button>
          <button
            className="text-action danger-text"
            onClick={() => handleOpenAnnulModal(charge.id)}
          >
            Anular
          </button>
        </div>
      ) : (
        "—"
      ),
    ];
  });

  const costsRows = costs.map((cost) => [
    cost.resourceType,
    money(cost.amount),
    cost.active ? "Activo" : "Inactivo",
  ]);

  return (
    <>
      <SectionTitle eyebrow="COSTOS Y RECAUDO" title="Cobros y costos" />

      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="eyebrow">COBROS GENERADOS</p>
            <h2>Seguimiento de cobros</h2>
          </div>
          <span className="pill">{pendingChargesCount} pendientes</span>
        </div>
        <Table
          headers={["Estudiante", "Concepto", "Monto", "Estado", "Acción"]}
          rows={chargesRows}
          empty="Todavía no hay cobros registrados."
        />
      </section>

      <div className="content-grid">
        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">CATÁLOGO M3</p>
              <h2>Costos por tipo de recurso</h2>
            </div>
          </div>
          <Table
            headers={["Tipo de recurso", "Valor", "Estado"]}
            rows={costsRows}
            empty="No hay costos configurados en el catálogo."
          />
        </section>

        <section className="panel">
          <div className="panel-head">
            <div>
              <p className="eyebrow">CONFIGURAR</p>
              <h2>Agregar costo</h2>
            </div>
          </div>
          <form className="form-stack" onSubmit={handleSaveCost}>
            <label>
              Tipo de recurso
              <input
                value={resourceType}
                onChange={(e) => setResourceType(e.target.value)}
                placeholder="Ej. VIDEO_PROYECTOR"
                required
              />
            </label>
            <label>
              Valor COP
              <input
                type="number"
                min="0"
                step="100"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="Ej. 150000"
                required
              />
            </label>
            <button className="primary">Guardar valor</button>
            <p className="hint">
              El cobro congela el monto vigente al momento en que se genera.
            </p>
          </form>
        </section>
      </div>

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
