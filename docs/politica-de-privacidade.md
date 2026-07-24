# Política de Privacidade — Facilitei

> **Status: minuta revisada com profissional nos pontos de LGPD (base legal por finalidade, DPO simplificado, transferência internacional), ainda não publicada.** Cobre os fluxos de dados reais da plataforma (cadastro do Negócio, dados do Cliente Final no agendamento, repasse à Asaas, hospedagem nos EUA). Campos entre `[colchetes]` dependem principalmente da abertura do CNPJ e do e-mail de contato definitivo, e o documento completo ainda deve ser formalmente aprovado por um profissional antes de publicação — ver `ROADMAP.md`.

**Última atualização:** [data de publicação]

Esta Política de Privacidade explica como a Facilitei trata dados pessoais, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018 — LGPD).

## 1. Quem trata os dados

A Facilitei é operada por **[Razão Social a definir]**, CNPJ **[em processo de abertura]**. Para dúvidas sobre privacidade: **[e-mail de contato]**.

Esta política trata de dois grupos de titulares diferentes, com papéis distintos na LGPD:

| Titular | Dados tratados | Papel da Facilitei |
|---|---|---|
| **Negócio (tenant)** — dono da conta no painel | Nome do negócio, e-mail de login, senha (com hash), horários de funcionamento, chave de API da própria conta Asaas | **Controladora**: a Facilitei decide como e para que trata esses dados, para viabilizar o próprio serviço |
| **Cliente Final** — quem agenda um horário | Nome completo, telefone (WhatsApp), CPF/CNPJ, dados da reserva | **Operadora em nome do Negócio**: o Negócio é quem decide oferecer o agendamento e coleta esses dados do próprio cliente; a Facilitei trata esses dados como ferramenta contratada pelo Negócio |

> Um contrato específico entre Facilitei e cada Negócio, detalhando essa divisão de papéis (controlador/operador) de forma mais formal, está previsto no roadmap do produto e ainda não substitui esta política.

## 2. Quais dados coletamos

**Do Negócio, no cadastro e uso do painel:**
- Nome do negócio e slug (link público de agendamento)
- E-mail e senha (armazenada com hash, nunca em texto puro)
- Horários de funcionamento e serviços cadastrados
- Chave de API da conta Asaas do próprio Negócio — **cifrada em repouso no banco de dados** (AES via Spring Security Crypto) e nunca devolvida em texto puro pela API depois de salva
- Endereço IP, para proteção contra tentativas de login por força bruta (rate limiting)

**Do Cliente Final, no momento do agendamento:**
- Nome completo
- Telefone (WhatsApp)
- CPF ou CNPJ
- Serviço escolhido, data/horário e, quando aplicável, status do pagamento do sinal

Não coletamos dados sensíveis (saúde, biometria, origem racial etc.) nem dados de menores de idade — a plataforma não é direcionada a crianças ou adolescentes.

## 3. Para que usamos esses dados, e com qual base legal (art. 7º da LGPD)

| Finalidade do tratamento | Dados usados | Base legal (art. 7º, LGPD) |
|---|---|---|
| Viabilizar agendamentos e gerenciar horários | Nome, e-mail, telefone | Execução de contrato (inciso V) |
| Gerar a cobrança Pix do sinal e conferir o pagamento — repassando dados à API da Asaas, usando a chave do próprio Negócio (ver seção 4 e o modelo BYOPP nos [Termos de Uso](termos-de-uso.md)) | Nome, CPF/CNPJ, dados transacionais | Execução de contrato e cumprimento de obrigação legal (incisos V e II) |
| Comunicar o status da reserva ao Cliente Final (confirmação, lembretes, expiração por falta de pagamento) | Telefone, e-mail | Legítimo interesse ou execução de contrato (incisos IX ou V) |
| Autenticar o Negócio no painel e proteger contra acesso indevido (rate limiting de login) | E-mail, senha (hash), endereço IP | **[Legítimo interesse (inciso IX) — sugestão a confirmar com o profissional]** |
| Cumprir obrigações legais, quando aplicável (ex.: fiscais, regulatórias) | Dados cadastrais do Negócio | Cumprimento de obrigação legal (inciso II) |

Não usamos os dados do Cliente Final para enviar publicidade da Facilitei, nem vendemos ou alugamos dados pessoais a terceiros.

## 4. Com quem compartilhamos dados

