package com.xalidmuslim.arabicgrammar.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class BookRepository(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val database: SQLiteDatabase
    private val sectionsCache = ConcurrentHashMap<String,List<BookSection>>()
    private val blocksCache = ConcurrentHashMap<String,List<BookBlock>>()
    private val readerGroupsCache = ConcurrentHashMap<String,List<ReaderGroup>>()
    @Volatile private var proverbsCache: List<ProverbEntry>? = null
    @Volatile private var conjugationCache: List<ConjugationQuestion>? = null
    init {
        // v9 forces one clean refresh of the packaged book database.
        // User progress/settings live in SharedPreferences and are not affected.
        val dbFile = File(appContext.filesDir, "arabic_grammar_book_v9.db")
        if (!dbFile.exists() || dbFile.length() < 300_000L) {
            appContext.assets.open("book/book.db").use { input -> dbFile.outputStream().use { output -> input.copyTo(output) } }
        }
        database = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
    fun meta(): BookMeta { val v=mutableMapOf<String,String>(); database.rawQuery("SELECT key,value FROM meta",null).use{c->while(c.moveToNext())v[c.getString(0)]=c.getString(1)}; return BookMeta(v["title"].orEmpty(),v["author"].orEmpty(),v["sourcePages"]?.toIntOrNull()?:0,v["blocks"]?.toIntOrNull()?:0,v["arabicChars"]?.toIntOrNull()?:0,v["coreSectionsFound"]?.toIntOrNull()?:0,v["badEncodingBlocks"]?.toIntOrNull()?:0) }
    fun sections(chapter:String?=null):List<BookSection>{val key=chapter?:"*";sectionsCache[key]?.let{return it};val sql=if(chapter==null)"SELECT id,chapter,number,title,found,exercise_count FROM sections ORDER BY CASE chapter WHEN 'syntax' THEN 1 WHEN 'morphology' THEN 2 ELSE 3 END, number" else "SELECT id,chapter,number,title,found,exercise_count FROM sections WHERE chapter=? ORDER BY number";val result=querySections(sql,chapter?.let{arrayOf(it)});sectionsCache.putIfAbsent(key,result);return sectionsCache[key]?:result}
    fun exerciseSections()=querySections("SELECT id,chapter,number,title,found,exercise_count FROM sections WHERE exercise_count>0 AND found=1 ORDER BY CASE chapter WHEN 'syntax' THEN 1 WHEN 'morphology' THEN 2 ELSE 3 END, number",null)
    fun section(id:String):BookSection?=sections().firstOrNull{it.id==id}
    fun adjacent(sectionId:String):Pair<BookSection?,BookSection?>{val current=section(sectionId)?:return null to null;val list=sections(current.chapter).filter{it.found};val i=list.indexOfFirst{it.id==sectionId};if(i<0)return null to null;return list.getOrNull(i-1) to list.getOrNull(i+1)}
    fun blocks(sectionId:String):List<BookBlock>{blocksCache[sectionId]?.let{return it};val out=mutableListOf<BookBlock>();database.rawQuery("SELECT block_index,type,text FROM blocks WHERE section_id=? AND type!='section-heading' ORDER BY block_index",arrayOf(sectionId)).use{c->while(c.moveToNext())out+=BookBlock(c.getInt(0),c.getString(1),cleanBookText(c.getString(2)))};blocksCache.putIfAbsent(sectionId,out);return blocksCache[sectionId]?:out}
    fun readerGroups(sectionId:String):List<ReaderGroup>{readerGroupsCache[sectionId]?.let{return it};val result=buildReaderGroups(blocks(sectionId));readerGroupsCache.putIfAbsent(sectionId,result);return readerGroupsCache[sectionId]?:result}
    fun search(rawQuery:String,limit:Int=40):List<SearchResult>{val q=rawQuery.trim();if(q.length<2)return emptyList();val like="%$q%";val sql="""SELECT s.id,s.chapter,s.number,s.title,s.found,s.exercise_count, MIN(b.text) AS snippet FROM sections s JOIN blocks b ON b.section_id=s.id WHERE s.found=1 AND (s.title LIKE ? OR b.text LIKE ?) GROUP BY s.id,s.chapter,s.number,s.title,s.found,s.exercise_count ORDER BY CASE s.chapter WHEN 'syntax' THEN 1 WHEN 'morphology' THEN 2 ELSE 3 END, s.number LIMIT ?""";val out=mutableListOf<SearchResult>();database.rawQuery(sql,arrayOf(like,like,limit.toString())).use{c->while(c.moveToNext()){out+=SearchResult(c.toSection(0),cleanBookText(c.getString(6).orEmpty()).replace(Regex("\\s+")," ").take(260))}};return out}
    fun proverbs():List<ProverbEntry>{proverbsCache?.let{return it};val ar=Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]");val out=mutableListOf<ProverbEntry>();for(section in sections().filter{it.found}){val bs=blocks(section.id);bs.forEachIndexed cue@{ci,cue->if(!cue.text.contains("пословиц",true))return@cue;var arabic="";var tr="";var ai=-1;for(i in (ci+1)..minOf(ci+4,bs.lastIndex)){val b=bs[i];if(!ar.containsMatchIn(b.text))continue;ai=i;val lines=b.text.lineSequence().map{it.trim()}.filter{it.isNotEmpty()}.toList();val qa=b.text.indexOf('«');if(qa>=0){arabic=b.text.substring(0,qa).trim();tr=b.text.substring(qa).trim()}else arabic=lines.firstOrNull{ar.containsMatchIn(it)}.orEmpty();break};if(arabic.isBlank()||ai<0)return@cue;if(tr.isBlank()){val parts=mutableListOf<String>();for(i in (ai+1)..minOf(ai+3,bs.lastIndex)){val tx=bs[i].text.trim();if(tx.isBlank())continue;if(parts.isEmpty()&&!tx.contains('«'))break;parts+=tx;if(tx.contains('»'))break};tr=parts.joinToString(" ").replace(Regex("\\s+")," ").trim()};if(tr.isNotBlank())out+=ProverbEntry(section.id+":proverb:"+cue.index,section.id,section.title,section.chapter,cleanBookText(arabic).replace(Regex("\\s+")," ").trim(),cleanBookText(tr))}};val result=out.distinctBy{it.arabic};proverbsCache=result;return result}
    fun quizQuestions(preferredSectionIds:Set<String> = emptySet(),limit:Int=10):List<QuizQuestion>{val all=sections().filter{it.found&&(it.chapter=="syntax"||it.chapter=="morphology")};val focus=if(preferredSectionIds.size>=4)all.filter{preferredSectionIds.contains(it.id)} else all;val q=mutableListOf<QuizQuestion>();for(s in focus.shuffled()){if(q.size>=limit)break;val c=blocks(s.id).firstOrNull{it.type in setOf("text","mixed","arabic")&&it.text.length in 70..380&&it.text.count{ch->ch=='\n'}<=2&&!it.text.contains("упражнения",true)}?:continue;val d=all.filter{it.chapter==s.chapter&&it.id!=s.id}.shuffled().take(3);if(d.size<3)continue;val opts=(d.map{it.title}+s.title).shuffled();q+=QuizQuestion(s.id+":"+c.index,s.id,s.title,s.chapter,c.text.trim(),opts,opts.indexOf(s.title),c.type=="arabic")};return q}
    fun flashCards(preferredSectionIds:Set<String> = emptySet(),limit:Int=40):List<FlashCard>{val all=sections().filter{it.found&&(it.chapter=="syntax"||it.chapter=="morphology")};val focus=if(preferredSectionIds.size>=3)all.filter{preferredSectionIds.contains(it.id)}else all;val cards=mutableListOf<FlashCard>();for(s in focus){val bs=blocks(s.id);bs.firstOrNull{it.type in setOf("text","mixed")&&it.text.length in 55..520&&!it.text.contains("упражнения",true)}?.let{r->cards+=FlashCard(s.id+":rule:"+r.index,s.id,s.title,s.chapter,"Вспомни основное правило темы «${s.title}».",r.text.trim(),false)};bs.firstOrNull{it.type=="arabic"&&it.text.length in 2..220}?.let{a->cards+=FlashCard(s.id+":arabic:"+a.index,s.id,s.title,s.chapter,a.text.trim(),"Этот пример относится к теме: ${s.title}",true)}};return cards.shuffled().take(limit)}
    fun conjugationQuestions(limit: Int = 60): List<ConjugationQuestion> {
        conjugationCache?.let { return it.shuffled().take(limit) }
        val out = mutableListOf<ConjugationQuestion>()
        val arabic = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]")
        val cyrillic = Regex("[А-Яа-яЁё]")
        val canonicalLabels = listOf(
            "1 лицо",
            "2 лицо жен. рода",
            "2 лицо муж. рода",
            "3 лицо жен. рода",
            "3 лицо муж. рода",
        )
        val lineList: (String) -> List<String> = { text ->
            text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

        for (section in sections("morphology").filter { it.found }) {
            val bs = blocks(section.id)
            for (i in bs.indices) {
                val headerLines = lineList(bs[i].text)
                val header = headerLines.joinToString(" ").lowercase()
                val isHeader = header.contains("1 лицо") &&
                    header.contains("2 лицо") &&
                    header.contains("3 лицо")
                if (!isHeader) continue

                val cue = ((i - 1) downTo maxOf(0, i - 4))
                    .map { bs[it].text.replace(Regex("\\s+"), " ").trim() }
                    .firstOrNull {
                        it.length in 2..180 &&
                            arabic.containsMatchIn(it) &&
                            cyrillic.containsMatchIn(it) &&
                            !it.contains("лицо", ignoreCase = true)
                    }
                val displayTitle = if (cue.isNullOrBlank()) section.title else section.title + "\n" + cue

                for (rowOffset in 1..4) {
                    val rowBlock = bs.getOrNull(i + rowOffset) ?: break
                    val rowLines = lineList(rowBlock.text)
                    val rowText = rowLines.joinToString(" ").lowercase()
                    val numberLabel = when {
                        rowText.contains("единств") -> "единственное число"
                        rowText.contains("двойств") -> "двойственное число"
                        rowText.contains("множеств") -> "множественное число"
                        else -> break
                    }
                    val forms = rowLines.take(5)
                    if (forms.size < 5) continue
                    forms.forEachIndexed { column, rawAnswer ->
                        val answer = rawAnswer.trim()
                        val usable = answer.isNotBlank() &&
                            answer.lowercase() != "нет" &&
                            !answer.contains('_') &&
                            arabic.containsMatchIn(answer)
                        if (!usable) return@forEachIndexed
                        out += ConjugationQuestion(
                            id = section.id + ":conj:" + bs[i].index + ":" + rowOffset + ":" + column,
                            sectionId = section.id,
                            sectionTitle = displayTitle,
                            personLabel = canonicalLabels[column],
                            numberLabel = numberLabel,
                            answer = answer,
                        )
                    }
                }
            }
        }
        val parsed = out.distinctBy { it.id }
        conjugationCache = parsed
        return parsed.shuffled().take(limit)
    }
    private fun querySections(sql:String,args:Array<String>?):List<BookSection>{val out=mutableListOf<BookSection>();database.rawQuery(sql,args).use{c->while(c.moveToNext())out+=c.toSection()};return out}
    private fun Cursor.toSection(offset:Int=0)=BookSection(getString(offset),getString(offset+1),getInt(offset+2),getString(offset+3),getInt(offset+4)==1,getInt(offset+5))
    override fun close(){database.close()}
}


private val bookGarbage = Regex("[\\x80-\\x9F\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF\\uFFFC\\uFFFD\\u25A1\\u25A0]")
private val arabicStart = Regex("^[\\s\\p{Punct}]*[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]")
fun cleanBookText(raw:String):String = raw
    .replace('\u00A0',' ')
    .replace(bookGarbage,"")
    .lineSequence()
    .joinToString("\n"){it.replace(Regex("[ \\t]+")," ").trim()}
    .trim()
private fun startsWithArabicText(text:String)=arabicStart.containsMatchIn(cleanBookText(text))

fun buildReaderGroups(blocks:List<BookBlock>):List<ReaderGroup>{val result=mutableListOf<ReaderGroup>();val pb=mutableListOf<BookBlock>();val tb=mutableListOf<BookBlock>();var ex=false;fun fp(){if(pb.isEmpty())return;pb.chunked(2).forEach{result+=ReaderGroup(ReaderKind.PARAGRAPH,it.toList(),ex)};pb.clear()};fun ft(){if(tb.isEmpty())return;result+=ReaderGroup(ReaderKind.TABLE,tb.toList(),ex);tb.clear()};for(raw in blocks){val b=raw.copy(text=cleanBookText(raw.text));when(b.type){"exercise-heading"->{fp();ft();ex=true;result+=ReaderGroup(ReaderKind.EXERCISE_TITLE,listOf(b),true)};"table"->{fp();tb+=b};"answer-line"->{fp();ft();result+=ReaderGroup(ReaderKind.ANSWER_LINE,listOf(b),ex)};"arabic"->{fp();ft();result+=ReaderGroup(ReaderKind.ARABIC,listOf(b),ex)};"mixed"->{if(startsWithArabicText(b.text)){fp();ft();result+=ReaderGroup(ReaderKind.ARABIC,listOf(b),ex)}else{ft();pb+=b;if(pb.size>=2)fp()}};else->{ft();pb+=b;if(pb.size>=2)fp()}}};fp();ft();return result}
