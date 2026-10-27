package com.example.kingdomguardsassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val targets = mutableListOf<AttackTarget>()
    private lateinit var allianceInput: EditText
    private lateinit var towerInput: EditText
    private lateinit var sideSpinner: Spinner
    private lateinit var intervalInput: EditText
    private lateinit var maxInput: EditText
    private lateinit var targetList: TextView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targets.addAll(PlanStore.loadTargets(this))
        buildUi()
        refreshTargets()
        requestNotificationPermissionIfNeeded()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(label("Kingdom Guards Assistant"))
        content.addView(label("Broken Lands • локальный планировщик / тестовый режим"))
        allianceInput = field("Название альянса")
        towerInput = field("Название башни")
        content.addView(allianceInput)
        content.addView(towerInput)

        content.addView(label("Сторона атаки"))
        sideSpinner = Spinner(this)
        sideSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("LEFT", "RIGHT", "TOP", "BOTTOM")
        )
        content.addView(sideSpinner)

        content.addView(button("Добавить цель") { addTarget() })
        content.addView(button("Удалить все цели") {
            targets.clear()
            persist()
            refreshTargets()
        })

        content.addView(label("Интервал, секунд (30–3600)"))
        intervalInput = field("60").apply { setText(PlanStore.interval(this@MainActivity).toString()) }
        content.addView(intervalInput)

        content.addView(label("Лимит тестовых шагов (1–100)"))
        maxInput = field("10").apply { setText(PlanStore.maxAttacks(this@MainActivity).toString()) }
        content.addView(maxInput)

        content.addView(button("Сохранить настройки") {
            if (persist()) toast("Настройки сохранены на устройстве")
        })
        content.addView(button("Запустить тестовый план") {
            if (persist() && targets.isNotEmpty()) {
                val intent = Intent(this, PlannerService::class.java).setAction(PlannerService.ACTION_START)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
                else startService(intent)
                statusText.text = "Статус: тестовый план запущен. Реальных кликов нет."
            } else toast("Добавь хотя бы одну цель")
        })
        content.addView(button("Остановить план") {
            startService(Intent(this, PlannerService::class.java).setAction(PlannerService.ACTION_STOP))
            statusText.text = "Статус: остановлен"
        })

        content.addView(label("Сохранённые цели"))
        targetList = label("")
        content.addView(targetList)
        statusText = label("Статус: остановлен")
        content.addView(statusText)
        content.addView(label("Важно: эта версия только проверяет расписание и записывает шаги в журнал. Она не нажимает кнопки игры и не выполняет атаки."))
        scroll.addView(content)
        root.addView(scroll, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        setContentView(root)
    }

    private fun addTarget() {
        val alliance = allianceInput.text.toString().trim()
        val tower = towerInput.text.toString().trim()
        if (alliance.isBlank() || tower.isBlank()) {
            toast("Укажи альянс и башню")
            return
        }
        targets.add(AttackTarget(alliance, tower, sideSpinner.selectedItem.toString()))
        allianceInput.text.clear()
        towerInput.text.clear()
        persist()
        refreshTargets()
    }

    private fun persist(): Boolean {
        val interval = intervalInput.text.toString().toIntOrNull()
        val max = maxInput.text.toString().toIntOrNull()
        if (interval == null || interval !in 30..3600) {
            toast("Интервал должен быть от 30 до 3600 секунд")
            return false
        }
        if (max == null || max !in 1..100) {
            toast("Лимит должен быть от 1 до 100")
            return false
        }
        PlanStore.save(this, targets, interval, max)
        return true
    }

    private fun refreshTargets() {
        targetList.text = if (targets.isEmpty()) "Пока нет целей" else targets.mapIndexed { i, t ->
            "${i + 1}. ${t.alliance} • ${t.tower} • ${t.side}"
        }.joinToString("\n")
    }

    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText
        singleLine = true
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setPadding(0, 10, 0, 10)
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        setOnClickListener { action() }
    }

    private fun toast(text: String) =
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
