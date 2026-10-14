package com.gerdau.desafio.dto.response;

public record ResumoResponse(
        long gruposServico,
        long unidadesMedida,
        long codigosFornecedor,
        long codigosServico,
        long codigosCompletos
) {
}
