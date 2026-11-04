export function SectionTitle({ eyebrow, title, extra }) {
  return (
    <div className="section-title">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
      </div>
      {extra}
    </div>
  );
}
