# Offline Camera Translator 📱

**Tradutor offline com câmera para Android**

Traduza textos de imagens usando a câmera do celular ou digitando diretamente, sem necessidade de internet após baixar os idiomas.

---

## 📊 ESTATÍSTICAS DE QUALIDADE

### Resultados da Comunidade

| Métrica | Valor |
|---------|-------|
| **Total de Traduções** | 0 |
| **Total Avaliadas** | 0 |
| **Taxa de Acerto** | 0.0% |
| **Nota Média** | 0.00 / 5.0 |

### Distribuição de Avaliações

| Estrelas | Quantidade | Descrição |
|----------|------------|-----------|
| ⭐⭐⭐⭐⭐ Excelente | 0 | Tradução perfeita |
| ⭐⭐⭐⭐ Bom | 0 | Tradução quase correta |
| ⭐⭐⭐ Regular | 0 | Tradução aceitável |
| ⭐⭐ Ruim | 0 | Vários erros |
| ⭐ Muito ruim | 0 | Tradução incompreensível |

### Por Tipo de Tradução
- 📝 **Texto:** 0 traduções
- 📷 **Imagem:** 0 traduções

> Dados atualizados automaticamente com as avaliações da comunidade.

---

## 📱 Funcionalidades

### Tradução de Texto
- Digite textos para traduzir
- Seleção de idiomas de origem e destino
- Troca rápida de idiomas

### Tradução de Imagem
- Tire fotos de textos para traduzir
- Seleção de área de tradução
- OCR (reconhecimento óptico de caracteres)

### Biblioteca de Idiomas
- Baixe idiomas para uso offline
- Gerencie idiomas baixados
- Suporte a múltiplos idiomas

### Estatísticas
- Acompanhe suas traduções
- Avalie a qualidade das traduções (1-5 estrelas)
- Taxa de acerto do app
- Distribuição das avaliações

## 📊 Sistema de Avaliação

Após cada tradução, você pode avaliar a qualidade:

| Estrelas | Avaliação | Descrição |
|----------|----------|-----------|
| ⭐⭐⭐⭐⭐ | Excelente | Tradução perfeita |
| ⭐⭐⭐⭐ | Bom | Tradução quase correta |
| ⭐⭐⭐ | Regular | Tradução aceitável |
| ⭐⭐ | Ruim | Vários erros |
| ⭐ | Muito ruim | Tradução incompreensível |

### Taxa de Acerto

A **taxa de acerto** é calculada como:
```
(Excelente + Bom) / Total de Avaliações × 100
```

## 🔧 Tecnologias

- **Android CameraX** - Captura de imagem
- **Google ML Kit** - Tradução offline e OCR
- **Kotlin Coroutines** - Programação assíncrona
- **ViewBinding** - Ligação de views
- **Material Design** - Interface moderna

## 📦 Estrutura do Projeto

```
app/src/main/
├── java/com/offline/translator/
│   ├── model/
│   │   ├── Language.kt         # Modelo de idioma
│   │   ├── TranslationService.kt
│   │   ├── TextRecognitionService.kt
│   │   ├── StatsManager.kt     # Gerenciamento de estatísticas
│   │   └── TranslationStats.kt
│   ├── view/
│   │   ├── MainActivity.kt
│   │   ├── TextTranslationFragment.kt
│   │   ├── ImageTranslationFragment.kt
│   │   ├── LanguageLibraryActivity.kt
│   │   └── StatsActivity.kt
│   └── ...
└── res/
    ├── layout/
    ├── drawable/
    └── values/
```

## 🚀 Como Usar

### 1. Baixar Idiomas
1. Abra o app
2. Toque em "Biblioteca"
3. Baixe os idiomas desejados (ex: English + Português)

### 2. Traduzir Texto
1. Vá para aba "Texto"
2. Selecione os idiomas
3. Digite o texto
4. Toque em "TRADUZIR"
5. Avalie a tradução (opcional)

### 3. Traduzir Imagem
1. Vá para aba "Imagem"
2. Aponte a câmera para o texto
3. Toque no botão de captura
4. Veja a tradução
5. Avalie a tradução (opcional)

### 4. Ver Estatísticas
1. Toque no ícone de gráficos no menu
2. Veja sua taxa de acerto
3. Distribuição das avaliações
4. Total de traduções por tipo

## 📈 Dados Coletados

O app coleta localmente:
- Total de traduções (texto/imagem)
- Avaliações de qualidade (1-5 estrelas)
- Idiomas utilizados
- Data/hora das traduções

**Privacidade:** Todos os dados são armazenados apenas no seu dispositivo.

## 🎯 Versão Mínima

- **Min SDK:** 21 (Android 5.0)
- **Target SDK:** 34 (Android 14)

## 📄 Licença

MIT License

---

Desenvolvido com ❤️ usando Google ML Kit