package com.ooruva.app.data.mock

import com.ooruva.app.data.models.Vendor

/**
 * Seed data for design work and for the repositories to fall back on before the
 * backend exists. Clearly marked as mock so nothing here can be mistaken for
 * live platform data.
 */
fun getMockVendors(): List<Vendor> = listOf(
    Vendor(
        id = "1", name = "Chai Wali", category = "CHAI",
        description = "Authentic Indian tea & snacks",
        latitude = 13.0827, longitude = 80.2707,
        address = "Main Street", phone = "9876543210",
        hours = "06:00-22:00", rating = 4.5f, reviewCount = 48, photoUrl = ""
    ),
    Vendor(
        id = "2", name = "Street Samosa", category = "FOOD",
        description = "Crispy samosas & pakora",
        latitude = 13.0835, longitude = 80.2715,
        address = "Market Road", phone = "9876543211",
        hours = "11:00-20:00", rating = 4.8f, reviewCount = 62, photoUrl = ""
    ),
    Vendor(
        id = "3", name = "Mobile Repair Pro", category = "SERVICE",
        description = "Phone repair & accessories",
        latitude = 13.0820, longitude = 80.2690,
        address = "Tech Lane", phone = "9876543212",
        hours = "09:00-19:00", rating = 4.2f, reviewCount = 35, photoUrl = ""
    ),
    Vendor(
        id = "4", name = "Beauty Studio", category = "SALON",
        description = "Hair & makeup services",
        latitude = 13.0840, longitude = 80.2720,
        address = "Fashion Street", phone = "9876543213",
        hours = "10:00-20:00", rating = 4.6f, reviewCount = 78, photoUrl = ""
    ),
    Vendor(
        id = "5", name = "Fresh Juice Corner", category = "JUICE",
        description = "Natural fruit juices",
        latitude = 13.0815, longitude = 80.2700,
        address = "Health Avenue", phone = "9876543214",
        hours = "07:00-19:00", rating = 4.7f, reviewCount = 92, photoUrl = ""
    )
)

