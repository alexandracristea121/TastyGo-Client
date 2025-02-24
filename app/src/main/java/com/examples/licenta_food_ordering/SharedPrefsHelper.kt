import android.content.Context

object SharedPrefsHelper {

    private const val PREFS_NAME = "food_ordering_prefs"
    private const val KEY_ADMIN_USER_ID = "admin_user_id"

    // Save admin user ID
    fun saveAdminUserId(context: Context, adminUserId: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_ADMIN_USER_ID, adminUserId)
        editor.apply()
    }

    // Get admin user ID
    fun getAdminUserId(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ADMIN_USER_ID, null)
    }
}