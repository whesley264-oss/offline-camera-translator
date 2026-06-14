# Offline Camera Translator 📱

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Build](https://img.shields.io/github/actions/workflow/status/whesley264-oss/offline-camera-translator/build_release.yml)
[![Release](https://img.shields.io/github/v/release/whesley264-oss/offline-camera-translator?include_prereleases)](https://github.com/whesley264-oss/offline-camera-translator/releases/latest)

**Tradutor offline com câmera para Android**

Traduza textos de imagens usando a câmera do celular ou digitando diretamente, **sem necessidade de internet** após baixar os idiomas.

---

## ⬇️ Download

| Versão | Tipo | Download |
|--------|------|----------|
| **v1.0.0** | Stable | [Download APK](https://github.com/whesley264-oss/offline-camera-translator/releases/latest) |
| Beta | Testes | [Download Beta](https://github.com/whesley264-oss/offline-camera-translator/releases) |

### 📥 Como Instalar

1. Baixe o APK da [última release](https://github.com/whesley264-oss/offline-camera-translator/releases/latest)
2. No celular, vá em **Configurações > Segurança**
3. Ative **"Fontes desconhecidas"**
4. Abra o arquivo `.apk` baixado
5. Toque em **Instalar**

---

## 📱 Funcionalidades

### 🔤 Tradução de Texto
- Digite textos para traduzir
- Seleção de idiomas de origem e destino
- Troca rápida de idiomas com um toque

### 📷 Tradução de Imagem
- Tire fotos de textos para traduzir
- Seleção de área específica
- OCR automático com ML Kit

### 📚 Biblioteca de Idiomas
- Baixe idiomas para uso offline
- Gerencie idiomas baixados
- Idiomas: 🇺🇸 English ↔ 🇧🇷 Português

---

## 🛠️ Tecnologias

| Tecnologia | Uso |
|------------|-----|
| **Kotlin** | Linguagem principal |
| **CameraX** | Captura de câmera |
| **ML Kit** | Tradução offline + OCR |
| **NDK/JNI** | Engine C nativa |

---

## 🔧 Como Compilar

```bash
git clone https://github.com/whesley264-oss/offline-camera-translator.git
cd offline-camera-translator
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/`

---

## 📄 Estrutura

```
app/src/main/
├── java/com/offline/translator/
│   ├── model/          # Lógica de negócio
│   ├── view/           # UI (Activities/Fragments)
│   └── presenter/      # Apresentação
├── res/                # Layouts, drawables, valores
└── cpp/                # Engine C nativa
```

---

## 🤝 Contribuir

Veja [CONTRIBUTING.md](CONTRIBUTING.md) para guidelines.

---

## 📝 Changelog

Veja [CHANGELOG.md](CHANGELOG.md) para histórico.

---

## ❓ FAQ

**P: Funciona offline?**
R: Sim! Após baixar os idiomas.

**P: Quanto espaço?**
R: ~100MB por par de idiomas.

**P: Idiomas?**
R: English ↔ Português.

---

## 📜 Licença

MIT License - veja [LICENSE](LICENSE)

---

<p align="center">
  Feito com ❤️ para traduções offline
</p>
