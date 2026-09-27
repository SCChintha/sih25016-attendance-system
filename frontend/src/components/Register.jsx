import { useEffect, useState } from "react";
import { ArrowLeft, LoaderCircle } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";

export function Register({ navigate }) {
  const { register, getSections, getApiErrors } = useAuth();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirmPassword: "", role: "", sectionId: "" });
  const [sections, setSections] = useState([]);
  const [sectionsLoading, setSectionsLoading] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  useEffect(() => {
    if (form.role !== "STUDENT") return;
    setSectionsLoading(true);
    getSections()
      .then(setSections)
      .catch(() => setErrors((current) => ({ ...current, sectionId: "Sections could not be loaded. Start the backend and try again." })))
      .finally(() => setSectionsLoading(false));
  }, [form.role]);

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!form.name.trim()) nextErrors.name = "Name is required.";
    if (!/^\S+@\S+\.\S+$/.test(form.email)) nextErrors.email = "Enter a valid email address.";
    if (form.password.length < 8) nextErrors.password = "Password must be at least 8 characters.";
    if (form.password !== form.confirmPassword) nextErrors.confirmPassword = "Passwords do not match.";
    if (!form.role) nextErrors.role = "Select a role.";
    if (form.role === "STUDENT" && !form.sectionId) nextErrors.sectionId = "Section is required for students.";
    if (Object.keys(nextErrors).length) return setErrors(nextErrors);
    setSubmitting(true); setErrors({});
    try {
      await register({ name: form.name, email: form.email, password: form.password, role: form.role, sectionId: form.role === "STUDENT" ? Number(form.sectionId) : null });
      navigate("/login");
    } catch (error) {
      const mapped = {};
      getApiErrors(error).forEach(({ field, message }) => { mapped[field || "form"] = message; });
      setErrors(mapped);
    } finally { setSubmitting(false); }
  }

  return <div className="min-h-screen bg-muted/50 p-4"><div className="mx-auto flex min-h-[90vh] max-w-lg items-center"><Card className="w-full"><CardHeader><button className="mb-5 flex w-fit items-center gap-2 text-sm text-muted-foreground hover:text-foreground" onClick={() => navigate("/")}><ArrowLeft className="h-4 w-4" /> Back</button><CardTitle>Create your account</CardTitle><CardDescription>Use your real account details to join SmartAttend.</CardDescription></CardHeader><CardContent><form onSubmit={handleSubmit} className="space-y-4">
    {errors.form && <p className="rounded-md bg-red-50 p-3 text-sm text-red-700">{errors.form}</p>}
    <div className="space-y-2"><Label htmlFor="name">Full name</Label><Input id="name" value={form.name} onChange={(e) => update("name", e.target.value)} />{errors.name && <p className="text-sm text-red-600">{errors.name}</p>}</div>
    <div className="space-y-2"><Label htmlFor="register-email">Email</Label><Input id="register-email" type="email" value={form.email} onChange={(e) => update("email", e.target.value)} />{errors.email && <p className="text-sm text-red-600">{errors.email}</p>}</div>
    <div className="grid gap-4 sm:grid-cols-2"><div className="space-y-2"><Label htmlFor="register-password">Password</Label><Input id="register-password" type="password" value={form.password} onChange={(e) => update("password", e.target.value)} />{errors.password && <p className="text-sm text-red-600">{errors.password}</p>}</div><div className="space-y-2"><Label htmlFor="confirm-password">Confirm password</Label><Input id="confirm-password" type="password" value={form.confirmPassword} onChange={(e) => update("confirmPassword", e.target.value)} />{errors.confirmPassword && <p className="text-sm text-red-600">{errors.confirmPassword}</p>}</div></div>
    <div className="space-y-2"><Label htmlFor="role">Role</Label><select id="role" value={form.role} onChange={(e) => update("role", e.target.value)} className="h-9 w-full rounded-md border bg-background px-3 text-sm"><option value="">Choose a role</option><option value="STUDENT">Student</option><option value="FACULTY">Faculty</option><option value="ADMIN">Administrator</option></select>{errors.role && <p className="text-sm text-red-600">{errors.role}</p>}</div>
    {form.role === "STUDENT" && <div className="space-y-2"><Label htmlFor="section-id">Section</Label><select id="section-id" value={form.sectionId} onChange={(e) => update("sectionId", e.target.value)} disabled={sectionsLoading || sections.length === 0} className="h-9 w-full rounded-md border bg-background px-3 text-sm"><option value="">{sectionsLoading ? "Loading sections..." : sections.length ? "Choose your section" : "No sections available"}</option>{sections.map((section) => <option key={section.id} value={section.id}>{section.department} - {section.name} (Semester {section.semester})</option>)}</select>{errors.sectionId && <p className="text-sm text-red-600">{errors.sectionId}</p>}</div>}
    <Button type="submit" className="w-full" disabled={submitting}>{submitting && <LoaderCircle className="h-4 w-4 animate-spin" />} Create account</Button>
    <p className="text-center text-sm text-muted-foreground">Already registered? <button type="button" className="font-medium text-primary underline" onClick={() => navigate("/login")}>Sign in</button></p>
  </form></CardContent></Card></div></div>;
}