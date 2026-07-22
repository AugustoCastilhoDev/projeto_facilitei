# Configurar o recebimento de pagamentos (Pix)

Este guia é para o **dono do negócio** (não para desenvolvedores): mostra como configurar sua própria conta Asaas para que o sinal Pix cobrado dos seus clientes caia direto na sua conta, sem passar pela Facilitei.

Se preferir, a equipe da Facilitei pode fazer essa configuração para você — mas se quiser fazer sozinho, o passo a passo é este.

## Por que preciso de uma conta Asaas?

A Facilitei não guarda nem intermedia o dinheiro do sinal: cada negócio recebe direto na própria conta Asaas. Isso significa que você precisa ter (ou criar) uma conta lá, e nos dar apenas a chave de acesso — nunca sua senha.

## Passo 1 — Criar (ou acessar) sua conta Asaas

Se ainda não tem, crie uma conta gratuita em **[asaas.com](https://www.asaas.com)** (ambiente de produção — use `sandbox.asaas.com` apenas se estiver testando, não para receber pagamentos reais).

## Passo 2 — Encontrar sua chave de API

1. No painel da Asaas, vá em **Integrações → Chaves de API**.
2. Gere (ou copie, se já existir) sua **chave de API**.
3. Guarde essa chave com cuidado — ela dá acesso à sua conta Asaas, não compartilhe por WhatsApp/e-mail sem necessidade.

## Passo 3 — Colar a chave no painel da Facilitei

1. Entre no painel da Facilitei e abra a aba **Pagamentos**.
2. Cole a chave no campo **"Chave da API Asaas"** e clique em **Salvar chave**.
3. A tela vai mostrar o status "Recebimento configurado" e exibir duas informações novas: a **URL do webhook** e o **token de autenticação**.

## Passo 4 — Configurar o webhook na Asaas

Isso é o que avisa a Facilitei quando um cliente paga o sinal, para confirmar a reserva automaticamente.

1. No painel da Asaas, vá em **Integrações → Webhooks → Adicionar Webhook**.
2. Cole a **URL do webhook** (copiada da tela Pagamentos) no campo "URL do Webhook".
3. Cole o **token** (copiado da mesma tela) no campo "Token de autenticação".
4. Em "Adicionar Eventos", marque pelo menos:
   - `PAYMENT_CONFIRMED`
   - `PAYMENT_RECEIVED`
   - `PAYMENT_OVERDUE`
5. **Importante:** confira se o botão **"Fila de sincronização ativada?"** está **ligado**. Ele costuma vir desligado por padrão — se ficar desligado, os eventos são gerados mas nunca chegam até a Facilitei, e as reservas não confirmam sozinhas.
6. Clique em **Salvar**.

## Pronto — como testar

Acesse a página pública de agendamento do seu negócio, faça uma reserva de teste num serviço com sinal, pague o Pix e confira se a reserva muda para "confirmada" automaticamente em alguns segundos. Se não confirmar, o passo mais provável de ter ficado faltando é o item 5 acima (fila de sincronização desligada).

## Dúvidas comuns

- **"Errei a chave, e agora?"** Sem problema — volte na aba Pagamentos e salve a chave certa. Isso não apaga nem recria o token do webhook, então não precisa reconfigurar o webhook na Asaas de novo.
- **"Troquei de conta Asaas."** Nesse caso configure a nova chave normalmente, mas o webhook também precisa ser recriado na nova conta (contas diferentes = webhooks diferentes).
