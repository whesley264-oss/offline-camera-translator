package com.offline.translator.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.offline.translator.R
import com.offline.translator.model.Language

class LanguageAdapter(
    private val languages: List<Language>,
    private val onDownloadClick: (Language) -> Unit,
    private val onDeleteClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtLanguageName)
        val txtNative: TextView = itemView.findViewById(R.id.txtLanguageNative)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val btnDownload: Button = itemView.findViewById(R.id.btnDownload)
        val imgDelete: ImageView = itemView.findViewById(R.id.imgDelete)
        val progressDownload: ProgressBar = itemView.findViewById(R.id.progressDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        
        holder.txtName.text = language.name
        holder.txtNative.text = language.nativeName
        
        when {
            language.isDownloading -> {
                holder.progressDownload.visibility = View.VISIBLE
                holder.txtStatus.visibility = View.GONE
                holder.btnDownload.visibility = View.GONE
                holder.imgDelete.visibility = View.GONE
            }
            language.isDownloaded -> {
                holder.progressDownload.visibility = View.GONE
                holder.txtStatus.visibility = View.VISIBLE
                holder.txtStatus.text = "✓ Baixado"
                holder.txtStatus.setTextColor(0xFF4CAF50.toInt())
                holder.btnDownload.visibility = View.GONE
                holder.imgDelete.visibility = View.VISIBLE
            }
            else -> {
                holder.progressDownload.visibility = View.GONE
                holder.txtStatus.visibility = View.GONE
                holder.btnDownload.visibility = View.VISIBLE
                holder.imgDelete.visibility = View.GONE
            }
        }
        
        holder.btnDownload.setOnClickListener { onDownloadClick(language) }
        holder.imgDelete.setOnClickListener { onDeleteClick(language) }
    }

    override fun getItemCount() = languages.size
}