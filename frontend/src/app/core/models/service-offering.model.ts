export interface ServiceOffering {
  id: number;
  nome: string;
  duracaoMin: number;
  preco: number;
  sinalPercentual: number;
  ativo: boolean;
}

export interface ServiceOfferingRequest {
  nome: string;
  duracaoMin: number;
  preco: number;
  sinalPercentual: number;
}
