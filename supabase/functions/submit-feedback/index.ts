// DebtTracker - submit-feedback Edge Function
//
// Receives the in-app "Send feedback" form (legal/feedback.html, served from
// GitHub Pages), emails it to the maintainer via Resend, and records a row in
// public.feedback as a fallback log.
//
// Public endpoint (verify_jwt = false): the form is filled by anonymous website
// visitors - there is no user session to verify. Abuse is bounded by a hidden
// honeypot field, strict length caps, a CORS origin allowlist, and Supabase's
// built-in per-IP rate limiting on Edge Functions.
//
// Optional photo: the client sends a compressed image as a data URL. The bytes
// go to the private `feedback-attachments` Storage bucket (never into Postgres;
// the feedback row keeps only the path) and ride along on the email. Rejected
// here if over MAX_ATTACHMENT_BYTES or not a JPEG/PNG/WebP.
//
// Required secret: RESEND_API_KEY  (Supabase -> Edge Functions -> Manage secrets)

import { createClient } from "jsr:@supabase/supabase-js@2";

const ALLOWED_ORIGINS = [
  "https://bobadronov.github.io",
  "http://localhost:8080",
  "http://localhost:3000",
];

const RESEND_API_KEY = Deno.env.get("RESEND_API_KEY");
const FEEDBACK_TO = "bobadronov@gmail.com";
// onboarding@resend.dev works with no domain verification, but only delivers to
// the Resend account owner's address - which is exactly FEEDBACK_TO here. Swap
// for a verified-domain sender once one is set up.
const FEEDBACK_FROM = "DebtTracker Feedback <onboarding@resend.dev>";

const ATTACHMENT_BUCKET = "feedback-attachments";

const MAX = { name: 200, email: 320, message: 5000, category: 32 };
const MAX_ATTACHMENT_BYTES = 2_500_000;
const ATTACHMENT_TYPES = ["image/jpeg", "image/png", "image/webp"];

function corsHeaders(origin: string | null): Record<string, string> {
  const allow = origin && ALLOWED_ORIGINS.includes(origin)
    ? origin
    : ALLOWED_ORIGINS[0];
  return {
    "Access-Control-Allow-Origin": allow,
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "authorization, apikey, content-type",
    "Vary": "Origin",
  };
}

const json = (body: unknown, status: number, origin: string | null) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders(origin), "Content-Type": "application/json" },
  });

const escapeHtml = (s: string): string =>
  s.replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ]!,
  );

type Attachment = {
  filename: string;
  contentType: string;
  bytes: Uint8Array;
  base64: string;
};

// Returns the parsed attachment, null when none was sent, or "invalid" when one
// was sent but is the wrong type / too big / malformed.
function parseAttachment(raw: unknown): Attachment | null | "invalid" {
  if (raw == null) return null;
  if (typeof raw !== "object") return "invalid";

  const a = raw as Record<string, unknown>;
  const dataUrl = typeof a.dataUrl === "string" ? a.dataUrl : "";
  const m = dataUrl.match(
    /^data:(image\/(?:jpeg|png|webp));base64,([A-Za-z0-9+/=\s]+)$/,
  );
  if (!m) return "invalid";

  const contentType = m[1];
  if (!ATTACHMENT_TYPES.includes(contentType)) return "invalid";
  const base64 = m[2].replace(/\s/g, "");

  let bytes: Uint8Array;
  try {
    const bin = atob(base64);
    bytes = Uint8Array.from(bin, (c) => c.charCodeAt(0));
  } catch {
    return "invalid";
  }
  if (bytes.length === 0 || bytes.length > MAX_ATTACHMENT_BYTES) return "invalid";

  const ext = contentType === "image/png"
    ? "png"
    : contentType === "image/webp"
    ? "webp"
    : "jpg";
  const rawName = typeof a.filename === "string" ? a.filename : "";
  const safe = rawName.replace(/[^a-zA-Z0-9._-]/g, "_").slice(0, 60) || "photo";
  const filename = /\.(jpe?g|png|webp)$/i.test(safe) ? safe : `${safe}.${ext}`;

  return { filename, contentType, bytes, base64 };
}

