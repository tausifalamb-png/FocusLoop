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
        page.addView(label("V2 • Offline creative workspace • No paid API",13f,Color.rgb(180,215,185)).apply { setPadding(0,dp(12),0,dp(12)) })
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
        page.addView(btn("Create content / production plan"){ makeOutput() })
        page.addView(btn("Render design PNG (poster / thumbnail / card)"){ renderPng() })
        page.addView(btn("Copy result"){ copyResult() })
        page.addView(btn("Share result"){ shareResult() })
        page.addView(btn("Speak narration (device TTS)"){ speakResult() })
        page.addView(btn("Save project"){ saveProject() })
        page.addView(btn("Open saved projects"){ openProjects() })
        page.addView(label("OUTPUT",12f,Color.rgb(155,160,255)).apply { setPadding(0,dp(16),0,dp(6)) })
        result=label("Your result will appear here.",14f,Color.LTGRAY).apply { setPadding(dp(12),dp(12),dp(12),dp(12)); setBackgroundColor(Color.rgb(31,35,60)) }
        page.addView(result,LinearLayout.LayoutParams(-1,-2).apply { minHeight=dp(180) })
        status=label("Ready. No login, token or cloud credits required.",13f,Color.rgb(190,215,190)).apply { setPadding(0,dp(12),0,dp(8)) }
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
    private fun copyResult(){val v=result.text.toString();if(v.isBlank()||v.startsWith("Your result")){status.text="Create a result first.";return};(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("BUYLORA",v));status.text="Copied."}
    private fun shareResult(){val v=result.text.toString();if(v.isBlank()||v.startsWith("Your result")){status.text="Create a result first.";return};startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,v)},"Share BUYLORA result"))}
    private fun speakResult(){val v=result.text.toString().take(3500);if(!ttsReady||v.isBlank()){status.text="Device text-to-speech is not ready.";return};tts?.speak(v,TextToSpeech.QUEUE_FLUSH,null,"buylora_preview");status.text="Speaking with device TTS."}
    private fun saved()=runCatching{JSONArray(prefs.getString("projects","[]"))}.getOrElse{JSONArray()}
    private fun saveProject(){val v=result.text.toString();if(v.isBlank()||v.startsWith("Your result")){status.text="Create a result first.";return};val a=saved();a.put(JSONObject().put("module",active).put("input",prompt.text.toString()).put("result",v));prefs.edit().putString("projects",a.toString()).apply();status.text="Project saved on this device."}
    private fun openProjects(){val a=saved();if(a.length()==0){status.text="No saved projects yet.";return};val names=(0 until a.length()).map{i->(i+1).toString()+". "+(a.optJSONObject(i)?.optString("module")?:"Project")}.toTypedArray();AlertDialog.Builder(this).setTitle("Saved projects").setItems(names){_,i->val o=a.optJSONObject(i)?:return@setItems;active=o.optString("module",modules[0]);selected.text="Selected: "+active;prompt.setText(o.optString("input"));result.text=o.optString("result");status.text="Project loaded."}.setNegativeButton("Close",null).show()}
    override fun onDestroy(){tts?.stop();tts?.shutdown();super.onDestroy()}
}
