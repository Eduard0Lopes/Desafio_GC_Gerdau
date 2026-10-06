import os
from typing import List, Dict, Any, Optional
from datetime import datetime
from app.services.fuzzy import normalizar_texto, calcular_similaridade, validar_limite_sap

# Dados Iniciais Sanitizados (Seed Data da Gerdau)
SEED_GRUPOS_SERVICO = [
    {"_id": "001", "nome": "Eletromecânica", "nomeNormalizado": "eletromecanica", "origem": "MIGRACAO"},
    {"_id": "002", "nome": "Facilities", "nomeNormalizado": "facilities", "origem": "MIGRACAO"},
    {"_id": "003", "nome": "Civil", "nomeNormalizado": "civil", "origem": "MIGRACAO"},
    {"_id": "004", "nome": "Automação Industrial", "nomeNormalizado": "automacao industrial", "origem": "MIGRACAO"}
]

SEED_CODIGOS_FORNECEDOR = [
    {"_id": "001", "nome": "0123456 - ACME Corp", "nomeNormalizado": "0123456 acme corp", "origem": "MIGRACAO"},
    {"_id": "002", "nome": "0987654 - TechServ Ltd", "nomeNormalizado": "0987654 techserv ltd", "origem": "MIGRACAO"},
    {"_id": "003", "nome": "0456123 - Engemec Serviços", "nomeNormalizado": "0456123 engemec servicos", "origem": "MIGRACAO"},
    {"_id": "004", "nome": "0789456 - Gerdau Serviços Gerais", "nomeNormalizado": "0789456 gerdau servicos gerais", "origem": "MIGRACAO"}
]

SEED_CODIGOS_SERVICO = [
    {"_id": "000001", "descricao": "MANUTENCAO MECANICA NORMAL", "descricaoNormalizada": "manutencao mecanica normal", "quantidadeCaracteres": 26, "origem": "MIGRACAO", "legacyIds": ["MEC.MANUT.NORM", "4521"]},
    {"_id": "000002", "descricao": "Eletricista Instalador Manutenção", "descricaoNormalizada": "eletricista instalador manutencao", "quantidadeCaracteres": 34, "origem": "MIGRACAO", "legacyIds": ["ELET-INST-01"]},
    {"_id": "000003", "descricao": "Serviço de Limpeza e Conservação", "descricaoNormalizada": "servico de limpeza e conservacao", "quantidadeCaracteres": 32, "origem": "MIGRACAO", "legacyIds": ["FAC-LIMP-01"]},
    {"_id": "000004", "descricao": "Reformas de Alvenaria e Obras Civil", "descricaoNormalizada": "reformas de alvenaria e obras civil", "quantidadeCaracteres": 35, "origem": "MIGRACAO", "legacyIds": ["CIVIL-OBRA-02"]}
]

SEED_UNIDADES_MEDIDA = [
    {"_id": "01", "nome": "Hora", "sigla": "HR", "nomeNormalizado": "hora hr", "origem": "MIGRACAO"},
    {"_id": "02", "nome": "Unidade", "sigla": "UN", "nomeNormalizado": "unidade un", "origem": "MIGRACAO"},
    {"_id": "03", "nome": "Metro Quadrado", "sigla": "M2", "nomeNormalizado": "metro quadrado m2", "origem": "MIGRACAO"},
    {"_id": "04", "nome": "Serviço Global", "sigla": "SV", "nomeNormalizado": "servico global sv", "origem": "MIGRACAO"}
]

SEED_CODIGOS_COMPLETOS = [
    {
        "_id": "00100100000101",
        "idCompleto": "00100100000101",
        "gsId": "001",
        "cfId": "001",
        "csId": "000001",
        "umId": "01",
        "descricaoSnapshot": "Eletromecânica | 0123456 - ACME Corp | MANUTENCAO MECANICA NORMAL | HR",
        "usuarioCriacaoId": "403921",
        "createdAt": "2026-10-03T23:24:40-03:00",
        "origem": "MIGRACAO",
        "legacyIds": ["SV-MEC-01-HN"]
    }
]

