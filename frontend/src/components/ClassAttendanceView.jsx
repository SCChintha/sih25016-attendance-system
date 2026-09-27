import { useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "./ui/table";
import { ArrowLeft, Download, Search } from "lucide-react";
import { Input } from "./ui/input";
import { api } from "../api";

export function ClassAttendanceView({ timetableSlotId, onBack }) {
  const [searchTerm, setSearchTerm] = useState("");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => { api.get("/dashboard/faculty/class", { params: { timetableSlotId } }).then(({ data: response }) => setData(response)).catch(() => setError("Attendance records could not be loaded.")); }, [timetableSlotId]);
  const students = useMemo(() => (data?.students || []).filter((student) => `${student.name} ${student.email}`.toLowerCase().includes(searchTerm.toLowerCase())), [data, searchTerm]);
  const exportData = () => { if (!data) return; const rows = students.map((student) => `${student.id},${student.name},${student.email},${student.attendedClasses},${student.totalClasses},${student.attendanceRate}%`).join("\n"); const link = document.createElement("a"); link.href = `data:text/csv;charset=utf-8,Student ID,Name,Email,Attended Classes,Total Classes,Attendance Rate\n${encodeURIComponent(rows)}`; link.download = `${data.code}-${data.section}-attendance.csv`; link.click(); };
  if (!data && !error) return <div className="py-10 text-center text-muted-foreground">Loading class attendance...</div>;
  if (!data) return <div className="py-10 text-center text-destructive">{error}</div>;
  const variant = (rate) => rate >= 90 ? "default" : rate >= 75 ? "secondary" : "destructive";
  return <div className="space-y-6"><div className="flex items-center justify-between"><div className="flex items-center gap-4"><Button variant="ghost" size="sm" onClick={onBack}><ArrowLeft className="h-4 w-4 mr-2"/>Back to Classes</Button><div><h2 className="text-xl font-semibold">{data.name}</h2><p className="text-muted-foreground">{data.code} - Section {data.section}</p></div></div><Button onClick={exportData} variant="outline"><Download className="h-4 w-4 mr-2"/>Export Data</Button></div>
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">{[[data.totalClasses,"Total Classes"],[data.enrolledStudents,"Enrolled Students"],[`${data.averageAttendance}%`,"Avg. Attendance"],[data.atRiskStudents,"At Risk Students"]].map(([value,label]) => <Card key={label}><CardContent className="p-4"><div className="text-center"><p className="text-2xl font-semibold">{value}</p><p className="text-sm text-muted-foreground">{label}</p></div></CardContent></Card>)}</div>
    <Card><CardHeader><CardTitle>Class Sessions</CardTitle></CardHeader><CardContent>{data.sessions.length ? <div className="space-y-4">{data.sessions.map((session) => <div key={session.sessionId} className="flex items-center justify-between p-4 border rounded-lg"><div><h3 className="font-medium">Attendance Session</h3><p className="text-sm text-muted-foreground">{new Date(session.startedAt).toLocaleString()}</p></div><div className="text-right space-y-1"><Badge variant={variant(session.attendanceRate)}>{session.attendanceRate}%</Badge><p className="text-sm text-muted-foreground">{session.presentStudents}/{session.totalStudents} present</p></div></div>)}</div> : <p className="py-4 text-center text-sm text-muted-foreground">No completed sessions for this class yet.</p>}</CardContent></Card>
    <Card><CardHeader><div className="flex justify-between items-center"><CardTitle>Student Attendance</CardTitle><div className="relative"><Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground"/><Input placeholder="Search students..." value={searchTerm} onChange={(event) => setSearchTerm(event.target.value)} className="pl-8 w-64"/></div></div></CardHeader><CardContent>{students.length ? <Table><TableHeader><TableRow><TableHead>Student ID</TableHead><TableHead>Name</TableHead><TableHead>Email</TableHead><TableHead>Classes Attended</TableHead><TableHead>Attendance Rate</TableHead><TableHead>Status</TableHead></TableRow></TableHeader><TableBody>{students.map((student) => <TableRow key={student.id}><TableCell className="font-medium">{student.id}</TableCell><TableCell>{student.name}</TableCell><TableCell className="text-muted-foreground">{student.email}</TableCell><TableCell>{student.attendedClasses}/{student.totalClasses}</TableCell><TableCell>{student.attendanceRate}%</TableCell><TableCell><Badge variant={variant(student.attendanceRate)}>{student.attendanceRate >= 90 ? "Excellent" : student.attendanceRate >= 75 ? "Good" : "At Risk"}</Badge></TableCell></TableRow>)}</TableBody></Table> : <p className="py-4 text-center text-sm text-muted-foreground">No students match this search.</p>}</CardContent></Card>
  </div>;
}
