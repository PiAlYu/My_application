interface Env {
  DB: D1Database;
  CHECKLIST_READ_TOKEN?: string;
  CHECKLIST_WRITE_TOKEN?: string;
}

interface ChecklistItem {
  name: string;
}

interface Checklist {
  id: string;
  title: string;
  updatedAt: number;
  items: ChecklistItem[];
}

interface ChecklistRow {
  id: string;
  title: string;
  updated_at: number;
  item_name: string | null;
}

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, PUT, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
};

const CHECKLISTS_SELECT_SQL = `
  SELECT
    c.id,
    c.title,
    c.updated_at,
    i.name AS item_name
  FROM checklists c
  LEFT JOIN checklist_items i ON i.checklist_id = c.id
  ORDER BY c.updated_at DESC, c.id ASC, i.position ASC
`;

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(request.url);

      if (request.method === "OPTIONS") {
        return new Response(null, {
          status: 204,
          headers: CORS_HEADERS,
        });
      }

      if (request.method === "GET" && url.pathname === "/health") {
        return handleHealth(env);
      }

      if (url.pathname !== "/api/checklists") {
        return jsonResponse(404, { error: "Not found" });
      }

      if (request.method === "GET") {
        return handleGetChecklists(request, env);
      }

      if (request.method === "PUT") {
        return handleReplaceChecklists(request, env);
      }

      return jsonResponse(405, { error: "Method not allowed" }, { Allow: "GET, PUT, OPTIONS" });
    } catch {
      return jsonResponse(500, { error: "Internal server error." });
    }
  },
};

async function handleHealth(env: Env): Promise<Response> {
  try {
    await env.DB.prepare("SELECT 1").first();
  } catch (error) {
    return jsonResponse(503, { status: "error", error: "Database unavailable" });
  }

  return jsonResponse(200, { status: "ok" });
}

async function handleGetChecklists(request: Request, env: Env): Promise<Response> {
  const readToken = normalizeToken(env.CHECKLIST_READ_TOKEN);
  if (!isAuthorized(request, readToken)) {
    return unauthorizedResponse();
  }

  const queryResult = await env.DB.prepare(CHECKLISTS_SELECT_SQL).all<ChecklistRow>();
  const rows = Array.isArray(queryResult.results) ? queryResult.results : [];

  const checklistsById = new Map<string, Checklist>();
  rows.forEach((row) => {
    const existing = checklistsById.get(row.id);
    if (existing) {
      if (typeof row.item_name === "string" && row.item_name.trim() !== "") {
        existing.items.push({ name: row.item_name });
      }
      return;
    }

    const checklist: Checklist = {
      id: row.id,
      title: row.title,
      updatedAt: Number.isFinite(row.updated_at) ? row.updated_at : nowMs(),
      items: [],
    };
    if (typeof row.item_name === "string" && row.item_name.trim() !== "") {
      checklist.items.push({ name: row.item_name });
    }
    checklistsById.set(row.id, checklist);
  });

  return jsonResponse(200, Array.from(checklistsById.values()));
}

async function handleReplaceChecklists(request: Request, env: Env): Promise<Response> {
  const writeToken = normalizeToken(env.CHECKLIST_WRITE_TOKEN);
  if (!isAuthorized(request, writeToken)) {
    return unauthorizedResponse();
  }

  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return jsonResponse(400, { error: "Invalid JSON payload." });
  }

  let normalized: Checklist[];
  try {
    normalized = normalizeChecklistsPayload(payload);
  } catch (error) {
    return jsonResponse(400, { error: toErrorMessage(error) });
  }

  const statements = [
    env.DB.prepare("DELETE FROM checklist_items"),
    env.DB.prepare("DELETE FROM checklists"),
  ];

  normalized.forEach((checklist) => {
    statements.push(
      env.DB
        .prepare("INSERT INTO checklists (id, title, updated_at) VALUES (?1, ?2, ?3)")
        .bind(checklist.id, checklist.title, checklist.updatedAt),
    );

    checklist.items.forEach((item, position) => {
      statements.push(
        env.DB
          .prepare("INSERT INTO checklist_items (checklist_id, position, name) VALUES (?1, ?2, ?3)")
          .bind(checklist.id, position, item.name),
      );
    });
  });

  await env.DB.batch(statements);
  return jsonResponse(200, normalized);
}

