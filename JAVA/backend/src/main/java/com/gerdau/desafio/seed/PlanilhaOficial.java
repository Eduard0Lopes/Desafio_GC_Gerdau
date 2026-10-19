package com.gerdau.desafio.seed;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlanilhaOficial {

    private PlanilhaOficial() {
    }

    public static List<Map<String, String>> ler(Path arquivo) throws IOException {
        try (InputStream entrada = Files.newInputStream(arquivo);
             Workbook workbook = new XSSFWorkbook(entrada)) {
            Sheet planilha = workbook.getSheetAt(0);

            Row cabecalho = planilha.getRow(0);
            if (cabecalho == null) {
                return List.of();
            }
            List<String> colunas = new ArrayList<>();
            for (Cell celula : cabecalho) {
                colunas.add(texto(celula).trim());
            }

            List<Map<String, String>> linhas = new ArrayList<>();
            for (int i = 1; i <= planilha.getLastRowNum(); i++) {
                Row linha = planilha.getRow(i);
                if (linha == null) {
                    continue;
                }
                Map<String, String> registro = new LinkedHashMap<>();
                boolean vazio = true;
                for (int c = 0; c < colunas.size(); c++) {
                    String valor = texto(linha.getCell(c));
                    if (!valor.isBlank()) {
                        vazio = false;
                    }
                    registro.put(colunas.get(c), valor);
                }
                if (!vazio) {
                    linhas.add(registro);
                }
            }
            return linhas;
        }
    }

    private static String texto(Cell celula) {
        if (celula == null) {
            return "";
        }
        return switch (celula.getCellType()) {
            case STRING -> celula.getStringCellValue();
            case NUMERIC -> {
                double numero = celula.getNumericCellValue();
                if (numero == Math.floor(numero) && !Double.isInfinite(numero) && Math.abs(numero) < 1e15) {
                    yield String.valueOf((long) numero);
                }
                yield String.valueOf(numero);
            }
            case BOOLEAN -> String.valueOf(celula.getBooleanCellValue());
            case FORMULA -> celula.getCellFormula();
            default -> "";
        };
    }
}
