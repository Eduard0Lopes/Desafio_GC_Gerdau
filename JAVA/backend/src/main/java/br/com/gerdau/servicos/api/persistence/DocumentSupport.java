package br.com.gerdau.servicos.api.persistence;

import org.bson.Document;

import java.util.Objects;

/** Leitura tolerante de documentos legados (campo ausente, nulo ou com tipo diferente do esperado). */
public final class DocumentSupport {

    private DocumentSupport() {}

    public static String str(Document doc, String field) {
        return Objects.toString(doc.get(field), "");
    }

    public static String id(Document doc) {
        return str(doc, "_id");
    }
}
