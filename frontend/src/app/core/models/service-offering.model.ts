export interface ServiceOffering {
  id: number;
  nome: string;
  duracaoMin: number;
  preco: number;
  sinalPercentual: number;
  ativo: boolean;
  profissionalIds: number[];
  profissionalNomes: string[];
}

export interface ServiceOfferingRequest {
  nome: string;
  duracaoMin: number;
  preco: number;
  sinalPercentual: number;
  profissionalIds: number[];
}
