import { useState } from "react";
import { ArrowLeft, LoaderCircle } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";

export function Login({ navigate }) {
  const { login, getApiErrors } = useAuth();
  const [form, setForm] = useState({ email: "", password: "" });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = {};
    if (!form.email) nextErrors.email = "Email is required.";
    if (!form.password) nextErrors.password = "Password is required.";
    if (Object.keys(nextErrors).length) return setErrors(nextErrors);
    setSubmitting(true); setErrors({});
    try {
      const user = await login(form);
      navigate(`/dashboard/${user.role.toLowerCase()}`);
    } catch (error) {
      const mapped = {};
      getApiErrors(error).forEach(({ field, message }) => { mapped[field || "form"] = message; });
      setErrors(mapped);
    } finally { setSubmitting(false); }
  }

  return <div className="min-h-screen bg-muted/50 p-4"><div className="mx-auto flex min-h-[90vh] max-w-md items-center"><Card className="w-full"><CardHeader><button className="mb-5 flex w-fit items-center gap-2 text-sm text-muted-foreground hover:text-foreground" onClick={() => navigate("/")}><ArrowLeft className="h-4 w-4" /> Back</button><CardTitle>Welcome back</CardTitle><CardDescription>Sign in with your SmartAttend account.</CardDescription></CardHeader><CardContent><form onSubmit={handleSubmit} className="space-y-4">
    {errors.form && <p className="rounded-md bg-red-50 p-3 text-sm text-red-700">{errors.form}</p>}
    <div className="space-y-2"><Label htmlFor="email">Email</Label><Input id="email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} aria-invalid={Boolean(errors.email)} />{errors.email && <p className="text-sm text-red-600">{errors.email}</p>}</div>
    <div className="space-y-2"><Label htmlFor="password">Password</Label><Input id="password" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} aria-invalid={Boolean(errors.password)} />{errors.password && <p className="text-sm text-red-600">{errors.password}</p>}</div>
    <Button type="submit" className="w-full" disabled={submitting}>{submitting && <LoaderCircle className="h-4 w-4 animate-spin" />} Sign in</Button>
    <p className="text-center text-sm text-muted-foreground">New here? <button type="button" className="font-medium text-primary underline" onClick={() => navigate("/register")}>Create an account</button></p>
  </form></CardContent></Card></div></div>;
}
