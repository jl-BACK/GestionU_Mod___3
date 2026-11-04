import { useState } from "react";
import { api, json } from "../../api.js";
import { SectionTitle } from "../../components/ui/SectionTitle.jsx";

export function StudentDamage({ reload, flash }) {
  const [resourceId, setResourceId] = useState("");
  const [reservationId, setReservationId] = useState("");
  const [description, setDescription] = useState("");
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setBusy(true);

    try {
      const payload = {
        resourceId: resourceId.trim(),
        reservationId: reservationId.trim() || null,
        description: description.trim(),
      };

      const report = await api("/damage-reports", {
        method: "POST",
        body: json(payload),
      });

      if (file && report.id) {
        const formData = new FormData();
        formData.append("file", file);
        await api(`/damage-reports/${report.id}/evidence`, {
          method: "POST",
          body: formData,
        });
      }

      flash("Novedad técnica registrada y enviada al módulo de inventario.");
      setResourceId("");
      setReservationId("");
      setDescription("");
      setFile(null);
      await reload();
    } catch (err) {
      flash(err.message, true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <SectionTitle
        eyebrow="RECURSOS"
        title="Reportar una novedad técnica"
      />

      <div className="single-panel">
        <section className="panel form-panel">
          <p className="muted">
            El reporte se enviará a inventario. Si hay una reserva asociada, se identifica
            al responsable con la copia informada por M2.
          </p>

          <form className="form-stack" onSubmit={handleSubmit}>
            <label>
              ID del recurso
              <input
                value={resourceId}
                onChange={(e) => setResourceId(e.target.value)}
                placeholder="Placa o identificador de inventario"
                required
              />
            </label>

            <label>
              ID de reserva (opcional)
              <input
                value={reservationId}
                onChange={(e) => setReservationId(e.target.value)}
                placeholder="Si el daño ocurrió durante una reserva"
              />
            </label>

            <label>
              Descripción
              <textarea
                rows={5}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Detalla detalladamente el estado o daño..."
                required
                maxLength={2000}
              />
            </label>

            <label>
              Evidencia (máximo 5 MB)
              <input
                type="file"
                accept="image/*,.pdf"
                onChange={(e) => setFile(e.target.files?.[0] || null)}
              />
            </label>

            <button className="primary" disabled={busy}>
              {busy ? "Enviando…" : "Enviar reporte"}
            </button>
          </form>
        </section>
      </div>
    </>
  );
}
