import { useEffect, useMemo, useState } from "react";
import { api } from "../api";
import { Button } from "./ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Badge } from "./ui/badge";
import "./admin-management.css";

const errorText = (error) => error.response?.data?.errors?.[0]?.message || "The request could not be completed.";

export function AdminManagement({ mode, onChanged }) {
  if (mode === "users") return <UserManagement onChanged={onChanged} />;
  if (mode === "catalog") return <CatalogManagement />;
  return <FacultyAssignmentManagement />;
}

function UserManagement({ onChanged }) {
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [pages, setPages] = useState(0);
  const [total, setTotal] = useState(0);
  const [role, setRole] = useState("");
  const [search, setSearch] = useState("");
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [catalog, setCatalog] = useState({ grades: [], streams: [] });
  const [selected, setSelected] = useState(null);
  const [form, setForm] = useState(null);
  const [confirm, setConfirm] = useState(null);
  const [busy, setBusy] = useState(false);

  const loadUsers = async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await api.get("/admin/users", { params: { page, size: 10, role: role || undefined, search, includeDeleted } });
      setRows(data.content || []);
      setPages(data.totalPages || 0);
      setTotal(data.totalElements || 0);
    } catch (requestError) { setError(errorText(requestError)); }
    finally { setLoading(false); }
  };

  useEffect(() => { const timer = setTimeout(loadUsers, 250); return () => clearTimeout(timer); }, [page, role, search, includeDeleted]);
  useEffect(() => { api.get("/admin/academic/catalog").then(({ data }) => setCatalog(data)).catch(() => {}); }, []);

  const openUser = async (id) => {
    try {
      const { data } = await api.get(`/admin/users/${id}`);
      setSelected(data);
      setForm({ name: data.name, email: data.email, role: data.role, active: data.active, gradeLevelId: data.gradeLevelId || "", streamId: data.streamId || "" });
      setError("");
    } catch (requestError) { setError(errorText(requestError)); }
  };

  const saveUser = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api.patch(`/admin/users/${selected.id}`, {
        name: form.name,
        email: form.email,
        role: form.role,
        active: form.active,
        gradeLevelId: form.role === "STUDENT" && form.gradeLevelId ? Number(form.gradeLevelId) : null,
        streamId: form.role === "STUDENT" && form.streamId ? Number(form.streamId) : null
      });
      setSelected(null);
      await loadUsers();
      await onChanged?.();
    } catch (requestError) { setError(errorText(requestError)); }
    finally { setBusy(false); }
  };

  const performAction = async () => {
    if (!confirm) return;
    setBusy(true);
    setError("");
    try {
      if (confirm.action === "delete") await api.delete(`/admin/users/${confirm.user.id}`);
      else await api.patch(`/admin/users/${confirm.user.id}/status`, { active: confirm.action === "activate" });
      setConfirm(null);
      setSelected(null);
      await loadUsers();
      await onChanged?.();
    } catch (requestError) { setError(errorText(requestError)); setConfirm(null); }
    finally { setBusy(false); }
  };

  const availableStreams = catalog.streams.filter((stream) => String(stream.gradeId) === String(form?.gradeLevelId) && stream.active);

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>User management</CardTitle>
          <CardDescription>Search, review, update, suspend, reactivate, or soft-delete accounts. Results are paginated on the server.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-col gap-3 md:flex-row">
            <input aria-label="Search users" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Search name or email" className="h-10 flex-1 rounded-md border bg-background px-3 text-sm" />
            <select aria-label="Filter role" value={role} onChange={(event) => { setRole(event.target.value); setPage(0); }} className="h-10 rounded-md border bg-background px-3 text-sm">
              <option value="">All roles</option><option value="ADMIN">Admin</option><option value="FACULTY">Faculty</option><option value="STUDENT">Student</option>
            </select>
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={includeDeleted} onChange={(event) => setIncludeDeleted(event.target.checked)} />Include deleted</label>
          </div>
          {error && <p role="alert" className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[680px] text-left text-sm">
              <thead><tr className="border-b bg-muted/40"><th className="p-3">Name</th><th className="p-3">Email</th><th className="p-3">Role</th><th className="p-3">Status</th><th className="p-3 text-right">Profile</th></tr></thead>
              <tbody className="divide-y">
                {rows.map((user) => <tr key={user.id} className="hover:bg-muted/30">
                  <td className="p-3 font-medium">{user.name}</td><td className="p-3">{user.email}</td><td className="p-3"><Badge variant="outline">{user.role}</Badge></td>
                  <td className="p-3">{user.deleted ? <Badge variant="destructive">Deleted</Badge> : <Badge variant={user.active ? "secondary" : "destructive"}>{user.active ? "Active" : "Suspended"}</Badge>}</td>
                  <td className="p-3 text-right"><Button size="sm" variant="outline" onClick={() => openUser(user.id)}>Review / Edit</Button></td>
                </tr>)}
                {!loading && rows.length === 0 && <tr><td className="p-8 text-center text-muted-foreground" colSpan={5}>No matching users.</td></tr>}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
            <span>{loading ? "Loading users…" : `${total} account${total === 1 ? "" : "s"}`}</span>
            <div className="flex items-center gap-2"><Button variant="outline" size="sm" disabled={page <= 0 || loading} onClick={() => setPage((current) => current - 1)}>Previous</Button><span>Page {pages ? page + 1 : 0} of {pages}</span><Button variant="outline" size="sm" disabled={page + 1 >= pages || loading} onClick={() => setPage((current) => current + 1)}>Next</Button></div>
          </div>
        </CardContent>
      </Card>

      {selected && form && <Modal title="Review user profile" onClose={() => setSelected(null)}>
        <form onSubmit={saveUser} className="space-y-4">
          {error && <p role="alert" className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Full name"><input required maxLength={160} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} className="form-input" /></Field>
            <Field label="Email"><input required type="email" maxLength={254} value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="form-input" /></Field>
            <Field label="Role"><select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })} className="form-input"><option value="STUDENT">Student</option><option value="FACULTY">Faculty</option><option value="ADMIN">Admin</option></select></Field>
            <Field label="Account status"><span className="flex min-h-10 items-center"><Badge variant={selected.deleted ? "destructive" : form.active ? "secondary" : "destructive"}>{selected.deleted ? "Deleted" : form.active ? "Active" : "Suspended"}</Badge></span></Field>
            {form.role === "STUDENT" && <>
              <Field label="Grade"><select required value={form.gradeLevelId} onChange={(event) => setForm({ ...form, gradeLevelId: event.target.value, streamId: "" })} className="form-input"><option value="">Select grade</option>{catalog.grades.filter((grade) => grade.active).map((grade) => <option key={grade.id} value={grade.id}>{grade.name}</option>)}</select></Field>
              <Field label="Stream"><select required value={form.streamId} onChange={(event) => setForm({ ...form, streamId: event.target.value })} className="form-input"><option value="">Select stream</option>{availableStreams.map((stream) => <option key={stream.id} value={stream.id}>{stream.name}</option>)}</select></Field>
            </>}
          </div>
          <p className="text-xs text-muted-foreground">Created {selected.createdAt ? new Date(selected.createdAt).toLocaleString() : "—"}{selected.deleted ? " · Soft-deleted account" : ""}</p>
          <div className="flex flex-wrap justify-between gap-2 border-t pt-4">
            <div className="flex gap-2">{!selected.deleted && <>
              <Button type="button" variant="outline" onClick={() => setConfirm({ user: selected, action: form.active ? "suspend" : "activate" })}>{form.active ? "Suspend" : "Reactivate"}</Button>
              <Button type="button" variant="destructive" onClick={() => setConfirm({ user: selected, action: "delete" })}>Soft delete</Button>
            </>}</div>
            <div className="flex gap-2"><Button type="button" variant="ghost" onClick={() => setSelected(null)}>Close</Button><Button type="submit" disabled={busy || selected.deleted}>{busy ? "Saving…" : "Save profile"}</Button></div>
          </div>
        </form>
      </Modal>}
      {confirm && <Modal title="Confirm account action" onClose={() => setConfirm(null)}>
        <p className="text-sm">{confirm.action === "delete" ? `Soft-delete ${confirm.user.name}? This preserves their history and immediately revokes their access.` : confirm.action === "suspend" ? `Suspend ${confirm.user.name}? Their active tokens will stop working immediately.` : `Reactivate ${confirm.user.name}?`}</p>
        <div className="mt-5 flex justify-end gap-2"><Button variant="outline" onClick={() => setConfirm(null)}>Cancel</Button><Button variant={confirm.action === "delete" || confirm.action === "suspend" ? "destructive" : "default"} onClick={performAction} disabled={busy}>{busy ? "Applying…" : "Confirm"}</Button></div>
      </Modal>}
    </div>
  );
}

