import { useEffect, useState } from "react";
import { CheckCircle2, LoaderCircle, LockKeyhole, Save, ShieldCheck, UserCheck } from "lucide-react";
import { api } from "../api";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";
import { Badge } from "./ui/badge";

export function FacultyProfileSetup({ user, onComplete, onClose }) {
  const [catalog, setCatalog] = useState(null);
  const [selected, setSelected] = useState([]);
  const [locked, setLocked] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.get("/faculty/subjects");
      setCatalog(data.availableSubjects || []);
      setSelected((data.selectedSubjects || []).map((subject) => subject.id));
      setLocked(data.locked);
    } catch (requestError) {
      setError(requestError.response?.data?.errors?.[0]?.message || "Could not load the subject catalog.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const toggle = (id) => setSelected((current) => current.includes(id)
    ? current.filter((subjectId) => subjectId !== id)
    : [...current, id]);

  const confirm = async () => {
    if (!selected.length) return setError("Select at least one subject before confirming.");
    setSubmitting(true);
    setError("");
    try {
      const { data } = await api.post("/faculty/subjects/confirm", { subjectIds: selected });
      setSelected((data.selectedSubjects || []).map((subject) => subject.id));
      setLocked(data.locked);
      setSaved(true);
      onComplete?.();
    } catch (requestError) {
      setError(requestError.response?.data?.errors?.[0]?.message || "Could not save your subject selection.");
      await load();
    } finally {
      setSubmitting(false);
    }
  };

  const grouped = (catalog || []).reduce((groups, subject) => {
    const key = `${subject.gradeName} · ${subject.streamName}`;
    groups[key] ||= [];
    groups[key].push(subject);
    return groups;
  }, {});

  return (
    <Card className="mx-auto max-w-4xl shadow-xl">
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary"><UserCheck className="h-5 w-5" /></div>
            <div>
              <CardTitle>Faculty subject selection</CardTitle>
              <CardDescription>Choose the active subjects you teach, then confirm your selection.</CardDescription>
            </div>
          </div>
          <Badge variant={locked ? "default" : "secondary"}>{locked ? "Locked" : `${selected.length} selected`}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="flex items-center gap-2 rounded-lg bg-muted p-3 text-sm text-muted-foreground">
          <ShieldCheck className="h-4 w-4 shrink-0 text-primary" />
          <span>{user?.name} · {user?.email}</span>
        </div>
        {locked && (
          <div role="status" className="flex items-center gap-2 rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
            <LockKeyhole className="h-4 w-4 shrink-0" />
            Selection locked. Contact an Administrator to make changes.
          </div>
        )}
        {saved && locked && <p className="flex items-center gap-2 text-sm text-emerald-700"><CheckCircle2 className="h-4 w-4" />Selection saved and locked.</p>}
        {error && <p role="alert" className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">{error}</p>}
        {loading ? (
          <div className="flex justify-center gap-2 py-10 text-muted-foreground"><LoaderCircle className="h-5 w-5 animate-spin" />Loading subject catalog…</div>
        ) : !catalog?.length ? (
          <p className="py-8 text-center text-sm text-muted-foreground">No active subjects are configured yet. Contact an Administrator.</p>
        ) : (
          <div className="max-h-[28rem] space-y-5 overflow-y-auto pr-1">
            {Object.entries(grouped).map(([group, subjects]) => (
              <section key={group}>
                <h3 className="mb-2 text-sm font-semibold">{group}</h3>
                <div className="grid gap-2 sm:grid-cols-2">
                  {subjects.map((subject) => (
                    <label key={subject.id} className={`flex items-start gap-3 rounded-lg border p-3 ${locked ? "cursor-default opacity-80" : "cursor-pointer hover:bg-muted/50"}`}>
                      <input type="checkbox" checked={selected.includes(subject.id)} disabled={locked || submitting} onChange={() => toggle(subject.id)} className="mt-1" />
                      <span className="min-w-0 flex-1"><span className="block font-medium">{subject.name}</span><span className="text-xs text-muted-foreground">{subject.code}</span></span>
                    </label>
                  ))}
                </div>
              </section>
            ))}
          </div>
        )}
        <div className="flex justify-end gap-2 border-t pt-4">
          {onClose && <Button variant="outline" onClick={onClose}>Close</Button>}
          {!locked && <Button onClick={confirm} disabled={loading || submitting || !catalog?.length} className="gap-2">
            {submitting ? <><LoaderCircle className="h-4 w-4 animate-spin" />Saving…</> : <><Save className="h-4 w-4" />Confirm &amp; Save</>}
          </Button>}
        </div>
      </CardContent>
    </Card>
  );
}
