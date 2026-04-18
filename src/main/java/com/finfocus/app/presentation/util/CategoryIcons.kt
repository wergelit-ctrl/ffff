package com.finfocus.app.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.BookOnline
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.LocalParking
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocalTaxi
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Nightlife
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SmokingRooms
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsVolleyball
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Surfing
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Маппинг iconKey → ImageVector для категорий расходов и счетов.
 * Используется в CategoryPickerDialog и карточках счетов.
 *
 * iconKey — строка, хранится в JSON-контракте.
 * Добавление новых иконок: добавить пару key→Icons.Outlined.XXX.
 */
object CategoryIcons {

    val all: Map<String, ImageVector> = mapOf(
        // Еда и напитки
        "restaurant"          to Icons.Outlined.Restaurant,
        "local_cafe"          to Icons.Outlined.LocalCafe,
        "local_pizza"         to Icons.Outlined.LocalPizza,
        "dinner_dining"       to Icons.Outlined.DinnerDining,
        "ice_cream"           to Icons.Outlined.Icecream,
        "local_bar"           to Icons.Outlined.LocalBar,
        "coffee"              to Icons.Outlined.Coffee,
        "night_life"          to Icons.Outlined.Nightlife,
        // Покупки
        "local_grocery_store" to Icons.Outlined.LocalGroceryStore,
        "shopping_cart"       to Icons.Outlined.ShoppingCart,
        "shopping_bag"        to Icons.Outlined.ShoppingBag,
        "checkroom"           to Icons.Outlined.ShoppingBag,      // одежда
        "inventory"           to Icons.Outlined.Inventory,
        "redeem"              to Icons.Outlined.Redeem,
        // Транспорт
        "directions_bus"      to Icons.Outlined.DirectionsBus,
        "local_taxi"          to Icons.Outlined.LocalTaxi,
        "directions_car"      to Icons.Outlined.DirectionsCar,
        "directions_bike"     to Icons.Outlined.DirectionsBike,
        "train"               to Icons.Outlined.Train,
        "flight"              to Icons.Outlined.Flight,
        "local_shipping"      to Icons.Outlined.LocalShipping,
        "local_parking"       to Icons.Outlined.LocalParking,
        // Жильё и быт
        "home"                to Icons.Outlined.Home,
        "bed"                 to Icons.Outlined.Bed,
        "chair"               to Icons.Outlined.Chair,
        "yard"                to Icons.Outlined.Yard,
        "build"               to Icons.Outlined.Build,
        "local_laundry_service" to Icons.Outlined.LocalLaundryService,
        "electric_bolt"       to Icons.Outlined.ElectricBolt,
        "water_drop"          to Icons.Outlined.WaterDrop,
        // Здоровье
        "local_hospital"      to Icons.Outlined.LocalHospital,
        "medication"          to Icons.Outlined.Medication,
        "local_pharmacy"      to Icons.Outlined.LocalPharmacy,
        "spa"                 to Icons.Outlined.Spa,
        "self_improvement"    to Icons.Outlined.SelfImprovement,
        "favorite"            to Icons.Outlined.Favorite,
        "science"             to Icons.Outlined.Science,
        // Образование
        "school"              to Icons.Outlined.School,
        "menu_book"           to Icons.Outlined.MenuBook,
        "laptop"              to Icons.Outlined.Laptop,
        "calculate"           to Icons.Outlined.Calculate,
        "language"            to Icons.Outlined.Language,
        // Развлечения
        "movie"               to Icons.Outlined.MovieCreation,
        "music_note"          to Icons.Outlined.MusicNote,
        "games"               to Icons.Outlined.Games,
        "tv"                  to Icons.Outlined.Tv,
        "photo_camera"        to Icons.Outlined.PhotoCamera,
        "theater_comedy"      to Icons.Outlined.TheaterComedy,
        "piano"               to Icons.Outlined.Piano,
        "mic"                 to Icons.Outlined.Mic,
        "casino"              to Icons.Outlined.Casino,
        "book_online"         to Icons.Outlined.BookOnline,
        // Спорт
        "fitness_center"      to Icons.Outlined.FitnessCenter,
        "sports_soccer"       to Icons.Outlined.SportsSoccer,
        "sports_basketball"   to Icons.Outlined.SportsBasketball,
        "sports_volleyball"   to Icons.Outlined.SportsVolleyball,
        "pool"                to Icons.Outlined.Pool,
        "surfing"             to Icons.Outlined.Surfing,
        // Связь и технологии
        "phone_iphone"        to Icons.Outlined.Smartphone,
        "phone"               to Icons.Outlined.Phone,
        "wifi"                to Icons.Outlined.Wifi,
        "computer"            to Icons.Outlined.Computer,
        "devices"             to Icons.Outlined.Smartphone,
        "subscriptions"       to Icons.Outlined.Subscriptions,
        // Финансы
        "savings"             to Icons.Outlined.Savings,
        "payments"            to Icons.Outlined.Payments,
        "credit_card"         to Icons.Outlined.CreditCard,
        "account_balance_wallet" to Icons.Outlined.AccountBalanceWallet,
        "attach_money"        to Icons.Outlined.AttachMoney,
        "trending_up"         to Icons.Outlined.TrendingUp,
        "bar_chart"           to Icons.Outlined.BarChart,
        // Люди и подарки
        "card_giftcard"       to Icons.Outlined.CardGiftcard,
        "people"              to Icons.Outlined.People,
        "child_care"          to Icons.Outlined.ChildCare,
        "volunteer_activism"  to Icons.Outlined.VolunteerActivism,
        "support_agent"       to Icons.Outlined.SupportAgent,
        // Разное
        "work"                to Icons.Outlined.Work,
        "pets"                to Icons.Outlined.Pets,
        "star"                to Icons.Outlined.Star,
        "more_horiz"          to Icons.Outlined.Category,
        "category"            to Icons.Outlined.Category,
        "face"                to Icons.Outlined.SelfImprovement,
        "watch"               to Icons.Outlined.Watch,
        "smoking_rooms"       to Icons.Outlined.SmokingRooms,
        "park"                to Icons.Outlined.Park,
        "forest"              to Icons.Outlined.Forest,
        "hotel"               to Icons.Outlined.Hotel,
        "mosque"              to Icons.Outlined.Mosque,
        "autorenew"           to Icons.Outlined.Autorenew,
        "rocket_launch"       to Icons.Outlined.RocketLaunch,
        "security"            to Icons.Outlined.Security,
        "print"               to Icons.Outlined.Print,
        "cake"                to Icons.Outlined.Cake,
    )

