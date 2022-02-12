package com.example.batteryservicekotlin.listFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.batteryservicekotlin.R
import com.example.batteryservicekotlin.database.Unit

class ListFragment : Fragment() {

    private lateinit var listRecyclerView: RecyclerView
    private var adapter: BatteryAdapter? = null

    private val listViewModel: ListViewModel by lazy {
        ViewModelProviders.of(this).get(ListViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        listRecyclerView = view.findViewById(R.id.recycler_view) as RecyclerView
        listRecyclerView.layoutManager = LinearLayoutManager(context)

        updateUI()

        return view
    }

    private fun updateUI() {

    }

    private inner class BatteryHolder(view: View) : RecyclerView.ViewHolder(view) {

        val dateTextView: TextView = itemView.findViewById(R.id.textViewItemDate)

    }

    private inner class BatteryAdapter(var units: List<Unit>) : RecyclerView.Adapter<BatteryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryHolder {
            val view = layoutInflater.inflate(R.layout.list_item, parent, false)
            return BatteryHolder(view)
        }

        override fun getItemCount() = units.size

        override fun onBindViewHolder(holder: BatteryHolder, position: Int) {
            val unit = units[position]
            holder.apply {
                dateTextView.text = unit.date.toString()
            }
        }
    }

    companion object {
        fun newInstance(): ListFragment {
            return ListFragment()
        }
    }
}