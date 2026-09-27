import { useEffect, useState } from "react";
import { Landing } from "./components/Landing";
import { Login } from "./components/Login";
import { Register } from "./components/Register";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { PublicRoute } from "./components/PublicRoute";
import { useAuth } from "./context/AuthContext";
import { StudentDashboard } from "./components/StudentDashboard";
import { FacultyDashboard } from "./components/FacultyDashboard";
import { AdminDashboard } from "./components/AdminDashboard";
import { Button } from "./components/ui/button";
import { LogOut } from "lucide-react";

export default function App() {
  const [path, setPath] = useState(window.location.pathname);
  const { logout } = useAuth();
  const navigate = (nextPath) => {
    window.history.pushState({}, "", nextPath);
    setPath(nextPath);
  };

  useEffect(() => {
    const handlePopState = () => setPath(window.location.pathname);
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  if (path === "/") return <PublicRoute navigate={navigate}><Landing navigate={navigate} /></PublicRoute>;
  if (path === "/login") return <PublicRoute navigate={navigate}><Login navigate={navigate} /></PublicRoute>;
  if (path === "/register") return <PublicRoute navigate={navigate}><Register navigate={navigate} /></PublicRoute>;

  const role = path.match(/^\/dashboard\/(student|faculty|admin)$/)?.[1];
  if (!role) return <Landing navigate={navigate} />;
  return <ProtectedRoute role={role} navigate={navigate}><DashboardPage role={role} logout={logout} />
  </ProtectedRoute>;
}

function DashboardPage({ role, logout }) {
  const { user } = useAuth();
  const dashboard = role === "student" ? <StudentDashboard user={user} /> : role === "faculty" ? <FacultyDashboard user={user} /> : <AdminDashboard user={user} />;
  return <div className="min-h-screen bg-background">
      <header className="border-b bg-card">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-semibold">AttendanceTracker</h1>
            <p className="text-muted-foreground">Smart Attendance Management System</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="font-medium">{user.name}</p>
              <p className="text-sm text-muted-foreground capitalize">{user.role}</p>
            </div>
            <Button variant="outline" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4 mr-2" />
              Logout
            </Button>
          </div>
        </div>
      </header>
      
      <main className="container mx-auto px-4 py-6">
        {dashboard}
      </main>
    </div>;
}
