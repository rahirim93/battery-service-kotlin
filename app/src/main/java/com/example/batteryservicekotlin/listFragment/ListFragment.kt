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
import com.example.batteryservicekotlin.BatteryBundleForAdapter
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

            val listFiltered = arrayListOf<Date>()
            units.forEach { unit ->
                if (listFiltered.isEmpty()) listFiltered.add(unit.date)
                val listFilteredIterator = listFiltered.listIterator()
                var inputDate: Date? = null
                while (listFilteredIterator.hasNext()) {
                    val tempNextDate = listFilteredIterator.next()
                    inputDate = if (tempNextDate.date != unit.date.date) {
                        unit.date
                    } else {
                        null
                    }
                }
                if (inputDate != null) listFilteredIterator.add(inputDate)
            }
            // Список с отфильтрованными датами
            var listDates = listFiltered


            // Дальше перебираем список с отфильтрованными датами
            // и для каждой даты вычисляем количество записей в эту дату
            // создавая при том список с количеством записей
            // Лист отфильтрованных списков
            var listSetFilteredInserts = arrayListOf<List<Unit>>()
            listDates.forEach { date ->
                //Начало дня перебираемой даты
                val startDayDate = Date(date.year,date.month,date.date,0,0,0)
                //Конец дня перебираемой даты
                val endDayDate = Date(date.year,date.month,date.date,23,59,59)
                // Даты в миллисекундах
                val startDayInMillis = startDayDate.time
                val endDayInMillis = endDayDate.time
                // Временный пустой лист для заполнения отфильтровнными записями в перебираемый день
                var listFilteredInserts = arrayListOf<Unit>()

                units.forEach {
                    if (it.date.time in (startDayInMillis + 1) until endDayInMillis) {
                        listFilteredInserts.add(it)
                    }
                }
                listSetFilteredInserts.add(listFilteredInserts)
            }
            // Список объектов для адаптера
            var listOfBatteryBundles = arrayListOf<BatteryBundleForAdapter>()
            for (i in listDates.indices) {
                listOfBatteryBundles.add(
                    BatteryBundleForAdapter(listDates[i], listSetFilteredInserts[i])
                )
            }

            adapter = ListAdapter(listOfBatteryBundles)
            recyclerView.adapter = adapter

            //Log.d(TAG, "Количество записей: ${units.size}")
            textViewInsertsTotal.text = "Количество записей в БД: ${units.size}"
            /** Для вывода даты и количества записей в элемент списка нужна сложная фильтрация.
             * Один из вопросов если делать ее в основном потоке не загрузить ли она его.
             * Начнем хотя бы с фильтрации в основном потоке
             * У нас уже есть функция для фильтрации дней.
             * Теперь нужно для каждого дня вывести количество записей в этот день.
             * Т.е. массива уже будет два, и связи между ними не будет.
             * Нужно создать класс где будут хранится два элемета: дата и количество записей в этот день.
             * Слить два массива в один и передать в адаптер.
             * Другой вариант это передать все записи в адаптер утилизатора и отфильтровать и заполнить все там */
        }
        listViewModel.unitListLiveData.observe(this, batteryObserver)

        val datesObserver = Observer<List<Date>> { dates ->
            //updateUI(dates)
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
        //val filteredDates = filterDate(dates)
        //adapter = ListAdapter(filteredDates)
        //recyclerView.adapter = adapter
    }

    private inner class ItemHolder(view: View)
        : RecyclerView.ViewHolder(view), View.OnClickListener {

        private lateinit var batteryBundle: BatteryBundleForAdapter

        private val dateTextView: TextView = itemView.findViewById(R.id.textViewItemDate)
        private val insertsNumberTextView: TextView = itemView.findViewById(R.id.textViewItemNumberToday)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(batteryBundle: BatteryBundleForAdapter) {
            this.batteryBundle = batteryBundle
            dateTextView.text = this.batteryBundle.date.toString()
            insertsNumberTextView.text = "Количество записей: ${this.batteryBundle.insertsDay.size} из 17280"
        }

        override fun onClick(p0: View?) {

        }
    }

    private inner class ListAdapter(var batteryBundles: List<BatteryBundleForAdapter>)
        : RecyclerView.Adapter<ItemHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
        : ItemHolder {
            val view = layoutInflater.inflate(R.layout.list_item, parent, false)
            return ItemHolder(view)
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val batteryBundle = batteryBundles[position]
            holder.bind(batteryBundle)
        }

        override fun getItemCount() = batteryBundles.size

    }

    companion object {
        fun newInstance(): ListFragment {
            return ListFragment()
        }
    }

    private fun filterDate(list: List<Date>): List<Date> {
        val listFiltered = arrayListOf<Date>()
        list.forEach { date ->
            if (listFiltered.isEmpty()) listFiltered.add(date)
            val listFilteredIterator = listFiltered.listIterator()
            var inputDate: Date? = null
            while (listFilteredIterator.hasNext()) {
                val tempNextDate = listFilteredIterator.next()
                inputDate = if (tempNextDate.date != date.date) {
                    date
                } else {
                    null
                }
            }
            if (inputDate != null) listFilteredIterator.add(inputDate)
        }
        return listFiltered
    }
}