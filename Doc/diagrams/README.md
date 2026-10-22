# Diagrammer til Produktbeskrivelse.md

Kilderne er Graphviz-filer (`*.dot`), fordi md2pdf ikke kan rendere mermaid. Efter en ændring genereres SVG'en med:

```
dot -Tsvg flow1-selvregistrering.dot -o flow1-selvregistrering.svg
```

PDF'en bygges fra `Doc/` med `md2pdf Produktbeskrivelse.md`.
