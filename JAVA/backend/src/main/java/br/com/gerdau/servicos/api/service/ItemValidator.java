package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.config.AppProperties;
import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.persistence.ComponenteRepository;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import br.com.gerdau.servicos.api.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Validações de negócio no SERVIDOR (doc. 3 e 10.4), inclusive o limite de 40 caracteres do SAP. */
@Component
public class ItemValidator {

    public record ItemValido(TipoComponente tipo, String texto, String sigla) {}

    private static final Pattern SIGLA = Pattern.compile("^[A-Z0-9²³]{1,6}$");

    private final AppProperties props;
    private final TextNormalizer normalizer;
    private final ComponenteRepository repo;

    public ItemValidator(AppProperties props, TextNormalizer normalizer, ComponenteRepository repo) {
        this.props = props;
        this.normalizer = normalizer;
        this.repo = repo;
    }

    public ItemValido validar(TipoComponente tipo, String textoRaw, String siglaRaw) {
        String texto = textoRaw == null ? "" : textoRaw.strip().replaceAll("\\s+", " ");
        if (texto.isEmpty() || normalizer.normalize(texto).isEmpty()) {
            throw unprocessable("TEXTO_OBRIGATORIO", "Informe o texto do item (letras ou números).", Map.of());
        }
        int max = tipo == TipoComponente.CS ? props.limits().descricaoMax() : props.limits().nomeMax();
        int tamanho = texto.codePointCount(0, texto.length());
        if (tamanho > max) {
            String codigo = tipo == TipoComponente.CS ? "DESCRICAO_ACIMA_DO_LIMITE" : "TEXTO_ACIMA_DO_LIMITE";
            throw unprocessable(codigo, "O texto tem " + tamanho + " caracteres; o limite é " + max + ".",
                    Map.of("limite", max, "tamanho", tamanho));
        }
        String sigla = null;
        if (tipo == TipoComponente.UM) {
            sigla = siglaRaw == null ? "" : siglaRaw.strip().toUpperCase(Locale.ROOT);
            if (!SIGLA.matcher(sigla).matches()) {
                throw unprocessable("SIGLA_INVALIDA", "Informe a sigla da unidade (1 a 6 letras/números, ex.: HR, M2).",
                        Map.of());
            }
            if (!repo.findBySigla(sigla).isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, "SIGLA_DUPLICADA",
                        "Já existe uma unidade de medida com a sigla " + sigla + ". Reutilize-a.");
            }
        }
        return new ItemValido(tipo, texto, sigla);
    }

    private static ApiException unprocessable(String codigo, String mensagem, Map<String, ?> extras) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensagem, extras);
    }
}
