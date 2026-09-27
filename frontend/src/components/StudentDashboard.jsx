import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Progress } from "./ui/progress";
import { QrCodeIcon, Calendar, Clock, TrendingUp, CheckCircle } from "lucide-react";
import { QRScanner } from "./QRScanner";
import { api } from "../api";

export function StudentDashboard({ user }) {
  const [showScanner, setShowScanner] = useState(false);
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");
  const loadDashboard = async () => {
    try { setError(""); const { data } = await api.get("/dashboard/student"); setDashboard(data); }
    catch { setError("Attendance data could not be loaded. Please refresh and try again."); }
  };
  useEffect(() => { loadDashboard(); }, []);
  if (!dashboard && !error) return <div className="py-12 text-center text-muted-foreground">Loading your attendance dashboard...</div>;
  if (!dashboard) return <div className="py-12 text-center text-destructive">{error}</div>;
  return <div className="space-y-6">
    <div className="flex justify-between items-start"><div><h2 className="text-2xl font-semibold">Welcome back, {user.name.split(" ")[0]}!</h2><p className="text-muted-foreground">Track your attendance and view your academic progress</p></div><Button onClick={() => setShowScanner(true)} className="flex items-center gap-2"><QrCodeIcon className="h-4 w-4" />Scan QR Code</Button></div>
    {error && <p className="rounded-md border border-destructive/20 bg-destructive/5 p-3 text-sm text-destructive">{error}</p>}
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      {[["Overall Attendance", `${dashboard.overallAttendance}%`, TrendingUp], ["Enrolled Subjects", dashboard.enrolledSubjects, Calendar], ["Today's Classes", dashboard.todayClasses, Clock], ["Present Today", dashboard.presentToday, CheckCircle]].map(([label, value, Icon]) => <Card key={label}><CardContent className="p-6"><div className="flex items-center justify-between"><div><p className="text-sm text-muted-foreground">{label}</p><p className="text-2xl font-semibold">{value}</p></div><Icon className="h-8 w-8 text-primary" /></div></CardContent></Card>)}
    </div>
    <Card><CardHeader><CardTitle>Today's Schedule</CardTitle></CardHeader><CardContent>{dashboard.schedule.length ? <div className="space-y-4">{dashboard.schedule.map((item) => <div key={item.timetableSlotId} className="flex items-center justify-between p-4 border rounded-lg"><div className="flex-1"><h3 className="font-medium">{item.name}</h3><p className="text-sm text-muted-foreground">{item.time} • {item.room}</p></div><Badge variant={item.status === "present" ? "default" : "secondary"}>{item.status === "present" ? "Present" : "Upcoming"}</Badge></div>)}</div> : <p className="py-4 text-center text-sm text-muted-foreground">No classes are scheduled for today.</p>}</CardContent></Card>
    <Card><CardHeader><CardTitle>Attendance Summary</CardTitle></CardHeader><CardContent>{dashboard.subjects.length ? <div className="space-y-6">{dashboard.subjects.map((subject) => <div key={subject.subjectId} className="space-y-2"><div className="flex justify-between items-center"><div><h3 className="font-medium">{subject.name}</h3><p className="text-sm text-muted-foreground">{subject.faculty}</p></div><div className="text-right"><p className="font-medium">{subject.attendance}%</p><p className="text-sm text-muted-foreground">{subject.attendedClasses}/{subject.totalClasses} classes</p></div></div><Progress value={subject.attendance} className="h-2" /></div>)}</div> : <p className="py-4 text-center text-sm text-muted-foreground">Attendance will appear after your first conducted class.</p>}</CardContent></Card>
    {showScanner && <QRScanner onSuccess={() => { setShowScanner(false); loadDashboard(); }} onClose={() => setShowScanner(false)} />}
  </div>;
}
