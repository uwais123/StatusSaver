package com.masuwes.statussaver.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.masuwes.statussaver.R
import com.masuwes.statussaver.ui.activity.ImageActivity
import com.masuwes.statussaver.ui.activity.VideoActivity
import umairayub.madialog.MaDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.*

class StatusAdapter(
    private val activity: Context,
    private val filesList: ArrayList<File>,
    private val saved: Boolean
) : RecyclerView.Adapter<StatusAdapter.FileHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        val inflatedView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recyclerview, parent, false)
        return FileHolder(inflatedView)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onBindViewHolder(holder: FileHolder, position: Int) {

        val currentFile = filesList[position]
        if (saved) {
            holder.button1.background = activity.getDrawable(R.drawable.ic_delete)
            holder.button1.setOnClickListener(deleteMediaItem(currentFile))
        } else {
            holder.button1.background = activity.getDrawable(R.drawable.ic_save)
            holder.button1.setOnClickListener(downloadMediaItem(currentFile))
        }
        val status = currentFile.absolutePath
        if (status.endsWith(".mp4")) {
            holder.statusItemType.text = "Video"
            holder.buttonShare.setOnClickListener(shareMediaItem(currentFile, false))
        } else {
            holder.statusItemType.text = "Image"
            holder.buttonShare.setOnClickListener(shareMediaItem(currentFile, true))
        }

        Glide.with(activity)
            .load(status)
            .centerCrop()
            .into(holder.statusItemView)

        holder.itemView.setOnClickListener {
            if (status.endsWith(".mp4")) {
                val intent = Intent(activity, VideoActivity::class.java)
                intent.putExtra("videoPath", status)
                activity.startActivity(intent)
            } else {
                val intent = Intent(activity, ImageActivity::class.java)
                intent.putExtra("ImagePath", status)
                activity.startActivity(intent)
            }
        }

        holder.itemView.setOnLongClickListener {
            Toast.makeText(it.context, currentFile.name, Toast.LENGTH_SHORT).show()
            true
        }

    }

    override fun getItemCount(): Int {
        return filesList.size
    }


    private fun downloadMediaItem(sourceFile: File): View.OnClickListener {
        return View.OnClickListener {
            val path =
                Environment.getExternalStorageDirectory().toString() + DIRECTORY_TO_SAVE_MEDIA_NOW
            val dstfile = File(path, sourceFile.name)
            Runnable {
                try {
                    copyFile(sourceFile, dstfile)
                    Toast.makeText(activity, "saved!", Toast.LENGTH_SHORT).show()
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    val contentUri = Uri.fromFile(dstfile)
                    mediaScanIntent.data = contentUri
                    activity.sendBroadcast(mediaScanIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("RecyclerV", "onClick: Error:" + e.message)
                    Toast.makeText(activity, "unable to save\n" + e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }.run()
        }
    }

    private fun shareMediaItem(sourceFile: File, isImage: Boolean): View.OnClickListener {
        return View.OnClickListener {

            try {
                val fileDirPath =
                    File(Environment.getExternalStorageDirectory(), "/WhatsApp/Media/.Statuses/")
                fileDirPath.mkdirs()
                val file = File(fileDirPath, sourceFile.name)
                val contentUri: Uri =
                    FileProvider.getUriForFile(
                        activity,
                        "com.masuwes.statussaver.fileprovider",
                        file
                    )

                val intent = Intent(Intent.ACTION_SEND)
                if (isImage) {
                    intent.type = "image/*"
                } else {
                    intent.type = "video/*"
                }
                intent.putExtra(Intent.EXTRA_STREAM, contentUri)
                activity.startActivity(Intent.createChooser(intent, "Share via "))
            } catch (e: IOException) {
                throw RuntimeException("Error generating file", e)
            }

        }
    }

    private fun deleteMediaItem(sourceFile: File): View.OnClickListener {
        return View.OnClickListener {
            Runnable {
                try {
                    if (sourceFile.exists()) {
                        MaDialog.Builder(activity)
                            .setTitle("Delete?")
                            .setTitleTextColor(Color.RED)
                            .setMessage("Are you sure you want to delete this file?")
                            .setPositiveButtonText("Yes")
                            .setNegativeButtonText("Cancel")
                            .setPositiveButtonListener {
                                sourceFile.delete()
                                if (!sourceFile.exists()) {
                                    filesList.remove(sourceFile)
                                    notifyDataSetChanged()
                                    Toast.makeText(
                                        activity,
                                        "File deleted!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .setNegativeButtonListener {

                            }
                            .build()


                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("RecyclerV", "onClick: Error:" + e.message)
                    Toast.makeText(activity, "unable to delete", Toast.LENGTH_SHORT).show()
                }
            }.run()
        }
    }

    class FileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var statusItemView: ImageView = itemView.findViewById<View>(R.id.videoView) as ImageView
        var statusItemType: TextView = itemView.findViewById<View>(R.id.itemType) as TextView
        var button1: ImageView = itemView.findViewById<View>(R.id.btn_save) as ImageView
        var buttonShare: ImageView = itemView.findViewById<View>(R.id.btn_share) as ImageView


    }

    companion object {
        private const val DIRECTORY_TO_SAVE_MEDIA_NOW = "/Saved Status/"

        /**
         * copy file to destination.
         *
         * @param sourceFile
         * @param destFile
         * @throws IOException
         */
        @Throws(IOException::class)
        fun copyFile(sourceFile: File?, destFile: File) {
            if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
            if (!destFile.exists()) {
                destFile.createNewFile()
            }
            var source: FileChannel? = null
            var destination: FileChannel? = null
            try {
                source = FileInputStream(sourceFile).channel
                destination = FileOutputStream(destFile).channel
                destination.transferFrom(source, 0, source.size())
            } finally {
                source?.close()
                destination?.close()
            }
        }
    }

}