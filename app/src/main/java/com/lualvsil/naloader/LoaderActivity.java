package com.lualvsil.naloader;

import android.app.NativeActivity;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.os.FileUtils;
import java.io.IOException;
import java.io.InputStream;

public class LoaderActivity extends NativeActivity {
	static {
		System.loadLibrary("native_loader");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		String fileUriString = getIntent().getStringExtra("fileUri");
		if (fileUriString == null) {
			finish();
			return;
		}

		super.onCreate(savedInstanceState);

		try {
			copyFileToInternalPath(Uri.parse(fileUriString));
		} catch (IOException e) {
			Toast.makeText(this, "Failed to copy file", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		deleteLogcatFile();
		runLibrary();
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveLogcat();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		closeLibrary();
		saveLogcat();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	private void copyFileToInternalPath(Uri fileUri) throws IOException {
		try (ParcelFileDescriptor pfd =
					 getContentResolver().openFileDescriptor(fileUri, "r");
			 InputStream in  = new FileInputStream(pfd.getFileDescriptor());
			 FileOutputStream out = new FileOutputStream(
					 new File(getFilesDir(), "libloaded.so"))) {
			FileUtils.copy(in, out);
		}
	}

	private native void runLibrary();
	private native void closeLibrary();
	private native void saveLogcat();
	private native void deleteLogcatFile();
}
