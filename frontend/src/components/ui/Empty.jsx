export function Empty({ children = "Todavía no hay registros para mostrar." }) {
  return <div className="empty">{children}</div>;
}
