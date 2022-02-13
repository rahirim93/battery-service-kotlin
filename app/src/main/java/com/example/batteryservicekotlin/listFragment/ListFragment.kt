package com.example.batteryservicekotlin.listFragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.batteryservicekotlin.R
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.service.Actions
import java.util.*

private const val TAG = "ListFragment"

class ListFragment : Fragment() {

    interface Callbacks {
        fun startStopService(action: Actions)
    }

    private val listViewModel: ListViewModel by lazy {
        ViewModelProviders.of(this).get(ListViewModel::class.java)
    }

    private lateinit var recyclerView: RecyclerView
    private var adapter: ListAdapter? = null

    private var callbacks: Callbacks? = null
    private lateinit var textViewInsertsTotal: TextView
    private lateinit var textViewInsertsToday: TextView
    private lateinit var textViewInsertsMustBeToday: TextView
    private lateinit var textViewInsertsDifference: TextView
    private lateinit var buttonStartService: Button
    private lateinit var buttonStopService: Button

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Наблюдатель за общим количеством записей в БД
        val batteryObserver = Observer<List<Unit>> { units ->
            //Log.d(TAG, "Количество записей: ${units.size}")
            textViewInsertsTotal.text = "Количество записей в БД: ${units.size}"
        }
        listViewModel.unitListLiveData.observe(this, batteryObserver)

        val datesObserver = Observer<List<Date>> { dates ->
            updateUI(dates)
        }
        listViewModel.listDatesLiveData.observe(this, datesObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)

        textViewInsertsTotal = view.findViewById(R.id.textViewInsertsTotal) as TextView
        textViewInsertsToday = view.findViewById(R.id.textViewInsertsToday) as TextView
        textViewInsertsMustBeToday = view.findViewById(R.id.textViewInsertsMustBeToday) as TextView
        textViewInsertsDifference = view.findViewById(R.id.textViewInsertsDifference) as TextView

        recyclerView = view.findViewById(R.id.recyclerView) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        buttonStartService = view.findViewById(R.id.buttonStartService) as Button
        buttonStopService = view.findViewById(R.id.buttonStopService) as Button

        buttonStartService.setOnClickListener {
            callbacks?.startStopService(Actions.START)
        }

        buttonStopService.setOnClickListener {
            callbacks?.startStopService(Actions.STOP)
        }

        return view
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateUI(dates: List<Date>) {
        val filteredDates = filterDate(dates)
        adapter = ListAdapter(filteredDates)
        recyclerView.adapter = adapter
    }

    private inner class ItemHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var date: Date

        private val dateTextView: TextView = itemView.findViewById(R.id.textViewItemDate)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(date: Date) {
            this.date = date
            dateTextView.text = this.date.toString()
        }

        override fun onClick(p0: View?) {

        }
    }

    private inner class ListAdapter(var dates: List<Date>)
        : RecyclerView.Adapter<ItemHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        : ItemHolder {
            val view = layoutInflater.inflate(R.layout.list_item, parent, false)
            return ItemHolder(view)
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val date = dates[position]
            holder.bind(date)
        }

        override fun getItemCount() = dates.size

    }

    companion object {
        fun newInstance(): ListFragment {
            return ListFragment()
        }
    }

    private fun filterDate(list: List<Date>): List<Date> {
        var listFiltered = arrayListOf<Date>()
        list.forEach { date ->
            if (listFiltered.isEmpty()) listFiltered.add(date)
            var listFilteredIterator = listFiltered.listIterator()
            var inputDate: Date? = null
            while (listFilteredIterator.hasNext()) {
                val tempNextDate = listFilteredIterator.next()
                if (tempNextDate.date != date.date) {
                    inputDate = date
                } else {
                    inputDate = null
                }
            }
            if (inputDate != null) listFilteredIterator.add(inputDate)
        }

        val list: List<Date> = listFiltered
        return list
    }
}