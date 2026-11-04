import { Empty } from "./Empty.jsx";

export function Table({
  headers,
  rows,
  empty = "Todavía no hay datos para este periodo.",
}) {
  if (!rows || rows.length === 0) {
    return <Empty>{empty}</Empty>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {headers.map((header) => (
              <th key={header}>{header}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td key={cellIndex}>{cell}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
