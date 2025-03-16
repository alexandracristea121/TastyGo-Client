package com.examples.licenta_food_ordering.model

import android.os.Parcel
import android.os.Parcelable
import java.util.ArrayList
import java.util.Date

class OrderDetails() : Parcelable {
    var userUid: String? = null
    var userName: String? = null
    var foodNames: MutableList<String>? = null
    var foodImages: MutableList<String>? = null
    var foodPrices: MutableList<String>? = null
    var foodQuantities: MutableList<Int>? = null
    var userLocation: String? = null
    var restaurantLocation: String? = null
    var totalPrice: String? = null
    var phoneNumber: String? = null
    var orderAccepted: Boolean = false
    var orderDelivered: Boolean = false
    var paymentReceived: Boolean = false
    var itemPushkey: String? = null
    var orderTime: String? = null
    var adminUserId: String? = null
    var restaurantName: String? = null

    constructor(parcel: Parcel) : this() {
        userUid = parcel.readString()
        userName = parcel.readString()
        foodNames = parcel.createStringArrayList()
        foodImages = parcel.createStringArrayList()
        foodPrices = parcel.createStringArrayList()
        foodQuantities = parcel.readArrayList(Int::class.java.classLoader) as MutableList<Int>?
        userLocation = parcel.readString()
        restaurantLocation = parcel.readString()
        totalPrice = parcel.readString()
        phoneNumber = parcel.readString()
        orderAccepted = parcel.readByte() != 0.toByte()
        orderDelivered = parcel.readByte() != 0.toByte()
        paymentReceived = parcel.readByte() != 0.toByte()
        itemPushkey = parcel.readString()
        orderTime = parcel.readString()
        adminUserId = parcel.readString()
        restaurantName = parcel.readString()
    }

    constructor(
        userId: String,
        name: String,
        foodItemName: ArrayList<String>,
        foodItemPrice: ArrayList<String>,
        foodItemImage: ArrayList<String>,
        foodItemQuantities: ArrayList<Int>,
        userLocation: String,
        restaurantLocation: String,
        totalAmount: String,
        phone: String,
        orderTime: String,
        itemPushKey: String?,
        orderAccepted: Boolean,
        paymentReceived: Boolean,
        adminUserId: String?,
        restaurantName: String?
    ) : this() {
        this.userUid = userId
        this.userName = name
        this.foodNames = foodItemName
        this.foodPrices = foodItemPrice
        this.foodImages = foodItemImage
        this.foodQuantities = foodItemQuantities
        this.userLocation = userLocation
        this.restaurantLocation = restaurantLocation
        this.totalPrice = totalAmount
        this.phoneNumber = phone
        this.orderTime = orderTime
        this.itemPushkey = itemPushKey
        this.orderAccepted = orderAccepted
        this.paymentReceived = paymentReceived
        this.adminUserId = adminUserId
        this.restaurantName = restaurantName
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userUid)
        parcel.writeString(userName)
        parcel.writeStringList(foodNames)
        parcel.writeStringList(foodImages)
        parcel.writeStringList(foodPrices)
        parcel.writeList(foodQuantities)
        parcel.writeString(userLocation)
        parcel.writeString(restaurantLocation)
        parcel.writeString(totalPrice)
        parcel.writeString(phoneNumber)
        parcel.writeByte(if (orderAccepted) 1 else 0)
        parcel.writeByte(if (orderDelivered) 1 else 0)
        parcel.writeByte(if (paymentReceived) 1 else 0)
        parcel.writeString(itemPushkey)
        parcel.writeString(orderTime)
        parcel.writeString(adminUserId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<OrderDetails> {
        override fun createFromParcel(parcel: Parcel): OrderDetails {
            return OrderDetails(parcel)
        }

        override fun newArray(size: Int): Array<OrderDetails?> {
            return arrayOfNulls(size)
        }
    }
}