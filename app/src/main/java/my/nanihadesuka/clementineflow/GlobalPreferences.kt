package my.nanihadesuka.clementineflow

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

enum class THEME_TYPE
{
    LIGHT, BLACK
}

var SharedPreferences.THEME_FOLLOW_SYSTEM by PreferenceDelegate_Boolean(true)
var SharedPreferences.THEME_TYPE by PreferenceDelegate_Enum(THEME_TYPE.BLACK) { enumValueOf(it) }
var SharedPreferences.REMOTE_IP by PreferenceDelegate_String("")
var SharedPreferences.REMOTE_PORT by PreferenceDelegate_Int(5500)
var SharedPreferences.REMOTE_AUTHCODE by PreferenceDelegate_Int(-1)
var SharedPreferences.REMOTE_NEEDS_AUTHCODE by PreferenceDelegate_Boolean(true)

fun SharedPreferences.THEME_FOLLOW_SYSTEM_flow() = toFlow(::THEME_FOLLOW_SYSTEM.name) { THEME_FOLLOW_SYSTEM }
fun SharedPreferences.THEME_TYPE_flow() = toFlow(::THEME_TYPE.name) { THEME_TYPE }
fun SharedPreferences.REMOTE_IP_flow() = toFlow(::REMOTE_IP.name) { REMOTE_IP }
fun SharedPreferences.REMOTE_PORT_flow() = toFlow(::REMOTE_PORT.name) { REMOTE_PORT }
fun SharedPreferences.REMOTE_AUTHCODE_flow() = toFlow(::REMOTE_AUTHCODE.name) { REMOTE_AUTHCODE }
fun SharedPreferences.REMOTE_NEEDS_AUTHCODE_flow() = toFlow(::REMOTE_NEEDS_AUTHCODE.name) { REMOTE_NEEDS_AUTHCODE }

fun Context.appSharedPreferences(): SharedPreferences =
    applicationContext.getSharedPreferences("${this.packageName}_preferences", Context.MODE_PRIVATE)

fun <T> SharedPreferences.toFlow(key: String, mapper: (String) -> T): Flow<T>
{
    val flow = MutableStateFlow(mapper(key))
    val scope = CoroutineScope(Dispatchers.Default)
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, vkey ->
        if (key == vkey)
            scope.launch { flow.value = mapper(vkey) }
    }

    return flow
        .onSubscription {
            App.instance.preferencesChangeListeners.add(listener)
            registerOnSharedPreferenceChangeListener(listener)
        }.onCompletion {
            App.instance.preferencesChangeListeners.remove(listener)
            unregisterOnSharedPreferenceChangeListener(listener)
        }.flowOn(Dispatchers.Default)
}

class PreferenceDelegate_Enum<T : Enum<T>>(val defaultValue: T, val deserializer: (String) -> T)
{
    operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>): T =
        thisRef.getString(property.name, null)?.let { kotlin.runCatching { deserializer(it) }.getOrNull() } ?: defaultValue

    operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: T) =
        thisRef.edit().putString(property.name, value.name).apply()
}

class PreferenceDelegate_Int(val defaultValue: Int)
{
    operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getInt(property.name, defaultValue)
    operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Int) =
        thisRef.edit().putInt(property.name, value).apply()
}

class PreferenceDelegate_Float(val defaultValue: Float)
{
    operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getFloat(property.name, defaultValue)
    operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Float) =
        thisRef.edit().putFloat(property.name, value).apply()
}

class PreferenceDelegate_String(val defaultValue: String)
{
    operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getString(property.name, null) ?: defaultValue
    operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: String?) =
        thisRef.edit().putString(property.name, value).apply()
}

class PreferenceDelegate_StringSet(val defaultValue: Set<String>)
{
    operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getStringSet(property.name, null)?.toSet() ?: defaultValue
    operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Set<String>?) =
        thisRef.edit().putStringSet(property.name, value).apply()
}

class PreferenceDelegate_Boolean(val defaultValue: Boolean)
{
    operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getBoolean(property.name, defaultValue)
    operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Boolean) =
        thisRef.edit().putBoolean(property.name, value).apply()
}