export type SlotStatus = 'DISPONIVEL' | 'RESERVADO' | 'CONFIRMADO';

export type StatusReserva = 'PENDENTE' | 'PAGO' | 'SEM_SINAL' | 'EXPIRADO' | 'CANCELADO';

export interface Slot {
  id: number;
  serviceId: number;
  serviceNome: string;
  profissionalId: number;
  profissionalNome: string;
  /** ISO OffsetDateTime vindo do backend, ex: "2026-07-21T09:00:00-03:00". */
  dataHora: string;
  status: SlotStatus;
  /** Campos abaixo so vem preenchidos quando o slot tem uma reserva (ver SlotResponse no backend). */
  bookingId: number | null;
  clienteNome: string | null;
  clienteTelefone: string | null;
  statusReserva: StatusReserva | null;
  compareceu: boolean | null;
}
