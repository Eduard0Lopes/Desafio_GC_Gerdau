function escaparCelula(value: unknown): string {
  return '"' + String(value).replace(/"/g, '""') + '"'
}

export function downloadCsv(nome: string, rows: string[][]): void {
  const csv = rows.map((row) => row.map(escaparCelula).join(';')).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = nome
  link.click()
  URL.revokeObjectURL(url)
}

export function parseCsv(content: string): string[][] {
  const clean = content.replace(/^\uFEFF/, '').replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  const firstLine = clean.split('\n')[0] || ''
  const separator = (firstLine.match(/;/g)?.length ?? 0) >= (firstLine.match(/,/g)?.length ?? 0) ? ';' : ','
  const rows: string[][] = []
  let row: string[] = []
  let field = ''
  let inQuotes = false
  for (let index = 0; index < clean.length; index++) {
    const character = clean[index]
    if (inQuotes) {
      if (character === '"') {
        if (clean[index + 1] === '"') {
          field += '"'
          index++
        } else {
          inQuotes = false
        }
      } else {
        field += character
      }
    } else if (character === '"') {
      inQuotes = true
    } else if (character === separator) {
      row.push(field)
      field = ''
    } else if (character === '\n') {
      row.push(field)
      field = ''
      if (row.some((cellValue) => cellValue.trim() !== '')) rows.push(row)
      row = []
    } else {
      field += character
    }
  }
  row.push(field)
  if (row.some((cellValue) => cellValue.trim() !== '')) rows.push(row)
  return rows
}
