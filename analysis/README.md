# 📊 Análise de Qualidade de Tradução

Este diretório contém ferramentas para analisar a qualidade das traduções do app.

## 📁 Estrutura

```
analysis/
├── datasets/           # Datasets de teste
│   └── test_dataset.json
├── notebooks/         # Jupyter Notebooks
│   └── translation_analysis.ipynb
├── scripts/           # Scripts Python
│   ├── run_analysis.py
│   └── collect_app_data.py
└── results/           # Resultados das análises
    ├── analysis_results.csv
    └── analysis_summary.json
```

## 🚀 Como Usar

### 1. Executar Análise Completa

```bash
cd analysis
pip install -r requirements.txt
python scripts/run_analysis.py
```

### 2. Usar Jupyter Notebook

```bash
cd analysis
jupyter notebook notebooks/translation_analysis.ipynb
```

### 3. Coletar Dados do App

```bash
# Exporte traduções do app para JSON/CSV
python scripts/collect_app_data.py --input translations.json --output datasets/user_data.json
```

## 📊 Métricas Calculadas

| Métrica | Descrição | Valor Ideal |
|---------|-----------|-------------|
| **Exact Match** | Tradução idêntica ao gabarito | 100% |
| **Similarity** | Similaridade textual (0-1) | > 0.9 |
| **BLEU Score** | Score de n-gramas em comum | > 0.7 |
| **WER** | Taxa de erros por palavra | < 10% |
| **CER** | Taxa de erros por caractere | < 5% |

## 📝 Dataset de Teste

O `test_dataset.json` contém 50 frases de teste divididas em:
- **common_phrases**: Frases do dia-a-dia
- **informal**: Linguagem informal
- **formal**: Linguagem formal
- **technical**: Termos técnicos

Cada frase tem:
- Texto fonte
- Tradução esperada (gabarito)
- Par de idiomas (en→pt, pt→en)
- Categoria

## 🔧 Scripts Disponíveis

### `run_analysis.py`
Executa análise completa das traduções.

```bash
python run_analysis.py --dataset datasets/test_dataset.json --output results
```

### `collect_app_data.py`
Coleta dados exportados do app para análise.

```bash
python collect_app_data.py --input app_export.json --ground-truth datasets/test_dataset.json
```

## 📈 Gerar Relatório

Execute o notebook `translation_analysis.ipynb` para gerar:
- Gráficos de distribuição de qualidade
- Análise por categoria
- Análise por par de idiomas
- Exemplos de erros
- Recomendações de melhoria

## 🎯 Interpretando Resultados

| Taxa de Acerto | Classificação |
|----------------|---------------|
| 90-100% | 🏆 Excelente |
| 70-89% | ✅ Bom |
| 50-69% | ⚠️ Regular |
| < 50% | 🔴 Necessita melhoria |

## 📦 Dependencies

```
pandas>=1.5.0
numpy>=1.23.0
matplotlib>=3.6.0
seaborn>=0.12.0
plotly>=5.10.0
scikit-learn>=1.1.0
jupyter>=1.0.0
```

## 🔄 Workflow de Análise

1. **Coletar**: Exporte traduções do app ou use dataset de teste
2. **Analisar**: Execute scripts ou notebooks para calcular métricas
3. **Visualizar**: Veja gráficos e relatórios gerados
4. **Decidir**: Use resultados para melhorar o app