    /** Возвращает иконку по ключу, или Category как fallback. */
    fun get(key: String): ImageVector = all[key] ?: Icons.Outlined.Category

    /** Список всех доступных ключей для пикера иконок. */
    val keys: List<String> get() = all.keys.toList()
}

/** Иконки для счетов — 30 вариантов. */
object AccountIcons {
    val all: Map<String, ImageVector> = mapOf(
        "wallet"              to Icons.Outlined.AccountBalanceWallet,
        "credit_card"         to Icons.Outlined.CreditCard,
        "savings"             to Icons.Outlined.Savings,
        "payments"            to Icons.Outlined.Payments,
        "attach_money"        to Icons.Outlined.AttachMoney,
        "work"                to Icons.Outlined.Work,
        "shopping_bag"        to Icons.Outlined.ShoppingBag,
        "local_grocery_store" to Icons.Outlined.LocalGroceryStore,
        "home"                to Icons.Outlined.Home,
        "directions_car"      to Icons.Outlined.DirectionsCar,
        "flight"              to Icons.Outlined.Flight,
        "hotel"               to Icons.Outlined.Hotel,
        "star"                to Icons.Outlined.Star,
        "favorite"            to Icons.Outlined.Favorite,
        "rocket_launch"       to Icons.Outlined.RocketLaunch,
        "trending_up"         to Icons.Outlined.TrendingUp,
        "bar_chart"           to Icons.Outlined.BarChart,
        "security"            to Icons.Outlined.Security,
        "school"              to Icons.Outlined.School,
        "fitness_center"      to Icons.Outlined.FitnessCenter,
        "local_cafe"          to Icons.Outlined.LocalCafe,
        "restaurant"          to Icons.Outlined.Restaurant,
        "phone"               to Icons.Outlined.Phone,
        "laptop"              to Icons.Outlined.Laptop,
        "pets"                to Icons.Outlined.Pets,
        "child_care"          to Icons.Outlined.ChildCare,
        "volunteer_activism"  to Icons.Outlined.VolunteerActivism,
        "music_note"          to Icons.Outlined.MusicNote,
        "park"                to Icons.Outlined.Park,
        "inventory"           to Icons.Outlined.Inventory,
    )

    fun get(key: String): ImageVector = all[key] ?: Icons.Outlined.AccountBalanceWallet
    val keys: List<String> get() = all.keys.toList()
}
