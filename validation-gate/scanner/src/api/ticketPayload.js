const TICKET_CODE_PATTERN = /\b(?:TICKET|VALID|USED|EXPIRED|CANCELLED|CANCELED)-[A-Z0-9_-]+\b/i;

export function extractTicketCode(rawPayload) {
  const payload = rawPayload.trim();

  if (!payload) {
    return "";
  }

  const jsonTicketCode = extractFromJson(payload);
  if (jsonTicketCode) {
    return jsonTicketCode;
  }

  const urlTicketCode = extractFromUrl(payload);
  if (urlTicketCode) {
    return urlTicketCode;
  }

  const patternMatch = payload.match(TICKET_CODE_PATTERN);
  if (patternMatch) {
    return patternMatch[0];
  }

  return payload;
}

function extractFromJson(payload) {
  try {
    const value = JSON.parse(payload);

    if (typeof value === "string") {
      return value;
    }

    return (
      value.ticketId ??
      value.externalTicketId ??
      value.ticketCode ??
      value.code ??
      value.ticket_id ??
      value.ticket_code ??
      value.ticket?.ticketId ??
      value.ticket?.ticketCode ??
      value.ticket?.code ??
      ""
    );
  } catch {
    return "";
  }
}

function extractFromUrl(payload) {
  try {
    const url = new URL(payload);
    const queryCode =
      url.searchParams.get("ticketId") ??
      url.searchParams.get("externalTicketId") ??
      url.searchParams.get("ticketCode") ??
      url.searchParams.get("code") ??
      url.searchParams.get("ticket_id") ??
      url.searchParams.get("ticket_code");

    if (queryCode) {
      return queryCode;
    }

    const lastPathSegment = url.pathname.split("/").filter(Boolean).at(-1);
    return lastPathSegment ?? "";
  } catch {
    return "";
  }
}
