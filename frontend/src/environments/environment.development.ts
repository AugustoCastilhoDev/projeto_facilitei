export const environment = {
  production: false,
  // Caminho relativo: o dev-server do Angular usa proxy.conf.json para
  // encaminhar /api para http://localhost:8080 sem precisar mexer em CORS
  // no backend. Em producao, "/api" assume que frontend e backend ficam
  // atras do mesmo reverse proxy (ajustavel quando a implantacao for definida).
  apiUrl: '/api',
};
