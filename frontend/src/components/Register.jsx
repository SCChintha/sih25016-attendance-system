import { useState } from "react";
import { ArrowLeft, LoaderCircle, ShieldCheck, UserCheck, GraduationCap } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";

export function Register({ navigate }) {
  const { register, getApiErrors } = useAuth();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    role: "STUDENT"
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = {};

    if (!form.name.trim()) nextErrors.name = "Full Name is required.";
    if (!/^\S+@\S+\.\S+$/.test(form.email)) nextErrors.email = "Enter a valid email address.";
    if (form.password.length < 8) nextErrors.password = "Password must be at least 8 characters.";
    if (form.password !== form.confirmPassword) nextErrors.confirmPassword = "Passwords do not match.";
    if (!form.role) nextErrors.role = "Role selection is required.";

    if (Object.keys(nextErrors).length > 0) return setErrors(nextErrors);

    setSubmitting(true);
    setErrors({});

    try {
      await register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role
      });
      navigate("/login");
    } catch (error) {
      const mapped = {};
      getApiErrors(error).forEach(({ field, message }) => {
        mapped[field || "form"] = message;
      });
      setErrors(mapped);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 p-4 flex items-center justify-center">
      <div className="w-full max-w-lg space-y-4">
        <Card className="border-slate-200 dark:border-slate-800 shadow-xl">
          <CardHeader className="space-y-3">
            <button
              className="flex w-fit items-center gap-2 text-sm text-slate-500 hover:text-slate-900 dark:hover:text-slate-100 transition-colors"
              onClick={() => navigate("/")}
            >
              <ArrowLeft className="h-4 w-4" /> Back to Home
            </button>
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold text-lg">
                SA
              </div>
              <div>
                <CardTitle className="text-2xl font-bold tracking-tight">Create SmartAttend Account</CardTitle>
                <CardDescription className="text-xs">
                  Intermediate College Automated Attendance System (SIH 2025 - PS 25016)
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              {errors.form && (
                <div className="rounded-lg bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-900 p-3 text-sm text-red-700 dark:text-red-300">
                  {errors.form}
                </div>
              )}

              {/* Full Name */}
              <div className="space-y-1.5">
                <Label htmlFor="name" className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Full Name
                </Label>
                <Input
                  id="name"
                  placeholder="e.g. Rahul Sharma"
                  value={form.name}
                  onChange={(e) => update("name", e.target.value)}
                  className="h-10"
                />
                {errors.name && <p className="text-xs text-red-600 font-medium">{errors.name}</p>}
              </div>

              {/* Email Address */}
              <div className="space-y-1.5">
                <Label htmlFor="register-email" className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Email Address
                </Label>
                <Input
                  id="register-email"
                  type="email"
                  placeholder="e.g. rahul.sharma@college.edu"
                  value={form.email}
                  onChange={(e) => update("email", e.target.value)}
                  className="h-10"
                />
                {errors.email && <p className="text-xs text-red-600 font-medium">{errors.email}</p>}
              </div>

              {/* Password Fields */}
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <Label htmlFor="register-password" className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                    Password
                  </Label>
                  <Input
                    id="register-password"
                    type="password"
                    placeholder="••••••••"
                    value={form.password}
                    onChange={(e) => update("password", e.target.value)}
                    className="h-10"
                  />
                  {errors.password && <p className="text-xs text-red-600 font-medium">{errors.password}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="confirm-password" className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                    Confirm Password
                  </Label>
                  <Input
                    id="confirm-password"
                    type="password"
                    placeholder="••••••••"
                    value={form.confirmPassword}
                    onChange={(e) => update("confirmPassword", e.target.value)}
                    className="h-10"
                  />
                  {errors.confirmPassword && <p className="text-xs text-red-600 font-medium">{errors.confirmPassword}</p>}
                </div>
              </div>

              {/* Lightweight Role Selection Dropdown */}
              <div className="space-y-1.5">
                <Label htmlFor="role" className="text-xs font-semibold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                  Account Role
                </Label>
                <select
                  id="role"
                  value={form.role}
                  onChange={(e) => update("role", e.target.value)}
                  className="h-10 w-full rounded-md border border-slate-300 dark:border-slate-700 bg-background px-3 text-sm font-medium focus:ring-2 focus:ring-primary focus:outline-none"
                >
                  <option value="STUDENT">Student (Intermediate 11th / 12th)</option>
                  <option value="FACULTY">Faculty / Lecturer</option>
                </select>
                {errors.role && <p className="text-xs text-red-600 font-medium">{errors.role}</p>}
                <p className="text-xs text-slate-500 flex items-center gap-1 mt-1">
                  <ShieldCheck className="h-3.5 w-3.5 text-emerald-600" />
                  Stream subjects & lecturer mapping will be configured upon your first login.
                </p>
              </div>

              <Button type="submit" className="w-full h-11 text-sm font-semibold" disabled={submitting}>
                {submitting ? (
                  <span className="flex items-center justify-center gap-2">
                    <LoaderCircle className="h-4 w-4 animate-spin" /> Creating Account...
                  </span>
                ) : (
                  <span className="flex items-center justify-center gap-2">
                    {form.role === "STUDENT" ? <GraduationCap className="h-4 w-4" /> : <UserCheck className="h-4 w-4" />}
                    Register as {form.role === "STUDENT" ? "Student" : "Faculty"}
                  </span>
                )}
              </Button>

              <p className="text-center text-sm text-slate-500 pt-2">
                Already registered?{" "}
                <button
                  type="button"
                  className="font-semibold text-primary hover:underline"
                  onClick={() => navigate("/login")}
                >
                  Sign in to your Portal
                </button>
              </p>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}