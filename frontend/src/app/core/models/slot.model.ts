export type SlotStatus = 'DISPONIVEL' | 'RESERVADO' | 'CONFIRMADO';

export interface Slot {
  id: number;
  serviceId: number;
  serviceNome: string;
  profissionalId: number;
  profissionalNome: string;
  /** ISO OffsetDateTime vindo do backend, ex: "2026-07-21T09:00:00-03:00". */
  dataHora: string;
  status: SlotStatus;
}
