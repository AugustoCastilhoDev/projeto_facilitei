/** Espelha o enum Plano do backend (fixo, nao editavel) - ver ROADMAP/README para a justificativa dos limites. */
export interface PlanoOpcao {
  valor: string;
  nome: string;
  precoMensal: number;
  limiteProfissionais: number | null;
}

export const PLANOS: PlanoOpcao[] = [
  { valor: 'BASICO', nome: 'Basico', precoMensal: 49, limiteProfissionais: 2 },
  { valor: 'PROFISSIONAL', nome: 'Profissional', precoMensal: 89, limiteProfissionais: 5 },
  { valor: 'PREMIUM', nome: 'Premium', precoMensal: 149, limiteProfissionais: null },
];
