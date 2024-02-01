using TMPro;
using UnityEngine;

public class BackgroundService : MonoBehaviour
{
    [SerializeField] private TextMeshProUGUI stepsText;
    [SerializeField] private TextMeshProUGUI totalStepsText;
    [SerializeField] private TextMeshProUGUI syncedDateText;

    private AndroidJavaClass unityClass;
    private AndroidJavaObject unityActivity;
    private AndroidJavaClass customClass;
    private const string PlayerPrefsTotalSteps = "totalSteps";
    private const string PackageName = "com.kdg.toast.plugin.Bridge";
    private const string UnityDefaultJavaClassName = "com.unity3d.player.UnityPlayer";
    private const string CustomClassReceiveActivityInstanceMethod = "ReceiveActivityInstance";
    private const string CustomClassStartModelInferenceMethod = "StartModelInference";
    private const string CustomClassStopModelInferenceMethod = "StopModelInference";
    private const string CustomClassStartServiceMethod = "StartService";
    private const string CustomClassStopServiceMethod = "StopService";
    private const string CustomClassGetCurrentStepsMethod = "GetCurrentSteps";
    private const string CustomClassSyncDataMethod = "SyncData";

    private void Awake()
    {
        Debug.Log("Awake called.");
        SendActivityReference(PackageName);
        GetCurrentSteps();
    }

    private void SendActivityReference(string packageName)
    {
        unityClass = new AndroidJavaClass(UnityDefaultJavaClassName);
        unityActivity = unityClass.GetStatic<AndroidJavaObject>("currentActivity");
        
        if (unityActivity == null)
        {
            Debug.LogError("Failed to get currentActivity.");
            return;
        }

        customClass = new AndroidJavaClass(packageName);
        customClass.CallStatic(CustomClassReceiveActivityInstanceMethod, unityActivity);
    }

    public void StartModelInference()
    {
        Debug.Log("StartModelInference called.");
        if (customClass != null)
        {
            customClass.CallStatic(CustomClassStartModelInferenceMethod);
            GetCurrentSteps();
        }
        else
        {
            Debug.LogError("customClass is null. Ensure SendActivityReference was called.");
        }
    }

    public void StopModelInference()
    {
        Debug.Log("StopModelInference called.");
        if (customClass != null)
        {
            customClass.CallStatic(CustomClassStopModelInferenceMethod);
        }
        else
        {
            Debug.LogError("customClass is null. Ensure SendActivityReference was called.");
        }
    }

    public void StartService()
    {
        Debug.Log("StartService called.");
        if (customClass != null)
        {
            customClass.CallStatic(CustomClassStartServiceMethod);
            GetCurrentSteps();
        }
        else
        {
            Debug.LogError("customClass is null. Ensure SendActivityReference was called.");
        }
    }

    public void StopService()
    {
        Debug.Log("StopService called.");
        if (customClass != null)
        {
            customClass.CallStatic(CustomClassStopServiceMethod);
        }
        else
        {
            Debug.LogError("customClass is null. Ensure SendActivityReference was called.");
        }
    }

    public void GetCurrentSteps()
    {
        Debug.Log("GetCurrentSteps called.");
        if (customClass != null)
        {
            int? stepsCount = customClass.CallStatic<int>(CustomClassGetCurrentStepsMethod);
            if (stepsCount != null)
            {
                stepsText.text = stepsCount.ToString();
            }
            else
            {
                Debug.LogError("Failed to get steps count.");
            }
        }
        else
        {
            Debug.LogError("customClass is null. Ensure SendActivityReference was called.");
        }
    }

    public void SyncData()
    {
        Debug.Log("SyncData called.");
        if (customClass != null)
        {
            var data = customClass.CallStatic<string>(CustomClassSyncDataMethod);

            if (!string.IsNullOrEmpty(data))
            {
                var parsedData = data.Split('#');
                if (parsedData.Length >= 3)
                {
                    var dateOfSync = parsedData[0] + " - " + parsedData[1];
                    syncedDateText.text = dateOfSync;
                    var receivedSteps = int.Parse(parsedData[2]);
                    var prefsSteps = PlayerPrefs.GetInt(PlayerPrefsTotalSteps, 0);
                    var prefsStepsToSave = prefsSteps + receivedSteps;
                    PlayerPrefs.SetInt(PlayerPrefsTotalSteps, prefsStepsToSave);
                    totalStepsText.text = prefsStepsToSave.ToString();

                    GetCurrentSteps();
                }
                else
                {
                    Debug.LogError("Invalid data format received.");
                }
            }
            else
            {
                Debug.LogError("Failed to get Sync data.");
            }
        }
        else
        {
            Debug.LogError("customClass is null. Ensure SendActivityReference was called.");
        }
    }
}
