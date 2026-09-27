import { useEffect } from "react";
import { useAuth } from "../context/AuthContext";

/** Prevents signed-in users from returning to public marketing/auth screens. */
export function PublicRoute({ children, navigate }) {
  const { user, loading } = useAuth();

  useEffect(() => {
    if (!loading && user) navigate(`/dashboard/${user.role.toLowerCase()}`);
  }, [loading, navigate, user]);

  if (loading) return <div className="auth-route-loading">Loading your workspace...</div>;
  if (user) return null;
  return children;
}
