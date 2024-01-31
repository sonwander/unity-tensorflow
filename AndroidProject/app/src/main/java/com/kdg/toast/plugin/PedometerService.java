package com.kdg.toast.plugin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class PedometerService extends Service implements SensorEventListener {
    private StepUpdateReceiver stepUpdateReceiver;
    private final Handler handler = new Handler();
    private Runnable runnableCode;
    private SharedPreferences sharedPreferences;
    private String TAG = "PEDOMETER";
    private SensorManager sensorManager;
    private boolean running;
    private Interpreter tflite;
    private static final String MODEL_FILENAME = "steps_model.tflite";
    private static final String CHANNEL_ID = "PedometerLib";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_STEPS_UPDATED = "com.kdg.toast.plugin.ACTION_STEPS_UPDATED";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "PedometerService created");
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Bridge.appContext = getApplicationContext();
        loadData();
        initSensorManager();
        try {
            tflite = new Interpreter(loadModelFile(MODEL_FILENAME));
            startNotification("Model 'steps_model.tflite' loaded successfully.");

        } catch (IOException e) {
            Log.e(TAG, "Erro ao carregar o modelo TensorFlow Lite.", e);
        }
    
        // Registrar o BroadcastReceiver
        stepUpdateReceiver = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter(ACTION_STEPS_UPDATED);
        registerReceiver(stepUpdateReceiver, filter);
    }
    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Debug.Log("onStartCommand: Iniciando o serviço...");

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        Debug.Log("onStartCommand: Canal de notificação criado e serviço em primeiro plano iniciado.");

        // Remova a tentativa de carregar o modelo aqui, pois já foi carregado no onCreate

        // Define o código que será executado periodicamente
        runnableCode = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Periodic task executing...");

                // Execute a inferência com seus dados
                float[] inputFeatures = preprocessData(Bridge.steps, caloriesConsumed, hoursOfSleep);
                float[] results = runInference(inputFeatures);

                // Log antes da atualização da notificação
                Debug.Log("runnableCode: Antes da atualização da notificação.");

                // Atualiza a notificação com o resultado da inferência
                String inferenceResult = "Inferência concluída: " + results[0];
                updateNotificationWithMessage(inferenceResult);

                // Log após a atualização da notificação
                Debug.Log("runnableCode: Após a atualização da notificação.");

                // Repete este mesmo runnable code a cada 10000 milissegundos (10 segundos)
                handler.postDelayed(this, 10000);
            }
        };

        // Inicia o processo de atualização periódica
        handler.post(runnableCode);

        // Chame o método de inferência imediatamente ao iniciar o serviço
        inferAndNotify();

        Debug.Log("onStartCommand: Serviço inicializado e em execução.");
        return START_STICKY;
    }

    private void startNotification(String notificationText) {
        Intent notificationIntent = new Intent(this, Bridge.myActivity.getClass());
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
    
        Notification notification = new NotificationCompat.Builder(this, "PedometerLib")
                .setContentTitle("Background Service Running")
                .setContentText(notificationText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
    
        startForeground(112, notification);
    }
    

    private void inferAndNotify() {
        Debug.Log("inferAndNotify: Iniciando inferência...");

        float[] inputFeatures = preprocessData(Bridge.steps, caloriesConsumed, hoursOfSleep);
        float[] results = runInference(inputFeatures);

        // Adicione logs para depuração
        Debug.Log("inferAndNotify: Resultado da inferência inicial: " + results[0]);

        // Atualiza a notificação com o resultado da inferência
        updateNotificationWithMessage("Inferência inicial concluída: " + results[0]);

        Debug.Log("inferAndNotify: Inferência concluída e notificação atualizada.");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "PedometerService destroyed");

        // Cancelar o registro do BroadcastReceiver
        if (stepUpdateReceiver != null) {
            unregisterReceiver(stepUpdateReceiver);
            stepUpdateReceiver = null;
        }

        // Parando o Handler que atualiza a notificação
        handler.removeCallbacks(runnableCode);

        // Seu código existente para limpeza
        disposeSensorManager();
        if (tflite != null) {
            tflite.close();
        }
        saveSummarySteps(Bridge.summarySteps + Bridge.steps);

        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            try {
                int totalSteps = (int) event.values[0];
                // Assumindo que você tenha métodos para obter as calorias consumidas e horas de sono
                float caloriesConsumed = getCaloriesConsumed();
                float hoursOfSleep = getHoursOfSleep();
    
                Bridge.steps = totalSteps - Bridge.initialSteps;
                saveSummarySteps(Bridge.summarySteps + Bridge.steps);
    
                // Prepara os dados para a inferência
                float[] inputFeatures = preprocessData(Bridge.steps, caloriesConsumed, hoursOfSleep);
    
                // Executa a inferência do TensorFlow Lite
                float[] results = runInference(inputFeatures);
    
                // Formata o resultado da inferência
                String inferenceResult = "Resultado: " + results[0];
    
                // Atualiza a notificação com o resultado da inferência
                updateNotificationWithMessage(inferenceResult);
    
                // Envio de broadcast para atualizar a interface do usuário no Unity
                Intent intent = new Intent(ACTION_STEPS_UPDATED);
                intent.putExtra("stepsCount", Bridge.summarySteps + Bridge.steps);
                sendBroadcast(intent);
            } catch (Exception e) {
                handleInferenceError(e);
            }
        }
    }
    
    private float[] preprocessData(int steps, float caloriesConsumed, float hoursOfSleep) {
        // Substitua estes valores com os mínimos e máximos providenciados pelo script Python
        final int MIN_STEPS = 3002;
        final int MAX_STEPS = 9998;
        final int MIN_CALORIES = 1500;
        final int MAX_CALORIES = 2999;
        final float MIN_SLEEP = 5.0f;
        final float MAX_SLEEP = 9.0f;
    
        float normalizedSteps = (steps - MIN_STEPS) / (float) (MAX_STEPS - MIN_STEPS);
        float normalizedCalories = (caloriesConsumed - MIN_CALORIES) / (float) (MAX_CALORIES - MIN_CALORIES);
        float normalizedSleep = (hoursOfSleep - MIN_SLEEP) / (MAX_SLEEP - MIN_SLEEP);
    
        return new float[] { normalizedSteps, normalizedCalories, normalizedSleep };
    }
    
    private float[] preprocessData(int steps) {
        // Substitua este código pelo seu próprio pré-processamento
        // Este é apenas um exemplo de normalização
        float mean = 5000;
        float std = 2000;
        return new float[]{(steps - mean) / std};
    }
    
    private float[] runInference(float[] input) {
        Log.d(TAG, "Executing TensorFlow Lite inference...");
    
        float[] output = new float[1];
        try {
            tflite.run(input, output);
            Log.d(TAG, "Model inference successful");
    
            String successMessage = "Model learned successfully";
            updateNotificationWithMessage(successMessage);
        } catch (Exception e) {
            handleInferenceError(e);
        }
    
        return output;
    }
    
    private void handleInferenceError(Exception e) {
        Log.e(TAG, "Error executing model inference.", e);
    
        // Adicione mais informações ao log, se necessário
        Log.e(TAG, "Error details: " + e.getMessage());
    
        // Registre a exceção em um arquivo ou outro meio, se necessário
    }
    
    

    private void updateNotificationWithMessage(String message) {
        // Cria uma nova notificação com a mensagem passada como argumento
        Notification notification = createNotificationWithMessage(message);
        
        // Obtém o NotificationManager do sistema
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Atualiza a notificação com o ID especificado
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    private Notification createNotificationWithMessage(String message) {
        // Cria uma intenção pendente que será acionada quando o usuário tocar na notificação
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        
        // Constrói a notificação com a mensagem de resultado da inferência
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pedometer Service")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_walk)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Opção para adicionar som, luzes ou vibração aqui, conforme necessário
        
        // Retorna a notificação construída
        return builder.build();
    }
    
    private float[] normalize(float[] input) {
        // Implemente a normalização conforme necessário para o seu modelo
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (input[i] - 0) / (1 - 0); // Exemplo de normalização para [0,1]
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Aqui, você pode implementar a lógica de mudança de acurácia se necessário.
    }

   // Em vez de carregar o modelo TensorFlow Lite, carregue o arquivo de texto
   private MappedByteBuffer loadModelFile(String filename) throws IOException {
    AssetFileDescriptor fileDescriptor = this.getAssets().openFd("file:///android_asset/" + filename);

    // Verifique se o arquivo é o modelo ou o arquivo de mensagem
    if (filename.equals("steps_model.tflite")) {
        // Carregue o modelo normalmente
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    } else if (filename.equals("message.txt")) {
        // Carregue o arquivo de texto
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);

        // Leia o conteúdo do arquivo de texto
        byte[] content = new byte[(int) declaredLength];
        buffer.get(content);

        // Exiba a mensagem
        Log.d(TAG, "Conteúdo do arquivo de texto: " + new String(content));

        return null; // Retorno nulo porque não estamos carregando o modelo
    }

    return null; // Retorno nulo se o arquivo não for reconhecido
}



    

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Service Channel",
                    NotificationManager.IMPORTANCE_HIGH // Mudança da importância para alta
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification() {
        Notification notification = createNotification(); // Chama o método que cria a notificação com os dados atuais
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    
        String contentText = "Contando passos: " + (Bridge.summarySteps + Bridge.steps);
    
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pedometer Service")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_walk)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void initSensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
            running = true;
        } else {
            Toast.makeText(this, "Sensor não encontrado.", Toast.LENGTH_LONG).show();
        }
    }

    private void disposeSensorManager() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            running = false;
        }
    }

    private void loadData() {
        Bridge.summarySteps = sharedPreferences.getInt(Bridge.SUMMARY_STEPS, 0);
        Bridge.initialSteps = sharedPreferences.getInt(Bridge.STEPS, 0);
    }

    private void saveSummarySteps(int steps) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Bridge.SUMMARY_STEPS, steps);
        editor.apply();
    }

    public static class StepUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.kdg.toast.plugin.ACTION_STEPS_UPDATED".equals(intent.getAction())) {
                int stepsCount = intent.getIntExtra("stepsCount", 0);
                UnityPlayer.UnitySendMessage("AndroidMessageReceiver", "OnStepsUpdated", String.valueOf(stepsCount));
            }
        }
    }

    public static class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();

                if (action.equals("com.kdg.toast.plugin.ACTION_STEPS_UPDATED")) {
                    // Aqui você pode adicionar o código para lidar com a ação específica
                    int stepsCount = intent.getIntExtra("stepsCount", 0);
                    Log.d("MyBroadcastReceiver", "Recebeu ação ACTION_STEPS_UPDATED. Contagem de passos: " + stepsCount);
                }
            }
        }
    }
}