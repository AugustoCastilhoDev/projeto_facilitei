export type PaymentStatus = 'PENDENTE' | 'PAGO' | 'SEM_SINAL' | 'EXPIRADO' | 'CANCELADO';

export interface Booking {
  id: number;
  slotId: number;
  clienteNome: string;
  clienteTelefone: string;
  statusPagamento: PaymentStatus;
  asaasPaymentId: string | null;
  asaasPixPayload: string | null;
  /** So vem preenchido na resposta de criacao da reserva, nunca nas consultas de status depois. */
  asaasPixQrCodeBase64: string | null;
}

export interface CriarBookingRequest {
  slotId: number;
  clienteNome: string;
  clienteTelefone: string;
  clienteCpfCnpj: string;
}
