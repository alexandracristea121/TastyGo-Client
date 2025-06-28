package com.examples.licenta_food_ordering.adaptar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.licenta_food_ordering.databinding.ItemChatBinding
import com.examples.licenta_food_ordering.Message

class ChatAdapter(
    private val messageList: List<Message>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (messageList[position].type) {
            Message.Type.USER -> 0
            Message.Type.BOT -> 1
            Message.Type.SUGGESTION -> 2 // Nou tip pentru sugestii
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            0 -> ChatViewHolder(ItemChatBinding.inflate(layoutInflater, parent, false))
            1 -> ChatViewHolder(ItemChatBinding.inflate(layoutInflater, parent, false))
            else -> throw IllegalArgumentException("ViewType necunoscut: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]

        when (holder) {
            is ChatViewHolder -> {
                holder.binding.userMessageTextView.visibility = View.GONE
                holder.binding.botMessageTextView.visibility = View.GONE

                if (message.type == Message.Type.USER) {
                    holder.binding.userMessageTextView.visibility = View.VISIBLE
                    holder.binding.userMessageTextView.text = "Utilizator: " + message.content
                } else {
                    holder.binding.botMessageTextView.visibility = View.VISIBLE
                    holder.binding.botMessageTextView.text = "FoodBot: " + message.content
                }
            }
        }
    }

    override fun getItemCount(): Int = messageList.size

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)
}