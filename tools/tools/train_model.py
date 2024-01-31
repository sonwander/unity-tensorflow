# train_model.py
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import pickle

# Carregar dados do CSV
data = pd.read_csv('dados_passos.csv')  # Substitua com o caminho do arquivo CSV

# Converter a coluna 'data' para um formato numérico
data['data'] = pd.to_datetime(data['data'])

# Criar colunas numéricas a partir da coluna 'data'
data['day_of_year'] = data['data'].dt.dayofyear
data['day_of_week'] = data['data'].dt.dayofweek

# Agora vamos descartar a coluna 'data' original, pois não é mais necessária
data.drop('data', axis=1, inplace=True)

# Preparar os dados
feature_cols = ['passos_ontem', 'media_passos_7dias', 'calorias_consumidas', 'horas_de_sono', 'day_of_year', 'day_of_week']
X = data[feature_cols].values
y = data['steps_next_day'].values

# Divisão dos dados
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# Normalização
scaler = StandardScaler().fit(X_train)
X_train_scaled = scaler.transform(X_train)
X_test_scaled = scaler.transform(X_test)

# Definição do modelo
model = tf.keras.Sequential([
    tf.keras.layers.Dense(64, activation='relu', input_shape=(X_train_scaled.shape[1],)),
    tf.keras.layers.Dense(64, activation='relu'),
    tf.keras.layers.Dense(1)
])

# Compilação do modelo
model.compile(optimizer='adam', loss='mse')

# Treinamento do modelo
model.fit(X_train_scaled, y_train, validation_split=0.2, epochs=100)

# Avaliação do modelo
test_loss = model.evaluate(X_test_scaled, y_test)
print(f"Test MSE: {test_loss}")

# Conversão para TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Salvar o modelo TFLite
tflite_model_name = 'steps_model.tflite'
with open(tflite_model_name, 'wb') as f:
    f.write(tflite_model)

# Salvar o StandardScaler como um arquivo .pkl
scaler_filename = 'scaler.pkl'
with open(scaler_filename, 'wb') as f:
    pickle.dump(scaler, f)

print(f"Modelo TFLite e Scaler foram salvos: {tflite_model_name}, {scaler_filename}")