package pl.chb.youtubeprogram

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity:AppCompatActivity() {
    private var project=QuizProject(); private var current=0; private var pending:String=""; private var rendered:File?=null
    private lateinit var root:LinearLayout; private lateinit var status:TextView; private lateinit var qSpinner:Spinner; private lateinit var preview:ImageView; private lateinit var video:VideoView
    private lateinit var language:Spinner; private lateinit var title:EditText; private lateinit var subtitle:EditText; private lateinit var difficulty:Spinner; private lateinit var flag:Button; private lateinit var qBg:Button
    private val answers=mutableListOf<AutoCompleteTextView>(); private val correct=mutableListOf<RadioButton>()
    private lateinit var bgMode:Spinner; private lateinit var blur:SeekBar; private lateinit var dim:SeekBar; private lateinit var musicVolume:SeekBar; private lateinit var music:Button; private lateinit var globalBg:Button

    private val openDocument=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri-> uri?.let { takePersistable(it); when(pending){
        "flag"->{question().flagUri=it.toString();flag.text="Flaga: wybrana"}; "qbg"->{question().backgroundUri=it.toString();qBg.text="Tło kraju: wybrane"}; "globalbg"->{project.globalBackgroundUri=it.toString();globalBg.text="Tło filmu: wybrane"}; "music"->{project.musicUri=it.toString();music.text="Muzyka: wybrana"};
        "easybg","mediumbg","hardbg"->{val l=mapOf("easybg" to "Łatwy","mediumbg" to "Średni","hardbg" to "Trudny")[pending]!!;project.levelBackgroundUris[l]=it.toString()}
    };refreshPreview() } }
    private val createVideo=registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")){uri->uri?.let{renderAndSave(it)}}
    private val openProject=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{ takePersistable(it); lifecycleScope.launch{runCatching{contentResolver.openInputStream(it)!!.bufferedReader().readText()}.onSuccess{project=QuizProject.fromJson(it);current=0;loadProjectUi();status.text="Projekt otwarty"}.onFailure{e->toast(e.message?:"Błąd")}}}}
    private val saveProject=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->uri?.let{contentResolver.openOutputStream(it,"w")!!.bufferedWriter().use{w->w.write(project.toJson())};status.text="Projekt zapisany"}}

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); project.questions.add(QuizQuestion());buildUi();loadProjectUi()}
    private fun takePersistable(uri:Uri){runCatching{contentResolver.takePersistableUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)}}
    private fun question()=project.questions[current.coerceIn(0,project.questions.lastIndex)]
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun section(t:String)=TextView(this).apply{text=t;textSize=18f;setTextColor(Color.rgb(240,190,62));setPadding(0,dp(18),0,dp(8))}
    private fun button(t:String,action:()->Unit)=Button(this).apply{text=t;setOnClickListener{action()}}
    private fun spinner(items:List<String>)=Spinner(this).apply{adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,items)}

    private fun buildUi(){
        val scroll=ScrollView(this);root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(10),dp(14),dp(30));setBackgroundColor(Color.rgb(17,20,28))};scroll.addView(root);setContentView(scroll)
        root.addView(TextView(this).apply{text="QUIZ VIDEO STUDIO — ANDROID";textSize=23f;setTextColor(Color.rgb(240,190,62))})
        val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};top.addView(button("Otwórz z Drive"){openProject.launch(arrayOf("application/json","text/plain"))});top.addView(button("Zapisz projekt"){sync();saveProject.launch("quiz-project.json")});root.addView(top)
        root.addView(section("Film"));language=spinner(listOf(LANG_EN,LANG_PL));root.addView(language);title=EditText(this).apply{hint="Tytuł";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY)};subtitle=EditText(this).apply{hint="Podtytuł";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY)};root.addView(title);root.addView(subtitle)
        root.addView(section("Pytania"));qSpinner=spinner(listOf("1"));qSpinner.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){ };override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){syncQuestion();current=pos;loadQuestionUi()}};root.addView(qSpinner)
        val addRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("Łatwy","Średni","Trudny").forEach{lvl->addRow.addView(button("+ $lvl"){sync();project.questions.add(QuizQuestion(difficulty=lvl));refreshQuestionSpinner(project.questions.lastIndex)})};root.addView(addRow)
        difficulty=spinner(listOf("Łatwy","Średni","Trudny"));root.addView(difficulty);flag=button("Wybierz flagę — telefon / Google Drive"){pending="flag";openDocument.launch(arrayOf("image/*"))};root.addView(flag);qBg=button("Opcjonalne tło kraju"){pending="qbg";openDocument.launch(arrayOf("image/*"))};root.addView(qBg)
        for(i in 0..3){val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val rb=RadioButton(this);correct+=rb;row.addView(rb);val ac=AutoCompleteTextView(this).apply{hint="${('A'.code+i).toChar()}: wpisz szwaj / switz";threshold=1;setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);layoutParams=LinearLayout.LayoutParams(0,dp(54),1f)};answers+=ac;row.addView(ac);root.addView(row)}
        root.addView(section("Tło (opcjonalne)"));bgMode=spinner(listOf("Bez tła","Jedno na cały film","Według poziomu","Według kraju"));root.addView(bgMode);globalBg=button("Wybierz tło całego filmu"){pending="globalbg";openDocument.launch(arrayOf("image/*"))};root.addView(globalBg)
        val lvlRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("easybg" to "Easy","mediumbg" to "Medium","hardbg" to "Hard").forEach{(key,label)->lvlRow.addView(button("Tło $label"){pending=key;openDocument.launch(arrayOf("image/*"))})};root.addView(lvlRow)
        root.addView(TextView(this).apply{text="Rozmycie tła 0–100%";setTextColor(Color.WHITE)});blur=SeekBar(this).apply{max=100};root.addView(blur);root.addView(TextView(this).apply{text="Przyciemnienie tła 0–100%";setTextColor(Color.WHITE)});dim=SeekBar(this).apply{max=100};root.addView(dim)
        root.addView(section("Muzyka i dźwięk"));music=button("Wybierz muzykę — telefon / Google Drive"){pending="music";openDocument.launch(arrayOf("audio/*"))};root.addView(music);root.addView(TextView(this).apply{text="Głośność muzyki 0–100% (SFX pozostają)";setTextColor(Color.WHITE)});musicVolume=SeekBar(this).apply{max=100};root.addView(musicVolume)
        root.addView(section("Podgląd"));preview=ImageView(this).apply{adjustViewBounds=true;minimumHeight=dp(210);setBackgroundColor(Color.BLACK)};root.addView(preview);video=VideoView(this).apply{visibility=View.GONE};root.addView(video,LinearLayout.LayoutParams(-1,dp(230)))
        val renderRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};renderRow.addView(button("Odśwież podgląd"){sync();refreshPreview()});renderRow.addView(button("▶ Odtwórz cały quiz"){sync();renderPreviewVideo()});root.addView(renderRow)
        root.addView(button("EKSPORTUJ MP4 → telefon / Google Drive"){sync();createVideo.launch("flag-quiz.mp4")});status=TextView(this).apply{setTextColor(Color.LTGRAY);setPadding(0,dp(12),0,0)};root.addView(status)
        language.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){ };override fun onItemSelected(p:android.widget.AdapterView<*>?,v:View?,pos:Int,id:Long){project.language=if(pos==0)LANG_EN else LANG_PL;setupCountryAdapters();loadQuestionUi()}}
    }

    private fun setupCountryAdapters(){val labels=CountryCatalog.all.map{CountryCatalog.label(it,project.language)};answers.forEach{ac->ac.setAdapter(ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line,labels));ac.setOnItemClickListener{_,_,pos,_->val c=CountryCatalog.all.firstOrNull{CountryCatalog.label(it,project.language)==ac.adapter.getItem(pos).toString()};if(c!=null)ac.setText(CountryCatalog.localized(c,project.language),false)}}}
    private fun refreshQuestionSpinner(select:Int=current){qSpinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,project.questions.mapIndexed{i,q->"${i+1}. ${q.difficulty}"});qSpinner.setSelection(select.coerceIn(0,project.questions.lastIndex));current=select.coerceIn(0,project.questions.lastIndex);loadQuestionUi()}
    private fun sync(){project.title=title.text.toString();project.subtitle=subtitle.text.toString();project.language=if(language.selectedItemPosition==0)LANG_EN else LANG_PL;project.backgroundMode=listOf("none","global","level","country")[bgMode.selectedItemPosition];project.backgroundBlur=blur.progress;project.backgroundDim=dim.progress;project.musicVolume=musicVolume.progress;syncQuestion()}
    private fun syncQuestion(){if(project.questions.isEmpty())return;val q=question();q.difficulty=difficulty.selectedItem?.toString()?:q.difficulty;q.answers=MutableList(4){i->answers.getOrNull(i)?.text?.toString()?.trim()?:q.answers.getOrElse(i){""}};q.answerCodes=MutableList(4){i->CountryCatalog.find(q.answers[i])?.code ?: q.answerCodes.getOrElse(i){""}};q.correctIndex=correct.indexOfFirst{it.isChecked}.let{if(it<0)0 else it}}
    private fun loadQuestionUi(){if(project.questions.isEmpty())return;val q=question();difficulty.setSelection(listOf("Łatwy","Średni","Trudny").indexOf(q.difficulty).coerceAtLeast(0));flag.text=if(q.flagUri.isBlank())"Wybierz flagę — telefon / Google Drive" else "Flaga: wybrana";qBg.text=if(q.backgroundUri.isBlank())"Opcjonalne tło kraju" else "Tło kraju: wybrane";answers.forEachIndexed{i,a->val c=CountryCatalog.all.firstOrNull{it.code==q.answerCodes.getOrNull(i)};a.setText(c?.let{CountryCatalog.localized(it,project.language)} ?: q.answers.getOrElse(i){""},false)};correct.forEachIndexed{i,r->r.isChecked=i==q.correctIndex};refreshPreview()}
    private fun loadProjectUi(){if(project.questions.isEmpty())project.questions.add(QuizQuestion());title.setText(project.title);subtitle.setText(project.subtitle);language.setSelection(if(project.language==LANG_EN)0 else 1);bgMode.setSelection(listOf("none","global","level","country").indexOf(project.backgroundMode).coerceAtLeast(0));blur.progress=project.backgroundBlur;dim.progress=project.backgroundDim;musicVolume.progress=project.musicVolume;music.text=if(project.musicUri.isBlank())"Wybierz muzykę — telefon / Google Drive" else "Muzyka: wybrana";setupCountryAdapters();refreshQuestionSpinner(0)}
    private fun refreshPreview(){if(project.questions.isEmpty())return;runCatching{preview.setImageBitmap(FrameRenderer.question(this,project,question(),current+1,project.questions.size,project.questionSeconds,false))}}
    private fun renderPreviewVideo(){status.text="Renderowanie podglądu…";lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){VideoExporter.render(this@MainActivity,project){p,m->runOnUiThread{status.text="$m $p%"}}}}.onSuccess{rendered=it.file;video.setVideoPath(it.file.absolutePath);preview.visibility=View.GONE;video.visibility=View.VISIBLE;video.start();status.text="Podgląd gotowy"}.onFailure{e->status.text="Błąd: ${e.message}"}}}
    private fun renderAndSave(uri:Uri){status.text="Renderowanie…";lifecycleScope.launch{runCatching{withContext(Dispatchers.IO){val r=VideoExporter.render(this@MainActivity,project){p,m->runOnUiThread{status.text="$m $p%"}};VideoExporter.copyToUri(this@MainActivity,r.file,uri);r}}.onSuccess{rendered=it.file;status.text="Gotowe — MP4 zapisany w wybranym miejscu";toast("Film zapisany")}.onFailure{e->status.text="Błąd: ${e.message}";toast("Eksport nieudany")}}}
}
