import pandas as pd

# Carregando seu arquivo CSV
df = pd.read_csv('dados_passos.csv')

# Calculando os valores mínimos e máximos para cada característica
min_steps_ontem = df['passos_ontem'].min()
max_steps_ontem = df['passos_ontem'].max()

min_media_passos_7dias = df['media_passos_7dias'].min()
max_media_passos_7dias = df['media_passos_7dias'].max()

min_calorias_consumidas = df['calorias_consumidas'].min()
max_calorias_consumidas = df['calorias_consumidas'].max()

min_horas_de_sono = df['horas_de_sono'].min()
max_horas_de_sono = df['horas_de_sono'].max()

# Imprimindo os valores mínimos e máximos
print(f'MIN_STEPS_ONTEM: {min_steps_ontem}')
print(f'MAX_STEPS_ONTEM: {max_steps_ontem}')
print(f'MIN_MEDIA_PASSOS_7DIAS: {min_media_passos_7dias}')
print(f'MAX_MEDIA_PASSOS_7DIAS: {max_media_passos_7dias}')
print(f'MIN_CALORIAS_CONSUMIDAS: {min_calorias_consumidas}')
print(f'MAX_CALORIAS_CONSUMIDAS: {max_calorias_consumidas}')
print(f'MIN_HORAS_DE_SONO: {min_horas_de_sono}')
print(f'MAX_HORAS_DE_SONO: {max_horas_de_sono}')