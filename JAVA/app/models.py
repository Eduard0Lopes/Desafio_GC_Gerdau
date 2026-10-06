from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime

class GrupoServico(BaseModel):
    id: str = Field(..., alias="_id")
    nome: str
    nomeNormalizado: str
    origem: str = "SISTEMA"

class CodigoFornecedor(BaseModel):
    id: str = Field(..., alias="_id")
    nome: str
    nomeNormalizado: str
    origem: str = "SISTEMA"

class CodigoServico(BaseModel):
    id: str = Field(..., alias="_id")
    descricao: str
    descricaoNormalizada: str
    quantidadeCaracteres: int
    origem: str = "SISTEMA"
    legacyIds: List[str] = []

class UnidadeMedida(BaseModel):
    id: str = Field(..., alias="_id")
    nome: str
    sigla: str
    nomeNormalizado: str
    origem: str = "SISTEMA"

class CodigoCompleto(BaseModel):
    idCompleto: str
    gsId: str
    cfId: str
    csId: str
    umId: str
    descricaoSnapshot: str
    usuarioCriacaoId: str = "403921"
    createdAt: str
    origem: str = "APLICACAO"
    legacyIds: List[str] = []

class NovoItemRequest(BaseModel):
    tipoComponente: str # "CS", "GS", "CF", "UM"
    descricao: str
    unidadeMedida: Optional[str] = "HR"
    usuarioId: Optional[str] = "403921"

class SimilaridadeRequest(BaseModel):
    descricao: str
    tipoComponente: Optional[str] = "CS"

class SolicitacaoGovernancaRequest(BaseModel):
    tipoComponente: str
    descricao: str
    justificativa: str
    usuarioId: Optional[str] = "403921"
