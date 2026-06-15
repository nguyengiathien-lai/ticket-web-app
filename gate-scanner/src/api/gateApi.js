const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function validateTicket({ ticketCode, gateId, stationId }) {
  const response = await fetch(`${API_BASE_URL}/api/gate/validate-ticket`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      ticketCode,
      gateId,
      stationId
    })
  });

  const payload = await response.json();

  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "Ticket validation failed");
  }

  return payload.data;
}
