package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.domain.TipoComponente;
import br.com.gerdau.servicos.api.dto.ComponenteDtos.ComponenteDto;
import br.com.gerdau.servicos.api.text.TextNormalizer;
import org.bson.Document;
import org.springframework.stereotype.Component;

import static br.com.gerdau.servicos.api.persistence.DocumentSupport.id;
import static br.com.gerdau.servicos.api.persistence.DocumentSupport.str;

@Component
public class ComponenteMapper {

    private final TextNormalizer normalizer;

    public ComponenteMapper(TextNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /** Usa o normalizado gravado no banco; se faltar (legado), calcula na hora. */
    public String normalizedOf(TipoComponente tipo, Document doc) {
        String stored = str(doc, tipo.normalizedField());
        return stored.isBlank() ? normalizer.normalize(str(doc, tipo.textField())) : stored;
    }

    public ComponenteDto toDto(TipoComponente tipo, Document doc, Double score, boolean aproximado) {
        String id = id(doc);
        String texto = str(doc, tipo.textField());
        String sigla = tipo == TipoComponente.UM ? str(doc, "sigla") : null;
        String rotulo = sigla != null && !sigla.isBlank() ? sigla + " - " + texto : id + " - " + texto;
        return new ComponenteDto(id, tipo, texto, sigla, rotulo, score, aproximado);
    }
}
