import { useEffect, useState } from "react";

export function PromptModal({
  isOpen,
  title,
  description,
  placeholder = "",
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  isDestructive = false,
  required = false,
  onConfirm,
  onCancel,
}) {
  const [value, setValue] = useState("");

  useEffect(() => {
    if (isOpen) {
      setValue("");
    }
  }, [isOpen]);

  if (!isOpen) return null;

  function handleSubmit(e) {
    e.preventDefault();
    if (required && !value.trim()) return;
    onConfirm(value.trim());
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div
        className="modal-card"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <div className="modal-header">
          <h3>{title}</h3>
          {description && <p className="muted">{description}</p>}
        </div>

        <form onSubmit={handleSubmit} className="form-stack">
          <label>
            <span>Detalle o motivo {required && <b className="text-danger">*</b>}</span>
            <textarea
              autoFocus
              rows={3}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              placeholder={placeholder}
              required={required}
            />
          </label>

          <div className="modal-actions">
            <button
              type="button"
              className="button secondary"
              onClick={onCancel}
            >
              {cancelLabel}
            </button>
            <button
              type="submit"
              className={`button primary ${isDestructive ? "btn-danger" : ""}`}
              disabled={required && !value.trim()}
            >
              {confirmLabel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
