package br.com.gerdau.servicos.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuração tipada (prefixo "app"). Os valores vêm do application.yml / variáveis de ambiente. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Search search,
        @DefaultValue Similarity similarity,
        @DefaultValue Ids ids,
        @DefaultValue Limits limits,
        @DefaultValue Security security,
        @DefaultValue Audit audit,
        @DefaultValue Maintenance maintenance) {

    public enum Engine { LOCAL, ATLAS }

    public enum Enforcement { OBSERVE, ENFORCE }

    public record Search(@DefaultValue("local") Engine engine,
                         @DefaultValue("default") String atlasIndexName) {}

    public record Similarity(@DefaultValue("enforce") Enforcement enforcement,
                             @DefaultValue("0.75") double alertThreshold,
                             @DefaultValue("0.90") double blockThreshold,
                             @DefaultValue("200") int candidatePool) {}

    public record Ids(@DefaultValue("3") int gsWidth,
                      @DefaultValue("3") int cfWidth,
                      @DefaultValue("6") int csWidth,
                      @DefaultValue("2") int umWidth) {}

    public record Limits(@DefaultValue("40") int descricaoMax,
                         @DefaultValue("100") int nomeMax) {}

    public record Security(@DefaultValue("") String devPassword,
                           @DefaultValue("roles") String rolesClaim) {}

    public record Audit(@DefaultValue("false") boolean recordIp) {}

    public record Maintenance(@DefaultValue("true") boolean ensureIndexes,
                              @DefaultValue("false") boolean backfillNormalized,
                              @DefaultValue("true") boolean startupDiagnostics) {}
}
