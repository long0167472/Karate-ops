export interface ApiErrorDetail {
  field?: string;
  message?: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  status: number;
  code: string;
  message: string;
  data: T;
  at: string;
  path: string;
  details?: ApiErrorDetail[];
}

export class ApiClientError extends Error {
  status: number;
  code: string;
  path?: string;
  details: ApiErrorDetail[];

  constructor(message: string, options: { status: number; code: string; path?: string; details?: ApiErrorDetail[] }) {
    super(message);
    this.name = "ApiClientError";
    this.status = options.status;
    this.code = options.code;
    this.path = options.path;
    this.details = options.details || [];
  }
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");
const AUTH_TOKEN_KEY = "karate-ops.authToken";

export function getApiBase() {
  return API_BASE;
}

export function getAuthToken() {
  return window.sessionStorage.getItem(AUTH_TOKEN_KEY);
}

export function setAuthToken(token: string | null) {
  if (token) window.sessionStorage.setItem(AUTH_TOKEN_KEY, token);
  else window.sessionStorage.removeItem(AUTH_TOKEN_KEY);
}

export function authHeaders(): Record<string, string> {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function apiGet<T>(path: string): Promise<T> {
  return apiRequest<T>(path);
}

export async function apiGetOptional<T>(path: string): Promise<T | null> {
  const response = await fetch(`${API_BASE}${path}`, { headers: authHeaders() });
  if (response.status === 204 || response.status === 404) return null;
  return parseResponse<T>(response);
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  return apiRequest<T>(path, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  return apiRequest<T>(path, {
    method: "PATCH",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  return apiRequest<T>(path, {
    method: "PUT",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body)
  });
}

export async function apiDelete(path: string): Promise<void> {
  await apiRequest<void>(path, { method: "DELETE" });
}

export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = {
    ...authHeaders(),
    ...(init.headers || {})
  };
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  return parseResponse<T>(response);
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T;
  const payload = await readJson(response);
  if (isEnvelope<T>(payload)) {
    if (response.ok && payload.success) return payload.data;
    throwError(response, payload);
  }
  if (!response.ok) {
    throwError(response, {
      success: false,
      status: response.status,
      code: "HTTP_ERROR",
      message: fallbackMessage(payload, response),
      data: null,
      at: new Date().toISOString(),
      path: response.url,
      details: []
    });
  }
  return payload as T;
}

async function readJson(response: Response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function isEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  if (!value || typeof value !== "object") return false;
  return true
    && "success" in value
    && "status" in value
    && "code" in value
    && "message" in value
    && "data" in value;
}

function throwError(response: Response, envelope: ApiEnvelope<unknown>): never {
  if (response.status === 401) {
    setAuthToken(null);
    if (!window.location.pathname.includes("login") && !window.location.pathname.includes("register")) {
      window.location.href = "/login";
    }
  }
  throw new ApiClientError(envelope.message || response.statusText, {
    status: envelope.status || response.status,
    code: envelope.code || "HTTP_ERROR",
    path: envelope.path,
    details: envelope.details
  });
}

function fallbackMessage(payload: unknown, response: Response) {
  if (payload && typeof payload === "object" && "message" in payload) {
    return String((payload as { message?: unknown }).message || response.statusText);
  }
  if (typeof payload === "string" && payload.trim()) return payload;
  return response.statusText || "Request failed";
}
