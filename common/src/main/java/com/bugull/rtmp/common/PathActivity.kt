package com.bugull.rtmp.common

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class PathActivity(private val permissions: Array<String>) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_path)
        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@PathActivity)
            addItemDecoration(DividerItemDecoration(this@PathActivity,
                DividerItemDecoration.VERTICAL))
            adapter = PathAdapter(defaultPathList) {

                if (allPermissionsGranted(permissions)) {
                    onPathClick(it)
                } else {
                    ActivityCompat.requestPermissions(
                        this@PathActivity, permissions, 0x99
                    )
                }
            }
        }
    }

    private fun allPermissionsGranted(list: Array<String>) = list.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    abstract fun onPathClick(it: PathData)

    private val defaultPathList = listOf(
        PathData("本地1", "rtmp://192.168.199.144/laputa/abcd"),
        PathData("远程1", "rtmp://121.40.115.131:11835/live/abcd"),
        PathData("斗鱼(直播码会刷新)",
            "rtmp://sendtc3a.douyu.com/live/9836699rWarJ8dRN?wsSecret=d463f474e6b8d233a536670626394c25&wsTime=60b47df5&wsSeek=off&wm=0&tw=0&roirecognition=0&record=flv&origin=tct"),
    )



    private class PathAdapter(
        private val list: List<PathData>,
        private val click: (PathData) -> Unit,
    ) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_path, null, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            list[position].run {
                holder.itemView.findViewById<TextView>(R.id.tv_name).text = this.name
                holder.itemView.findViewById<TextView>(R.id.tv_value).text = this.value
                holder.itemView.setOnClickListener {
                    click(this)
                }
            }
        }

        override fun getItemCount(): Int = list.size

        private class VH(itemView: View) : RecyclerView.ViewHolder(itemView)

    }

    data class PathData(val name: String, val value: String)


}