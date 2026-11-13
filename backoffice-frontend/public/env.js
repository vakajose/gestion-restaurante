// Archivo inyectado dinámicamente en runtime por el contenedor Docker/Nginx (Twelve-Factor).
// En desarrollo local con Vite proxy (/api -> http://localhost:8080), API_BASE_URL vacío usa mismo origen.
window.__ENV__ = {
  API_BASE_URL: ''
};
