import axios from "axios";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("smartattend_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && window.location.pathname !== "/login") {
      localStorage.removeItem("smartattend_token");
      window.location.assign("/login");
    }
    return Promise.reject(error);
  }
);

export function getApiErrors(error) {
  const errors = error.response?.data?.errors;
  if (Array.isArray(errors)) return errors;
  return [{ field: null, message: "Unable to complete the request." }];
}