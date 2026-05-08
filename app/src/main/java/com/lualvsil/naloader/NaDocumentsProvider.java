package com.lualvsil.naloader;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

public class NaDocumentsProvider extends DocumentsProvider {
	private static final String ROOT_ID = "root";
	private File baseDir;

	@Override
	public boolean onCreate() {
		baseDir = getContext().getFilesDir();
		return true;
	}

	@Override
	public Cursor queryRoots(String[] projection) {
		MatrixCursor result = new MatrixCursor(projection != null ? projection : new String[]{
			DocumentsContract.Root.COLUMN_ROOT_ID,
			DocumentsContract.Root.COLUMN_FLAGS,
			DocumentsContract.Root.COLUMN_ICON,
			DocumentsContract.Root.COLUMN_TITLE,
			DocumentsContract.Root.COLUMN_DOCUMENT_ID,
			DocumentsContract.Root.COLUMN_MIME_TYPES,
			DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
			DocumentsContract.Root.COLUMN_SUMMARY
		});

		MatrixCursor.RowBuilder row = result.newRow();
		row.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID);
		row.add(DocumentsContract.Root.COLUMN_TITLE, "NALoader");
		row.add(DocumentsContract.Root.COLUMN_SUMMARY, null);
		row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
		row.add(DocumentsContract.Root.COLUMN_FLAGS,
			DocumentsContract.Root.FLAG_SUPPORTS_CREATE |
			DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD |
			DocumentsContract.Root.FLAG_SUPPORTS_SEARCH |
			DocumentsContract.Root.FLAG_LOCAL_ONLY);
		row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, baseDir.getAbsolutePath());
		row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*");
		row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, baseDir.getFreeSpace());

		return result;
	}

	@Override
	public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
		MatrixCursor result = new MatrixCursor(projection != null ? projection : new String[]{
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_MIME_TYPE,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_LAST_MODIFIED,
			DocumentsContract.Document.COLUMN_FLAGS,
			DocumentsContract.Document.COLUMN_SIZE
		});
		includeFile(result, documentId, null);
		return result;
	}

	@Override
	public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
		MatrixCursor result = new MatrixCursor(projection != null ? projection : new String[]{
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_MIME_TYPE,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_LAST_MODIFIED,
			DocumentsContract.Document.COLUMN_FLAGS,
			DocumentsContract.Document.COLUMN_SIZE
		});
		File parent = getFileForDocId(parentDocumentId);
		File[] files = parent.listFiles();
		if (files != null) {
			for (File f : files) {
				includeFile(result, null, f);
			}
		}
		return result;
	}

	@Override
	public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
			throws FileNotFoundException {
		File file = getFileForDocId(documentId);
		if (mode.contains("w") && file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
	}

	@Override
	public String createDocument(String parentDocumentId, String mimeType, String displayName)
			throws FileNotFoundException {
		File parent = getFileForDocId(parentDocumentId);
		File file = new File(parent, displayName);
		int i = 1;
		while (file.exists()) {
			file = new File(parent, displayName + " (" + i++ + ")");
		}
		try {
			if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
				if (!file.mkdirs()) throw new IOException("Failed to create directory");
			} else {
				if (!file.createNewFile()) throw new IOException("Failed to create file");
			}
		} catch (IOException e) {
			throw new FileNotFoundException(e.getMessage());
		}
		return getDocIdForFile(file);
	}

	@Override
	public void deleteDocument(String documentId) throws FileNotFoundException {
		File file = getFileForDocId(documentId);
		if (!file.delete()) {
			throw new FileNotFoundException("Failed to delete: " + documentId);
		}
	}

	@Override
	public boolean isChildDocument(String parentDocumentId, String documentId) {
		return documentId.startsWith(parentDocumentId);
	}

	private void includeFile(MatrixCursor cursor, String docId, File file) throws FileNotFoundException {
		if (docId == null) {
			docId = getDocIdForFile(file);
		} else {
			file = getFileForDocId(docId);
		}

		int flags = 0;
		if (file.isDirectory()) {
			if (file.canWrite()) {
				flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
			}
		} else if (file.canWrite()) {
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
		}
		File parent = file.getParentFile();
		if (parent != null && parent.canWrite()) {
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
		}

		String mime = file.isDirectory()
			? DocumentsContract.Document.MIME_TYPE_DIR
			: getMimeType(file.getName());

		if (mime.startsWith("image/")) {
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL;
		}

		MatrixCursor.RowBuilder row = cursor.newRow();
		row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId);
		row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.getName());
		row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mime);
		row.add(DocumentsContract.Document.COLUMN_SIZE, file.length());
		row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
		row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
	}

	private String getDocIdForFile(File file) {
		return file.getAbsolutePath();
	}

	private File getFileForDocId(String docId) throws FileNotFoundException {
		if (docId == null || docId.equals(ROOT_ID)) return baseDir;

		if (docId.startsWith(ROOT_ID + "/")) {
			String relative = docId.substring(ROOT_ID.length() + 1);
			File f = new File(baseDir, relative);
			if (!f.exists()) throw new FileNotFoundException(f.getAbsolutePath() + " not found");
			return f;
		}

		File f = new File(docId);
		if (!f.exists()) throw new FileNotFoundException(f.getAbsolutePath() + " not found");
		return f;
	}

	private String getMimeType(String name) {
		int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0) {
			String ext = name.substring(lastDot + 1).toLowerCase(Locale.ROOT);
			String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
			if (mime != null) return mime;
		}
		return "application/octet-stream";
	}
}
