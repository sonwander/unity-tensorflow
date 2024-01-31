import csv
from random import randint, uniform
from datetime import datetime, timedelta

# Função para gerar dados fictícios
def generate_data(num_samples):
    base_date = datetime(2023, 1, 1)
    data = [["data", "passos_ontem", "media_passos_7dias", "calorias_consumidas", "horas_de_sono", "steps_next_day"]]
    
    for _ in range(num_samples):
        # Gerar dados aleatórios para passos, calorias e sono
        passos_ontem = randint(3000, 10000)
        media_passos_7dias = randint(3000, 10000)
        calorias_consumidas = randint(1500, 3000)
        horas_de_sono = round(uniform(5.0, 9.0), 1)
        steps_next_day = randint(3000, 10000)
        
        # Adicionar uma nova linha de dados
        data.append([
            (base_date + timedelta(days=_)).strftime('%Y-%m-%d'),
            passos_ontem,
            media_passos_7dias,
            calorias_consumidas,
            horas_de_sono,
            steps_next_day
        ])
    
    return data

# Número de amostras desejado
num_samples = 1000
dados = generate_data(num_samples)

# Caminho onde o arquivo CSV será salvo
caminho_csv = 'dados_passos.csv'

# Escrever os dados no arquivo CSV
with open(caminho_csv, 'w', newline='') as file:
    writer = csv.writer(file)
    writer.writerows(dados)

print(f"{num_samples} amostras de dados foram geradas e salvas em '{caminho_csv}'.")