package com.examples.licenta_food_ordering.model

data class MenuItem(
    val key: String? = null,
    val foodName: String? = null,
    val foodPrice: String? = null,
    val foodDescription: String? = null,
    val foodImage: String? = null,
    val foodIngredient: String? = null,
    val foodCategory: String? = null,
    val restaurantName: String? = null,
    val category: String? = null,

    // Local use only (not saved in Firebase)
    val isCategoryHeader: Boolean = false
)