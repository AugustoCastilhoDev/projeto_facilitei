/** Mesmos valores do enum CategoriaNegocio no backend (ver tenant.CategoriaNegocio). */
export type CategoriaNegocio =
  | 'BARBEARIA'
  | 'SALAO_DE_BELEZA'
  | 'ESTETICA'
  | 'CLINICA'
  | 'ACADEMIA'
  | 'PET'
  | 'OUTRO';

export interface DiretorioTenant {
  nome: string;
  slug: string;
  categoria: CategoriaNegocio | null;
  cidade: string | null;
  descricao: string | null;
}

/** Configuracao do tenant para aparecer (ou nao) no diretorio publico - tela em features/admin/diretorio. */
export interface DiretorioConfig {
  categoria: CategoriaNegocio | null;
  cidade: string | null;
  descricao: string | null;
  visivelNoDiretorio: boolean;
}

/** Rotulos em portugues, usados tanto no filtro do diretorio (portal) quanto na tela de configuracao (admin). */
export const ROTULOS_CATEGORIA: Record<CategoriaNegocio, string> = {
  BARBEARIA: 'Barbearia',
  SALAO_DE_BELEZA: 'Salao de beleza',
  ESTETICA: 'Estetica',
  CLINICA: 'Clinica',
  ACADEMIA: 'Academia',
  PET: 'Pet',
  OUTRO: 'Outro',
};
