const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

export async function getScannerOptions() {
  const response = await fetch(`${API_BASE_URL}/api/gate/scanner-options`);
  const payload = await response.json();

  if (!response.ok) {
    throw new Error(payload.message || "Could not load scanner configuration");
  }

  return payload;
}

export async function validateTicket({ qrPayload, deviceCode, stationCode, eventType }) {
  const response = await fetch(`${API_BASE_URL}/api/gate/validate-ticket`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      qrPayload,
      deviceCode,
      stationCode,
      eventType
    })
  });

  const payload = await response.json();

  if (!response.ok) {
    throw new Error(payload.message || payload.errors?.[0] || "Ticket validation failed");
  }

  const accepted = payload === true;

  return {
    status: accepted ? "SENT" : "INVALID",
    ticketId: extractTicketId(qrPayload),
    qrPayload,
    deviceCode,
    stationCode,
    eventType,
    message: accepted
      ? "Validation request accepted by the gate service"
      : "Ticket validation was rejected"
  };
}

function extractTicketId(qrPayload) {
  const parts = qrPayload?.trim().split(":") ?? [];
  return parts.length >= 3 && parts[0] === "AFCQR" ? parts[2] : qrPayload;
}