Deno.serve(async (req) => {
  const origin = req.headers.get("Origin");

  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders(origin) });
  }
  if (req.method !== "POST") {
    return json({ error: "method_not_allowed" }, 405, origin);
  }

  let payload: Record<string, unknown>;
  try {
    payload = await req.json();
  } catch {
    return json({ error: "invalid_json" }, 400, origin);
  }

  // Honeypot: real users never fill this; bots do. Pretend success.
  if (typeof payload.company === "string" && payload.company.trim() !== "") {
    return json({ ok: true }, 200, origin);
  }

  const str = (v: unknown) => (typeof v === "string" ? v.trim() : "");
  const name = str(payload.name).slice(0, MAX.name);
  const email = str(payload.email).slice(0, MAX.email);
  let category = str(payload.category).slice(0, MAX.category).toLowerCase();
  if (!["suggestion", "bug", "other"].includes(category)) category = "other";
  const message = str(payload.message).slice(0, MAX.message);
  const context = (payload.context && typeof payload.context === "object")
    ? payload.context as Record<string, unknown>
    : {};

  if (message.length < 3) {
    return json({ error: "message_required" }, 400, origin);
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return json({ error: "invalid_email" }, 400, origin);
  }

  const attachment = parseAttachment(payload.attachment);
  if (attachment === "invalid") {
    return json({ error: "invalid_attachment" }, 400, origin);
  }

  const feedbackId = crypto.randomUUID();
  let attachmentPath: string | null = null;

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const supabase = supabaseUrl && serviceKey
    ? createClient(supabaseUrl, serviceKey)
    : null;

  // --- store the photo in Storage (best effort; the email copy is primary) ---
  if (supabase && attachment) {
    const path = `${feedbackId}/${attachment.filename}`;
    const { error: upErr } = await supabase.storage
      .from(ATTACHMENT_BUCKET)
      .upload(path, attachment.bytes, {
        contentType: attachment.contentType,
        upsert: false,
      });
    if (upErr) {
      console.error("attachment upload failed:", upErr.message);
    } else {
      attachmentPath = path;
    }
  }

  // --- record it (best effort - the email is the primary channel) ---
  if (supabase) {
    const { error } = await supabase.from("feedback").insert({
      id: feedbackId,
      category,
      name: name || null,
      email: email || null,
      message,
      context,
      user_agent: req.headers.get("User-Agent"),
      attachment_path: attachmentPath,
    });
    if (error) console.error("feedback insert failed:", error.message);
  } else {
    console.error("Supabase env not available; skipping DB/Storage record");
  }

  // --- email it ---
  if (!RESEND_API_KEY) {
    console.error("RESEND_API_KEY is not configured");
    return json({ error: "email_not_configured" }, 500, origin);
  }

  const attachmentLine = attachment
    ? `${attachment.filename} (${attachment.bytes.length} bytes)${
      attachmentPath ? "" : " - Storage copy failed, see email attachment"
    }`
    : "(none)";
  const ctxLines = Object.entries(context)
    .map(([k, v]) => `${k}: ${String(v)}`)
    .join("\n");
  const subjectLine = message.split("\n")[0].slice(0, 80);
  const text = [
    `Category:   ${category}`,
    `Name:       ${name || "(none)"}`,
    `Email:      ${email || "(none)"}`,
    `Attachment: ${attachmentLine}`,
    "",
    message,
    "",
    "---",
    ctxLines,
  ].join("\n");
  const html = `
    <h2 style="margin:0 0 12px">DebtTracker feedback</h2>
    <p style="margin:0 0 12px">
      <strong>Category:</strong> ${escapeHtml(category)}<br>
      <strong>Name:</strong> ${escapeHtml(name || "(none)")}<br>
      <strong>Email:</strong> ${escapeHtml(email || "(none)")}<br>
      <strong>Attachment:</strong> ${escapeHtml(attachmentLine)}
    </p>
    <pre style="white-space:pre-wrap;font:inherit;background:#f5f5f5;padding:12px;border-radius:8px">${
    escapeHtml(message)
  }</pre>
    <hr style="border:none;border-top:1px solid #ddd;margin:16px 0">
    <pre style="white-space:pre-wrap;color:#666;font-size:12px">${
    escapeHtml(ctxLines)
  }</pre>
  `;

  const emailBody: Record<string, unknown> = {
    from: FEEDBACK_FROM,
    to: [FEEDBACK_TO],
    reply_to: email || undefined,
    subject: `[DebtTracker] ${category}: ${subjectLine}`,
    text,
    html,
  };
  if (attachment) {
    emailBody.attachments = [
      { filename: attachment.filename, content: attachment.base64 },
    ];
  }

  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${RESEND_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(emailBody),
  });

  if (!res.ok) {
    console.error("resend failed:", res.status, await res.text());
    return json({ error: "email_failed" }, 502, origin);
  }

  return json({ ok: true }, 200, origin);
});