function CatalogManagement() {
  const [catalog, setCatalog] = useState({ grades: [], streams: [], subjects: [] });
  const [gradeName, setGradeName] = useState("");
  const [gradeOrder, setGradeOrder] = useState(0);
  const [streamForm, setStreamForm] = useState({ gradeId: "", code: "", name: "" });
  const [subjectForm, setSubjectForm] = useState({ streamId: "", code: "", name: "" });
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const activeStreams = catalog.streams.filter((stream) => stream.active);

  const refresh = async () => { const { data } = await api.get("/admin/academic/catalog"); setCatalog(data); };
  useEffect(() => { refresh().catch((requestError) => setError(errorText(requestError))); }, []);

  const submit = async (event, action) => {
    event.preventDefault(); setBusy(true); setError(""); setMessage("");
    try {
      if (action === "grade") await api.post("/admin/academic/grades", { name: gradeName, displayOrder: Number(gradeOrder) });
      if (action === "stream") await api.post(`/admin/academic/grades/${streamForm.gradeId}/streams`, { code: streamForm.code, name: streamForm.name });
      if (action === "subject") await api.post(`/admin/academic/streams/${subjectForm.streamId}/subjects`, { code: subjectForm.code, name: subjectForm.name });
      if (action === "grade") setGradeName("");
      if (action === "stream") setStreamForm((value) => ({ ...value, code: "", name: "" }));
      if (action === "subject") setSubjectForm((value) => ({ ...value, code: "", name: "" }));
      await refresh(); setMessage("Academic catalog saved.");
    } catch (requestError) { setError(errorText(requestError)); }
    finally { setBusy(false); }
  };

  const toggleSubject = async (subject) => {
    setError("");
    try { await api.patch(`/admin/academic/subjects/${subject.id}/active`, { active: !subject.active }); await refresh(); }
    catch (requestError) { setError(errorText(requestError)); }
  };

  return <div className="space-y-4">
    <Card><CardHeader><CardTitle>Academic taxonomy</CardTitle><CardDescription>Define grade levels, streams under each grade, and active subjects mapped directly to a stream.</CardDescription></CardHeader>
      <CardContent className="grid gap-5 lg:grid-cols-3">
        <form className="space-y-3 rounded-lg border p-4" onSubmit={(event) => submit(event, "grade")}><h3 className="font-semibold">Add grade</h3><Field label="Grade name"><input required maxLength={80} placeholder="11th Grade" value={gradeName} onChange={(event) => setGradeName(event.target.value)} className="form-input" /></Field><Field label="Display order"><input type="number" min="0" value={gradeOrder} onChange={(event) => setGradeOrder(event.target.value)} className="form-input" /></Field><Button disabled={busy} type="submit">Create grade</Button></form>
        <form className="space-y-3 rounded-lg border p-4" onSubmit={(event) => submit(event, "stream")}><h3 className="font-semibold">Add stream</h3><Field label="Grade"><select required value={streamForm.gradeId} onChange={(event) => setStreamForm({ ...streamForm, gradeId: event.target.value })} className="form-input"><option value="">Select grade</option>{catalog.grades.filter((grade) => grade.active).map((grade) => <option key={grade.id} value={grade.id}>{grade.name}</option>)}</select></Field><Field label="Code"><input required maxLength={32} placeholder="MPC" value={streamForm.code} onChange={(event) => setStreamForm({ ...streamForm, code: event.target.value })} className="form-input" /></Field><Field label="Name"><input required maxLength={120} placeholder="Maths, Physics & Chemistry" value={streamForm.name} onChange={(event) => setStreamForm({ ...streamForm, name: event.target.value })} className="form-input" /></Field><Button disabled={busy || !streamForm.gradeId} type="submit">Create stream</Button></form>
        <form className="space-y-3 rounded-lg border p-4" onSubmit={(event) => submit(event, "subject")}><h3 className="font-semibold">Add subject</h3><Field label="Grade · Stream"><select required value={subjectForm.streamId} onChange={(event) => setSubjectForm({ ...subjectForm, streamId: event.target.value })} className="form-input"><option value="">Select stream</option>{activeStreams.map((stream) => <option key={stream.id} value={stream.id}>{stream.gradeName} · {stream.name}</option>)}</select></Field><Field label="Subject code"><input required maxLength={32} placeholder="PHY11" value={subjectForm.code} onChange={(event) => setSubjectForm({ ...subjectForm, code: event.target.value })} className="form-input" /></Field><Field label="Subject name"><input required maxLength={160} placeholder="Physics" value={subjectForm.name} onChange={(event) => setSubjectForm({ ...subjectForm, name: event.target.value })} className="form-input" /></Field><Button disabled={busy || !subjectForm.streamId} type="submit">Create subject</Button></form>
      </CardContent>
    </Card>
    {error && <p role="alert" className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}{message && <p role="status" className="text-sm text-emerald-700">{message}</p>}
    <Card><CardHeader><CardTitle>Configured catalog</CardTitle></CardHeader><CardContent className="space-y-5">
      <div><h3 className="mb-2 text-sm font-semibold">Grades and streams</h3><div className="flex flex-wrap gap-2">{catalog.grades.map((grade) => <span key={grade.id} className="rounded-md border px-3 py-2 text-sm">{grade.name} <span className="text-muted-foreground">· {catalog.streams.filter((stream) => stream.gradeId === grade.id).map((stream) => stream.name).join(", ") || "No streams"}</span></span>)}</div></div>
      <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr className="border-b bg-muted/40"><th className="p-3">Subject</th><th className="p-3">Code</th><th className="p-3">Grade · Stream</th><th className="p-3">State</th><th className="p-3 text-right">Action</th></tr></thead><tbody className="divide-y">{catalog.subjects.map((subject) => <tr key={subject.id}><td className="p-3 font-medium">{subject.name}</td><td className="p-3">{subject.code}</td><td className="p-3">{subject.gradeName && subject.streamName ? `${subject.gradeName} · ${subject.streamName}` : "Legacy subject"}</td><td className="p-3">{subject.active ? "Active" : "Inactive"}</td><td className="p-3 text-right"><Button size="sm" variant="outline" onClick={() => toggleSubject(subject)}>{subject.active ? "Deactivate" : "Activate"}</Button></td></tr>)}{!catalog.subjects.length && <tr><td colSpan={5} className="p-6 text-center text-muted-foreground">No subjects have been created.</td></tr>}</tbody></table></div>
    </CardContent></Card>
  </div>;
}

