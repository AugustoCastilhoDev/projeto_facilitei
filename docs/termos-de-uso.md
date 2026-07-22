# Termos de Uso — Facilitei

> **Status: minuta inicial, ainda não publicada.** Este documento cobre a estrutura jurídica do modelo de negócio (BYOPP, papel da plataforma no pagamento, relação com o Negócio e com o Cliente Final), mas os campos entre `[colchetes]` dependem da abertura do CNPJ e devem ser revisados por um contador/advogado antes de qualquer publicação real para usuários. Ver `ROADMAP.md`.

**Última atualização:** [data de publicação]

## 1. Quem somos

A Facilitei é uma plataforma de agendamento online com cobrança de sinal via Pix, oferecida por **[Razão Social a definir]**, inscrita no CNPJ sob o nº **[CNPJ em processo de abertura — Microempresa (ME)]**, com sede em **[endereço]** ("Facilitei", "nós").

Estes Termos de Uso regulam o acesso e uso da plataforma Facilitei (site, painel administrativo e página pública de agendamento) por dois tipos de usuário:

- **Negócio** (ou "tenant"): a pessoa física ou jurídica que contrata a Facilitei para gerenciar sua agenda e receber agendamentos de seus próprios clientes (ex.: barbearias, salões, esteticistas).
- **Cliente Final**: a pessoa que agenda um horário com um Negócio através da página pública da Facilitei.

Ao criar uma conta ou usar a plataforma, você concorda com estes Termos. Se você é o Cliente Final apenas fazendo um agendamento, as seções 5 (Pagamento e modelo BYOPP) e 8 (Limitação de responsabilidade) são as que mais afetam você diretamente.

## 2. O que a Facilitei oferece

- Painel administrativo para o Negócio cadastrar seus serviços, horários de expediente e agenda.
- Página pública de agendamento (`facilitei.com/agendar/[nome-do-negócio]`) para o Cliente Final escolher serviço, horário e pagar o sinal, quando aplicável.
- Geração da cobrança Pix do sinal e confirmação automática da reserva quando o pagamento é identificado.
- Abstração de notificações ao Cliente Final sobre o status da reserva (hoje registrada internamente; um canal real — como WhatsApp — pode ser adicionado no futuro, sem alterar esta cláusula).

A Facilitei **não presta serviços de barbearia, estética, saúde ou qualquer outro serviço agendado** — esses serviços são de responsabilidade exclusiva do Negócio. A Facilitei é apenas a ferramenta de agendamento e cobrança do sinal.

## 3. Cadastro do Negócio

- O Negócio é responsável por fornecer informações verdadeiras, completas e atualizadas no cadastro (nome, horário de funcionamento, serviços oferecidos, preços, política de sinal).
- O Negócio é responsável por manter a confidencialidade de sua senha de acesso ao painel administrativo, e por qualquer atividade realizada através de sua conta.
- Um cadastro por Negócio; contas duplicadas ou fraudulentas podem ser suspensas sem aviso prévio.

## 4. Cadastro e dados do Cliente Final

- Para concluir um agendamento, o Cliente Final fornece nome completo, telefone (WhatsApp) e CPF/CNPJ, usados exclusivamente para identificar a reserva e, quando houver sinal, gerar a cobrança Pix.
- O tratamento desses dados segue a [Política de Privacidade](politica-de-privacidade.md), que também explica o papel do Negócio como parte responsável perante seu próprio cliente (LGPD).

## 5. Pagamento do sinal — modelo BYOPP ("traga sua própria conta")

Este é o ponto mais importante para entender como o dinheiro se movimenta na Facilitei:

- **A Facilitei nunca recebe, guarda ou intermedia o valor do sinal pago pelo Cliente Final.** Cada Negócio conecta sua própria conta na Asaas (instituição de pagamento parceira, regulada pelo Banco Central) e a cobrança Pix é gerada diretamente nessa conta.
- O sinal pago pelo Cliente Final cai **diretamente na conta Asaas do Negócio**, sem passar pela Facilitei em nenhum momento.
- A Facilitei apenas aciona a API da Asaas, em nome do Negócio e com a chave de API fornecida por ele, para gerar a cobrança e confirmar automaticamente a reserva quando o pagamento é identificado.
- Qualquer disputa, estorno, reembolso ou problema relacionado ao pagamento do sinal é de responsabilidade do Negócio junto à Asaas — a Facilitei não é parte nessa relação financeira e não tem acesso ao saldo, extrato ou movimentação da conta do Negócio, além da confirmação de status do pagamento específico daquela reserva.
- O restante do valor do serviço (o que não for cobrado como sinal) é acertado diretamente entre Negócio e Cliente Final, fora da plataforma.

## 6. Assinatura da Facilitei

- O Negócio paga à Facilitei uma assinatura pelo uso da plataforma (modelo e valores informados no momento da contratação ou em [tabela de planos]).
- A assinatura é cobrada da conta do próprio Negócio — não é descontada do sinal do Cliente Final nem de qualquer valor que circule pela conta Asaas do Negócio.
- Enquanto a cobrança automática de assinatura não estiver disponível na plataforma, o valor combinado pode ser cobrado manualmente, fora do sistema, mediante acordo direto com a Facilitei.

## 7. Obrigações do Negócio

O Negócio se compromete a:

- Cumprir a legislação aplicável ao seu próprio ramo de atividade e ao Código de Defesa do Consumidor na relação com seus clientes finais;
- Não usar a plataforma para fins ilícitos, fraudulentos ou para cobrar por serviços que não pretende prestar;
- Manter sua própria conta Asaas em conformidade com os termos daquela instituição;
- Informar corretamente sua política de cancelamento/reembolso de sinal aos seus clientes finais (a Facilitei não define nem arbitra essa política).

## 8. Limitação de responsabilidade

- A Facilitei é fornecida "como está", com melhor esforço razoável de disponibilidade, mas sem garantia de operação ininterrupta — especialmente nesta fase inicial do produto, sem acordo de nível de serviço (SLA) formal.
- A Facilitei não se responsabiliza por: (a) indisponibilidade ou instabilidade da Asaas ou de outros provedores externos; (b) disputas entre Negócio e Cliente Final quanto à qualidade do serviço agendado, cancelamentos ou reembolsos; (c) uso indevido da plataforma por qualquer uma das partes.
- Nada nesta cláusula exclui responsabilidades que não podem ser limitadas por lei (ex.: danos causados por dolo ou culpa grave da Facilitei).

## 9. Propriedade intelectual

O software, marca, layout e conteúdo da Facilitei pertencem à **[Razão Social a definir]**. O uso da plataforma não transfere nenhum direito de propriedade intelectual ao Negócio ou ao Cliente Final.

## 10. Cancelamento e suspensão

- O Negócio pode encerrar sua conta a qualquer momento, mediante solicitação.
- A Facilitei pode suspender ou encerrar contas que violem estes Termos, mediante aviso prévio quando possível, exceto em casos de fraude ou risco à plataforma ou a terceiros, onde a suspensão pode ser imediata.

## 11. Alterações destes Termos

Podemos atualizar estes Termos para refletir mudanças no produto, na legislação ou no modelo de negócio. Alterações materiais serão comunicadas ao Negócio com antecedência razoável pelos canais de contato cadastrados.

## 12. Legislação e foro

Estes Termos são regidos pelas leis da República Federativa do Brasil. Fica eleito o foro da comarca de **[cidade/UF]** para dirimir eventuais controvérsias, com renúncia a qualquer outro, por mais privilegiado que seja.

## 13. Contato

Dúvidas sobre estes Termos: **[e-mail de contato]**.
