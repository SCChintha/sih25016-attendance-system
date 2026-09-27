import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog";
import { Button } from "./ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Label } from "./ui/label";
import { Card, CardContent } from "./ui/card";
import { Badge } from "./ui/badge";
import { QrCode, RefreshCw, Copy, Check, Square } from "lucide-react";
import { api, getApiErrors } from "../api";

export function QRGenerator({ classes, onClose, onSessionChanged }) {
  const [selectedClass, setSelectedClass] = useState("");
  const [qrCode, setQrCode] = useState("");
  const [expiresAt, setExpiresAt] = useState(null);
  const [timeLeft, setTimeLeft] = useState(0);
  const [sessionId, setSessionId] = useState(null);
  const [copied, setCopied] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const selected = classes.find((item) => String(item.timetableSlotId) === selectedClass);
  const createQr = async () => {
    if (!selected) return;
    setSubmitting(true); setError("");
    try {
      let id = selected.activeSessionId;
      if (!id) {
        const { data } = await api.post("/sessions/start", { subjectId: selected.subjectId, sectionId: selected.sectionId, timetableSlotId: selected.timetableSlotId });
        id = data.id; onSessionChanged();
      }
      const { data } = await api.get(`/sessions/${id}/qr`);
      setSessionId(id); setQrCode(data.token); setExpiresAt(new Date(data.expiresAt)); setTimeLeft(Math.max(0, Math.ceil((new Date(data.expiresAt).getTime() - Date.now()) / 1000)));
    } catch (requestError) { setError(getApiErrors(requestError)[0]?.message || "QR code could not be generated."); }
    finally { setSubmitting(false); }
  };
  const stopSession = async () => { if (!sessionId) return; setSubmitting(true); setError(""); try { await api.post(`/sessions/${sessionId}/stop`); setQrCode(""); setSessionId(null); onSessionChanged(); } catch (requestError) { setError(getApiErrors(requestError)[0]?.message || "Session could not be stopped."); } finally { setSubmitting(false); } };
  useEffect(() => { if (!timeLeft) return; const timer = setTimeout(() => setTimeLeft((value) => value - 1), 1000); return () => clearTimeout(timer); }, [timeLeft]);
  const copy = async () => { await navigator.clipboard.writeText(qrCode); setCopied(true); setTimeout(() => setCopied(false), 2000); };
  const formatTime = (seconds) => `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
  return <Dialog open={true} onOpenChange={onClose}><DialogContent className="sm:max-w-lg"><DialogHeader><DialogTitle>Generate Attendance QR Code</DialogTitle></DialogHeader><div className="space-y-6">
    <div className="space-y-2"><Label htmlFor="class-select">Select Class</Label><Select value={selectedClass} onValueChange={setSelectedClass}><SelectTrigger id="class-select"><SelectValue placeholder="Choose a class to generate QR code"/></SelectTrigger><SelectContent>{classes.map((item) => <SelectItem key={item.timetableSlotId} value={String(item.timetableSlotId)}>{item.code} - {item.name} (Section {item.section})</SelectItem>)}</SelectContent></Select></div>
    {error && <p className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">{error}</p>}
    <Button onClick={createQr} disabled={!selected || submitting} className="w-full"><QrCode className="mr-2 h-4 w-4"/>{submitting ? "Preparing session..." : qrCode ? "Refresh QR Code" : "Start Session & Generate QR"}</Button>
    {qrCode && <Card><CardContent className="space-y-4 p-6"><div className="text-center"><div className="mx-auto flex h-48 w-48 items-center justify-center rounded-lg border-2 border-primary bg-background"><QrCode className="h-16 w-16 text-primary"/></div><h3 className="mt-3 font-medium">{selected?.name}</h3><p className="text-sm text-muted-foreground">{selected?.code} - Section {selected?.section}</p></div><div className="space-y-2"><Label>QR Code Value</Label><div className="flex gap-2"><div className="flex-1 break-all rounded bg-muted p-2 font-mono text-sm">{qrCode}</div><Button variant="outline" size="sm" onClick={copy}>{copied ? <Check className="h-4 w-4"/> : <Copy className="h-4 w-4"/>}</Button></div></div><div className="text-center"><Badge variant={timeLeft ? "secondary" : "destructive"}>{timeLeft ? `Expires in ${formatTime(timeLeft)}` : "Expired"}</Badge>{expiresAt && <p className="mt-1 text-xs text-muted-foreground">Server expiry: {expiresAt.toLocaleTimeString()}</p>}</div><Button variant="outline" className="w-full" onClick={stopSession} disabled={submitting}><Square className="mr-2 h-4 w-4"/>Stop Attendance Session</Button></CardContent></Card>}
    <div className="text-center"><Button variant="ghost" onClick={onClose}>Close</Button></div>
  </div></DialogContent></Dialog>;
}
