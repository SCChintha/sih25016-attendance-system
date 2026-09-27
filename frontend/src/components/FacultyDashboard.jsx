import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "./ui/tabs";
import { QrCode, Users, Calendar, BookOpen, Download } from "lucide-react";
import { QRGenerator } from "./QRGenerator";
import { ClassAttendanceView } from "./ClassAttendanceView";
import { api } from "../api";

export function FacultyDashboard() {
  const [showQRGenerator, setShowQRGenerator] = useState(false);
  const [selectedClass, setSelectedClass] = useState(null);
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");
  const loadDashboard = async () => { try { setError(""); const { data } = await api.get("/dashboard/faculty"); setDashboard(data); } catch { setError("Class data could not be loaded. Please refresh and try again."); } };
  useEffect(() => { loadDashboard(); }, []);
  if (!dashboard && !error) return <div className="py-12 text-center text-muted-foreground">Loading your faculty dashboard...</div>;
  if (!dashboard) return <div className="py-12 text-center text-destructive">{error}</div>;
  const statusBadge = (status) => status === "closed" ? "default" : status === "open" ? "destructive" : "secondary";
  const statusLabel = (status) => status === "closed" ? "Completed" : status === "open" ? "Active" : "Upcoming";
  return <div className="space-y-6">
    <div className="flex justify-between items-start"><div><h2 className="text-2xl font-semibold">Faculty Dashboard</h2><p className="text-muted-foreground">Manage your classes and track student attendance</p></div><Button onClick={() => setShowQRGenerator(true)} className="flex items-center gap-2"><QrCode className="h-4 w-4"/>Generate QR Code</Button></div>
    {error && <p className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">{error}</p>}
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">{[["Total Classes", dashboard.totalClasses, BookOpen], ["Total Students", dashboard.totalStudents, Users], ["Avg. Attendance", `${dashboard.averageAttendance}%`, Calendar], ["Today's Sessions", dashboard.todaySessions, Calendar]].map(([label,value,Icon]) => <Card key={label}><CardContent className="p-6"><div className="flex items-center justify-between"><div><p className="text-sm text-muted-foreground">{label}</p><p className="text-2xl font-semibold">{value}</p></div><Icon className="h-8 w-8 text-primary"/></div></CardContent></Card>)}</div>
    <Tabs defaultValue="today" className="space-y-4"><TabsList><TabsTrigger value="today">Today's Sessions</TabsTrigger><TabsTrigger value="classes">My Classes</TabsTrigger><TabsTrigger value="attendance">Attendance Records</TabsTrigger></TabsList>
      <TabsContent value="today" className="space-y-4"><Card><CardHeader><CardTitle>Today's Class Sessions</CardTitle></CardHeader><CardContent>{dashboard.sessions.length ? <div className="space-y-4">{dashboard.sessions.map((session) => <div key={session.timetableSlotId} className="flex items-center justify-between p-4 border rounded-lg"><div className="flex-1"><h3 className="font-medium">{session.className}</h3><p className="text-sm text-muted-foreground">{session.time} • {session.room}</p>{session.status === "closed" && <p className="text-sm text-muted-foreground">{session.studentsPresent}/{session.totalStudents} students present ({session.attendanceRate}%)</p>}</div><div className="flex items-center gap-2"><Badge variant={statusBadge(session.status)}>{statusLabel(session.status)}</Badge>{session.status !== "closed" && <Button size="sm" onClick={() => setShowQRGenerator(true)}>{session.status === "open" ? "View QR" : "Start & Generate QR"}</Button>}</div></div>)}</div> : <p className="py-4 text-center text-sm text-muted-foreground">No sessions are scheduled for today.</p>}</CardContent></Card></TabsContent>
      <TabsContent value="classes" className="space-y-4">{dashboard.classes.length ? <div className="grid gap-4">{dashboard.classes.map((course) => <Card key={course.timetableSlotId}><CardContent className="p-6"><div className="flex justify-between items-start"><div className="flex-1"><h3 className="font-medium text-lg">{course.name}</h3><p className="text-muted-foreground">{course.code} - Section {course.section}</p><div className="mt-2 grid grid-cols-2 gap-4 text-sm"><div><span className="text-muted-foreground">Schedule: </span><span>{course.schedule}</span></div><div><span className="text-muted-foreground">Room: </span><span>{course.room}</span></div><div><span className="text-muted-foreground">Enrolled: </span><span>{course.enrolledStudents} students</span></div><div><span className="text-muted-foreground">Avg. Attendance: </span><span>{course.averageAttendance}%</span></div></div></div><div className="flex gap-2"><Button variant="outline" size="sm" onClick={() => setSelectedClass(course.timetableSlotId)}>View Details</Button><Button variant="outline" size="sm" onClick={() => setSelectedClass(course.timetableSlotId)}><Download className="h-4 w-4"/></Button></div></div></CardContent></Card>)}</div> : <Card><CardContent className="p-6 text-center text-muted-foreground">No classes are assigned to this faculty account.</CardContent></Card>}</TabsContent>
      <TabsContent value="attendance" className="space-y-4">{selectedClass ? <ClassAttendanceView timetableSlotId={selectedClass} onBack={() => setSelectedClass(null)} /> : <Card><CardContent className="p-6 text-center text-muted-foreground">Select a class from the "My Classes" tab to view attendance records.</CardContent></Card>}</TabsContent>
    </Tabs>
    {showQRGenerator && <QRGenerator classes={dashboard.classes} onClose={() => setShowQRGenerator(false)} onSessionChanged={loadDashboard}/>} 
  </div>;
}
