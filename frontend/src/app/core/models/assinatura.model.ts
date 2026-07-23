export type AssinaturaStatus = 'TRIAL' | 'ATIVA' | 'INADIMPLENTE' | 'CANCELADA';

export interface FaturaPendente {
  valor: number;
  vencimento: string;
  pixPayload: string;
  pixQrCodeBase64: string;
}

export interface Assinatura {
  plano: string;
  status: AssinaturaStatus;
  trialAte: string | null;
  proximaCobrancaEm: string | null;
  faturaPendente: FaturaPendente | null;
}
