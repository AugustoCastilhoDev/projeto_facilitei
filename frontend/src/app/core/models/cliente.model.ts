import { PaymentStatus } from './booking.model';

export interface ClienteLoginResponse {
  token: string;
  clienteId: number;
  nome: string;
  telefone: string;
}

export interface CriarReservaPortalRequest {
  slotId: number;
  clienteNome: string;
  clienteCpfCnpj: string;
}

/**
 * Diferente de Booking (usado por /agendar/:slug): aqui o cliente cruza
 * varios negocios, entao a resposta ja traz negocio/servico/horario (ver
 * PortalReservaResponse no backend).
 */
export interface PortalReserva {
  id: number;
  tenantNome: string;
  tenantSlug: string;
  serviceNome: string;
  /** ISO OffsetDateTime vindo do backend, ex: "2026-07-21T09:00:00-03:00". */
  dataHora: string;
  statusPagamento: PaymentStatus;
  asaasPaymentId: string | null;
  asaasPixPayload: string | null;
  /** So vem preenchido na resposta de criacao, nunca no historico. */
  asaasPixQrCodeBase64: string | null;
}
