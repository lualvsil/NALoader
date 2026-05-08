package com.lualvsil.naloader;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;

import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	final int FILE_PICKER_RESULT = 1;

	Button chooseFileButton;
	Button loadButton;
	TextView fileNameView;

	Uri selectedFileUri;
	String selectedFileUriString;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		fileNameView = findViewById(R.id.fileName);
		chooseFileButton = findViewById(R.id.btnChooseFile);
		loadButton = findViewById(R.id.btnLoad);

		chooseFileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openFilePicker();
			}
		});
		loadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectedFileUriString == null) {
					Toast.makeText(MainActivity.this,
						"No file selected", Toast.LENGTH_SHORT).show();
					return;
				}
				Intent loader = new Intent(MainActivity.this, LoaderActivity.class);
				loader.putExtra("fileUri", selectedFileUriString);
				startActivity(loader);
			}
		});
	}

	private void openFilePicker() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		startActivityForResult(intent, FILE_PICKER_RESULT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == FILE_PICKER_RESULT && resultCode == RESULT_OK) {
			selectedFileUri	   = data.getData();
			selectedFileUriString = selectedFileUri.toString();

			getContentResolver().takePersistableUriPermission(
				selectedFileUri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION);

			fileNameView.setText(selectedFileUriString);
		}
	}
}
