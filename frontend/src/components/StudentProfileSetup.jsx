import { useEffect, useState } from "react";
import { AlertCircle, BookOpen, CheckCircle2, ChevronLeft, ChevronRight, GraduationCap, LoaderCircle, Lock, LogOut, UserCheck } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { api } from "../api";
import { Button } from "./ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Label } from "./ui/label";
import { Badge } from "./ui/badge";

export function StudentProfileSetup({ user, onComplete }) {
  const { getSections, updateUser, logout } = useAuth();
  const [step, setStep] = useState(1);
  const [grades, setGrades] = useState([]);
  const [gradeLevelId, setGradeLevelId] = useState("");
  const [streams, setStreams] = useState([]);
  const [streamId, setStreamId] = useState("");
  const [subjects, setSubjects] = useState([]);
  const [sections, setSections] = useState([]);
  const [sectionId, setSectionId] = useState("");
  const [facultyMap, setFacultyMap] = useState({});
  const [selectedFaculty, setSelectedFaculty] = useState({});
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [sectionsLoading, setSectionsLoading] = useState(false);
  const [facultyLoading, setFacultyLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const selectedGrade = grades.find((grade) => String(grade.id) === String(gradeLevelId));
  const selectedStream = streams.find((stream) => String(stream.id) === String(streamId));

  useEffect(() => {
    api.get("/public/academic/grades")
      .then(({ data }) => setGrades(data))
      .catch(() => setError("Could not load grades. Please ask an administrator to configure the academic catalog."));

    setSectionsLoading(true);
    getSections()
      .then((data) => {
        setSections(data);
        if (data.length === 1) setSectionId(String(data[0].id));
      })
      .catch(() => setError("Could not load class sections. Please refresh and try again."))
      .finally(() => setSectionsLoading(false));
  }, []);

  useEffect(() => {
    if (!gradeLevelId) {
      setStreams([]);
      setStreamId("");
      setSubjects([]);
      return;
    }

    let cancelled = false;
    setCatalogLoading(true);
    setStreams([]);
    setStreamId("");
    setSubjects([]);
    api.get(`/public/academic/grades/${gradeLevelId}/streams`)
      .then(({ data }) => { if (!cancelled) setStreams(data); })
      .catch(() => { if (!cancelled) setError("Could not load streams for this grade."); })
      .finally(() => { if (!cancelled) setCatalogLoading(false); });
    return () => { cancelled = true; };
  }, [gradeLevelId]);

  useEffect(() => {
    if (!streamId) {
      setSubjects([]);
      return;
    }

    let cancelled = false;
    setCatalogLoading(true);
    setSubjects([]);
    api.get(`/public/academic/streams/${streamId}/subjects`)
      .then(({ data }) => { if (!cancelled) setSubjects(data); })
      .catch(() => { if (!cancelled) setError("Could not load subjects for this stream."); })
      .finally(() => { if (!cancelled) setCatalogLoading(false); });
    return () => { cancelled = true; };
  }, [streamId]);

  const handleNextStep1 = () => {
    if (!selectedGrade) return setError("Please select your grade.");
    if (!selectedStream) return setError("Please select your stream.");
    if (!sectionId) return setError("Please select your class section.");
    setError("");
    setStep(2);
  };

  const handleNextStep2 = async () => {
    setError("");
    setFacultyLoading(true);
    try {
      const entries = await Promise.all(subjects.map(async (subject) => {
        const { data } = await api.get("/faculty/by-subject", { params: { subjectId: subject.id } });
        return [subject.id, data];
      }));
      setFacultyMap(Object.fromEntries(entries));
      setStep(3);
    } catch {
      setFacultyMap({});
      setError("Could not load faculty assignments. You can save your profile and assign lecturers later.");
      setStep(3);
    } finally {
      setFacultyLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!selectedGrade || !selectedStream || !sectionId) {
      setStep(1);
      setError("Choose your grade, stream, and class section before saving.");
      return;
    }

    setSubmitting(true);
    setError("");
    const academicGrade = selectedGrade.name;
    const mappings = subjects
      .filter((subject) => selectedFaculty[subject.id])
      .map((subject) => ({ subjectId: Number(subject.id), facultyId: Number(selectedFaculty[subject.id]) }));

    try {
      const { data } = await api.post("/students/profile-setup", {
        gradeLevelId: Number(gradeLevelId),
        streamId: Number(streamId),
        sectionId: Number(sectionId),
        mappings
      });

      updateUser({
        onboardingCompleted: true,
        sectionId: Number(sectionId),
        academicGrade,
        gradeLevelId: Number(gradeLevelId),
        streamId: Number(streamId),
        streamName: selectedStream.name
      });
      if (onComplete) onComplete(data);
    } catch (submitError) {
      setError(submitError.response?.data?.message || "Could not save your profile. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  const steps = [
    { id: 1, label: "Grade & Stream", icon: GraduationCap },
    { id: 2, label: "Subjects", icon: BookOpen },
    { id: 3, label: "Lecturers", icon: UserCheck }
  ];

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 py-8 px-4 flex items-center justify-center">
      <div className="w-full max-w-4xl space-y-6">
        <div className="flex justify-end">
          <Button type="button" variant="outline" size="sm" onClick={logout}>
            <LogOut className="mr-2 h-4 w-4" /> Log out
          </Button>
        </div>
        <div className="text-center space-y-2">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wide uppercase">
            <GraduationCap className="h-4 w-4" /> Student Profile Setup
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">Welcome, {user?.name}!</h1>
          <p className="text-sm text-slate-600 dark:text-slate-400 max-w-lg mx-auto">
            Choose your grade and stream after registration, then review your subjects and class lecturers.
          </p>
        </div>

        <div className="grid grid-cols-3 gap-3 bg-white dark:bg-slate-900 p-4 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800">
          {steps.map(({ id, label, icon: Icon }) => {
            const active = step === id;
            const done = step > id;
            return (
              <div key={id} className={`flex items-center gap-3 p-3.5 rounded-xl ${active ? "bg-primary text-primary-foreground shadow-md" : done ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400" : "text-slate-400 bg-slate-100/60 dark:bg-slate-800/40"}`}>
                <div className="flex h-8 w-8 items-center justify-center rounded-full border border-current font-bold text-xs shrink-0">
                  {done ? <CheckCircle2 className="h-5 w-5" /> : <Icon className="h-4 w-4" />}
                </div>
                <span className="text-xs font-semibold truncate">{label}</span>
              </div>
            );
          })}
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 dark:bg-red-950/50 dark:border-red-900 dark:text-red-300 text-sm flex items-center gap-2">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-500" /><span>{error}</span>
          </div>
        )}

        {step === 1 && (
          <Card className="shadow-xl border-slate-200 dark:border-slate-800">
            <CardHeader>
              <CardTitle className="text-xl flex items-center gap-2"><GraduationCap className="h-5 w-5 text-primary" /> Choose your academic profile</CardTitle>
              <CardDescription>Select the grade and stream that apply to you. These options are configured by your administrator.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="profile-grade">Grade</Label>
                <select id="profile-grade" value={gradeLevelId} onChange={(event) => { setGradeLevelId(event.target.value); setError(""); }} className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm">
                  <option value="">{grades.length ? "Choose grade" : "No grades configured"}</option>
                  {grades.map((grade) => <option key={grade.id} value={grade.id}>{grade.name}</option>)}
                </select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="profile-stream">Stream</Label>
                <select id="profile-stream" value={streamId} onChange={(event) => { setStreamId(event.target.value); setError(""); }} disabled={!gradeLevelId || catalogLoading} className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm disabled:opacity-60">
                  <option value="">{catalogLoading ? "Loading options..." : "Choose stream"}</option>
                  {streams.map((stream) => <option key={stream.id} value={stream.id}>{stream.name}</option>)}
                </select>
                {gradeLevelId && !catalogLoading && streams.length === 0 && <p className="text-xs text-amber-700">No active streams are configured for this grade yet.</p>}
              </div>

              <div className="space-y-2">
                <Label htmlFor="profile-section">Class Section</Label>
                <select id="profile-section" value={sectionId} onChange={(event) => setSectionId(event.target.value)} disabled={sectionsLoading || sections.length === 0} className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm disabled:opacity-60">
                  <option value="">{sectionsLoading ? "Loading sections..." : "Choose class section"}</option>
                  {sections.map((section) => <option key={section.id} value={section.id}>{section.department} — {section.name} (Year {section.semester})</option>)}
                </select>
                {!sectionsLoading && sections.length === 0 && <p className="text-xs text-amber-700">No class sections are available. Contact your administrator.</p>}
              </div>

              <div className="flex justify-end pt-2">
                <Button onClick={handleNextStep1} disabled={catalogLoading || sectionsLoading} size="lg" className="gap-2 font-semibold">Continue <ChevronRight className="h-4 w-4" /></Button>
              </div>
            </CardContent>
          </Card>
        )}

        {step === 2 && (
          <Card className="shadow-xl border-slate-200 dark:border-slate-800">
            <CardHeader>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                  <CardTitle className="text-xl flex items-center gap-2"><BookOpen className="h-5 w-5 text-primary" /> Subjects for {selectedGrade?.name} · {selectedStream?.name}</CardTitle>
                  <CardDescription>These subjects are managed by your administrator. You can continue if none have been added yet.</CardDescription>
                </div>
                <Badge variant="secondary" className="w-fit text-xs px-3 py-1 font-semibold flex items-center gap-1"><Lock className="h-3 w-3 text-emerald-600" /> Admin-configured</Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-5">
              {catalogLoading ? (
                <div className="py-10 text-center text-slate-500 flex items-center justify-center gap-2"><LoaderCircle className="h-5 w-5 animate-spin" /> Loading subjects...</div>
              ) : subjects.length ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-96 overflow-y-auto pr-1">
                  {subjects.map((subject) => (
                    <div key={subject.id} className="p-4 rounded-xl border border-emerald-200 dark:border-emerald-950 bg-emerald-50/50 dark:bg-emerald-950/20 flex items-start gap-3">
                      <CheckCircle2 className="h-5 w-5 text-emerald-600 mt-0.5 shrink-0" />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-2"><span className="font-bold text-sm truncate">{subject.name}</span><Badge variant="outline" className="text-xs shrink-0 font-mono">{subject.code}</Badge></div>
                        <p className="text-xs text-slate-500 mt-1">{subject.department || selectedStream?.name}</p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="p-4 rounded-xl bg-slate-100 dark:bg-slate-900 text-sm text-slate-600 dark:text-slate-400">No active subjects are configured for this stream yet. You can still complete your profile.</div>
              )}

              <div className="flex justify-between pt-4 border-t">
                <Button variant="outline" onClick={() => setStep(1)} className="gap-2"><ChevronLeft className="h-4 w-4" /> Back</Button>
                <Button onClick={handleNextStep2} disabled={catalogLoading || facultyLoading} size="lg" className="gap-2 font-semibold">{facultyLoading ? <><LoaderCircle className="h-4 w-4 animate-spin" /> Loading...</> : <>Continue <ChevronRight className="h-4 w-4" /></>}</Button>
              </div>
            </CardContent>
          </Card>
        )}

        {step === 3 && (
          <Card className="shadow-xl border-slate-200 dark:border-slate-800">
            <CardHeader>
              <CardTitle className="text-xl flex items-center gap-2"><UserCheck className="h-5 w-5 text-primary" /> Class lecturer assignment</CardTitle>
              <CardDescription>Choose lecturers if they are listed. You can save your profile without assigning them now.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-5">
              {subjects.length === 0 ? (
                <div className="p-4 rounded-xl bg-slate-100 dark:bg-slate-900 text-sm text-slate-600 dark:text-slate-400">Your grade and stream are ready to save. Lecturers can be assigned after subjects are configured.</div>
              ) : (
                <div className="space-y-4 max-h-[28rem] overflow-y-auto pr-1">
                  {subjects.map((subject) => {
                    const facultyOptions = facultyMap[subject.id] || [];
                    return (
                      <div key={subject.id} className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 space-y-3 shadow-sm">
                        <div className="flex items-center justify-between gap-2 border-b pb-2">
                          <div><h4 className="font-bold">{subject.name}</h4><p className="text-xs text-slate-500">{subject.code} · {subject.department || selectedStream?.name}</p></div>
                          <Badge variant="secondary" className="w-fit text-xs">{facultyOptions.length} available</Badge>
                        </div>
                        <Label htmlFor={`faculty-${subject.id}`} className="text-xs font-semibold">Assigned lecturer (optional)</Label>
                        <select id={`faculty-${subject.id}`} value={selectedFaculty[subject.id] || ""} onChange={(event) => setSelectedFaculty((current) => ({ ...current, [subject.id]: event.target.value }))} className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm">
                          <option value="">No lecturer selected</option>
                          {facultyOptions.map((faculty) => <option key={faculty.id} value={faculty.id}>{faculty.name} ({faculty.email})</option>)}
                        </select>
                      </div>
                    );
                  })}
                </div>
              )}

              <div className="flex justify-between pt-4 border-t">
                <Button variant="outline" onClick={() => setStep(2)} className="gap-2"><ChevronLeft className="h-4 w-4" /> Back to subjects</Button>
                <Button onClick={handleSubmit} size="lg" disabled={submitting} className="gap-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold">
                  {submitting ? <><LoaderCircle className="h-4 w-4 animate-spin" /> Saving...</> : <><CheckCircle2 className="h-4 w-4" /> Save profile</>}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
