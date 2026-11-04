export const BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1";

export async function api(path, options = {}) {
  const token = localStorage.getItem("gestion-token");
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData) && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const response = await fetch(`${BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new Error(payload?.message || payload?.detail || `Error ${response.status}`);
  }
  if (response.status === 204) return null;
  const type = response.headers.get("content-type") || "";
  return type.includes("application/json") ? response.json() : response.text();
}
export const json = value => JSON.stringify(value);