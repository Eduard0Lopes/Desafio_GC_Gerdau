package br.com.gerdau.servicos.api.search;

import org.bson.Document;

/** Resultado bruto do mecanismo de busca. A nota final (0..1) é sempre calculada pela aplicação. */
public record SearchHit(String id, Document document, double engineScore) {}
