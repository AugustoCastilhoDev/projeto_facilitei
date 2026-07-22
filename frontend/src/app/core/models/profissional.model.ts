export interface Profissional {
  id: number;
  nome: string;
  /** "HH:mm" */
  horarioAbertura: string;
  /** "HH:mm" */
  horarioFechamento: string;
  ativo: boolean;
  servicoIds: number[];
  servicoNomes: string[];
}

export interface ProfissionalRequest {
  nome: string;
  horarioAbertura: string;
  horarioFechamento: string;
  servicoIds: number[];
}
