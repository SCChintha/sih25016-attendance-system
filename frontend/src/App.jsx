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
import { StudentOnboardingWizard } from "./components/StudentOnboardingWizard";
import { Button } from "./components/ui/button";
import { LogOut, Settings } from "lucide-react";

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
  
  return (
    <ProtectedRoute role={role} navigate={navigate}>
      <DashboardPage role={role} logout={logout} navigate={navigate} />
    </ProtectedRoute>
  );
}

function DashboardPage({ role, logout, navigate }) {
  const { user } = useAuth();
  const [showWizard, setShowWizard] = useState(false);

  if (role === "student" && (showWizard || (user && !user.onboardingCompleted))) {
    return (
      <StudentOnboardingWizard
        user={user}
        onComplete={() => {
          setShowWizard(false);
          navigate("/dashboard/student");
        }}
      />
    );
  }

  const dashboard =
    role === "student" ? (
      <StudentDashboard user={user} onEditOnboarding={() => setShowWizard(true)} />
    ) : role === "faculty" ? (
      <FacultyDashboard user={user} />
    ) : (
      <AdminDashboard user={user} />
    );

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-card">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-semibold">SmartAttend</h1>
            <p className="text-muted-foreground text-sm">Automated Student Attendance System</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="font-medium">{user?.name}</p>
              <p className="text-xs text-muted-foreground capitalize">
                {user?.role} {user?.academicGrade ? `• ${user.academicGrade}` : ""}
              </p>
            </div>
            {role === "student" && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowWizard(true)}
                title="Edit Course & Faculty Assignments"
              >
                <Settings className="h-4 w-4 mr-1" />
                Setup
              </Button>
            )}
            <Button variant="outline" size="sm" onClick={logout}>
              <LogOut className="h-4 w-4 mr-2" />
              Logout
            </Button>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-6">{dashboard}</main>
    </div>
  );
}
