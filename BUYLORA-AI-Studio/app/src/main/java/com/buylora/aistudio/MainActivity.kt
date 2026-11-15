package com.buylora.aistudio

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var tokenStore: SecureTokenStore
    private lateinit var root: LinearLayout
    private lateinit var prompt: EditText
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var generate: Button
    private lateinit var tokenInput: EditText
    private lateinit var moduleInfo: TextView
    private var selectedModule = "AI Image Generator"

    private val modules = listOf(
        "AI Image Generator" to "Generate an image from a text prompt using Hugging Face Inference Providers.",
        "Product Photoshoot" to "Create polished studio, lifestyle, hero-banner and product-ad concepts.",
        "Brandkit Studio" to "Build a brand direction: palette, typography, logo brief and social identity.",
        "Marketing Studio" to "Prepare UGC, unboxing, product showcase and commercial-video concepts.",
        "Faceless Video" to "Create a scene-by-scene explainer or story production brief.",
        "YouTube Thumbnail" to "Create a truthful, high-contrast thumbnail concept and text direction.",
        "Marketplace Cards" to "Plan compliant main images, secondary product cards and feature modules.",
        "Soul ID Workflow" to "Prepare an identity-consistency workflow; identity training needs a supported provider.",
        "Website Studio" to "Create a landing-page structure, visual direction and asset checklist.",
        "3D & Audio Lab" to "Prepare a 3D asset or sound design brief; live generation needs a compatible provider."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenStore = SecureTokenStore(this)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 14f, color: Int = Color.WHITE): TextView = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color)
    }
    private fun button(value: String, action: () -> Unit): Button = Button(this).apply {
        text = value; isAllCaps = false; setOnClickListener { action() }
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(15, 18, 37)) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(28)) }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(text("BUYLORA", 13f, Color.rgb(155, 160, 255)))
        root.addView(text("AI STUDIO", 30f).apply { setTypeface(null, 1) })
        root.addView(text("Smart picks. Simple buying.", 13f, Color.LTGRAY))
        root.addView(text("One workspace for creative workflows", 15f).apply { setPadding(0, dp(12), 0, dp(8)) })

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modules.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            pair.forEach { (name, _) ->
                val b = button(name) { selectModule(name) }
                row.addView(b, LinearLayout.LayoutParams(0, dp(54), 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, dp(54), 1f))
            grid.addView(row)
        }
        root.addView(grid)

        moduleInfo = text(modules.first().second, 14f, Color.LTGRAY).apply { setPadding(0, dp(12), 0, dp(10)) }
        root.addView(moduleInfo)
        root.addView(text("PROMPT", 12f, Color.rgb(155, 160, 255)))
        prompt = EditText(this).apply {
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); hint = "Describe what you want to create…"
            minLines = 4; gravity = Gravity.TOP; setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(31, 35, 60))
        }
        root.addView(prompt, LinearLayout.LayoutParams(-1, dp(125)).apply { topMargin = dp(6) })

        generate = button("Generate image (free-tier provider)") { generateImage() }
        root.addView(generate, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        root.addView(button("Open Higgsfield website") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://higgsfield.ai")))
        })
        root.addView(text("Provider setup", 19f).apply { setPadding(0, dp(18), 0, dp(4)) })
        root.addView(text("For free-mode image generation, add a Hugging Face token with Inference permissions. The token is encrypted on this device. Never paste it into chat.", 13f, Color.LTGRAY))
        tokenInput = EditText(this).apply {
            hint = "hf_… token"; setSingleLine(true); inputType = 0x00000081
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.rgb(31, 35, 60))
        }
        root.addView(tokenInput, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
        root.addView(button("Save token securely") { saveToken() })
        root.addView(button("Clear saved token") { tokenStore.clear(); tokenInput.text.clear(); status.text = "Saved token cleared from this device." })
        root.addView(button("Create Hugging Face token") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/settings/tokens")))
        })
        status = text("Ready. Select a module to begin.", 13f, Color.rgb(190, 210, 190)).apply { setPadding(0, dp(12), 0, dp(8)) }
        root.addView(status)
        preview = ImageView(this).apply { adjustViewBounds = true; scaleType = ImageView.ScaleType.FIT_CENTER }
        root.addView(preview, LinearLayout.LayoutParams(-1, dp(300)))
        root.addView(text("Important: free Hugging Face credits are limited and can change. Higgsfield account login does not grant free Higgsfield API credits. Other modules currently create production briefs/prompts; they are not falsely presented as live generators.", 12f, Color.LTGRAY).apply { setPadding(0, dp(12), 0, 0) })
    }

    private fun selectModule(name: String) {
        selectedModule = name
        val desc = modules.first { it.first == name }.second
        moduleInfo.text = desc
        prompt.setText(when (name) {
            "Product Photoshoot" -> "Premium product photo of [product], studio lighting, clean background, realistic material details, commercial photography."
            "Brandkit Studio" -> "Create a coherent visual identity for [brand]: brand positioning, palette, typography, logo direction, packaging and social templates."
            "Marketing Studio" -> "Create a short product showcase ad for [product], with hook, scene sequence, camera direction, on-screen text and CTA."
            "Faceless Video" -> "Create a faceless explainer about [topic], with narration, scene-by-scene visual prompts and sound design notes."
            "YouTube Thumbnail" -> "Design a truthful YouTube thumbnail concept for [video topic], one clear focal subject, readable short headline, strong contrast."
            "Marketplace Cards" -> "Create a compliant marketplace image set for [product], main image, feature callouts and secondary lifestyle cards."
            "Soul ID Workflow" -> "Prepare a consent-based consistent fictional character workflow for [character description]."
            "Website Studio" -> "Plan a responsive landing page for [business], sections, copy, visual system and required assets."
            "3D & Audio Lab" -> "Create a 3D asset and audio brief for [project], with style, format and production requirements."
            else -> "A cinematic editorial image of [subject], premium lighting, intentional composition, high detail."
        })
        generate.text = if (name == "AI Image Generator") "Generate image (free-tier provider)" else "Create workflow brief"
        status.text = if (name == "AI Image Generator") "Live image generation is available through Hugging Face when the selected model/provider and account credits permit." else "This module prepares a reusable creative brief. Live media generation needs a compatible provider/API."
    }

    private fun saveToken() {
        val value = tokenInput.text.toString().trim()
        if (!value.startsWith("hf_") || value.length < 15) {
            status.text = "Enter a valid-looking Hugging Face token. Create one with Inference permissions."; return
        }
        runCatching { tokenStore.save(value) }
            .onSuccess { tokenInput.text.clear(); status.text = "Token encrypted and saved on this device." }
            .onFailure { status.text = "Could not securely save token: ${it.message}" }
    }

    private fun generateImage() {
        if (selectedModule != "AI Image Generator") {
            val brief = prompt.text.toString().trim()
            if (brief.isBlank()) { status.text = "Write a short brief first."; return }
            val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "BUYLORA AI Studio — $selectedModule\n\n$brief\n\nUse this brief with a compatible provider. Higgsfield official website: https://higgsfield.ai") }
            startActivity(Intent.createChooser(share, "Export creative brief"))
            status.text = "Brief ready to share. Live generation is not enabled for this module yet."
            return
        }
        val token = tokenStore.read()
        if (token.isNullOrBlank()) { status.text = "Save your Hugging Face token first."; return }
        val userPrompt = prompt.text.toString().trim()
        if (userPrompt.isBlank()) { status.text = "Enter an image prompt first."; return }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(prompt.windowToken, 0)
        generate.isEnabled = false
        status.text = "Sending image request… Provider availability and free credits may limit this request."
        worker.execute {
            var connection: HttpURLConnection? = null
            try {
                val endpoint = URL("https://router.huggingface.co/hf-inference/models/stabilityai/stable-diffusion-3-medium-diffusers")
                val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 30_000; readTimeout = 180_000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "image/png")
                }
                connection = conn
                val payload = JSONObject().put("inputs", userPrompt).toString().toByteArray(Charsets.UTF_8)
                conn.outputStream.use { it.write(payload) }
                val code = conn.responseCode
                if (code !in 200..299) {
                    val error = (if (code >= 400) conn.errorStream else conn.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty().take(700)
                    throw IllegalStateException("Provider returned HTTP $code. ${friendlyError(code, error)}")
                }
                val bytes = conn.inputStream.use { input ->
                    val out = ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
                    while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 15 * 1024 * 1024) { "Image response exceeded 15 MB limit" }; out.write(buffer, 0, n) }
                    out.toByteArray()
                }
                require(bytes.isNotEmpty()) { "Provider returned an empty image." }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Response was not a valid image. Check model/provider availability.")
                val uri = saveImage(bytes)
                runOnUiThread {
                    preview.setImageBitmap(bitmap)
                    status.text = "Image generated and saved to Pictures/BUYLORA AI Studio. $uri"
                    generate.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = when {
                        e.message?.contains("HTTP 401") == true -> "Token rejected. Check that it is valid and has Inference permissions."
                        e.message?.contains("HTTP 402") == true -> "No usable credits for this request. Check your Hugging Face billing/credits."
                        else -> "Generation failed: ${e.message ?: e.javaClass.simpleName}"
                    }
                    generate.isEnabled = true
                }
            } finally { connection?.disconnect() }
        }
    }

    private fun friendlyError(code: Int, body: String): String = when (code) {
        401 -> "Authentication failed; verify token permissions."
        402 -> "Provider credits or billing are unavailable."
        404 -> "This model is not currently served by this provider."
        429 -> "Rate limit or provider capacity reached; retry later."
        503 -> "Model is loading or temporarily unavailable."
        else -> body.ifBlank { "No additional error details." }
    }

    private fun saveImage(bytes: ByteArray): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "BUYLORA_AI_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/BUYLORA AI Studio")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Could not create media file")
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write media file")
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) { contentResolver.delete(uri, null, null); throw e }
    }

    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