class DatabaseService:
    def __init__(self):
        self.mongo_uri = os.getenv("MONGO_URI", "mongodb://localhost:27017/gerdau_db")
        self.use_mongo = False
        
        # Em-memória Fallback inicializado com dados padronizados
        self.db_grupos = list(SEED_GRUPOS_SERVICO)
        self.db_fornecedores = list(SEED_CODIGOS_FORNECEDOR)
        self.db_servicos = list(SEED_CODIGOS_SERVICO)
        self.db_unidades = list(SEED_UNIDADES_MEDIDA)
        self.db_completos = list(SEED_CODIGOS_COMPLETOS)
        self.db_governanca = []

    def get_grupos(self, query: Optional[str] = None) -> List[Dict[str, Any]]:
        if not query:
            return self.db_grupos
        q_norm = normalizar_texto(query)
        return [g for g in self.db_grupos if q_norm in g["nomeNormalizado"] or query.lower() in g["_id"].lower()]

    def get_fornecedores(self, query: Optional[str] = None) -> List[Dict[str, Any]]:
        if not query:
            return self.db_fornecedores
        q_norm = normalizar_texto(query)
        return [f for f in self.db_fornecedores if q_norm in f["nomeNormalizado"] or query.lower() in f["_id"].lower()]

    def get_servicos(self, query: Optional[str] = None) -> List[Dict[str, Any]]:
        if not query:
            return self.db_servicos
        q_norm = normalizar_texto(query)
        return [s for s in self.db_servicos if q_norm in s["descricaoNormalizada"] or query.lower() in s["_id"].lower()]

    def get_unidades(self, query: Optional[str] = None) -> List[Dict[str, Any]]:
        if not query:
            return self.db_unidades
        q_norm = normalizar_texto(query)
        return [u for u in self.db_unidades if q_norm in u["nomeNormalizado"] or query.lower() in u["_id"].lower()]

    def checar_similaridade_servico(self, descricao: str) -> List[Dict[str, Any]]:
        resultados = []
        norm_busca = normalizar_texto(descricao)
        for s in self.db_servicos:
            score = calcular_similaridade(descricao, s["descricao"])
            if score > 0.40:  # Retornar correspondências relevantes
                resultados.append({
                    "id": s["_id"],
                    "descricao": s["descricao"],
                    "matchScore": round(score * 100, 1),
                    "scoreDecimal": score,
                    "acaoRecomendada": "BLOQUEAR" if score >= 0.90 else ("ALERTAR" if score >= 0.75 else "PERMITIR")
                })
        resultados.sort(key=lambda x: x["scoreDecimal"], reverse=True)
        return resultados

    def salvar_codigo_completo(self, gs_id: str, cf_id: str, cs_id: str, um_id: str, usuario_id: str = "403921") -> Dict[str, Any]:
        id_completo = f"{gs_id}{cf_id}{cs_id}{um_id}"
        
        # Verificar duplicidade exata de código completo
        for item in self.db_completos:
            if item["idCompleto"] == id_completo:
                return {"sucesso": False, "mensagem": "Código Completo já cadastrado na base!", "item": item}

        gs = next((g for g in self.db_grupos if g["_id"] == gs_id), None)
        cf = next((c for c in self.db_fornecedores if c["_id"] == cf_id), None)
        cs = next((s for s in self.db_servicos if s["_id"] == cs_id), None)
        um = next((u for u in self.db_unidades if u["_id"] == um_id), None)

        snapshot = f"{gs['nome'] if gs else gs_id} | {cf['nome'] if cf else cf_id} | {cs['descricao'] if cs else cs_id} | {um['sigla'] if um else um_id}"

        novo_registro = {
            "_id": id_completo,
            "idCompleto": id_completo,
            "gsId": gs_id,
            "cfId": cf_id,
            "csId": cs_id,
            "umId": um_id,
            "descricaoSnapshot": snapshot,
            "usuarioCriacaoId": usuario_id,
            "createdAt": datetime.now().isoformat(),
            "origem": "APLICACAO",
            "legacyIds": []
        }
        self.db_completos.append(novo_registro)
        return {"sucesso": True, "mensagem": "Código Completo vinculado com sucesso!", "item": novo_registro}

    def cadastrar_novo_servico(self, descricao: str, usuario_id: str = "403921") -> Dict[str, Any]:
        valido, tamanho = validar_limite_sap(descricao)
        if not valido:
            return {"sucesso": False, "mensagem": f"Descrição excede o limite estrito do SAP de 40 caracteres ({tamanho}/40)."}

        # Checar se já existe similar
        similares = self.checar_similaridade_servico(descricao)
        bloqueados = [s for s in similares if s["scoreDecimal"] >= 0.90]
        
        if bloqueados:
            return {
                "sucesso": False,
                "bloqueado": True,
                "mensagem": "Já existe um serviço semelhante entre os itens cadastrados.",
                "itemSimilar": bloqueados[0]
            }

        proximo_id = f"{len(self.db_servicos) + 1:06d}"
        novo = {
            "_id": proximo_id,
            "descricao": descricao.upper(),
            "descricaoNormalizada": normalizar_texto(descricao),
            "quantidadeCaracteres": len(descricao),
            "origem": "APLICACAO",
            "legacyIds": []
        }
        self.db_servicos.append(novo)
        return {"sucesso": True, "mensagem": f"Serviço cadastrado com sucesso! ID gerado: {proximo_id}", "item": novo}

    def salvar_solicitacao_governanca(self, tipo: str, descricao: str, justificativa: str, usuario_id: str = "403921") -> Dict[str, Any]:
        solicitacao = {
            "id": f"GOV-{len(self.db_governanca) + 1:04d}",
            "tipoComponente": tipo,
            "descricao": descricao,
            "justificativa": justificativa,
            "usuarioId": usuario_id,
            "status": "PENDENTE_VALIDACAO_HUMANA",
            "createdAt": datetime.now().isoformat()
        }
        self.db_governanca.append(solicitacao)
        return {
            "sucesso": True,
            "mensagem": "Solicitação enviada com sucesso para a Equipe de Governança de Dados!",
            "solicitacao": solicitacao
        }

db_service = DatabaseService()
