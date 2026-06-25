const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

export async function validateTicket({ qrPayload, gateId, stationId, eventType }) {
  const response = await fetch(`${API_BASE_URL}/api/gate/validate-ticket`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      qrPayload,
      gateId,
      stationId,
      eventType
    })
  });

  const payload = await response.json();

  if (!response.ok) {
    throw new Error(payload.message || "Ticket validation failed");
  }

  return {
    status: "SENT",
    ticketId: payload.ticketId,
    gateId: payload.gateId,
    stationId: payload.stationId,
    eventType: payload.eventType,
    message: payload.message
  };
}
