package com.example.ecocollector


import android.app.Activity
import android.app.ProgressDialog
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.nio.file.Files

class UploadUtility(activity: Activity) {

    var activity = activity;
    var dialog: ProgressDialog? = null
    var serverURL: String = "https://ecodata.leofigy.xyz/api/v1/bundle"
    var serverUploadDirectoryPath: String = "Ecodata"
    val client = OkHttpClient()

    fun uploadFile(sourceFilePath: String, uploadedFileName: String? = null) {
        uploadFile(File(sourceFilePath), uploadedFileName)
    }

    fun uploadFile(sourceFile: File, uploadedFileName: String? = null){

        Thread {
            val mimeType = getMimeType(sourceFile);
            if (mimeType == null) {
                Log.d("COUT", "Not able to get mime type")
                return@Thread
            }

            val fileName: String = if (uploadedFileName == null)  sourceFile.name else uploadedFileName
            toggleProgressDialog(true)
            try {
                val requestBody: RequestBody =
                    MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("uploaded_file", fileName,sourceFile.asRequestBody(mimeType.toMediaTypeOrNull()))
                        .build()


                val request: Request = Request.Builder().url(serverURL).post(requestBody).build()

                val response: Response = client.newCall(request).execute()

                if (response.isSuccessful) {

                    Log.d("COUT","success, ${sourceFile.absoluteFile}")
                    showToast("File $fileName uploaded successfully at $serverUploadDirectoryPath")
                    Files.delete(sourceFile.toPath())

                } else {
                    Log.d("COUT", "Failed to upload from: ${activity.title}")
                    showToast("File uploading failed")

                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.e("File upload", "failed")
                showToast("File uploading failed")
            }
            toggleProgressDialog(false)
        }.start()
    }

    // url = file path or whatever suitable URL you want.
    fun getMimeType(file: File): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(file.path)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }

    fun showToast(message: String) {
        activity.runOnUiThread {
            Toast.makeText( activity, message, Toast.LENGTH_LONG ).show()
        }
    }

    fun toggleProgressDialog(show: Boolean) {
        activity.runOnUiThread {
            if (show) {
                dialog = ProgressDialog.show(activity, "", "Uploading file...", true);
            } else {
                dialog?.dismiss();
            }
        }
    }

}