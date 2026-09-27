import { createContext, useContext, useEffect, useState } from "react";
import { api, getApiErrors } from "../api";

const AuthContext = createContext(null);
const TOKEN_KEY = "smartattend_token";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      setLoading(false);
      return;
    }
    api.get("/auth/me")
      .then(({ data }) => setUser(data))
      .catch(() => {
        localStorage.removeItem(TOKEN_KEY);
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  async function login(credentials) {
    const { data } = await api.post("/auth/login", credentials);
    localStorage.setItem(TOKEN_KEY, data.token);
    setUser(data.user);
    return data.user;
  }

  async function register(values) {
    await api.post("/auth/register", values);
  }

  async function getSections() {
    const { data } = await api.get("/sections");
    return data;
  }

  function updateUser(updates) {
    setUser((prev) => (prev ? { ...prev, ...updates } : prev));
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
    window.history.pushState({}, "", "/");
    window.dispatchEvent(new PopStateEvent("popstate"));
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, getSections, updateUser, logout, getApiErrors }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}