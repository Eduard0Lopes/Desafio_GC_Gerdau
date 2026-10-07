package br.com.gerdau.servicos.api.service;

import br.com.gerdau.servicos.api.web.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Date;
import java.util.HexFormat;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Idempotência (doc. 10.3): a mesma Idempotency-Key + mesmo corpo devolve o resultado já produzido
 * em vez de criar de novo (duplo clique, retentativa de rede). Registros expiram em 24h (índice TTL).
 * Falhas NÃO ficam gravadas: o usuário pode corrigir o problema e reenviar com a mesma chave.
 */
@Service
public class IdempotencyService {

    private static final String COLLECTION = "idempotency_keys";
    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9._:-]{8,128}$");

    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    private final Clock clock;

    public IdempotencyService(MongoTemplate mongo, ObjectMapper mapper, Clock clock) {
        this.mongo = mongo;
        this.mapper = mapper;
        this.clock = clock;
    }

    public <T> T execute(String key, String user, String scope, Object requestBody,
                         Class<T> responseType, Supplier<T> action) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_INVALIDA",
                    "Envie o cabeçalho Idempotency-Key (8 a 128 caracteres: letras, números, '.', '_', ':' ou '-').");
        }
        String id = scope + ":" + user + ":" + key;
        String hash = sha256(toJson(requestBody));
        Document record = new Document("_id", id)
                .append("status", "IN_PROGRESS")
                .append("requestHash", hash)
                .append("createdAt", Date.from(clock.instant()));
        try {
            mongo.insert(record, COLLECTION);
        } catch (DuplicateKeyException e) {
            return replay(id, hash, responseType);
        }
        try {
            T result = action.get();
            mongo.updateFirst(byId(id), new Update().set("status", "COMPLETED").set("response", toJson(result)),
                    COLLECTION);
            return result;
        } catch (RuntimeException e) {
            mongo.remove(byId(id), COLLECTION);
            throw e;
        }
    }

    private <T> T replay(String id, String hash, Class<T> type) {
        Document existing = mongo.findOne(byId(id), Document.class, COLLECTION);
        if (existing == null) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUISICAO_EM_ANDAMENTO",
                    "Requisição repetida em processamento. Tente novamente em instantes.");
        }
        if (!hash.equals(existing.getString("requestHash"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUTILIZADA",
                    "Esta Idempotency-Key já foi usada com um conteúdo diferente. Gere uma nova chave.");
        }
        if ("COMPLETED".equals(existing.getString("status"))) {
            try {
                return mapper.readValue(existing.getString("response"), type);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Resposta idempotente corrompida", e);
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, "REQUISICAO_EM_ANDAMENTO",
                "Requisição repetida em processamento. Tente novamente em instantes.");
    }

    private Query byId(String id) {
        return Query.query(Criteria.where("_id").is(id));
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar", e);
        }
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
