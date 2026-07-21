-- CPF/CNPJ do cliente e opcional no MVP: a API do Asaas nao exige o campo
-- para criar um cliente (so "name" e obrigatorio), mas guardamos quando o
-- cliente informa para reduzir duplicidade de cadastros no Asaas.
ALTER TABLE bookings ADD COLUMN cliente_cpf_cnpj VARCHAR(20);
