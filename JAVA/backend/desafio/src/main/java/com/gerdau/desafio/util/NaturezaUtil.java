package com.gerdau.desafio.util;

import java.util.List;
import java.util.Locale;

/**
 * Classificação da natureza do serviço — resposta da área ao comitê:
 * "o que traria maior ganho seria a similaridade de MÃO DE OBRA... contemplar
 * também materiais, equipamentos e locações... para torná-los identificáveis"
 * (ex.: somar "mobilização" entre contratos).
 *
 * Classificação por vocabulário da categoria (heurística configurável) —
 * sinal para busca e agrupamento, não equivalência garantida.
 */
public final class NaturezaUtil {

    public static final String MAO_DE_OBRA = "Mão de obra";
    public static final String MATERIAL = "Material";
    public static final String EQUIPAMENTO = "Equipamento";
    public static final String LOCACAO = "Locação";
    public static final String OUTROS = "Outros serviços";

    private static final List<String> TERMOS_MAO_DE_OBRA = List.of(
            "mecanic", "soldad", "supervisor", "engenheir", "tecnic", "operador", "pedreir",
            "eletricist", "eletric", "pintor", "encanador", "ajudante", "encarregad", "vigia",
            "vigilante", "motorist", "montador", "serralheir", "caldeireir", "inspetor",
            "projetist", "analist", "ferramenteiro", "soldador", "torneiro", "marceneiro",
            "carpinteiro", "armador", "servente", "mestre", "hora normal", "diaria", "mensal",
            "hora homem", "hora-homem", " jornada", "escala", "folga", "hora extra");

    private static final List<String> TERMOS_MATERIAL = List.of(
            "material", "materiais", "tinta", "cimento", "areia", "brita", "aco", "ferro",
            "parafuso", "porca", "rebite", "eletrodo", "disco", "fita", "cola", "argamassa",
            "tijolo", "massa", "aditivo", "selante", "epoxi", "epóxi", "verniz", "solvente",
            "luva", "epi", "fio", "cabo", "disjuntor", "tomada", "interruptor", "lampada",
            "lâmpada", "chave", "registro", "valvula", "válvula", "tubo", "tubulacao", "mangueira");

    private static final List<String> TERMOS_EQUIPAMENTO = List.of(
            "equipamento", "equipamentos", "maquina", "máquina", "compressor", "gerador",
            "andaime", "escada", "betoneira", "bomba", "guindaste", "empilhadeira", "trator",
            "retroescavadeira", "caminhao", "caminhão", "rebocador", "plataforma", "ponte rolante",
            "torno", "fresa", "furadeira", "esmerilhadeira", "solda portatil", "solda portátil",
            "extensao", "extensão", "roldana", "talha", "macaco", "vibrador");

    private static final List<String> TERMOS_LOCACAO = List.of(
            "locacao", "locação", "locado", "aluguel", "alugado", "renting", "fretado",
            "com motorista", "sem motorista");

    private NaturezaUtil() {
    }

    /** Classifica a descrição em uma natureza de serviço. */
    public static String classificar(String descricao) {
        String texto = " " + TextoUtil.normalizar(descricao == null ? "" : descricao) + " ";
        if (contem(texto, TERMOS_LOCACAO)) {
            return LOCACAO;
        }
        if (contem(texto, TERMOS_EQUIPAMENTO)) {
            return EQUIPAMENTO;
        }
        if (contem(texto, TERMOS_MATERIAL)) {
            return MATERIAL;
        }
        if (contem(texto, TERMOS_MAO_DE_OBRA)) {
            return MAO_DE_OBRA;
        }
        return OUTROS;
    }

    private static boolean contem(String texto, List<String> termos) {
        for (String termo : termos) {
            if (texto.contains(termo.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