- **Asaas** (instituição de pagamento): recebe nome, CPF/CNPJ e telefone do Cliente Final para gerar a cobrança Pix, diretamente na conta do Negócio que fez o agendamento. A Asaas tem sua própria política de privacidade como instituição de pagamento regulada.
- **Provedores de infraestrutura** (hospedagem, banco de dados): armazenam os dados em nome da Facilitei, sob obrigação contratual de confidencialidade. Ver seção 5 sobre a localização do servidor.
- Não compartilhamos dados com nenhum outro terceiro para fins de marketing ou revenda.

## 5. Transferência internacional de dados

Os dados tratados pela Facilitei são armazenados em servidor localizado em **Boston, Estados Unidos** (infraestrutura contratada junto à Hostinger). Isso caracteriza uma transferência internacional de dados pessoais, nos termos do art. 33 da Lei Geral de Proteção de Dados.

- Os Estados Unidos não constam, até o momento, na lista de países com nível de proteção de dados reconhecido como adequado pela Autoridade Nacional de Proteção de Dados (ANPD) — hoje limitada à União Europeia e ao Espaço Econômico Europeu.
- A transferência é fundamentada na adoção das Cláusulas-Padrão Contratuais (CPCs) aprovadas pela ANPD por meio da Resolução CD/ANPD nº 19/2024, ou, alternativamente, na estrita necessidade de transferência para a execução do contrato do qual o titular é parte (art. 33, inciso IX, da LGPD), garantindo salvaguardas e nível de proteção equivalente ao exigido pela legislação brasileira.
- Isso não impede nem torna irregular o uso desse provedor — é uma prática comum inclusive entre empresas brasileiras —, mas a LGPD exige essa divulgação clara do país de destino e do mecanismo utilizado.

> Falta apenas formalizar essa cláusula no contrato com o provedor de hospedagem (Hostinger), quando aplicável — ver `ROADMAP.md`.

## 6. Por quanto tempo guardamos os dados

- Dados do Negócio: enquanto a conta estiver ativa, e por prazo adicional exigido por obrigações legais/fiscais após o encerramento.
- Dados do Cliente Final: pelo tempo necessário para viabilizar a reserva e eventual necessidade de comprovação do pagamento do sinal perante o Negócio, ou por prazo legal aplicável.
- Dados podem ser mantidos por período adicional quando necessário para cumprimento de obrigação legal ou exercício regular de direitos em processo judicial/administrativo.

## 7. Segurança

- Senhas armazenadas com hash (nunca em texto puro).
- Chave de API da conta Asaas de cada Negócio cifrada em repouso no banco de dados.
- Comunicação via HTTPS em produção.
- Proteção contra força bruta no login (limite de tentativas por IP).
- Como toda plataforma, não há garantia absoluta contra incidentes de segurança, mas adotamos medidas técnicas razoáveis proporcionais ao estágio e porte atual do produto.

## 8. Seus direitos como titular de dados (art. 18 da LGPD)

Você pode solicitar, a qualquer momento:

- Confirmação da existência de tratamento de seus dados;
- Acesso aos dados que temos sobre você;
- Correção de dados incompletos, inexatos ou desatualizados;
- Anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade com a lei;
- Portabilidade dos dados a outro fornecedor de serviço;
- Eliminação dos dados tratados com base em consentimento, quando aplicável;
- Informação sobre com quem compartilhamos seus dados;
- Revogação do consentimento, quando o tratamento se basear nele.

Se você é **Cliente Final** e quer exercer algum desses direitos sobre os dados de um agendamento específico, entre em contato diretamente com o Negócio onde agendou — ele é o responsável pela relação com você. Se preferir, também pode nos contatar em **[e-mail de contato]** e faremos a intermediação com o Negócio.

Se você é o **Negócio**, pode exercer esses direitos diretamente pelo painel (edição de cadastro) ou pelo e-mail acima.

## 9. Cookies e armazenamento local

O painel administrativo usa armazenamento local do navegador (localStorage) para manter a sessão autenticada (token de acesso). Não usamos cookies de rastreamento publicitário.

## 10. Alterações desta política

Podemos atualizar esta política para refletir mudanças na plataforma ou na legislação. A versão vigente estará sempre disponível nesta página, com a data da última atualização no topo.

## 11. Contato e encarregado (DPO)

Na qualidade de Agente de Tratamento de Pequeno Porte e em conformidade com as flexibilizações previstas na Resolução CD/ANPD nº 2/2022, a Facilitei fica dispensada da indicação formal de um Encarregado de Proteção de Dados (DPO) nomeado.

Para exercer seus direitos previstos na LGPD ou tirar dúvidas sobre esta política, o titular pode utilizar nosso canal direto e centralizado de atendimento: **[e-mail de contato]**.
