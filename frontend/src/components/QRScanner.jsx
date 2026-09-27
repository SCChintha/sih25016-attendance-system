import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { QrCode, Check } from "lucide-react";
import { api, getApiErrors } from "../api";

function sessionIdFromToken(token) {
  const [body] = token.split(".");
  const [sessionId] = (body || "").split(":");
  return /^\d+$/.test(sessionId) ? Number(sessionId) : null;
}

export function QRScanner({ onSuccess, onClose }) {
  const [manualCode, setManualCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const handleSubmit = async () => {
    const qrToken = manualCode.trim();
    const sessionId = sessionIdFromToken(qrToken);
    if (!sessionId) return setError("Enter a valid attendance QR token.");
    setSubmitting(true); setError("");
    try {
      await api.post("/attendance/mark", { sessionId, qrToken, clientUuid: crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random()}` });
      onSuccess();
    } catch (requestError) {
      setError(getApiErrors(requestError)[0]?.message || "Attendance could not be recorded.");
    } finally { setSubmitting(false); }
  };
  return <Dialog open={true} onOpenChange={onClose}><DialogContent className="sm:max-w-md"><DialogHeader><DialogTitle>Scan Attendance QR Code</DialogTitle></DialogHeader><div className="space-y-6">
    <div className="rounded-lg border bg-muted/20 p-6 text-center"><QrCode className="mx-auto mb-3 h-12 w-12 text-muted-foreground"/><p className="text-sm font-medium">Use your device scanner or enter the QR token below.</p><p className="mt-1 text-xs text-muted-foreground">Tokens are verified by the server and expire automatically.</p></div>
    <div className="space-y-2"><Label htmlFor="manual-code">Attendance QR Token</Label><Input id="manual-code" placeholder="Paste or scan the QR token here..." value={manualCode} onChange={(event) => setManualCode(event.target.value)} onKeyDown={(event) => event.key === "Enter" && handleSubmit()} />{error && <p className="text-sm text-destructive">{error}</p>}</div>
    <Button onClick={handleSubmit} className="w-full" disabled={!manualCode.trim() || submitting}>{submitting ? "Recording attendance..." : <><Check className="mr-2 h-4 w-4"/>Submit Attendance</>}</Button>
    <div className="text-center"><Button variant="ghost" onClick={onClose}>Cancel</Button></div>
  </div></DialogContent></Dialog>;
}
