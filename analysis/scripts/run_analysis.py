#!/usr/bin/env python3
"""
Script de Automação para Análise de Tradução
============================================

Este script automatiza o processo de:
1. Carregar dataset de teste
2. Executar tradução via app (simulado)
3. Calcular métricas de qualidade
4. Gerar relatório
5. Atualizar arquivo de estatísticas

Uso:
    python run_analysis.py --dataset datasets/test_dataset.json
"""

import json
import argparse
import sys
from datetime import datetime
from pathlib import Path

# Adicionar parent dir ao path
sys.path.insert(0, str(Path(__file__).parent.parent))


def load_dataset(path):
    """Carrega dataset de teste"""
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)


def calculate_metrics(predicted, expected):
    """Calcula métricas de qualidade"""
    import re
    from difflib import SequenceMatcher
    
    def normalize(text):
        text = text.lower().strip()
        text = re.sub(r'\s+', ' ', text)
        return text
    
    # Exact Match
    exact = normalize(predicted) == normalize(expected)
    
    # Similaridade
    similarity = SequenceMatcher(None, normalize(predicted), normalize(expected)).ratio()
    
    # WER simplificado
    pred_words = normalize(predicted).split()
    exp_words = normalize(expected).split()
    if len(exp_words) == 0:
        wer = 0.0 if len(pred_words) == 0 else 1.0
    else:
        matcher = SequenceMatcher(None, exp_words, pred_words)
        matches = sum(tag == 'equal' for tag in matcher.get_opcodes())
        wer = (len(exp_words) - matches) / len(exp_words)
    
    # Aproximação BLEU
    def get_ngrams(words, n):
        return set(tuple(words[i:i+n]) for i in range(len(words)-n+1))
    
    pred_ngrams = get_ngrams(pred_words, 2)
    exp_ngrams = get_ngrams(exp_words, 2)
    
    if len(pred_ngrams) == 0 or len(exp_ngrams) == 0:
        bleu = 0.0
    else:
        matches = len(pred_ngrams & exp_ngrams)
        precision = matches / len(pred_ngrams)
        brevity = min(1.0, len(pred_words) / max(1, len(exp_words)))
        bleu = precision * brevity
    
    return {
        'exact_match': exact,
        'similarity': similarity,
        'wer': wer,
        'bleu': bleu
    }


def analyze_translations(dataset, results_callback=None):
    """Analisa todas as traduções do dataset"""
    results = []
    
    for sample in dataset['samples']:
        # Aqui você integraria com o app real
        # Por enquanto, simulamos
        predicted = sample['expected_translation']  # Placeholder
        
        metrics = calculate_metrics(predicted, sample['expected_translation'])
        
        result = {
            'id': sample['id'],
            'source': sample['source_text'],
            'expected': sample['expected_translation'],
            'predicted': predicted,
            'category': sample['category'],
            'pair': f"{sample['source_lang']}→{sample['target_lang']}",
            **metrics
        }
        
        results.append(result)
        
        if results_callback:
            results_callback(result)
    
    return results


def generate_summary(results):
    """Gera resumo das análises"""
    import numpy as np
    
    df = pd.DataFrame(results)
    
    summary = {
        'generated_at': datetime.now().isoformat(),
        'total_samples': len(results),
        'exact_match_rate': round(df['exact_match'].mean() * 100, 2),
        'avg_similarity': round(df['similarity'].mean() * 100, 2),
        'avg_bleu': round(df['bleu'].mean() * 100, 2),
        'avg_wer': round(df['wer'].mean() * 100, 2),
        'by_category': {},
        'by_language_pair': {}
    }
    
    # Por categoria
    for cat in df['category'].unique():
        cat_df = df[df['category'] == cat]
        summary['by_category'][cat] = {
            'count': len(cat_df),
            'exact_match_rate': round(cat_df['exact_match'].mean() * 100, 2),
            'avg_similarity': round(cat_df['similarity'].mean() * 100, 2),
            'avg_bleu': round(cat_df['bleu'].mean() * 100, 2)
        }
    
    # Por par de idiomas
    for pair in df['pair'].unique():
        pair_df = df[df['pair'] == pair]
        summary['by_language_pair'][pair] = {
            'count': len(pair_df),
            'exact_match_rate': round(pair_df['exact_match'].mean() * 100, 2),
            'avg_similarity': round(pair_df['similarity'].mean() * 100, 2)
        }
    
    return summary


def print_results(results, summary):
    """Imprime resultados formatados"""
    print("\n" + "=" * 60)
    print("📊 RESULTADOS DA ANÁLISE DE TRADUÇÃO")
    print("=" * 60)
    
    print(f"\n📅 Data: {summary['generated_at']}")
    print(f"📝 Total de amostras: {summary['total_samples']}")
    
    print(f"\n┌────────────────────────────────────────────────────┐")
    print(f"│  🎯 TAXA DE ACERTO:        {summary['exact_match_rate']:>6.1f}%           │")
    print(f"│  📈 SIMILARIDADE MÉDIA:   {summary['avg_similarity']:>6.1f}%           │")
    print(f"│  📊 BLEU SCORE:           {summary['avg_bleu']:>6.1f}%           │")
    print(f"└────────────────────────────────────────────────────┘")
    
    print("\n📋 Por Categoria:")
    for cat, stats in summary['by_category'].items():
        print(f"   {cat:15} → Acerto: {stats['exact_match_rate']:5.1f}% | BLEU: {stats['avg_bleu']:5.1f}%")
    
    print("\n🔤 Por Par de Idiomas:")
    for pair, stats in summary['by_language_pair'].items():
        print(f"   {pair:10} → Acerto: {stats['exact_match_rate']:5.1f}% | Similaridade: {stats['avg_similarity']:5.1f}%")


def save_results(results, summary, output_dir):
    """Salva resultados em arquivos"""
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Salvar CSV
    import pandas as pd
    df = pd.DataFrame(results)
    csv_path = output_dir / 'analysis_results.csv'
    df.to_csv(csv_path, index=False, encoding='utf-8')
    print(f"\n💾 CSV salvo em: {csv_path}")
    
    # Salvar JSON
    json_path = output_dir / 'analysis_summary.json'
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(summary, f, indent=2, ensure_ascii=False)
    print(f"💾 JSON salvo em: {json_path}")
    
    return csv_path, json_path


def main():
    parser = argparse.ArgumentParser(description='Análise de Qualidade de Tradução')
    parser.add_argument('--dataset', '-d', 
                       default='datasets/test_dataset.json',
                       help='Caminho para o dataset JSON')
    parser.add_argument('--output', '-o',
                       default='results',
                       help='Diretório de saída')
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Modo verboso')
    
    args = parser.parse_args()
    
    print(f"📂 Carregando dataset: {args.dataset}")
    dataset = load_dataset(args.dataset)
    print(f"✅ Dataset carregado: {dataset['total_samples']} amostras")
    
    print("\n🔄 Executando análise...")
    results = analyze_translations(dataset)
    
    summary = generate_summary(results)
    print_results(results, summary)
    
    csv_path, json_path = save_results(results, summary, args.output)
    
    print("\n✅ Análise concluída!")


if __name__ == '__main__':
    import pandas as pd  # Importar aqui para não quebrar se não instalado
    main()
