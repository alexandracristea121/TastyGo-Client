import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import com.examples.licenta_food_ordering.service.courier.FirebaseCourierService
import com.google.android.gms.maps.model.LatLng

// CourierTrackingService.kt - Serviciul GPS care trimite locatia in Firebase
class CourierTrackingService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, this)
    }

    override fun onLocationChanged(location: Location) {
        val courierId = "courier_123" // Ar trebui sa fie unic pentru fiecare curier
        val firebaseCourierService = FirebaseCourierService()
        firebaseCourierService.updateCourierLocation(courierId, LatLng(location.latitude, location.longitude))
    }

    override fun onBind(intent: Intent?): IBinder? = null
}