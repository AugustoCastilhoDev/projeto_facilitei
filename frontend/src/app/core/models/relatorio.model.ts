export interface ClienteRecorrente {
  clienteNome: string;
  clienteTelefone: string;
  totalAgendamentos: number;
  /** ISO OffsetDateTime vindo do backend. */
  ultimoAgendamento: string;
}

export interface Relatorio {
  faturamentoTotal: number;
  totalReservasConfirmadas: number;
  totalComparecimentosMarcados: number;
  totalNaoComparecimentos: number;
  taxaNaoComparecimentoPercentual: number;
  clientesRecorrentes: ClienteRecorrente[];
}
