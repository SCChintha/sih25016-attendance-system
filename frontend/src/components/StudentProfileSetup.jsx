import { useEffect, useState } from "react";
import { CheckCircle2, ChevronRight, ChevronLeft, BookOpen, UserCheck, GraduationCap, LoaderCircle, AlertCircle, ShieldCheck, Lock } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { api } from "../api";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";
import { Label } from "./ui/label";
import { Badge } from "./ui/badge";

// Intermediate Board Standard Streams & Fixed Board Curriculum Subject Definitions
const INTERMEDIATE_STREAMS = [
  { id: "MPC", name: "MPC — Maths, Physics, Chemistry" },
  { id: "BIPC", name: "BiPC — Biology (Botany/Zoology), Physics, Chemistry" },
  { id: "CEC", name: "CEC — Civics, Economics, Commerce" },
  { id: "MEC", name: "MEC — Maths, Economics, Commerce" },
  { id: "HEC", name: "HEC — History, Economics, Civics" }
];

const STREAM_SUBJECT_PRESETS = {
  "MPC-1": [
    { code: "MATH1A", name: "Mathematics 1A (Algebra & Trigonometry)", category: "Core Math" },
    { code: "MATH1B", name: "Mathematics 1B (Calculus & Coordinate Geometry)", category: "Core Math" },
    { code: "PHY1", name: "Physics (Paper 1)", category: "Science" },
    { code: "CHE1", name: "Chemistry (Paper 1)", category: "Science" },
    { code: "ENG1", name: "English (General)", category: "Language" },
    { code: "SAN1", name: "Sanskrit / Second Language (Paper 1)", category: "Language" }
  ],
  "MPC-2": [
    { code: "MATH2A", name: "Mathematics 2A (Complex Numbers & Probability)", category: "Core Math" },
    { code: "MATH2B", name: "Mathematics 2B (Calculus & Vectors)", category: "Core Math" },
    { code: "PHY2", name: "Physics (Paper 2)", category: "Science" },
    { code: "CHE2", name: "Chemistry (Paper 2)", category: "Science" },
    { code: "ENG2", name: "English (Paper 2)", category: "Language" },
    { code: "SAN2", name: "Sanskrit / Second Language (Paper 2)", category: "Language" }
  ],
  "BIPC-1": [
    { code: "BOT1", name: "Botany (Paper 1 - Plant Diversity)", category: "Biology" },
    { code: "ZOO1", name: "Zoology (Paper 1 - Structural Organization)", category: "Biology" },
    { code: "PHY1", name: "Physics (Paper 1)", category: "Science" },
    { code: "CHE1", name: "Chemistry (Paper 1)", category: "Science" },
    { code: "ENG1", name: "English (General)", category: "Language" },
    { code: "SAN1", name: "Sanskrit / Second Language (Paper 1)", category: "Language" }
  ],
  "BIPC-2": [
    { code: "BOT2", name: "Botany (Paper 2 - Plant Physiology)", category: "Biology" },
    { code: "ZOO2", name: "Zoology (Paper 2 - Human Anatomy)", category: "Biology" },
    { code: "PHY2", name: "Physics (Paper 2)", category: "Science" },
    { code: "CHE2", name: "Chemistry (Paper 2)", category: "Science" },
    { code: "ENG2", name: "English (Paper 2)", category: "Language" },
    { code: "SAN2", name: "Sanskrit / Second Language (Paper 2)", category: "Language" }
  ],
  "CEC-1": [
    { code: "CIV1", name: "Civics (Paper 1 - Political Theory)", category: "Humanities" },
    { code: "ECO1", name: "Economics (Paper 1 - Microeconomics)", category: "Commerce" },
    { code: "COM1", name: "Commerce & Accountancy (Paper 1)", category: "Commerce" },
    { code: "ENG1", name: "English (General)", category: "Language" },
    { code: "SAN1", name: "Sanskrit / Second Language (Paper 1)", category: "Language" }
  ],
  "CEC-2": [
    { code: "CIV2", name: "Civics (Paper 2 - Indian Constitution)", category: "Humanities" },
    { code: "ECO2", name: "Economics (Paper 2 - Indian Economy)", category: "Commerce" },
    { code: "COM2", name: "Commerce & Accountancy (Paper 2)", category: "Commerce" },
    { code: "ENG2", name: "English (Paper 2)", category: "Language" },
    { code: "SAN2", name: "Sanskrit / Second Language (Paper 2)", category: "Language" }
  ],
  "MEC-1": [
    { code: "MATH1A", name: "Mathematics 1A", category: "Core Math" },
    { code: "ECO1", name: "Economics (Paper 1)", category: "Commerce" },
    { code: "COM1", name: "Commerce & Accountancy (Paper 1)", category: "Commerce" },
    { code: "ENG1", name: "English (General)", category: "Language" },
    { code: "SAN1", name: "Sanskrit / Second Language (Paper 1)", category: "Language" }
  ],
  "MEC-2": [
    { code: "MATH2A", name: "Mathematics 2A", category: "Core Math" },
    { code: "ECO2", name: "Economics (Paper 2)", category: "Commerce" },
    { code: "COM2", name: "Commerce & Accountancy (Paper 2)", category: "Commerce" },
    { code: "ENG2", name: "English (Paper 2)", category: "Language" },
    { code: "SAN2", name: "Sanskrit / Second Language (Paper 2)", category: "Language" }
  ],
  "HEC-1": [
    { code: "HIS1", name: "History (Paper 1 - Ancient & Medieval)", category: "Humanities" },
    { code: "ECO1", name: "Economics (Paper 1)", category: "Commerce" },
    { code: "CIV1", name: "Civics (Paper 1)", category: "Humanities" },
    { code: "ENG1", name: "English (General)", category: "Language" },
    { code: "SAN1", name: "Sanskrit / Second Language (Paper 1)", category: "Language" }
  ],
  "HEC-2": [
    { code: "HIS2", name: "History (Paper 2 - Modern World)", category: "Humanities" },
    { code: "ECO2", name: "Economics (Paper 2)", category: "Commerce" },
    { code: "CIV2", name: "Civics (Paper 2)", category: "Humanities" },
    { code: "ENG2", name: "English (Paper 2)", category: "Language" },
    { code: "SAN2", name: "Sanskrit / Second Language (Paper 2)", category: "Language" }
  ]
};

