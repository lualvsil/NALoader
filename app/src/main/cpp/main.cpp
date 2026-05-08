#include <string>
#include <fstream>
#include <cstdlib>

#include <android/native_activity.h>
#include <android/log.h>
#include <dlfcn.h>
#include <jni.h>

#define TAG "NaLoader"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static ANativeActivity* appActivity;
static void*   appSavedState;
static size_t  appStateSize;

typedef void (*OnCreateFnType)(ANativeActivity*, void*, size_t);

class NativeActivity {
private:
    void*          handle = nullptr;
    OnCreateFnType fn     = nullptr;

public:
    std::string appInternalPath;

    ~NativeActivity() {
        if (handle) dlclose(handle);
    }

    void load(const char* path) {
        std::ofstream dl_logs(appInternalPath + "/logdl.txt");

        handle = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
        if (!handle) {
            dl_logs << dlerror() << std::endl;
            return;
        }
        dl_logs << "Library open: success" << std::endl;

        fn = (OnCreateFnType)dlsym(handle, "ANativeActivity_onCreate");
        if (!fn) {
            dl_logs << dlerror() << std::endl;
            return;
        }
        dl_logs << "Function search: success" << std::endl;

        fn(appActivity, appSavedState, appStateSize);
    }

    void deleteLogcatFile() {
        std::system("logcat -c");
        std::remove((appInternalPath + "/logcat.txt").c_str());
    }

    void dumpLogcat() {
        std::string cmd = "logcat -d >> " + appInternalPath + "/logcat.txt";
        std::system(cmd.c_str());
        std::system("logcat -c");
    }
};

static NativeActivity* native_activity = nullptr;

extern "C" {
	void ANativeActivity_onCreate(
	        ANativeActivity* activity,
	        void* savedState,
	        size_t savedStateSize)
	{
	    appActivity   = activity;
	    appSavedState = savedState;
	    appStateSize  = savedStateSize;
	
	    delete native_activity;
	    native_activity = new NativeActivity();
	    native_activity->appInternalPath = activity->internalDataPath;
	}
	
	JNIEXPORT void JNICALL Java_com_lualvsil_naloader_LoaderActivity_runLibrary(JNIEnv*, jobject)
	{
		if (native_activity)
			native_activity->load((native_activity->appInternalPath + "/libloaded.so").c_str());
	}
	
	JNIEXPORT void JNICALL Java_com_lualvsil_naloader_LoaderActivity_closeLibrary(JNIEnv*, jobject)
	{
	    delete native_activity;
	    native_activity = nullptr;
	}
	
	JNIEXPORT void JNICALL Java_com_lualvsil_naloader_LoaderActivity_deleteLogcatFile(JNIEnv*, jobject)
	{
	    if (native_activity) native_activity->deleteLogcatFile();
	}
	
	JNIEXPORT void JNICALL Java_com_lualvsil_naloader_LoaderActivity_saveLogcat(JNIEnv*, jobject)
	{
	    if (native_activity) native_activity->dumpLogcat();
	}
} // extern "C"
