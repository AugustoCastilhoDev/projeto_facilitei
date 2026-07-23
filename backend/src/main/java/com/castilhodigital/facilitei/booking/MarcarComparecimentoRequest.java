package com.castilhodigital.facilitei.booking;

import jakarta.validation.constraints.NotNull;

public record MarcarComparecimentoRequest(@NotNull Boolean compareceu) {
}
