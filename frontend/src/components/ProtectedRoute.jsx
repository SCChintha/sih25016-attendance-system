import { useEffect } from "react";
import { useAuth } from "../context/AuthContext";

export function ProtectedRoute({ role, children, navigate }) {
  const { user, loading } = useAuth();
  useEffect(() => {
    if (!loading && !user) navigate("/login");
    if (!loading && user && role && user.role.toLowerCase() !== role) navigate(`/dashboard/${user.role.toLowerCase()}`);
  }, [loading, navigate, role, user]);
  if (loading) return <div className="flex min-h-screen items-center justify-center">Loading your workspace...</div>;
  if (!user || (role && user.role.toLowerCase() !== role)) return null;
  return children;
}