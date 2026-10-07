// Cria os índices do Atlas Search usados com SEARCH_ENGINE=atlas.
//
//   mongosh "SUA_MONGODB_URI" scripts/mongo/create-atlas-search-indexes.js
//
// É seguro rodar mais de uma vez: índices que já existem são ignorados.
// Requer MongoDB Atlas (cluster com Atlas Search) e um usuário com permissão de criar índices.
// Cada índice leva alguns minutos para ficar "queryable" — acompanhe em Atlas > Search.

const INDEX_NAME = "default"; // precisa ser igual a ATLAS_SEARCH_INDEX (padrão: default)

const targets = [
  { collection: "grupos_servico", field: "nomeNormalizado" },
  { collection: "codigos_fornecedor", field: "nomeNormalizado" },
  { collection: "codigos_servico", field: "descricaoNormalizada" },
  { collection: "unidades_medida", field: "nomeNormalizado" },
];

for (const { collection, field } of targets) {
  const col = db.getCollection(collection);
  const existing = col.getSearchIndexes(INDEX_NAME);
  if (existing && existing.length > 0) {
    print(`= ${collection}: índice '${INDEX_NAME}' já existe (status: ${existing[0].status})`);
    continue;
  }
  col.createSearchIndex({
    name: INDEX_NAME,
    definition: {
      mappings: {
        dynamic: false,
        fields: {
          [field]: [
            // texto completo, com stemming em português (palavras inteiras, erro de digitação via fuzzy)
            { type: "string", analyzer: "lucene.portuguese" },
            // autocomplete por prefixo enquanto o usuário digita
            { type: "autocomplete", analyzer: "lucene.standard", tokenization: "edgeGram", minGrams: 2, maxGrams: 15, foldDiacritics: true },
          ],
        },
      },
    },
  });
  print(`+ ${collection}: índice '${INDEX_NAME}' solicitado sobre '${field}'`);
}
print("Pronto. Aguarde o status 'READY/queryable' no Atlas antes de usar SEARCH_ENGINE=atlas.");
