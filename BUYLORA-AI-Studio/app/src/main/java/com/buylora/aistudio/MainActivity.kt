package com.buylora.aistudio

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Zero-budget, offline-first creative workspace.
 * This version creates and manages reusable creative briefs locally.
 * It does not claim to render images/video/audio without a model/runtime.
 */
class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var prompt: EditText
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var moduleInfo: TextView
    private lateinit var createButton: Button
    private var selectedModule = "AI Image Generator"
    private val prefs by lazy { getSharedPreferences("buylora_offline_studio", Context.MODE_PRIVATE) }

    private val modules = listOf(
        "AI Image Generator" to "Offline image-prompt builder. Creates a detailed prompt; it does not render an image.",
        "Product Photoshoot" to "Build studio, lifestyle, hero-banner and product-ad photography briefs.",
        "Brandkit Studio" to "Create a brand direction: positioning, palette, typography and identity brief.",
        "Marketing Studio" to "Prepare UGC, unboxing, product showcase and commercial campaign briefs.",
        "Faceless Video" to "Create a scene-by-scene explainer or story production plan.",
        "YouTube Thumbnail" to "Create a truthful, high-contrast thumbnail concept and text direction.",
        "Marketplace Cards" to "Plan main images, secondary product cards and feature modules.",
        "Soul ID Workflow" to "Prepare a consent-based character consistency brief. No face training is performed.",
        "Website Studio" to "Create a landing-page structure, visual direction and asset checklist.",
        "3D & Audio Lab" to "Prepare 3D asset and sound design briefs. No media is rendered."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 14f, color: Int = Color.WHITE): TextView =
        TextView(this).apply { text = value; textSize = size; setTextColor(color) }

    private fun button(value: String, action: () -> Unit): Button =
        Button(this).apply { text = value; isAllCaps = false; setOnClickListener { action() } }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(15, 18, 37)) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("BUYLORA", 13f, Color.rgb(155, 160, 255)))
        root.addView(text("AI STUDIO", 30f).apply { setTypeface(null, 1) })
        root.addView(text("Smart picks. Simple buying.", 13f, Color.LTGRAY))
        root.addView(text("Offline creative workspace • No paid API required", 14f, Color.rgb(190, 210, 190)).apply {
            setPadding(0, dp(12), 0, dp(8))
        })

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modules.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (name, _) ->
                val b = button(name) { selectModule(name) }
                row.addView(b, LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(54), 1f))
            grid.addView(row)
        }
        root.addView(grid)

        moduleInfo = text(modules.first().second, 14f, Color.LTGRAY).apply {
            setPadding(0, dp(12), 0, dp(10))
        }
        root.addView(moduleInfo)
        root.addView(text("YOUR INPUT", 12f, Color.rgb(155, 160, 255)))
        prompt = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            hint = "Describe the product, brand, video or idea…"
            minLines = 4
            gravity = Gravity.TOP
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(31, 35, 60))
        }
        root.addView(prompt, LinearLayout.LayoutParams(-1, dp(125)).apply { topMargin = dp(6) })

        createButton = button("Create offline brief") { createBrief() }
        root.addView(createButton, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        root.addView(button("Copy brief") { copyBrief() })
        root.addView(button("Share brief") { shareBrief() })
        root.addView(button("Save brief on this device") { saveBrief() })
        root.addView(button("View saved briefs") { showSavedBriefs() })

        root.addView(text("RESULT", 12f, Color.rgb(155, 160, 255)).apply {
            setPadding(0, dp(16), 0, dp(6))
        })
        output = text("Your generated creative brief will appear here.", 14f, Color.LTGRAY).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(31, 35, 60))
        }
        root.addView(output, LinearLayout.LayoutParams(-1, -2).apply { minHeight = dp(150) })
        status = text("Ready. All brief creation and saving work offline.", 13f, Color.rgb(190, 210, 190)).apply {
            setPadding(0, dp(12), 0, dp(8))
        }
        root.addView(status)
        root.addView(text(
            "Honest limitation: this zero-budget build creates prompts, plans and briefs locally. It does not generate image, video, audio or 3D files. Real on-device image generation requires a compatible model download and sufficient phone memory/processing power; no such model is bundled in this build.",
            12f, Color.LTGRAY
        ).apply { setPadding(0, dp(14), 0, 0) })
    }

    private fun selectModule(name: String) {
        selectedModule = name
        moduleInfo.text = modules.first { it.first == name }.second
        prompt.setText(when (name) {
            "AI Image Generator" -> "A premium commercial image of [subject], clear composition, professional lighting, realistic materials, refined color palette."
            "Product Photoshoot" -> "Product: [name]. Create hero, close-up, lifestyle and detail-shot concepts with realistic materials and clean studio lighting."
            "Brandkit Studio" -> "Brand: [name]. Audience: [audience]. Positioning: [positioning]. Build a coherent visual identity."
            "Marketing Studio" -> "Product: [name]. Audience: [audience]. Offer: [offer]. Create a campaign concept with hook, proof and CTA."
            "Faceless Video" -> "Topic: [topic]. Audience: [audience]. Duration: [duration]. Create a narration-led scene plan."
            "YouTube Thumbnail" -> "Video topic: [topic]. Main subject: [subject]. Create a truthful thumbnail concept with a short readable headline."
            "Marketplace Cards" -> "Product: [name]. Features: [features]. Create a compliant main image and secondary feature-card plan."
            "Soul ID Workflow" -> "Character description: [fictional character]. Consent/rights confirmed: [yes]. Define a consistent visual reference brief."
            "Website Studio" -> "Business: [name]. Audience: [audience]. Main offer: [offer]. Plan a mobile-first landing page."
            else -> "Project: [description]. Target format: [format]. Style references: [style]. Prepare a 3D/audio production brief."
        })
        output.text = "Tap Create offline brief to build your result."
        status.text = "Selected: $name. This module works offline."
    }

    private fun createBrief() {
        val input = prompt.text.toString().trim()
        if (input.isBlank()) {
            status.text = "Add a product, brand or project description first."
            return
        }
        val brief = when (selectedModule) {
            "AI Image Generator" -> """
                IMAGE GENERATION PROMPT
                Subject: $input

                Style direction: premium commercial photography, intentional composition, controlled professional lighting, realistic materials and refined color harmony.
                Quality checks: clear focal point, readable silhouette, natural proportions, no unintended text, no watermark.
                Output note: This is a ready-to-use prompt, not a rendered image.
            """.trimIndent()
            "Product Photoshoot" -> """
                PRODUCT PHOTOSHOOT BRIEF
                $input

                Shot list:
                1. Clean hero image on a simple background
                2. Three-quarter product angle
                3. Material/detail close-up
                4. Lifestyle context image
                5. Feature-focused secondary image

                Keep product proportions, colors and branding accurate. Do not invent product claims.
            """.trimIndent()
            "Brandkit Studio" -> """
                BRANDKIT BRIEF
                $input

                Deliverables: positioning statement, audience profile, personality keywords, primary/secondary palette, typography direction, logo usage brief, social post direction and do/don't rules.
            """.trimIndent()
            "Marketing Studio" -> """
                MARKETING CAMPAIGN BRIEF
                $input

                Structure: opening hook → customer problem → product demonstration → substantiated benefit → clear call to action.
                Prepare 3 hook options, a short caption, shot list and platform-specific variants. Avoid unsupported claims.
            """.trimIndent()
            "Faceless Video" -> """
                FACELESS VIDEO PRODUCTION PLAN
                $input

                Structure: hook, context, 3 key beats, conclusion and CTA.
                For each scene define narration, on-screen text, visual direction, approximate duration and sound cue.
            """.trimIndent()
            "YouTube Thumbnail" -> """
                YOUTUBE THUMBNAIL BRIEF
                $input

                Direction: one focal subject, one short headline, strong subject/background separation and mobile-first readability.
                Do not imply results or events that the video does not contain.
            """.trimIndent()
            "Marketplace Cards" -> """
                MARKETPLACE CREATIVE BRIEF
                $input

                Plan: compliant primary product image, feature overview, dimensions/specifications, use-case image and what's-in-the-box card.
                Keep factual claims verifiable and follow the marketplace's current image rules.
            """.trimIndent()
            "Soul ID Workflow" -> """
                CHARACTER CONSISTENCY BRIEF
                $input

                Define: approved character description, fixed visual traits, wardrobe range, allowed scene variation and reference checklist.
                Use only characters and likenesses you have permission to use. This app does not train or clone a face.
            """.trimIndent()
            "Website Studio" -> """
                WEBSITE STRUCTURE BRIEF
                $input

                Sections: hero, value proposition, services/features, proof, process, FAQ and contact CTA.
                Include mobile layout notes, content checklist and required visual assets.
            """.trimIndent()
            else -> """
                3D & AUDIO PRODUCTION BRIEF
                $input

                Define target platform, output format, dimensions/duration, style, required assets, technical constraints and review checklist.
                This brief does not render a 3D model or audio file.
            """.trimIndent()
        }
        output.text = brief
        status.text = "Brief created locally. No API, token, subscription or internet required."
    }

    private fun currentBrief(): String = output.text.toString().trim()

    private fun copyBrief() {
        val value = currentBrief()
        if (value.isBlank() || value.startsWith("Your generated")) {
            status.text = "Create a brief before copying."
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BUYLORA creative brief", value))
        status.text = "Brief copied."
    }

    private fun shareBrief() {
        val value = currentBrief()
        if (value.isBlank() || value.startsWith("Your generated")) {
            status.text = "Create a brief before sharing."
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "BUYLORA AI Studio — $selectedModule\n\n$value")
        }
        startActivity(Intent.createChooser(intent, "Share creative brief"))
    }

    private fun saveBrief() {
        val value = currentBrief()
        if (value.isBlank() || value.startsWith("Your generated")) {
            status.text = "Create a brief before saving."
            return
        }
        val list = readSaved()
        list.put(JSONObject().put("module", selectedModule).put("brief", value).put("savedAt", System.currentTimeMillis()))
        prefs.edit().putString("saved_briefs", list.toString()).apply()
        status.text = "Brief saved privately on this device."
    }

    private fun readSaved(): JSONArray = runCatching {
        JSONArray(prefs.getString("saved_briefs", "[]"))
    }.getOrElse { JSONArray() }

    private fun showSavedBriefs() {
        val list = readSaved()
        if (list.length() == 0) {
            status.text = "No saved briefs yet."
            return
        }
        val labels = (0 until list.length()).map { i ->
            val item = list.optJSONObject(i) ?: JSONObject()
            "${i + 1}. ${item.optString("module", "Creative brief")}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Saved briefs")
            .setItems(labels) { _, which ->
                val item = list.optJSONObject(which) ?: return@setItems
                selectedModule = item.optString("module", selectedModule)
                output.text = item.optString("brief", "")
                status.text = "Loaded saved brief."
            }
            .setNegativeButton("Close", null)
            .show()
    }
}
