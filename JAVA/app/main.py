from fastapi import FastAPI, Query, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, FileResponse
import os

from app.database import db_service
from app.models import (
    NovoItemRequest,
    SimilaridadeRequest,
    SolicitacaoGovernancaRequest
)
from app.services.fuzzy import validar_limite_sap

app = FastAPI(
    title="Gerdau Service Code Standardization API",
    description="API REST de Governança, Padronização e Prevenção de Duplicidades no SAP Gerdau",
    version="1.0.0"
)

# Permitir requisições CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Endpoints REST ---

@app.get("/api/v1/grupos-servico", summary="Listar/Buscar Grupos de Serviço (G.S)")
def listar_grupos_servico(query: str = Query(None, description="Termo de pesquisa para autocomplete")):
    return db_service.get_grupos(query)

@app.get("/api/v1/codigos-fornecedor", summary="Listar/Buscar Códigos de Fornecedor (C.F)")
def listar_codigos_fornecedor(query: str = Query(None, description="Termo de pesquisa para autocomplete")):
    return db_service.get_fornecedores(query)

@app.get("/api/v1/codigos-servico", summary="Listar/Buscar Códigos de Serviço (C.S)")
def listar_codigos_servico(query: str = Query(None, description="Termo de pesquisa para autocomplete")):
    return db_service.get_servicos(query)

@app.get("/api/v1/unidades-medida", summary="Listar/Buscar Unidades de Medida (U.M)")
def listar_unidades_medida(query: str = Query(None, description="Termo de pesquisa para autocomplete")):
    return db_service.get_unidades(query)

@app.post("/api/v1/itens/similaridade", summary="Checar Similaridade (Fuzzy Match / Stemming)")
def checar_similaridade(req: SimilaridadeRequest):
    valido, tamanho = validar_limite_sap(req.descricao)
    similares = db_service.checar_similaridade_servico(req.descricao)
    return {
        "descricaoOriginal": req.descricao,
        "tamanho": tamanho,
        "dentroDoLimiteSAP": valido,
        "similaresEncontrados": similares
    }

@app.post("/api/v1/codigos-completos/preview", summary="Prévia de vinculação do Código Completo")
def preview_codigo_completo(gsId: str, cfId: str, csId: str, umId: str):
    id_completo = f"{gsId}{cfId}{csId}{umId}"
    valido_cs = True
    return {
        "idCompleto": id_completo,
        "validacaoTamanhoSAP": valido_cs,
        "podeVincular": True
    }

@app.post("/api/v1/codigos-completos", summary="Vincular e Criar Transação de Código Completo")
def criar_codigo_completo(gsId: str, cfId: str, csId: str, umId: str, usuarioId: str = "403921"):
    res = db_service.salvar_codigo_completo(gsId, cfId, csId, umId, usuarioId)
    if not res["sucesso"]:
        raise HTTPException(status_code=400, detail=res["mensagem"])
    return res

@app.post("/api/v1/codigos-servico", summary="Cadastrar Novo Código de Serviço com Trava do SAP")
def cadastrar_codigo_servico(req: NovoItemRequest):
    res = db_service.cadastrar_novo_servico(req.descricao, req.usuarioId or "403921")
    if not res["sucesso"]:
        if res.get("bloqueado"):
            return {
                "bloqueado": True,
                "mensagem": res["mensagem"],
                "itemSimilar": res["itemSimilar"]
            }
        raise HTTPException(status_code=400, detail=res["mensagem"])
    return res

@app.post("/api/v1/governanca/solicitacoes", summary="Enviar Solicitação para Validação Humana da Governança")
def enviar_solicitacao_governanca(req: SolicitacaoGovernancaRequest):
    if not req.justificativa or len(req.justificativa.strip()) < 5:
        raise HTTPException(status_code=400, detail="A justificativa de negócio é obrigatória.")
    res = db_service.salvar_solicitacao_governanca(
        tipo=req.tipoComponente,
        descricao=req.descricao,
        justificativa=req.justificativa,
        usuario_id=req.usuarioId or "403921"
    )
    return res

# Montar frontend estático se a pasta existir
frontend_dir = os.path.join(os.path.dirname(__file__), "..", "frontend")
if os.path.exists(frontend_dir):
    app.mount("/static", StaticFiles(directory=frontend_dir), name="static")

    @app.get("/", response_class=HTMLResponse)
    def root():
        index_path = os.path.join(frontend_dir, "index.html")
        if os.path.exists(index_path):
            return FileResponse(index_path)
        return "Gerdau API Rodando!"
