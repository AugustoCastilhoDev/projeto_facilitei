export interface LoginResponse {
  token: string;
  tenantId: number;
  tenantSlug: string;
  email: string;
}

export interface RegistrarTenantRequest {
  nomeNegocio: string;
  slug: string;
  horarioAbertura: string;
  horarioFechamento: string;
  emailAdmin: string;
  senhaAdmin: string;
  cpfCnpj: string;
  plano: string;
}

export interface RegistrarTenantResponse {
  tenantId: number;
  slug: string;
  adminUserId: number;
  adminEmail: string;
}
