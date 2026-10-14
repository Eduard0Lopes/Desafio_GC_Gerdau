package com.gerdau.desafio.service;

import com.gerdau.desafio.dto.response.ResumoResponse;
import com.gerdau.desafio.repository.CodigoCompletoRepository;
import com.gerdau.desafio.repository.CodigoFornecedorRepository;
import com.gerdau.desafio.repository.CodigoServicoRepository;
import com.gerdau.desafio.repository.GrupoServicoRepository;
import com.gerdau.desafio.repository.UnidadeMedidaRepository;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final GrupoServicoRepository grupos;
    private final UnidadeMedidaRepository unidades;
    private final CodigoFornecedorRepository fornecedores;
    private final CodigoServicoRepository servicos;
    private final CodigoCompletoRepository completos;

    public ResumoService(GrupoServicoRepository grupos, UnidadeMedidaRepository unidades,
                         CodigoFornecedorRepository fornecedores, CodigoServicoRepository servicos,
                         CodigoCompletoRepository completos) {
        this.grupos = grupos;
        this.unidades = unidades;
        this.fornecedores = fornecedores;
        this.servicos = servicos;
        this.completos = completos;
    }

    public ResumoResponse obter() {
        return new ResumoResponse(grupos.count(), unidades.count(), fornecedores.count(), servicos.count(),
                completos.count());
    }
}
