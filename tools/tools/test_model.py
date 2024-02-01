# test_model.py
import numpy as np
import pandas as pd
import tensorflow as tf
import pickle

# Carregar o StandardScaler salvo
with open('scaler.pkl', 'rb') as f:
    scaler = pickle.load(f)

# Carregar o modelo TensorFlow Lite
interpreter = tf.lite.Interpreter(model_path='steps_model.tflite')
interpreter.allocate_tensors()

# Obter detalhes dos tensores de entrada e saída
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

# Carregar dados de teste
# Substitua 'dados_passos_test.csv' pelo caminho real do seu arquivo CSV de testes
test_data = pd.read_csv('dados_passos.csv')

# Converter a coluna 'data' para um formato numérico
test_data['data'] = pd.to_datetime(test_data['data'])

# Criar colunas numéricas a partir da coluna 'data'
test_data['day_of_year'] = test_data['data'].dt.dayofyear
test_data['day_of_week'] = test_data['data'].dt.dayofweek

# Descartar a coluna 'data' original
test_data.drop('data', axis=1, inplace=True)

# Preparar os dados de teste
feature_cols = ['passos_ontem', 'media_passos_7dias', 'calorias_consumidas', 'horas_de_sono', 'day_of_year', 'day_of_week']
X_test = test_data[feature_cols].values

# Normalização dos dados de teste
X_test_scaled = scaler.transform(X_test)

# Fazer previsões com os dados de teste
predictions = []
for instance in X_test_scaled:
    # Redimensionar os dados para a entrada do modelo (adicionando uma dimensão extra)
    instance = np.expand_dims(instance, axis=0).astype(np.float32)
    # Fazer a previsão com o modelo TFLite
    interpreter.set_tensor(input_details[0]['index'], instance)
    interpreter.invoke()
    prediction = interpreter.get_tensor(output_details[0]['index'])
    predictions.append(prediction[0])

# Mostrar as previsões
for i, prediction in enumerate(predictions):
    print(f"Previsão para a instância {i}: {prediction}")

# Se necessário, também pode salvar as previsões em um arquivo CSV
# pd.DataFrame(predictions, columns=['predicted_steps_next_day']).to_csv('predictions.csv', index=False)