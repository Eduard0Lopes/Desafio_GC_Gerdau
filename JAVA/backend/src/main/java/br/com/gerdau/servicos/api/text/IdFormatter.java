package br.com.gerdau.servicos.api.text;

import br.com.gerdau.servicos.api.domain.TipoComponente;

import java.util.EnumMap;
import java.util.Map;

/**
 * Monta IDs de largura fixa e o ID completo (doc. seção 8.3): G.S.(3) + C.F.(3) + C.S.(6) + U.M.(2)
 * = 14 posições, sem siglas nem pontuação. As larguras vêm da configuração (app.ids.*).
 */
public final class IdFormatter {

    private final Map<TipoComponente, Integer> widths = new EnumMap<>(TipoComponente.class);

    public IdFormatter(int gs, int cf, int cs, int um) {
        widths.put(TipoComponente.GS, gs);
        widths.put(TipoComponente.CF, cf);
        widths.put(TipoComponente.CS, cs);
        widths.put(TipoComponente.UM, um);
    }

    public int width(TipoComponente tipo) {
        return widths.get(tipo);
    }

    /** Formata o número sequencial com zeros à esquerda. */
    public String format(TipoComponente tipo, long number) {
        int width = width(tipo);
        String s = Long.toString(number);
        if (number < 0 || s.length() > width) {
            throw new IllegalStateException("Capacidade de IDs esgotada para " + tipo
                    + " (largura " + width + "). Aumente app.ids." + tipo.name().toLowerCase() + "-width.");
        }
        return "0".repeat(width - s.length()) + s;
    }

    /** Valida o ID de um componente e completa com zeros à esquerda quando numérico e mais curto. */
    public String canonical(TipoComponente tipo, String raw) {
        String id = raw == null ? "" : raw.trim();
        int width = width(tipo);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("ID de " + tipo.label() + " não informado.");
        }
        if (id.length() > width) {
            throw new IllegalArgumentException("ID '" + id + "' de " + tipo.label()
                    + " excede a largura padrão de " + width + " posições.");
        }
        boolean numeric = id.chars().allMatch(Character::isDigit);
        if (numeric) {
            return "0".repeat(width - id.length()) + id;
        }
        if (id.length() != width) {
            throw new IllegalArgumentException("ID '" + id + "' de " + tipo.label()
                    + " fora do padrão (esperado " + width + " posições).");
        }
        return id;
    }

    public String completeId(String gs, String cf, String cs, String um) {
        return canonical(TipoComponente.GS, gs) + canonical(TipoComponente.CF, cf)
                + canonical(TipoComponente.CS, cs) + canonical(TipoComponente.UM, um);
    }
}
