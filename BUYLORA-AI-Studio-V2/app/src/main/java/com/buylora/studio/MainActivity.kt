package com.buylora.studio

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val modules = listOf("AI Image Generator","Product Photoshoot","Brandkit Studio","Marketing Studio","Faceless Video","YouTube Thumbnail","Marketplace Cards","Soul ID Workflow","Website Studio","3D & Audio Lab")
    private val prefs by lazy { getSharedPreferences("buylora_v2", Context.MODE_PRIVATE) }
    private lateinit var prompt: EditText
    private lateinit var result: TextView
    private lateinit var status: TextView
    private lateinit var selected: TextView
    private lateinit var apiKeyInput: EditText
    private var active = modules[0]
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
    }
    override fun onInit(code: Int) {
        ttsReady = code == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun label(s: String, size: Float = 14f, color: Int = Color.WHITE) =
        TextView(this).apply { text=s; textSize=size; setTextColor(color) }
    private fun btn(s: String, action: () -> Unit) =
        Button(this).apply { text=s; isAllCaps=false; setOnClickListener { action() } }

    private fun buildUi() {
        val scroll=ScrollView(this).apply { setBackgroundColor(Color.rgb(15,18,37)) }
        val page=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(20),dp(18),dp(28)) }
        scroll.addView(page); setContentView(scroll)
        page.addView(label("BUYLORA",13f,Color.rgb(155,160,255)))
        page.addView(label("AI STUDIO",32f).apply { setTypeface(null,1) })
        page.addView(label("Smart picks. Simple buying.",15f,Color.LTGRAY))
        page.addView(label("V2 • Free AI + offline creative workspace",13f,Color.rgb(180,215,185)).apply { setPadding(0,dp(12),0,dp(12)) })
        page.addView(label("OPENROUTER FREE AI SETUP",12f,Color.rgb(155,160,255)).apply { setPadding(0,dp(8),0,dp(4)) })
        apiKeyInput=EditText(this).apply {
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); hint="OpenRouter API key (sk-or-...)"
            inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true); setPadding(dp(12),dp(10),dp(12),dp(10)); setBackgroundColor(Color.rgb(31,35,60))
        }
        page.addView(apiKeyInput,LinearLayout.LayoutParams(-1,dp(54)))
        page.addView(btn("Save API key securely on this phone"){ saveApiKey() })
        page.addView(btn("Clear saved API key"){ clearApiKey() })
        page.addView(label("Uses only OpenRouter's openrouter/free router. No paid model fallback is configured. Free availability and daily limits apply.",12f,Color.LTGRAY))
        modules.chunked(2).forEach { pair ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            pair.forEach { name -> row.addView(btn(name){ choose(name) },LinearLayout.LayoutParams(0,dp(54),1f).apply { setMargins(dp(3),dp(3),dp(3),dp(3)) }) }
            page.addView(row)
        }
        selected=label("Selected: "+active,16f,Color.rgb(155,160,255)).apply { setPadding(0,dp(14),0,dp(6)) }
        page.addView(selected); page.addView(label("PROJECT / PROMPT",12f,Color.rgb(155,160,255)))
        prompt=EditText(this).apply {
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); hint="Describe the product, brand, character or idea..."
            minLines=4; gravity=Gravity.TOP; setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(Color.rgb(31,35,60))
        }
        page.addView(prompt,LinearLayout.LayoutParams(-1,dp(132)).apply { topMargin=dp(6) })
        page.addView(btn("Ask Free AI (OpenRouter)"){ askFreeAi() })
        page.addView(btn("Create offline content / production plan"){ makeOutput() })
        page.addView(btn("Render design PNG (poster / thumbnail / card)"){ renderPng() })
        page.addView(btn("Copy result"){ copyResult() })
        page.addView(btn("Share result"){ shareResult() })
        page.addView(btn("Speak narration (device TTS)"){ speakResult() })
        page.addView(btn("Save project"){ saveProject() })
        page.addView(btn("Open saved projects"){ openProjects() })
        page.addView(label("OUTPUT",12f,Color.rgb(155,160,255)).apply { setPadding(0,dp(16),0,dp(6)) })
        result=label("Your result will appear here.",14f,Color.LTGRAY).apply { setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(Color.rgb(31,35,60)) }
        page.addView(result,LinearLayout.LayoutParams(-1,-2).apply { minimumHeight=dp(180) })
        status=label("Ready. Add your OpenRouter key to use free text AI.",13f,Color.rgb(190,215,190)).apply { setPadding(0,dp(12),0,dp(8)) }
        page.addView(status)
        page.addView(label("Honest limits: this build renders designed graphics as PNG and creates offline content. It does not yet synthesize photorealistic humans or new product photos. That needs a compatible on-device diffusion model and native runtime; multi-GB model weights are not bundled. Video currently creates storyboards, not MP4 files.",12f,Color.LTGRAY).apply { setPadding(0,dp(14),0,0) })
    }

    private fun choose(name:String) {
        active=name; selected.text="Selected: "+active
        prompt.setText(when(name) {
            "AI Image Generator" -> "Subject: [describe]. Style: photorealistic, cinematic light, realistic texture, natural proportions, editorial composition."
            "Product Photoshoot" -> "Product: [name]. Brand: [brand]. Create hero, detail, lifestyle and feature-shot directions. Preserve exact product design."
            "Brandkit Studio" -> "Brand: [name]. Audience: [audience]. Values: [values]. Create a cohesive identity system."
            "Marketing Studio" -> "Product: [name]. Audience: [audience]. Offer: [offer]. Create campaign hooks, caption and CTA."
            "Faceless Video" -> "Topic: [topic]. Audience: [audience]. Length: [seconds]. Create narration and scene-by-scene storyboard."
            "YouTube Thumbnail" -> "Video topic: [topic]. Headline: [short phrase]. Visual subject: [subject]."
            "Marketplace Cards" -> "Product: [name]. Features: [facts]. Create primary image concept and four secondary listing cards."
            "Soul ID Workflow" -> "Character: [description]. Rights/consent confirmed: [yes]. Define consistent traits and scene variations."
            "Website Studio" -> "Business: [name]. Offer: [offer]. Audience: [audience]. Create mobile-first landing page."
            else -> "Project: [description]. Format: [3D/audio]. Style: [style]. Output specs: [specifications]."
        })
        result.text="Add details, then tap Create content / production plan."
    }

    private fun makeOutput() {
        val input=prompt.text.toString().trim()
        if(input.isBlank()){status.text="First enter a project description.";return}
        result.text=when(active){
            "AI Image Generator" -> "IMAGE PROMPT\n"+input+"\n\nStyle: photorealistic editorial/commercial photography, intentional composition, controlled lighting, realistic anatomy and material texture. Avoid unwanted text, logos and watermarks.\n\nThis is a prompt only; no image model is bundled."
            "Product Photoshoot" -> "PRODUCT SHOOT PLAN\n"+input+"\n\n1. Hero image\n2. Three-quarter angle\n3. Detail macro\n4. Lifestyle context\n5. Feature card\n6. Packaging / what's included\n\nPreserve exact product proportions, color and branding."
            "Brandkit Studio" -> "BRANDKIT\n"+input+"\n\nDeliver: positioning, audience, brand personality, color palette, typography, logo direction, social templates and usage rules."
            "Marketing Studio" -> "CAMPAIGN\n"+input+"\n\nHook options: problem-first; product demonstration; benefit-first. Flow: Hook → problem → demonstration → verified benefit → CTA. Do not invent product claims."
            "Faceless Video" -> "VIDEO STORYBOARD\n"+input+"\n\nScene 1: Hook (0–3s)\nScene 2: Context\nScene 3: Main point / demonstration\nScene 4: Supporting proof\nScene 5: Conclusion + CTA\n\nAdd narration, visual, on-screen text and sound cue per scene. MP4 rendering is not included."
            "YouTube Thumbnail" -> "THUMBNAIL PLAN\n"+input+"\n\nUse one focal subject, short headline, strong contrast and mobile-readable type. Do not misrepresent the video."
            "Marketplace Cards" -> "LISTING CARD PLAN\n"+input+"\n\n1. Compliant primary product image\n2. Key features\n3. Dimensions/specifications\n4. Use case\n5. What's in the box\n\nClaims must be factual."
            "Soul ID Workflow" -> "CHARACTER CONSISTENCY PLAN\n"+input+"\n\nLock character description, wardrobe, palette and permitted scene variations. Use only likenesses you have permission to use. No face training is performed."
            "Website Studio" -> websiteHtml(input)
            else -> "3D & AUDIO PLAN\n"+input+"\n\nDefine dimensions, topology, materials, lighting, file format, sound layers, duration, sample rate and delivery checks. This is a production plan, not rendered media."
        }
        status.text="Created offline. No paid API or token used."
    }

    private fun websiteHtml(input:String)= "<!doctype html><html lang='en'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Website Draft</title><style>body{font-family:Arial;margin:0;color:#1a1a1a}header,section{padding:48px 7%}header{background:#1f2a6b;color:white}a{background:#ff7a1a;color:white;padding:12px 18px;text-decoration:none}</style><header><h1>BUYLORA</h1><p>"+input+"</p><a href='#contact'>Get started</a></header><section><h2>What we offer</h2><p>Describe services and benefits.</p></section><section><h2>Why choose us</h2><p>Add verified proof, process and FAQs.</p></section><section id='contact'><h2>Contact</h2><p>Add business contact details.</p></section></html>"

    private fun renderPng() {
        if(result.text.isNullOrBlank()||result.text.startsWith("Your result")) makeOutput()
        val bmp=Bitmap.createBitmap(1080,1350,Bitmap.Config.ARGB_8888); val c=Canvas(bmp); val p=Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader=LinearGradient(0f,0f,1080f,1350f,intArrayOf(Color.rgb(18,22,54),Color.rgb(40,48,105),Color.rgb(12,14,28)),null,Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,1080f,1350f,p); p.shader=null
        p.color=Color.rgb(255,122,26); c.drawRoundRect(70f,95f,1010f,108f,8f,8f,p)
        p.color=Color.rgb(165,170,255);p.textSize=34f;p.typeface=Typeface.create("sans-serif-medium",Typeface.BOLD);c.drawText("BUYLORA  •  SMART PICKS. SIMPLE BUYING.",76f,180f,p)
        p.color=Color.WHITE;p.textSize=62f;drawWrapped(c,active.uppercase(),76f,340f,900f,p,78f)
        p.color=Color.LTGRAY;p.textSize=42f;p.typeface=Typeface.create("sans-serif",Typeface.NORMAL);drawWrapped(c,prompt.text.toString().trim(),76f,560f,900f,p,58f)
        p.color=Color.rgb(255,122,26);p.textSize=30f;c.drawText("BUYLORA AI STUDIO",76f,1250f,p)
        val values=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"BUYLORA_"+System.currentTimeMillis()+".png");put(MediaStore.Images.Media.MIME_TYPE,"image/png");if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/BUYLORA Studio")}
        val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        if(uri==null){status.text="Could not create image file.";bmp.recycle();return}
        val ok=contentResolver.openOutputStream(uri)?.use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)}?:false
        bmp.recycle();status.text=if(ok)"PNG saved to Pictures/BUYLORA Studio. This is designed artwork, not AI-generated photography." else "PNG export failed."
    }
    private fun drawWrapped(c:Canvas,s:String,x:Float,startY:Float,maxW:Float,p:Paint,lineH:Float){
        var y=startY;var line=""
        s.split(Regex("\\s+")).forEach{word->
            val next=if(line.isEmpty())word else "$line $word"
            if(p.measureText(next)>maxW&&line.isNotEmpty()){c.drawText(line,x,y,p);y+=lineH;line=word}else line=next
        }
        if(line.isNotEmpty())c.drawText(line,x,y,p)
    }
    private fun saveApiKey() {
        val token=apiKeyInput.text.toString().trim()
        if(!token.startsWith("sk-or-") || token.length<20){ status.text="That does not look like an OpenRouter key. Check it and try again."; return }
        runCatching {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val encrypted=cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            val packed=Base64.encodeToString(cipher.iv,Base64.NO_WRAP)+"."+Base64.encodeToString(encrypted,Base64.NO_WRAP)
            prefs.edit().putString("openrouter_key_enc",packed).apply()
            apiKeyInput.setText("")
            status.text="API key encrypted and saved on this phone."
        }.onFailure { status.text="Secure save failed: "+(it.message ?: "unknown error") }
    }

    private fun clearApiKey() {
        prefs.edit().remove("openrouter_key_enc").apply()
        apiKeyInput.setText("")
        status.text="Saved API key cleared from this app."
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("buylora_openrouter_aes",null) as? SecretKey)?.let { return it }
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder("buylora_openrouter_aes",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private fun readApiKey(): String? = runCatching {
        val packed=prefs.getString("openrouter_key_enc",null) ?: return null
        val parts=packed.split(".")
        if(parts.size!=2) return null
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,getOrCreateSecretKey(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8)
    }.getOrNull()

    private fun askFreeAi() {
        val key=readApiKey()
        if(key.isNullOrBlank()){ status.text="Save your OpenRouter API key first."; return }
        val userPrompt=prompt.text.toString().trim()
        if(userPrompt.isBlank()){ status.text="Enter a prompt first."; return }
        status.text="Connecting to OpenRouter free model router..."
        Thread {
            var connection: HttpURLConnection?=null
            try {
                connection=(URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod="POST"; connectTimeout=20000; readTimeout=90000; doOutput=true
                    setRequestProperty("Authorization","Bearer "+key)
                    setRequestProperty("Content-Type","application/json")
                    setRequestProperty("HTTP-Referer","https://github.com/tausifalamb-png/FocusLoop")
                    setRequestProperty("X-Title","BUYLORA AI Studio")
                }
                val system="You are BUYLORA AI Studio, a practical creative assistant. User selected workflow: $active. Give accurate, usable outputs. Do not invent product specifications, testimonials, prices or claims. Clearly label assumptions."
                val body=JSONObject().put("model","openrouter/free")
                    .put("messages",JSONArray()
                        .put(JSONObject().put("role","system").put("content",system))
                        .put(JSONObject().put("role","user").put("content",userPrompt)))
                    .put("max_tokens",1400)
                OutputStreamWriter(connection.outputStream,Charsets.UTF_8).use { it.write(body.toString()) }
                val code=connection.responseCode
                val stream=if(code in 200..299) connection.inputStream else connection.errorStream
                val response=stream.bufferedReader().use { it.readText() }
                if(code !in 200..299) throw IllegalStateException(openRouterError(code,response))
                val json=JSONObject(response)
                val answer=json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                if(answer.isNullOrBlank()) throw IllegalStateException("The free model returned an empty response.")
                val model=json.optString("model","free model")
                runOnUiThread {
                    result.text=answer
                    status.text="Free AI response received. Model: $model. No paid fallback requested."
                }
            } catch(e:Exception) {
                runOnUiThread { status.text="Free AI unavailable: "+(e.message ?: "network error")+". No paid model was called." }
            } finally { connection?.disconnect() }
        }.start()
    }

    private fun openRouterError(code:Int, body:String): String {
        val detail=runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
        return "HTTP $code: "+(detail?.takeIf { it.isNotBlank() } ?: body.take(220))
    }

    private fun copyResult(){val v=result.text.toString();if(v.isBlank()||v.startsWith("Your result")){status.text="Create a result first.";return};(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("BUYLORA",v));status.text="Copied."}
    private fun shareResult(){val v=result.text.toString();if(v.isBlank()||v.startsWith("Your result")){status.text="Create a result first.";return};startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,v)},"Share BUYLORA result"))}
    private fun speakResult(){val v=result.text.toString().take(3500);if(!ttsReady||v.isBlank()){status.text="Device text-to-speech is not ready.";return};tts?.speak(v,TextToSpeech.QUEUE_FLUSH,null,"buylora_preview");status.text="Speaking with device TTS."}
    private fun saved()=runCatching{JSONArray(prefs.getString("projects","[]"))}.getOrElse{JSONArray()}
    private fun saveProject(){val v=result.text.toString();if(v.isBlank()||v.startsWith("Your result")){status.text="Create a result first.";return};val a=saved();a.put(JSONObject().put("module",active).put("input",prompt.text.toString()).put("result",v));prefs.edit().putString("projects",a.toString()).apply();status.text="Project saved on this device."}
    private fun openProjects(){val a=saved();if(a.length()==0){status.text="No saved projects yet.";return};val names=(0 until a.length()).map{i->(i+1).toString()+". "+(a.optJSONObject(i)?.optString("module")?:"Project")}.toTypedArray();AlertDialog.Builder(this).setTitle("Saved projects").setItems(names){_,i->val o=a.optJSONObject(i)?:return@setItems;active=o.optString("module",modules[0]);selected.text="Selected: "+active;prompt.setText(o.optString("input"));result.text=o.optString("result");status.text="Project loaded."}.setNegativeButton("Close",null).show()}
    override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
}
