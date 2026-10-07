package br.com.gerdau.servicos.api.maintenance;

import br.com.gerdau.servicos.api.dto.AdminDtos.ColecaoInfo;
import br.com.gerdau.servicos.api.dto.AdminDtos.DiagnosticoResponse;
import br.com.gerdau.servicos.api.service.DiagnosticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Ao subir, mostra no log a que banco a API se conectou e o que encontrou (ou como corrigir se falhar). */
@Component
@Order(20)
@ConditionalOnProperty(name = "app.maintenance.startup-diagnostics", havingValue = "true", matchIfMissing = true)
public class StartupDiagnosticsRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupDiagnosticsRunner.class);

    private final DiagnosticsService diagnostics;

    public StartupDiagnosticsRunner(DiagnosticsService diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            DiagnosticoResponse d = diagnostics.snapshot();
            log.info("MongoDB OK — banco='{}', busca={}, similaridade={}", d.banco(), d.motorBusca(), d.enforcement());
            for (ColecaoInfo c : d.colecoes()) {
                if (c.existe()) {
                    log.info("  coleção {}: {} documento(s) {}", c.nome(), c.documentos(),
                            c.indicesBusca().isEmpty() ? "" : "| índices de busca: " + c.indicesBusca());
                } else {
                    log.warn("  coleção {}: NÃO ENCONTRADA neste banco (confira o nome do banco na MONGODB_URI)", c.nome());
                }
            }
        } catch (Exception e) {
            log.error("NÃO foi possível ler o MongoDB: {}. Verifique MONGODB_URI, usuário/senha, IP liberado no Atlas "
                    + "e rede — passo a passo em docs/CONECTAR-MONGODB.md (seção Problemas comuns).", e.getMessage());
        }
    }
}
