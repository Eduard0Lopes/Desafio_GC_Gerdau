package com.gerdau.desafio.service;

import com.gerdau.desafio.exception.BusinessException;
import com.gerdau.desafio.exception.ConflictException;
import com.gerdau.desafio.model.Contador;
import com.gerdau.desafio.model.GrupoServico;
import com.gerdau.desafio.model.UnidadeMedida;
import com.gerdau.desafio.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SequenceServiceTest {

    private MongoTemplate mongoComContador(long valor) {
        MongoTemplate mongo = Mockito.mock(MongoTemplate.class);
        Contador atual = new Contador();
        atual.setId("codigos_servico");
        atual.setValor(valor);
        when(mongo.findById(eq("codigos_servico"), eq(Contador.class))).thenReturn(atual);
        
        when(mongo.findAndModify(any(), any(), any(), eq(Contador.class))).thenAnswer(inv -> {
            atual.setValor(atual.getValor() + 1);
            return atual;
        });
        return mongo;
    }

    @Test
    void pulaIdsJaUtilizadosPeloRegistroMigrado() {
        MongoTemplate mongo = mongoComContador(5);
        CodigoServicoRepository repo = Mockito.mock(CodigoServicoRepository.class);
        when(repo.existsById("CS-000006")).thenReturn(true);   
        when(repo.existsById("CS-000007")).thenReturn(false);  

        SequenceService sequence = new SequenceService(mongo, repo);
        assertEquals("CS-000007", sequence.proximoCodigoServico());
    }

    @Test
    void contadorAlinhadoAosMigradosEhReajustadoNaoEsgota() {
        
        MongoTemplate mongo = mongoComContador(999999);
        CodigoServicoRepository repo = Mockito.mock(CodigoServicoRepository.class);
        when(repo.existsById(anyString())).thenReturn(false);

        SequenceService sequence = new SequenceService(mongo, repo);
        assertEquals("CS-000001", sequence.proximoCodigoServico());
    }

    @Test
    void criarGrupoDuplicadoNaoSobrescreveOExistente() {
        GrupoServicoRepository grupoRepo = Mockito.mock(GrupoServicoRepository.class);
        when(grupoRepo.existsById("CIVIL")).thenReturn(true);
        CatalogoService service = new CatalogoService(grupoRepo,
                Mockito.mock(CodigoFornecedorRepository.class), Mockito.mock(CodigoServicoRepository.class),
                Mockito.mock(UnidadeMedidaRepository.class), Mockito.mock(CodigoCompletoRepository.class),
                Mockito.mock(SimilaridadeService.class),
                Mockito.mock(AuditoriaService.class), Mockito.mock(SequenceService.class));

        assertThrows(ConflictException.class, () -> service.criarGrupo("Civil"));
    }

    @Test
    void criarUnidadeDuplicadaNaoSobrescreveAExistente() {
        UnidadeMedidaRepository unidadeRepo = Mockito.mock(UnidadeMedidaRepository.class);
        when(unidadeRepo.existsById("HR")).thenReturn(true);
        CatalogoService service = new CatalogoService(Mockito.mock(GrupoServicoRepository.class),
                Mockito.mock(CodigoFornecedorRepository.class), Mockito.mock(CodigoServicoRepository.class),
                unidadeRepo, Mockito.mock(CodigoCompletoRepository.class),
                Mockito.mock(SimilaridadeService.class),
                Mockito.mock(AuditoriaService.class), Mockito.mock(SequenceService.class));

        assertThrows(ConflictException.class, () -> service.criarUnidade("HR", "Hora"));
    }

    @Test
    void faixaRealmenteEsgotadaGeraErroDeDominio() {
        MongoTemplate mongo = mongoComContador(999999);
        CodigoServicoRepository repo = Mockito.mock(CodigoServicoRepository.class);
        
        when(repo.existsById(anyString())).thenReturn(true);

        SequenceService sequence = new SequenceService(mongo, repo);
        BusinessException erro = assertThrows(BusinessException.class, sequence::proximoCodigoServico);
        org.junit.jupiter.api.Assertions.assertTrue(erro.getMessage().contains("código de serviço"));
    }
}
