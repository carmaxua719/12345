package com.example.kingdomguardsassistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PlanStore {
    private const val PREFS = "kg_assistant_local"
    private const val KEY_TARGETS = "targets"
    private const val KEY_INTERVAL = "interval"
    private const val KEY_MAX = "max_attacks"

    fun save(context: Context, targets: List<AttackTarget>, intervalSeconds: Int, maxAttacks: Int) {
        val array = JSONArray()
        targets.forEach { target ->
            array.put(JSONObject().apply {
                put("alliance", target.alliance)
                put("tower", target.tower)
                put("side", target.side)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TARGETS, array.toString())
            .putInt(KEY_INTERVAL, intervalSeconds)
            .putInt(KEY_MAX, maxAttacks)
            .apply()
    }

    fun loadTargets(context: Context): List<AttackTarget> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TARGETS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val alliance = obj.optString("alliance").trim()
                val tower = obj.optString("tower").trim()
                val side = obj.optString("side", "LEFT")
                if (alliance.isBlank() || tower.isBlank()) null
                else AttackTarget(alliance, tower, side)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun interval(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_INTERVAL, 60)

    fun maxAttacks(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MAX, 10)
}
