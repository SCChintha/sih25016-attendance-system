import { useEffect, useState } from "react";
import { CheckCircle2, BookOpen, LoaderCircle, AlertCircle, Save, ShieldCheck, UserCheck } from "lucide-react";
import { api } from "../api";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";
import { Badge } from "./ui/badge";

export function FacultyProfileSetup({ user, onComplete, onClose }) {
  const [allSubjects, setAllSubjects] = useState([]);
  const [selectedSubjectIds, setSelectedSubjectIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Fetch all board subjects and current faculty's assigned subjects
  useEffect(() => {
    async function loadData() {
      setLoading(true);
      setError("");
      try {
        const [subjRes, profileRes] = await Promise.all([
          api.get("/subjects/by-grade"),
          api.get("/faculty/profile-setup").catch(() => ({ data: { assignedSubjects: [] } }))
        ]);

        setAllSubjects(subjRes.data || []);

        if (profileRes.data && profileRes.data.assignedSubjects) {
          setSelectedSubjectIds(profileRes.data.assignedSubjects.map((s) => s.id));
        }
      } catch {
        setError("Failed to load subject catalog. Please refresh.");
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  const toggleSubject = (id) => {
    setSelectedSubjectIds((prev) =>
      prev.includes(id) ? prev.filter((sId) => sId !== id) : [...prev, id]
    );
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (selectedSubjectIds.length === 0) {
      return setError("Please select at least one Intermediate subject that you teach.");
    }

    setSubmitting(true);
    setError("");
    setSuccessMsg("");

    try {
      await api.post("/faculty/profile-setup", {
        subjectIds: selectedSubjectIds.map(Number)
      });

      setSuccessMsg("Faculty subject catalog saved successfully! You are now discoverable to students.");
      if (onComplete) onComplete();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update faculty subjects.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card className="border-slate-200 dark:border-slate-800 shadow-xl max-w-3xl mx-auto">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold">
              <UserCheck className="h-5 w-5" />
            </div>
            <div>
              <CardTitle className="text-xl font-bold">Faculty Teaching Profile Configuration</CardTitle>
              <CardDescription className="text-xs">
                Select the Intermediate Board subjects you teach to appear in student lecturer selection lists.
              </CardDescription>
            </div>
          </div>
          <Badge variant="secondary" className="text-xs px-3 py-1 font-semibold">
            {selectedSubjectIds.length} Subject(s) Selected
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-6">
        
        {error && (
          <div className="p-3.5 rounded-lg bg-red-50 border border-red-200 text-red-700 dark:bg-red-950/50 dark:border-red-900 dark:text-red-300 text-sm flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="p-3.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-700 dark:bg-emerald-950/50 dark:border-emerald-900 dark:text-emerald-300 text-sm flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
            <span>{successMsg}</span>
          </div>
        )}

        <div className="p-4 rounded-xl bg-slate-100 dark:bg-slate-900 text-xs text-slate-600 dark:text-slate-400 flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-primary shrink-0" />
          <span>
            Lecturer Name: <strong>{user?.name}</strong> ({user?.email}) — Check all subjects you teach.
          </span>
        </div>

        {loading ? (
          <div className="py-12 text-center text-slate-500 flex items-center justify-center gap-2">
            <LoaderCircle className="h-5 w-5 animate-spin text-primary" /> Loading subject catalog...
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-96 overflow-y-auto pr-1">
            {allSubjects.map((subj) => {
              const isSelected = selectedSubjectIds.includes(subj.id);
              return (
                <div
                  key={subj.id}
                  onClick={() => toggleSubject(subj.id)}
                  className={`p-3.5 rounded-xl border transition-all cursor-pointer flex items-start gap-3 ${
                    isSelected
                      ? "border-primary bg-primary/5 shadow-sm dark:bg-primary/10"
                      : "border-slate-200 dark:border-slate-800 hover:border-slate-300 hover:bg-slate-50 dark:hover:bg-slate-900"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() => {}} // handled by parent div
                    className="mt-1 h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary cursor-pointer"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-bold text-sm text-slate-900 dark:text-slate-100 truncate">
                        {subj.name}
                      </span>
                      <Badge variant="outline" className="text-xs shrink-0 font-mono">
                        {subj.code}
                      </Badge>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">{subj.department} Stream</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        <div className="flex justify-end gap-3 pt-4 border-t">
          {onClose && (
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
          )}
          <Button
            onClick={handleSave}
            disabled={submitting || loading}
            className="gap-2 bg-primary text-primary-foreground font-semibold"
          >
            {submitting ? (
              <>
                <LoaderCircle className="h-4 w-4 animate-spin" /> Saving Catalog...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" /> Save Teaching Catalog
              </>
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
