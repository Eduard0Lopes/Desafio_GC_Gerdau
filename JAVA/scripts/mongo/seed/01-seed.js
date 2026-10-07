// Dados de EXEMPLO para o MongoDB local de teste (perfil "local-db").
// Executado automaticamente pelo container do Mongo na primeira inicialização do volume.
// Segue o modelo de dados do documento (seção 7). NÃO é usado quando você conecta no SEU banco.
const database = db.getSiblingDB("gerdau_servicos");
const len = (s) => Array.from(s).length;
const now = new Date();

database.grupos_servico.insertMany([
  { _id: "001", nome: "Eletromecânica", nomeNormalizado: "eletromecanica", origem: "MIGRACAO" },
  { _id: "002", nome: "Facilities", nomeNormalizado: "facilities", origem: "MIGRACAO" },
  { _id: "003", nome: "Civil", nomeNormalizado: "civil", origem: "MIGRACAO" },
]);

database.codigos_fornecedor.insertMany([
  { _id: "001", nome: "Fornecedor de manutenção industrial", nomeNormalizado: "fornecedor manutencao industrial", origem: "MIGRACAO" },
  { _id: "002", nome: "Fornecedor de serviços de facilities", nomeNormalizado: "fornecedor servicos facilities", origem: "MIGRACAO" },
  { _id: "003", nome: "Fornecedor de obras civis", nomeNormalizado: "fornecedor obras civis", origem: "MIGRACAO" },
]);

const cs = [
  { _id: "000001", descricao: "Manutenção mecânica normal", norm: "manutencao mecanica normal", legacyIds: ["4521", "MEC.MANUT.NORM"] },
  { _id: "000002", descricao: "Eletricista instalador", norm: "eletricista instalador", legacyIds: [] },
  { _id: "000003", descricao: "Mecânico manutenção com especialização", norm: "mecanico manutencao especializacao", legacyIds: [] },
  { _id: "000004", descricao: "Limpeza predial", norm: "limpeza predial", legacyIds: [] },
  { _id: "000005", descricao: "Pintura de paredes", norm: "pintura paredes", legacyIds: [] },
].map((d) => ({
  _id: d._id,
  descricao: d.descricao,
  descricaoNormalizada: d.norm,
  quantidadeCaracteres: len(d.descricao),
  origem: "MIGRACAO",
  legacyIds: d.legacyIds,
}));
database.codigos_servico.insertMany(cs);

database.unidades_medida.insertMany([
  { _id: "01", nome: "Hora", sigla: "HR", nomeNormalizado: "hora", origem: "MIGRACAO" },
  { _id: "02", nome: "Unidade", sigla: "UN", nomeNormalizado: "unidade", origem: "MIGRACAO" },
  { _id: "03", nome: "Metro quadrado", sigla: "M2", nomeNormalizado: "metro quadrado", origem: "MIGRACAO" },
]);

database.codigos_completos.insertOne({
  _id: "00100100000101",
  idCompleto: "00100100000101",
  gsId: "001", cfId: "001", csId: "000001", umId: "01",
  descricaoSnapshot: "Eletromecânica | Fornecedor de manutenção industrial | Manutenção mecânica normal | HR",
  usuarioCriacaoId: null,
  createdAt: null,
  origem: "MIGRACAO",
  legacyImportedAt: now,
  legacyIds: [],
});

print("Seed concluído: banco gerdau_servicos populado com dados de exemplo.");
