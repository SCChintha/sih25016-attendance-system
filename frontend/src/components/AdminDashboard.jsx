import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "./ui/card";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";
import { Progress } from "./ui/progress";
import { Users, GraduationCap, TrendingUp, Download, AlertTriangle, BookOpen, ShieldCheck, Edit3, Trash2, Search, RefreshCw, FileText } from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from "recharts";
import { api } from "../api";
import { AdminManagement } from "./AdminManagement";

const COLORS = ["#18181b", "#52525b", "#a1a1aa", "#d4d4d8"];

export function AdminDashboard() {
  const [dashboard, setDashboard] = useState(null);
  const [adminRecords, setAdminRecords] = useState(null);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const [error, setError] = useState("");
  const [mappingSearch, setMappingSearch] = useState("");
  const [editingMapping, setEditingMapping] = useState(null);
  const [overrideFacultyId, setOverrideFacultyId] = useState("");
  const [facultyList, setFacultyList] = useState([]);
  const [submittingOverride, setSubmittingOverride] = useState(false);
  const [actionMsg, setActionMsg] = useState("");

  const loadDashboard = async () => {
    try {
      setError("");
      const { data } = await api.get("/dashboard/admin");
      setDashboard(data);
    } catch (requestError) {
      const status = requestError.response?.status;
      if (status === 403) {
        setError("This account does not have administrator access. Sign in with an ADMIN account.");
      } else if (status >= 500) {
        setError(`The server could not load system analytics (HTTP ${status}). Restart the backend and check its console for the error details.`);
      } else if (!requestError.response) {
        setError("Could not reach the backend. Make sure the Spring Boot server is running, then try again.");
      } else {
        setError("System analytics could not be loaded. Please try again.");
      }
    }
  };

  const loadAdminRecords = async () => {
    setLoadingRecords(true);
    try {
      const { data } = await api.get("/admin/records");
      setAdminRecords(data);
    } catch {
      setError("Failed to load global structural logs and mapping records.");
    } finally {
      setLoadingRecords(false);
    }
  };

  const refreshAdminData = async () => {
    await Promise.all([loadDashboard(), loadAdminRecords()]);
  };

  useEffect(() => {
    loadDashboard();
    loadAdminRecords();
  }, []);

  const exportSystemReport = () => {
    if (!dashboard) return;
    const report = {
      generatedAt: new Date().toISOString(),
      overview: {
        totalStudents: dashboard.totalStudents,
        totalFaculty: dashboard.totalFaculty,
        activeClasses: dashboard.activeClasses,
        overallAttendance: dashboard.overallAttendance
      },
      departments: dashboard.departments,
      atRiskStudents: dashboard.atRiskStudents,
      topClasses: dashboard.topClasses,
      academicMappings: adminRecords?.mappings || [],
      structuralLogs: adminRecords?.logs || []
    };
    const link = document.createElement("a");
    link.href = URL.createObjectURL(new Blob([JSON.stringify(report, null, 2)], { type: "application/json" }));
    link.download = `smartattend-system-report-${new Date().toISOString().split("T")[0]}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const handleStartOverride = async (mapping) => {
    setEditingMapping(mapping);
    setOverrideFacultyId(mapping.facultyId);
    try {
      const { data } = await api.get(`/faculty/by-subject?subjectId=${mapping.subjectId}`);
      setFacultyList(data);
    } catch {
      setFacultyList([]);
    }
  };

  const handleSaveOverride = async (mappingId) => {
    if (!overrideFacultyId) return;
    setSubmittingOverride(true);
    setActionMsg("");
    try {
      await api.put(`/admin/mappings/${mappingId}`, {
        facultyId: Number(overrideFacultyId)
      });
      setActionMsg("Student-Lecturer mapping successfully updated!");
      setEditingMapping(null);
      loadAdminRecords();
    } catch {
      setActionMsg("Failed to override mapping.");
    } finally {
      setSubmittingOverride(false);
    }
  };

  const handleDeleteMapping = async (mappingId) => {
    if (!window.confirm("Are you sure you want to delete this Student-Lecturer mapping definition?")) return;
    try {
      await api.delete(`/admin/mappings/${mappingId}`);
      setActionMsg("Mapping removed successfully.");
      loadAdminRecords();
    } catch {
      setActionMsg("Failed to delete mapping.");
    }
  };

  if (!dashboard && !error) return <div className="py-12 text-center text-muted-foreground">Loading system analytics...</div>;
  if (!dashboard) return (
    <div className="space-y-5">
      <div className="rounded-md border border-destructive/20 bg-destructive/5 py-5 text-center">
        <p className="text-destructive">{error}</p>
        <Button variant="outline" className="mt-4" onClick={loadDashboard}>Retry analytics</Button>
      </div>
      <Tabs defaultValue="users" className="space-y-4">
        <TabsList className="h-auto w-full flex-wrap justify-start gap-1">
          <TabsTrigger value="users">Users</TabsTrigger>
          <TabsTrigger value="catalog">Academic Setup</TabsTrigger>
          <TabsTrigger value="faculty">Faculty Subjects</TabsTrigger>
        </TabsList>
        <TabsContent value="users"><AdminManagement mode="users" onChanged={refreshAdminData} /></TabsContent>
        <TabsContent value="catalog"><AdminManagement mode="catalog" /></TabsContent>
        <TabsContent value="faculty"><AdminManagement mode="faculty" /></TabsContent>
      </Tabs>
    </div>
  );

  const filteredMappings = (adminRecords?.mappings || []).filter((m) => {
    const q = mappingSearch.toLowerCase();
    return (
      m.studentName.toLowerCase().includes(q) ||
      m.studentEmail.toLowerCase().includes(q) ||
      m.subjectName.toLowerCase().includes(q) ||
      m.subjectCode.toLowerCase().includes(q) ||
      m.facultyName.toLowerCase().includes(q) ||
      m.sectionName.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-semibold">System Administration & Control Panel</h2>
          <p className="text-muted-foreground text-sm">Monitor system health, override student-lecturer mappings, and inspect structural logs</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={loadAdminRecords} className="gap-2">
            <RefreshCw className="h-4 w-4" /> Refresh Records
          </Button>
          <Button onClick={exportSystemReport} className="gap-2">
            <Download className="h-4 w-4" /> Export Report
          </Button>
        </div>
      </div>

      {error && <p className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">{error}</p>}
      {actionMsg && <p className="rounded-md border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700">{actionMsg}</p>}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {[
          ["Total Students", dashboard.totalStudents.toLocaleString(), GraduationCap],
          ["Total Faculty", dashboard.totalFaculty, Users],
          ["Active Mappings", adminRecords?.totalMappings || 0, ShieldCheck],
          ["System Attendance", `${dashboard.overallAttendance}%`, TrendingUp]
        ].map(([label, value, Icon]) => (
          <Card key={label}>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">{label}</p>
                  <p className="text-2xl font-semibold">{value}</p>
                </div>
                <Icon className="h-8 w-8 text-primary" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <Tabs defaultValue="mappings" className="space-y-4">
        <TabsList className="h-auto w-full flex-wrap justify-start gap-1">
          <TabsTrigger value="mappings">Academic Mappings ({adminRecords?.totalMappings || 0})</TabsTrigger>
          <TabsTrigger value="logs">Structural Audit Logs</TabsTrigger>
          <TabsTrigger value="analytics">Analytics</TabsTrigger>
          <TabsTrigger value="departments">Departments & Streams</TabsTrigger>
        <TabsTrigger value="alerts">Alerts & Reports</TabsTrigger>
        <TabsTrigger value="users">Users</TabsTrigger>
        <TabsTrigger value="catalog">Academic Setup</TabsTrigger>
        <TabsTrigger value="faculty-subjects">Faculty Subjects</TabsTrigger>
      </TabsList>

        <TabsContent value="users"><AdminManagement mode="users" onChanged={refreshAdminData} /></TabsContent>
        <TabsContent value="catalog"><AdminManagement mode="catalog" /></TabsContent>
        <TabsContent value="faculty-subjects"><AdminManagement mode="faculty" /></TabsContent>

        {/* TAB 1: ACADEMIC MAPPINGS MANAGEMENT */}
        <TabsContent value="mappings" className="space-y-4">
          <Card>
            <CardHeader>
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <CardTitle className="text-xl flex items-center gap-2">
                    <ShieldCheck className="h-5 w-5 text-primary" /> Student-Lecturer Mapping Definitions
                  </CardTitle>
                  <CardDescription>
                    Review, search, and override any incorrect Student-Lecturer class mapping definitions across all streams.
                  </CardDescription>
                </div>
                <div className="relative w-full sm:w-72">
                  <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
                  <input
                    type="text"
                    placeholder="Search student, subject, lecturer..."
                    value={mappingSearch}
                    onChange={(e) => setMappingSearch(e.target.value)}
                    className="w-full h-9 pl-9 pr-3 rounded-md border text-sm bg-background"
                  />
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {loadingRecords ? (
                <div className="py-8 text-center text-muted-foreground">Loading academic mapping records...</div>
              ) : filteredMappings.length === 0 ? (
                <p className="py-8 text-center text-sm text-muted-foreground">No student-lecturer mapping records found.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm text-left border-collapse">
                    <thead>
                      <tr className="border-b bg-muted/40">
                        <th className="p-3 font-semibold">Student Name & Stream</th>
                        <th className="p-3 font-semibold">Section</th>
                        <th className="p-3 font-semibold">Subject</th>
                        <th className="p-3 font-semibold">Assigned Lecturer</th>
                        <th className="p-3 font-semibold text-right">Actions / Override</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {filteredMappings.map((m) => {
                        const isEditing = editingMapping?.id === m.id;

                        return (
                          <tr key={m.id} className="hover:bg-muted/30 transition-colors">
                            <td className="p-3">
                              <p className="font-semibold text-foreground">{m.studentName}</p>
                              <p className="text-xs text-muted-foreground">{m.studentEmail} • {m.academicGrade}</p>
                            </td>
                            <td className="p-3">
                              <Badge variant="outline">{m.sectionName}</Badge>
                            </td>
                            <td className="p-3">
                              <p className="font-medium">{m.subjectName}</p>
                              <p className="text-xs font-mono text-muted-foreground">{m.subjectCode}</p>
                            </td>
                            <td className="p-3">
                              {isEditing ? (
                                <select
                                  value={overrideFacultyId}
                                  onChange={(e) => setOverrideFacultyId(e.target.value)}
                                  className="h-8 rounded border px-2 text-xs bg-background"
                                >
                                  {facultyList.map((f) => (
                                    <option key={f.id} value={f.id}>
                                      {f.name} ({f.email})
                                    </option>
                                  ))}
                                </select>
                              ) : (
                                <div>
                                  <p className="font-medium text-emerald-700 dark:text-emerald-400">{m.facultyName}</p>
                                  <p className="text-xs text-muted-foreground">{m.facultyEmail}</p>
                                </div>
                              )}
                            </td>
                            <td className="p-3 text-right">
                              {isEditing ? (
                                <div className="flex justify-end gap-2">
                                  <Button size="sm" onClick={() => handleSaveOverride(m.id)} disabled={submittingOverride}>
                                    Save
                                  </Button>
                                  <Button size="sm" variant="ghost" onClick={() => setEditingMapping(null)}>
                                    Cancel
                                  </Button>
                                </div>
                              ) : (
                                <div className="flex justify-end gap-2">
                                  <Button size="sm" variant="outline" onClick={() => handleStartOverride(m)} title="Override Assigned Lecturer">
                                    <Edit3 className="h-3.5 w-3.5 mr-1" /> Override
                                  </Button>
                                  <Button size="sm" variant="ghost" className="text-destructive" onClick={() => handleDeleteMapping(m.id)} title="Delete Mapping">
                                    <Trash2 className="h-3.5 w-3.5" />
                                  </Button>
                                </div>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* TAB 2: STRUCTURAL AUDIT LOGS */}
        <TabsContent value="logs" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-xl flex items-center gap-2">
                <FileText className="h-5 w-5 text-primary" /> Global Structural & Audit Logs
              </CardTitle>
              <CardDescription>System initialization, catalog changes, and structural audit entries</CardDescription>
            </CardHeader>
            <CardContent>
              {adminRecords?.logs?.length ? (
                <div className="space-y-3">
                  {adminRecords.logs.map((log, i) => (
                    <div key={i} className="p-4 border rounded-lg bg-muted/20 flex justify-between items-start">
                      <div>
                        <Badge variant="secondary" className="mb-1 font-mono text-xs">{log.category}</Badge>
                        <p className="text-sm font-medium">{log.message}</p>
                      </div>
                      <span className="text-xs text-muted-foreground">{new Date(log.timestamp).toLocaleString()}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="py-4 text-center text-sm text-muted-foreground">No structural logs recorded.</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* TAB 3: ANALYTICS */}
        <TabsContent value="analytics" className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Monthly Attendance Trend</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={dashboard.monthlyAttendance}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="month" />
                    <YAxis domain={[0, 100]} />
                    <Tooltip />
                    <Line type="monotone" dataKey="attendance" stroke="#18181b" strokeWidth={2} />
                  </LineChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Attendance Distribution</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie data={dashboard.distribution} cx="50%" cy="50%" outerRadius={100} dataKey="value" label={({ value }) => `${value}%`}>
                      {dashboard.distribution.map((entry, index) => (
                        <Cell key={entry.name} fill={COLORS[index]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* TAB 4: DEPARTMENTS & STREAMS */}
        <TabsContent value="departments" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Stream & Department Overview</CardTitle>
            </CardHeader>
            <CardContent>
              {dashboard.departments.length ? (
                <ResponsiveContainer width="100%" height={400}>
                  <BarChart data={dashboard.departments}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis domain={[0, 100]} />
                    <Tooltip />
                    <Bar dataKey="attendance" fill="#18181b" />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <p className="py-4 text-center text-sm text-muted-foreground">No stream data available.</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* TAB 5: ALERTS & REPORTS */}
        <TabsContent value="alerts" className="space-y-4">
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-destructive" />
                <CardTitle>At-Risk Students</CardTitle>
                <Badge variant="destructive">{dashboard.atRiskStudents.length}</Badge>
              </div>
            </CardHeader>
            <CardContent>
              {dashboard.atRiskStudents.length ? (
                <div className="space-y-4">
                  {dashboard.atRiskStudents.map((student) => (
                    <div key={student.id} className="flex items-center justify-between p-4 border rounded-lg border-destructive/20 bg-destructive/5">
                      <div>
                        <h3 className="font-medium">{student.name}</h3>
                        <p className="text-sm text-muted-foreground">{student.id} • {student.department}</p>
                      </div>
                      <div className="text-right">
                        <Badge variant="destructive">{student.attendance}%</Badge>
                        <p className="mt-1 text-sm text-muted-foreground">{student.classesAttended}/{student.totalClasses} classes</p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="py-4 text-center text-sm text-muted-foreground">No students are currently below the 75% attendance threshold.</p>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