function FacultyAssignmentManagement() {
  const [faculty, setFaculty] = useState([]);
  const [facultyId, setFacultyId] = useState("");
  const [profile, setProfile] = useState(null);
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const loadFaculty = async () => { const { data } = await api.get("/admin/faculty"); setFaculty(data); if (!facultyId && data.length) setFacultyId(String(data[0].id)); };
  const loadProfile = async (id) => {
    if (!id) { setProfile(null); return; }
    const { data } = await api.get(`/admin/faculty/${id}/subjects`);
    setProfile(data); setSelected((data.selectedSubjects || []).map((subject) => subject.id));
  };
  useEffect(() => { loadFaculty().catch((requestError) => setError(errorText(requestError))); }, []);
  useEffect(() => { loadProfile(facultyId).catch((requestError) => setError(errorText(requestError))); }, [facultyId]);

  const groups = useMemo(() => (profile?.availableSubjects || []).reduce((result, subject) => {
    const key = `${subject.gradeName} · ${subject.streamName}`;
    result[key] ||= []; result[key].push(subject); return result;
  }, {}), [profile]);

  const save = async () => {
    if (!selected.length) { setError("Select at least one active subject."); return; }
    setBusy(true); setError("");
    try { await api.put(`/admin/faculty/${facultyId}/subjects`, { subjectIds: selected }); await loadProfile(facultyId); await loadFaculty(); }
    catch (requestError) { setError(errorText(requestError)); }
    finally { setBusy(false); }
  };
  const unlock = async () => {
    setBusy(true); setError("");
    try { await api.post(`/admin/faculty/${facultyId}/subjects/unlock`); await loadProfile(facultyId); await loadFaculty(); }
    catch (requestError) { setError(errorText(requestError)); }
    finally { setBusy(false); }
  };

  return <div className="space-y-4">
    <Card><CardHeader><CardTitle>Faculty subject assignments</CardTitle><CardDescription>Administrators can replace a locked assignment or unlock it for faculty to make a new selection.</CardDescription></CardHeader><CardContent className="space-y-4">
      {error && <p role="alert" className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
      <div className="flex flex-wrap items-end gap-3"><Field label="Faculty"><select value={facultyId} onChange={(event) => setFacultyId(event.target.value)} className="form-input min-w-72"><option value="">Select faculty</option>{faculty.map((person) => <option key={person.id} value={person.id}>{person.name} · {person.email}</option>)}</select></Field>{profile && <Badge variant={profile.locked ? "default" : "secondary"}>{profile.locked ? "Selection locked" : "Selection open"}</Badge>}
        {profile?.locked && <Button variant="outline" onClick={unlock} disabled={busy}>Unlock for faculty</Button>}
      </div>
      {!profile ? <p className="py-8 text-center text-sm text-muted-foreground">Choose a faculty account and create academic subjects to manage assignments.</p> : !profile.availableSubjects.length ? <p className="py-8 text-center text-sm text-muted-foreground">No active grade and stream subjects are configured.</p> : <div className="space-y-5">{Object.entries(groups).map(([group, subjects]) => <section key={group}><h3 className="mb-2 text-sm font-semibold">{group}</h3><div className="grid gap-2 sm:grid-cols-2">{subjects.map((subject) => <label key={subject.id} className="flex items-center gap-3 rounded-lg border p-3"><input type="checkbox" checked={selected.includes(subject.id)} disabled={busy} onChange={() => setSelected((current) => current.includes(subject.id) ? current.filter((id) => id !== subject.id) : [...current, subject.id])} /><span>{subject.name} <span className="text-xs text-muted-foreground">({subject.code})</span></span></label>)}</div></section>)}<div className="flex justify-end border-t pt-4"><Button onClick={save} disabled={busy}>{busy ? "Saving…" : "Save and lock assignment"}</Button></div></div>}
    </CardContent></Card>
  </div>;
}

function Field({ label, children }) { return <label className="block space-y-1.5 text-sm"><span className="font-medium">{label}</span>{children}</label>; }
function Modal({ title, children, onClose }) { return <div role="presentation" className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}><section role="dialog" aria-modal="true" aria-label={title} className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-xl bg-background p-5 shadow-2xl"><div className="mb-4 flex items-center justify-between gap-4"><h2 className="text-lg font-semibold">{title}</h2><Button variant="ghost" size="sm" onClick={onClose}>Close</Button></div>{children}</section></div>; }
