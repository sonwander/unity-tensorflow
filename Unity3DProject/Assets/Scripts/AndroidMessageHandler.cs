using UnityEngine;
using System.Runtime.InteropServices;

public class AndroidMessageHandler : MonoBehaviour
{
    // Declaração do método que será chamado pelo Android
    public void OnStepsUpdated(string message)
    {
        Debug.Log("Recebi uma mensagem do Android: " + message);
        // Aqui você pode adicionar código para atualizar a UI ou outra lógica com base na mensagem recebida
    }

    // Método para registrar o BroadcastReceiver quando o GameObject for ativado
    void OnEnable()
    {
        RegisterBroadcastReceiver();
    }

    // Método para cancelar o registro do BroadcastReceiver quando o GameObject for desativado
    void OnDisable()
    {
        UnregisterBroadcastReceiver();
    }

   void RegisterBroadcastReceiver()
{
    using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
    using (AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
    using (AndroidJavaObject intentFilter = new AndroidJavaObject("android.content.IntentFilter", "com.kdg.toast.plugin.ACTION_STEPS_UPDATED"))
    {
        // Obtém o caminho completo da classe PedometerService
        string pedometerServiceClassName = "com.kdg.toast.plugin.PedometerService";
        AndroidJavaObject pedometerService = new AndroidJavaObject(pedometerServiceClassName);

        // Obtém o caminho completo da classe MyBroadcastReceiver dentro de PedometerService
        string broadcastReceiverClassName = pedometerServiceClassName + "$MyBroadcastReceiver";
        AndroidJavaObject broadcastReceiver = new AndroidJavaObject(broadcastReceiverClassName, new AndroidJavaRunnable(() =>
        {
            // Este código é chamado pelo BroadcastReceiver quando uma mensagem é recebida
            OnStepsUpdated("Passos atualizados no Android!");
        }));

        currentActivity.Call("registerReceiver", broadcastReceiver, intentFilter);
        Debug.Log("BroadcastReceiver registrado");
    }
}

void UnregisterBroadcastReceiver()
{
    using (AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
    using (AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
    {
        // Obtém o caminho completo da classe MyBroadcastReceiver dentro de PedometerService
        string receiverClassName = "com.kdg.toast.plugin.PedometerService$MyBroadcastReceiver";
        AndroidJavaObject receiver = new AndroidJavaObject(receiverClassName);
        
        currentActivity.Call("unregisterReceiver", receiver);
        Debug.Log("BroadcastReceiver cancelado");
    }
}

}
