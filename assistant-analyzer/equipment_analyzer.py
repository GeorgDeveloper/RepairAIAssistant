#!/usr/bin/env python3
"""
Оптимизированный анализатор оборудования
"""

import csv
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Dict, List, Tuple, Optional

class EquipmentAnalyzer:
    """Анализатор данных оборудования"""
    
    PROBLEM_PATTERN = re.compile(r'Что Произошло:\s*([^;]+)')
    SOLUTION_PATTERN = re.compile(r'Что ты сделал:\s*([^;]+)')
    
    def __init__(self, csv_file_path: str):
        self.csv_file_path = Path(csv_file_path)
        self.repair_instructions = []
    
    def extract_problem_solution(self, comments: str) -> Tuple[str, str]:
        """Извлекает проблему и решение из комментариев"""
        if not comments:
            return "", ""
        
        problem_match = self.PROBLEM_PATTERN.search(comments)
        solution_match = self.SOLUTION_PATTERN.search(comments)
        
        problem = problem_match.group(1).strip() if problem_match else ""
        solution = solution_match.group(1).strip() if solution_match else ""
        
        return problem, solution
    
    def analyze(self) -> List[Dict]:
        """Анализирует CSV файл и создает инструкции по ремонту"""
        grouped_data = defaultdict(lambda: defaultdict(lambda: defaultdict(list)))
        
        try:
            with open(self.csv_file_path, 'r', encoding='utf-8') as csvfile:
                reader = csv.DictReader(csvfile)
                
                for row in reader:
                    area = row.get('area', 'Unknown')
                    machine_name = row.get('machine_name', 'Unknown')
                    mechanism_node = row.get('mechanism_node', 'Unknown')
                    comments = row.get('comments', '')
                    
                    problem, solution = self.extract_problem_solution(comments)
                    
                    if problem and solution:
                        grouped_data[area][machine_name][mechanism_node].append({
                            'problem': problem,
                            'solution': solution
                        })
        
        except FileNotFoundError:
            raise FileNotFoundError(f"Файл не найден: {self.csv_file_path}")
        except Exception as e:
            raise Exception(f"Ошибка чтения файла: {e}")
        
        return self._create_instructions(grouped_data)
    
    def _create_instructions(self, grouped_data) -> List[Dict]:
        """Создает инструкции по ремонту из сгруппированных данных"""
        instructions = []
        
        for area, machines in grouped_data.items():
            for machine, nodes in machines.items():
                for node, repairs in nodes.items():
                    problem_groups = defaultdict(set)
                    
                    for repair in repairs:
                        problem_groups[repair['problem']].add(repair['solution'])
                    
                    for problem, solutions in problem_groups.items():
                        instructions.append({
                            "area": area,
                            "equipment_group": machine,
                            "component": node,
                            "problem": problem,
                            "solution": "; ".join(solutions)
                        })
        
        return instructions
    
    def save_to_json(self, instructions: List[Dict], output_path: str) -> None:
        """Сохраняет инструкции в JSON файл"""
        with open(output_path, 'w', encoding='utf-8') as f:
            json.dump(instructions, f, ensure_ascii=False, indent=2)

def main():
    """Основная функция для запуска анализа"""
    csv_file = "assistant-core/src/main/resources/equipment_maintenance_records_202508150901.csv"
    output_file = "repair_instructions.json"
    
    try:
        analyzer = EquipmentAnalyzer(csv_file)
        instructions = analyzer.analyze()
        analyzer.save_to_json(instructions, output_file)
        
        print(f"Анализ завершен. Создано {len(instructions)} инструкций.")
        print(f"Результат сохранен в: {output_file}")
        
    except Exception as e:
        print(f"Ошибка: {e}")

if __name__ == "__main__":
    main()