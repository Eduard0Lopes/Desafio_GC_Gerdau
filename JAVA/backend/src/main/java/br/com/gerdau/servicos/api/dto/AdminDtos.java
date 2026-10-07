package br.com.gerdau.servicos.api.dto;

import java.util.List;

public final class AdminDtos {

    private AdminDtos() {}

    public record MeResponse(String username, List<String> roles) {}

    public record ColecaoInfo(String nome, boolean existe, Long documentos, List<String> indicesBusca) {}

    public record DiagnosticoResponse(String banco, String motorBusca, String enforcement,
                                      List<ColecaoInfo> colecoes) {}
}
