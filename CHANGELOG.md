# Changelog

Todas as mudançasnotáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [1.0.0] - 2024-01-15

### ✅ Adicionado

#### Tradução
- Tradução de texto digitado
- Tradução de imagem (câmera)
- Seleção de idiomas de origem e destino
- Troca rápida de idiomas

#### Biblioteca de Idiomas
- Download de idiomas para uso offline
- Gerenciamento de idiomas baixados
- Suporte a múltiplos pares de idiomas (en↔pt)

#### Interface
- Tema escuro moderno
- Abas para texto e imagem
- Seleção de área de tradução
- Preview da câmera

#### Recursos Técnicos
- 100% offline (sem internet após download dos idiomas)
- NDK/JNI para engine C de tradução
- CameraX para captura de imagem
- ML Kit para OCR e tradução
- ViewBinding para performance

### 📱 Requisitos

- Android 5.0+ (API 21)
- ~100MB de armazenamento para idiomas
- Câmera para tradução de imagem

---

## [0.1.0] - 2024-01-10

### 🧪 Adicionado

- Projeto inicial com estrutura MVP
- Interface básica de tradução
- Engine C de tradução
- Layouts iniciais

---

## Modelos de Release

| Tipo | Descrição | Quando usar |
|------|-----------|-------------|
| **major** | Mudanças incompatíveis | Rewrites, mudanças de API |
| **minor** | Funcionalidades novas | Novas features |
| **patch** | Correções de bugs | Bug fixes |

## Formato de Commits

Este projeto usa commits convencionais:

```
feat: nova funcionalidade
fix: correção de bug
docs: documentação
style: formatação
refactor: refatoração
test: testes
chore: manutenção
```

---

## Links Úteis

- [Releases](https://github.com/whesley264-oss/offline-camera-translator/releases)
- [Issues](https://github.com/whesley264-oss/offline-camera-translator/issues)
- [Wiki](https://github.com/whesley264-oss/offline-camera-translator/wiki)