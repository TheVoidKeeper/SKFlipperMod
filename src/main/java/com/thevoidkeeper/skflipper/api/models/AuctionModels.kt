package com.thevoidkeeper.skflipper.api

data class AuctionsResponse(
    val success: Boolean,
    val page: Int,
    val totalPages: Int,
    val auctions: List<Auction>?
)

data class Auction(
    val uuid: String,
    val item_name: String?,
    val tier: String?,
    val starting_bid: Long,
    val highest_bid_amount: Long,
    val bin: Boolean?
)
