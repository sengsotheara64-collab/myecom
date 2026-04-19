package com.example.myecomapp.ui.adapters

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myecomapp.R
import com.example.myecomapp.data.Order
import com.example.myecomapp.data.OrderStatus
import com.example.myecomapp.data.getOrderStatus
import com.example.myecomapp.databinding.RvItemOrderBinding
import androidx.core.graphics.drawable.toDrawable

class OrderListAdapter : RecyclerView.Adapter<OrderListAdapter.OrdersViewHolder>() {

    inner class OrdersViewHolder(private val binding: RvItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            binding.apply {
                tvOrderId.text = order.orderId.toString()
                val status =
                    "Status: ${if (order.orderStatus == "") "Unknown" else order.orderStatus}"
                tvOrderStatus.text = status
                tvOrderDate.text = order.date
                imageOrderState.setImageDrawable(
                    when (getOrderStatus(order.orderStatus)) {
                        is OrderStatus.Pending -> {
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.g_blue_gray200
                            ).toDrawable()
                        }

                        is OrderStatus.Confirmed -> {
                            ContextCompat.getColor(
                                itemView.context,
                                R.color.g_orange_yellow
                            ).toDrawable()
                        }

                        is OrderStatus.Delivered -> {
                            ContextCompat.getColor(itemView.context, R.color.g_green).toDrawable()
                        }

                        is OrderStatus.Shipped -> {
                            ContextCompat.getColor(itemView.context, R.color.g_green).toDrawable()
                        }

                        is OrderStatus.Canceled -> {
                            ContextCompat.getColor(itemView.context, R.color.g_red).toDrawable()
                        }

                        is OrderStatus.Returned -> {
                            ContextCompat.getColor(itemView.context, R.color.g_red).toDrawable()
                        }
                    }
                )
            }
        }
    }

    private val diffUtil = object : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.products == newItem.products
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdersViewHolder {
        return OrdersViewHolder(
            RvItemOrderBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OrdersViewHolder, position: Int) {
        val order = differ.currentList[position]
        holder.bind(order)

        holder.itemView.setOnClickListener {
            onClick?.invoke(order)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onClick: ((Order) -> Unit)? = null
}