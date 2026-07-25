# Marca — Facilitei

Ícone "Horário reservado": uma grade 2×2 de horários com um deles marcado — a ação central de quem agenda.

- `logo-mark.svg` — ícone colorido (fundo índigo), fonte de verdade. Usado no `favicon.svg`, no `<app-brand-mark>` (`frontend/src/app/shared/brand-mark/`) e como base para os PNGs abaixo.
- `logo-mark-mono.svg` — versão monocromática (`fill="currentColor"`, sem fundo), para uso sobre uma única cor (papelaria, carimbo, superfícies escuras).
- `logo-social-avatar.png` (512×512) — avatar quadrado para redes sociais (Instagram/LinkedIn/X recortam em círculo automaticamente; o ícone já foi desenhado com margem de segurança para isso).
- `logo-icon-1024.png` (1024×1024) — mesma arte em alta resolução, para materiais impressos ou lojas de app.
- `logo-lockup.png` (960×220) — ícone + wordmark "Facilitei" (Manrope ExtraBold, índigo `#4338ca`), para banners, cabeçalho de e-mail, apresentações.

`favicon.ico` e `apple-touch-icon.png` (na raiz de `public/`) foram gerados a partir do mesmo `logo-mark.svg`.

Paleta de origem: `frontend/src/theme-colors.scss` (gerada via `ng generate @angular/material:m3-theme`, semente `#4338ca`).
