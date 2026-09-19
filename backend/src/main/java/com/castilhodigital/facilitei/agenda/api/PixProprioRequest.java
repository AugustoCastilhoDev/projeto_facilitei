package com.castilhodigital.facilitei.agenda.api;

import com.castilhodigital.facilitei.pix.TipoChavePix;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Nome e cidade do recebedor sao opcionais: na falta deles vale o nome do negocio e a cidade do diretorio. */
public record PixProprioRequest(

        @NotNull(message = "Informe o tipo da chave Pix.")
        TipoChavePix tipoChave,

        @NotBlank(message = "Informe a chave Pix.")
        @Size(max = 100, message = "Chave Pix longa demais.")
        String chave,

        @Size(max = 60, message = "Nome do recebedor longo demais.")
        String nomeRecebedor,

        @Size(max = 60, message = "Cidade do recebedor longa demais.")
        String cidadeRecebedor

) {
}
