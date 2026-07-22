# Política de Privacidade — Facilitei

> **Status: minuta inicial, ainda não publicada.** Cobre os fluxos de dados reais da plataforma (cadastro do Negócio, dados do Cliente Final no agendamento, repasse à Asaas). Campos entre `[colchetes]` dependem da abertura do CNPJ e definição do encarregado (DPO), e todo o documento deve ser revisado por um profissional antes de publicação — ver `ROADMAP.md`.

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

## 3. Para que usamos esses dados

- **Viabilizar o agendamento**: identificar quem reservou o quê, com quem e quando.
- **Gerar a cobrança Pix do sinal**, quando o serviço escolhido exigir — repassando nome, CPF/CNPJ e telefone do Cliente Final para a API da Asaas, usando a chave do próprio Negócio (ver seção 4 e o modelo BYOPP descrito nos [Termos de Uso](termos-de-uso.md)).
- **Autenticar o Negócio** no painel administrativo e proteger contra acesso indevido (rate limiting de login).
- **Comunicar o status da reserva** ao Cliente Final (confirmação, expiração por falta de pagamento etc.).
- **Cumprir obrigações legais**, quando aplicável (ex.: fiscais, regulatórias).

Não usamos os dados do Cliente Final para enviar publicidade da Facilitei, nem vendemos ou alugamos dados pessoais a terceiros.

## 4. Com quem compartilhamos dados

- **Asaas** (instituição de pagamento): recebe nome, CPF/CNPJ e telefone do Cliente Final para gerar a cobrança Pix, diretamente na conta do Negócio que fez o agendamento. A Asaas tem sua própria política de privacidade como instituição de pagamento regulada.
- **Provedores de infraestrutura** (hospedagem, banco de dados): armazenam os dados em nome da Facilitei, sob obrigação contratual de confidencialidade.
- Não compartilhamos dados com nenhum outro terceiro para fins de marketing ou revenda.

## 5. Por quanto tempo guardamos os dados

- Dados do Negócio: enquanto a conta estiver ativa, e por prazo adicional exigido por obrigações legais/fiscais após o encerramento.
- Dados do Cliente Final: pelo tempo necessário para viabilizar a reserva e eventual necessidade de comprovação do pagamento do sinal perante o Negócio, ou por prazo legal aplicável.
- Dados podem ser mantidos por período adicional quando necessário para cumprimento de obrigação legal ou exercício regular de direitos em processo judicial/administrativo.

## 6. Segurança

- Senhas armazenadas com hash (nunca em texto puro).
- Chave de API da conta Asaas de cada Negócio cifrada em repouso no banco de dados.
- Comunicação via HTTPS em produção.
- Proteção contra força bruta no login (limite de tentativas por IP).
- Como toda plataforma, não há garantia absoluta contra incidentes de segurança, mas adotamos medidas técnicas razoáveis proporcionais ao estágio e porte atual do produto.

## 7. Seus direitos como titular de dados (art. 18 da LGPD)

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

## 8. Cookies e armazenamento local

O painel administrativo usa armazenamento local do navegador (localStorage) para manter a sessão autenticada (token de acesso). Não usamos cookies de rastreamento publicitário.

## 9. Alterações desta política

Podemos atualizar esta política para refletir mudanças na plataforma ou na legislação. A versão vigente estará sempre disponível nesta página, com a data da última atualização no topo.

## 10. Contato e encarregado (DPO)

Para exercer seus direitos ou tirar dúvidas sobre esta política: **[e-mail de contato]**.

Encarregado de proteção de dados (DPO): **[nome — a definir; em estágio inicial, pode ser o próprio responsável pela empresa]**.
