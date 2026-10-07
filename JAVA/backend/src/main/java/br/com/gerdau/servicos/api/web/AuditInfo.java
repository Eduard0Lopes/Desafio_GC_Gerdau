package br.com.gerdau.servicos.api.web;

/** Quem fez a ação. O usuário vem SEMPRE do token/credencial autenticada; o IP é só metadado (opcional). */
public record AuditInfo(String usuario, String ip) {}
