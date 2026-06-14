#!/usr/bin/env python3
"""
Collect App Translations
=======================

Este script coleta traduções do app (via logs, CSV export, etc.)
e prepara para análise.

Instruções:
1. Exporte as traduções do app para um arquivo JSON/CSV
2. Execute este script para processar os dados
3. Os dados serão salvos em datasets/user_translations.json
"""

import json
import csv
from datetime import datetime
from pathlib import Path


def load_from_json(filepath):
    """Carrega traduções de arquivo JSON"""
    with open(filepath, 'r', encoding='utf-8') as f:
        return json.load(f)


def load_from_csv(filepath):
    """Carrega traduções de arquivo CSV"""
    translations = []
    with open(filepath, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            translations.append({
                'timestamp': row.get('timestamp', datetime.now().isoformat()),
                'source_text': row['source_text'],
                'translated_text': row['translated_text'],
                'source_lang': row.get('source_lang', 'en'),
                'target_lang': row.get('target_lang', 'pt'),
                'type': row.get('type', 'text'),
                'rating': int(row['rating']) if row.get('rating') else None
            })
    return translations


def merge_with_ground_truth(translations, ground_truth_path):
    """Adiciona gabarito para comparação"""
    with open(ground_truth_path, 'r', encoding='utf-8') as f:
        gt_data = json.load(f)
    
    # Criar lookup por texto fonte
    gt_lookup = {}
    for sample in gt_data['samples']:
        key = sample['source_text'].lower().strip()
        gt_lookup[key] = sample['expected_translation']
    
    # Adicionar expected aos resultados
    for trans in translations:
        key = trans['source_text'].lower().strip()
        if key in gt_lookup:
            trans['expected'] = gt_lookup[key]
            trans['has_ground_truth'] = True
        else:
            trans['expected'] = None
            trans['has_ground_truth'] = False
    
    return translations


def calculate_immediate_accuracy(translations):
    """Calcula precisão baseado em ratings dos usuários"""
    rated = [t for t in translations if t.get('rating') is not None]
    
    if not rated:
        return 0.0, 0
    
    # Rating >= 4 = "bom" ou "excelente"
    good_ratings = [t for t in rated if t['rating'] >= 4]
    
    return (len(good_ratings) / len(rated)) * 100, len(rated)


def generate_stats_report(translations):
    """Gera relatório de estatísticas"""
    total = len(translations)
    rated = [t for t in translations if t.get('rating') is not None]
    
    # Por tipo
    text_count = len([t for t in translations if t.get('type') == 'text'])
    image_count = len([t for t in translations if t.get('type') == 'image'])
    
    # Por par de idiomas
    lang_pairs = {}
    for t in translations:
        pair = f"{t.get('source_lang', '?')}→{t.get('target_lang', '?')}"
        lang_pairs[pair] = lang_pairs.get(pair, 0) + 1
    
    # Por rating
    rating_dist = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
    for t in rated:
        rating_dist[t['rating']] = rating_dist.get(t['rating'], 0) + 1
    
    # Calcular métricas
    success_rate, rated_count = calculate_immediate_accuracy(translations)
    
    # Calcular nota média
    avg_rating = sum(t['rating'] for t in rated) / len(rated) if rated else 0.0
    
    report = {
        'generated_at': datetime.now().isoformat(),
        'total_translations': total,
        'rated_translations': rated_count,
        'success_rate': round(success_rate, 2),
        'average_rating': round(avg_rating, 2),
        'rating_distribution': rating_dist,
        'by_type': {
            'text': text_count,
            'image': image_count
        },
        'by_language_pair': lang_pairs,
        'translations_with_ground_truth': len([t for t in translations if t.get('has_ground_truth')])
    }
    
    return report


def save_collected_data(translations, output_path):
    """Salva dados coletados"""
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump({
            'collected_at': datetime.now().isoformat(),
            'total': len(translations),
            'translations': translations
        }, f, indent=2, ensure_ascii=False)
    
    print(f"💾 Dados salvos em: {output_path}")


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Coleta traduções do app')
    parser.add_argument('--input', '-i', required=True, help='Arquivo de entrada (JSON ou CSV)')
    parser.add_argument('--ground-truth', '-g', help='Arquivo de gabarito (opcional)')
    parser.add_argument('--output', '-o', default='datasets/user_translations.json', help='Arquivo de saída')
    
    args = parser.parse_args()
    
    # Detectar formato
    if args.input.endswith('.csv'):
        translations = load_from_csv(args.input)
    else:
        translations = load_from_json(args.input)
    
    print(f"📂 Carregadas {len(translations)} traduções")
    
    # Adicionar ground truth se fornecido
    if args.ground_truth:
        translations = merge_with_ground_truth(translations, args.ground_truth)
        gt_count = len([t for t in translations if t.get('has_ground_truth')])
        print(f"✅ {gt_count} traduções têm gabarito")
    
    # Gerar relatório
    report = generate_stats_report(translations)
    
    print("\n📊 RELATÓRIO:")
    print(f"   Total de traduções: {report['total_translations']}")
    print(f"   Traduções avaliadas: {report['rated_translations']}")
    print(f"   Taxa de sucesso: {report['success_rate']}%")
    print(f"   Nota média: {report['average_rating']}/5")
    
    print("\n📋 Distribuição de Ratings:")
    for rating, count in report['rating_distribution'].items():
        if count > 0:
            print(f"   {'⭐' * rating}: {count}")
    
    # Salvar
    save_collected_data(translations, args.output)
    
    return report


if __name__ == '__main__':
    main()