function normalizeChecklistsPayload(payload: unknown): Checklist[] {
  if (!Array.isArray(payload)) {
    throw new Error("Payload must be a JSON array.");
  }

  const seenIds = new Set<string>();

  return payload.map((rawChecklist, index) => {
    if (!rawChecklist || typeof rawChecklist !== "object") {
      throw new Error(`Checklist at index ${index} must be an object.`);
    }

    const checklist = rawChecklist as Record<string, unknown>;
    const checklistId = String(checklist.id ?? "").trim();
    const title = String(checklist.title ?? "").trim();
    const rawItems = checklist.items;
    const rawUpdatedAt = checklist.updatedAt;

    if (checklistId === "") {
      throw new Error(`Checklist at index ${index} must contain a non-empty id.`);
    }
    if (seenIds.has(checklistId)) {
      throw new Error(`Checklist id '${checklistId}' is duplicated in payload.`);
    }
    if (title === "") {
      throw new Error(`Checklist at index ${index} must contain a non-empty title.`);
    }
    if (!Array.isArray(rawItems)) {
      throw new Error(`Checklist '${title}' must contain an items array.`);
    }

    const updatedAt = normalizeUpdatedAt(rawUpdatedAt, title);

    const items: ChecklistItem[] = [];
    rawItems.forEach((rawItem, itemIndex) => {
      if (!rawItem || typeof rawItem !== "object") {
        throw new Error(`Item ${itemIndex} in checklist '${title}' must be an object.`);
      }
      const itemName = String((rawItem as Record<string, unknown>).name ?? "").trim();
      if (itemName !== "") {
        items.push({ name: itemName });
      }
    });

    seenIds.add(checklistId);
    return {
      id: checklistId,
      title,
      updatedAt,
      items,
    };
  });
}

function normalizeUpdatedAt(rawUpdatedAt: unknown, title: string): number {
  if (rawUpdatedAt === undefined || rawUpdatedAt === null) {
    return nowMs();
  }
  const parsed = Number(rawUpdatedAt);
  if (!Number.isFinite(parsed)) {
    throw new Error(`Checklist '${title}' has an invalid updatedAt value.`);
  }
  return Math.trunc(parsed);
}

function jsonResponse(
  status: number,
  payload: unknown,
  extraHeaders: Record<string, string> = {},
): Response {
  const headers = new Headers({
    "Content-Type": "application/json; charset=utf-8",
    ...CORS_HEADERS,
    ...extraHeaders,
  });
  return new Response(JSON.stringify(payload), { status, headers });
}

function unauthorizedResponse(): Response {
  return jsonResponse(
    401,
    { error: "Unauthorized" },
    { "WWW-Authenticate": 'Bearer realm="store-checklist"' },
  );
}

function isAuthorized(request: Request, requiredToken: string): boolean {
  if (requiredToken === "") return true;
  const providedToken = extractBearerToken(request);
  if (providedToken === "") return false;
  return constantTimeEquals(providedToken, requiredToken);
}

function extractBearerToken(request: Request): string {
  const authHeader = request.headers.get("Authorization") ?? "";
  const [scheme, token] = authHeader.split(/\s+/, 2);
  if ((scheme ?? "").toLowerCase() !== "bearer") return "";
  return normalizeToken(token);
}

function normalizeToken(token: string | undefined): string {
  return String(token ?? "").trim();
}

function constantTimeEquals(left: string, right: string): boolean {
  if (left.length !== right.length) return false;
  let diff = 0;
  for (let index = 0; index < left.length; index += 1) {
    diff |= left.charCodeAt(index) ^ right.charCodeAt(index);
  }
  return diff === 0;
}

function nowMs(): number {
  return Date.now();
}

function toErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message.trim() !== "") {
    return error.message;
  }
  return "Unexpected error.";
}