export function StudentProfileSetup({ user, onComplete }) {
  const { getSections, updateUser } = useAuth();
  const [step, setStep] = useState(1);

  // Step 1: Stream, Year, Section
  const [year, setYear] = useState("1"); // "1" for 1st Year, "2" for 2nd Year
  const [stream, setStream] = useState("MPC");
  const [sections, setSections] = useState([]);
  const [sectionId, setSectionId] = useState("");
  const [sectionsLoading, setSectionsLoading] = useState(false);

  // Step 2: Fixed Stream Subjects
  const [fixedSubjects, setFixedSubjects] = useState([]);
  const [allDbSubjects, setAllDbSubjects] = useState([]); // Database subjects
  const [subjectsLocked, setSubjectsLocked] = useState(false);

  // Step 3: Lecturer Mapping
  const [facultyMap, setFacultyMap] = useState({}); // subjectId -> Array of faculty
  const [facultyLoading, setFacultyLoading] = useState(false);
  const [selectedFaculty, setSelectedFaculty] = useState({}); // subjectId -> facultyId

  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Load sections on mount
  useEffect(() => {
    setSectionsLoading(true);
    getSections()
      .then((data) => {
        setSections(data);
        if (data.length > 0) {
          setSectionId(data[0].id);
        }
      })
      .catch(() => setError("Failed to fetch sections. Please refresh."))
      .finally(() => setSectionsLoading(false));
  }, []);

  // Fetch all DB subjects once to map code to DB ID
  useEffect(() => {
    api.get("/subjects/by-grade")
      .then(({ data }) => setAllDbSubjects(data))
      .catch(() => {});
  }, []);

  // Handle Step 1 -> Step 2
  const handleNextStep1 = () => {
    if (!sectionId) return setError("Please select your allocated class section.");
    setError("");

    // Look up fixed preset subjects for chosen Stream + Year
    const presetKey = `${stream}-${year}`;
    const preset = STREAM_SUBJECT_PRESETS[presetKey] || STREAM_SUBJECT_PRESETS["MPC-1"];

    // Match presets to actual DB subjects
    const resolved = preset.map((p) => {
      const dbMatch = allDbSubjects.find((s) => s.code.toUpperCase() === p.code.toUpperCase());
      return {
        ...p,
        id: dbMatch ? dbMatch.id : null,
        dbName: dbMatch ? dbMatch.name : p.name
      };
    });

    setFixedSubjects(resolved);
    setSubjectsLocked(true);
    setStep(2);
  };

  // Handle Step 2 -> Step 3
  const handleNextStep2 = async () => {
    if (!subjectsLocked) return setError("Please verify and lock your board subjects to continue.");
    setError("");
    setFacultyLoading(true);

    try {
      const map = {};
      const selections = { ...selectedFaculty };

      for (const subj of fixedSubjects) {
        if (!subj.id) continue;
        const { data } = await api.get("/faculty/by-subject", {
          params: { subjectId: subj.id }
        });
        map[subj.id] = data;
        // Pre-select first available lecturer if available
        if (!selections[subj.id] && data.length > 0) {
          selections[subj.id] = data[0].id;
        }
      }

      setFacultyMap(map);
      setSelectedFaculty(selections);
      setStep(3);
    } catch {
      setError("Unable to load faculty lecturers. Please try again.");
    } finally {
      setFacultyLoading(false);
    }
  };

  // Submit Profile Setup
  const handleSubmit = async () => {
    // Validate faculty selection for every subject
    for (const subj of fixedSubjects) {
      if (subj.id && !selectedFaculty[subj.id]) {
        return setError(`Please assign a lecturer for subject: ${subj.name}`);
      }
    }

    setSubmitting(true);
    setError("");

    const academicGradeStr = `Intermediate ${year === "1" ? "1st" : "2nd"} Year — ${stream}`;

    try {
      const mappings = fixedSubjects
        .filter((subj) => subj.id)
        .map((subj) => ({
          subjectId: Number(subj.id),
          facultyId: Number(selectedFaculty[subj.id])
        }));

      const payload = {
        sectionId: Number(sectionId),
        academicGrade: academicGradeStr,
        mappings
      };

      const { data } = await api.post("/students/profile-setup", payload);

      updateUser({
        onboardingCompleted: true,
        sectionId: Number(sectionId),
        academicGrade: academicGradeStr
      });

      if (onComplete) onComplete(data);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save profile setup. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 py-8 px-4 flex items-center justify-center">
      <div className="w-full max-w-4xl space-y-6">
        
        {/* Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full bg-primary/10 text-primary text-xs font-bold tracking-wide uppercase">
            <GraduationCap className="h-4 w-4" /> Intermediate Student Profile Setup
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
            Welcome, {user?.name}!
          </h1>
          <p className="text-sm text-slate-600 dark:text-slate-400 max-w-lg mx-auto">
            Configure your Academic Stream, verify your Board-provided subjects, and map your class lecturers.
          </p>
        </div>

        {/* Stepper Navigation */}
        <div className="grid grid-cols-3 gap-3 bg-white dark:bg-slate-900 p-4 rounded-2xl shadow-sm border border-slate-200 dark:border-slate-800">
          {[
            { id: 1, label: "Step 1: Stream & Section", icon: GraduationCap },
            { id: 2, label: "Step 2: Fixed Stream Subjects", icon: BookOpen },
            { id: 3, label: "Step 3: Lecturer Assignment", icon: UserCheck }
          ].map((item) => {
            const Icon = item.icon;
            const isActive = step === item.id;
            const isDone = step > item.id;
            return (
              <div
                key={item.id}
                className={`flex items-center gap-3 p-3.5 rounded-xl transition-all ${
                  isActive
                    ? "bg-primary text-primary-foreground shadow-md"
                    : isDone
                    ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-400"
                    : "text-slate-400 bg-slate-100/60 dark:bg-slate-800/40"
                }`}
              >
                <div className="flex h-8 w-8 items-center justify-center rounded-full border border-current font-bold text-xs shrink-0">
                  {isDone ? <CheckCircle2 className="h-5 w-5" /> : <Icon className="h-4 w-4" />}
                </div>
                <span className="text-xs font-semibold truncate">{item.label}</span>
              </div>
            );
          })}
        </div>

        {error && (
          <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 dark:bg-red-950/50 dark:border-red-900 dark:text-red-300 text-sm flex items-center gap-2">
            <AlertCircle className="h-5 w-5 shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
        )}

        {/* STEP 1: Stream & Year Selection */}
        {step === 1 && (
          <Card className="shadow-xl border-slate-200 dark:border-slate-800">
            <CardHeader>
              <CardTitle className="text-xl flex items-center gap-2">
                <GraduationCap className="h-5 w-5 text-primary" /> Step 1: Select Academic Stream, Year & Section
              </CardTitle>
              <CardDescription>
                Choose your Intermediate standard (1st or 2nd Year), your core group/stream, and allocated section.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              
              {/* Year Selection */}
              <div className="space-y-2">
                <Label className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Academic Year
                </Label>
                <div className="grid grid-cols-2 gap-4">
                  {[
                    { id: "1", title: "1st Year (Junior Inter / 11th)", desc: "Intermediate First Year Curriculum" },
                    { id: "2", title: "2nd Year (Senior Inter / 12th)", desc: "Intermediate Second Year Curriculum" }
                  ].map((y) => (
                    <div
                      key={y.id}
                      onClick={() => setYear(y.id)}
                      className={`p-4 rounded-xl border-2 cursor-pointer transition-all ${
                        year === y.id
                          ? "border-primary bg-primary/5 shadow-sm"
                          : "border-slate-200 dark:border-slate-800 hover:border-slate-300"
                      }`}
                    >
                      <p className="font-bold text-sm text-slate-900 dark:text-slate-100">{y.title}</p>
                      <p className="text-xs text-slate-500 mt-1">{y.desc}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Stream Selection */}
              <div className="space-y-2">
                <Label htmlFor="stream-select" className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Core Stream / Group
                </Label>
                <select
                  id="stream-select"
                  value={stream}
                  onChange={(e) => setStream(e.target.value)}
                  className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm font-medium focus:ring-2 focus:ring-primary focus:outline-none"
                >
                  {INTERMEDIATE_STREAMS.map((st) => (
                    <option key={st.id} value={st.id}>
                      {st.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Section Selection */}
              <div className="space-y-2">
                <Label htmlFor="section-select" className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Allocated Class Section
                </Label>
                <select
                  id="section-select"
                  value={sectionId}
                  onChange={(e) => setSectionId(e.target.value)}
                  disabled={sectionsLoading || sections.length === 0}
                  className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm font-medium focus:ring-2 focus:ring-primary focus:outline-none disabled:opacity-60"
                >
                  <option value="">
                    {sectionsLoading ? "Loading class sections..." : "Choose allocated section"}
                  </option>
                  {sections.map((sec) => (
                    <option key={sec.id} value={sec.id}>
                      {sec.department} — {sec.name} (Year {sec.semester})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end pt-4">
                <Button onClick={handleNextStep1} size="lg" className="gap-2 font-semibold">
                  Next: Lock Board Subjects <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* STEP 2: Fixed Stream Subjects */}
        {step === 2 && (
          <Card className="shadow-xl border-slate-200 dark:border-slate-800">
            <CardHeader>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                  <CardTitle className="text-xl flex items-center gap-2">
                    <BookOpen className="h-5 w-5 text-primary" /> Step 2: Fixed Board-Provided Subjects
                  </CardTitle>
                  <CardDescription>
                    These are the mandatory Board-provided subjects for Intermediate {year === "1" ? "1st" : "2nd"} Year ({stream}).
                  </CardDescription>
                </div>
                <Badge variant="secondary" className="w-fit text-xs px-3 py-1 font-semibold flex items-center gap-1">
                  <Lock className="h-3 w-3 text-emerald-600" /> Fixed Board Curriculum
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-96 overflow-y-auto pr-1">
                {fixedSubjects.map((subj, idx) => (
                  <div
                    key={subj.code || idx}
                    className="p-4 rounded-xl border border-emerald-200 dark:border-emerald-950 bg-emerald-50/50 dark:bg-emerald-950/20 flex items-start gap-3"
                  >
                    <CheckCircle2 className="h-5 w-5 text-emerald-600 mt-0.5 shrink-0" />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-bold text-sm text-slate-900 dark:text-slate-100 truncate">
                          {subj.name}
                        </span>
                        <Badge variant="outline" className="text-xs shrink-0 font-mono">
                          {subj.code}
                        </Badge>
                      </div>
                      <p className="text-xs text-slate-500 mt-1">{subj.category}</p>
                    </div>
                  </div>
                ))}
              </div>

              <div className="p-4 rounded-xl bg-slate-100 dark:bg-slate-900 text-xs text-slate-600 dark:text-slate-400 flex items-center gap-2">
                <ShieldCheck className="h-4 w-4 text-primary shrink-0" />
                <span>All listed subjects are verified based on Intermediate Board guidelines for {stream} Stream.</span>
              </div>

              <div className="flex justify-between pt-4 border-t">
                <Button variant="outline" onClick={() => setStep(1)} className="gap-2">
                  <ChevronLeft className="h-4 w-4" /> Back to Stream
                </Button>
                <Button onClick={handleNextStep2} size="lg" className="gap-2 font-semibold">
                  Next: Assign Lecturers <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* STEP 3: Lecturer Mapping */}
        {step === 3 && (
          <Card className="shadow-xl border-slate-200 dark:border-slate-800">
            <CardHeader>
              <CardTitle className="text-xl flex items-center gap-2">
                <UserCheck className="h-5 w-5 text-primary" /> Step 3: Class Lecturer Assignment
              </CardTitle>
              <CardDescription>
                For each subject, pick the specific faculty member / lecturer assigned to teach your class.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {facultyLoading ? (
                <div className="py-12 text-center text-slate-500 flex items-center justify-center gap-2">
                  <LoaderCircle className="h-5 w-5 animate-spin text-primary" /> Loading subject lecturers...
                </div>
              ) : (
                <div className="space-y-4 max-h-[28rem] overflow-y-auto pr-1">
                  {fixedSubjects.map((subj) => {
                    const subjId = subj.id;
                    const facultyOptions = facultyMap[subjId] || [];
                    const chosenFacultyId = selectedFaculty[subjId] || "";

                    return (
                      <div
                        key={subj.code}
                        className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 space-y-3 shadow-sm"
                      >
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b pb-2">
                          <div>
                            <h4 className="font-bold text-slate-900 dark:text-slate-100">
                              {subj.name}
                            </h4>
                            <p className="text-xs text-slate-500">
                              Code: {subj.code} • Category: {subj.category}
                            </p>
                          </div>
                          <Badge variant="secondary" className="w-fit text-xs font-medium">
                            {facultyOptions.length} Lecturer(s) Available
                          </Badge>
                        </div>

                        <div className="space-y-1">
                          <Label className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                            Select Assigned Lecturer
                          </Label>
                          <select
                            value={chosenFacultyId}
                            onChange={(e) =>
                              setSelectedFaculty((prev) => ({
                                ...prev,
                                [subjId]: Number(e.target.value)
                              }))
                            }
                            className="w-full h-11 px-3 rounded-lg border border-slate-300 dark:border-slate-700 bg-background text-sm font-medium focus:ring-2 focus:ring-primary focus:outline-none"
                          >
                            <option value="">-- Choose Lecturer --</option>
                            {facultyOptions.map((fac) => (
                              <option key={fac.id} value={fac.id}>
                                {fac.name} ({fac.email})
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              <div className="flex justify-between pt-4 border-t">
                <Button variant="outline" onClick={() => setStep(2)} className="gap-2">
                  <ChevronLeft className="h-4 w-4" /> Back to Subjects
                </Button>
                <Button
                  onClick={handleSubmit}
                  size="lg"
                  disabled={submitting || facultyLoading}
                  className="gap-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold"
                >
                  {submitting ? (
                    <>
                      <LoaderCircle className="h-4 w-4 animate-spin" /> Saving Profile Setup...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="h-4 w-4" /> Save Profile & Complete Setup
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
}
