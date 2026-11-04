export function MetricCard({ label, value, helper, tone = "blue" }) {
  return (
    <article className="metric-card">
      <div className="metric-top">
        <span>{label}</span>
        <span className={`metric-icon ${tone}`}>↗</span>
      </div>
      <strong>{value}</strong>
      <small>{helper}</small>
    </article>
  );
}
