package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.data.*
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class ReaderSheet { NOTE, SETTINGS }
private val arabicRun=Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]+(?:\\s+[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]+)*")
private fun tableLines(text:String)=text.lineSequence().map{it.trim()}.filter{it.isNotEmpty()}.toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderScreen(repository:BookRepository,progress:ProgressStore,sectionId:String,navigate:(Screen)->Unit,back:()->Unit){
 val section=remember(sectionId){repository.section(sectionId)};val groups=remember(sectionId){repository.readerGroups(sectionId)};val settings=progress.settings;var sheet by remember{mutableStateOf<ReaderSheet?>(null)};var note by remember(sectionId){mutableStateOf(progress.note(sectionId))};if(section==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("Раздел не найден")};return};val adjacent=remember(sectionId){repository.adjacent(sectionId)};val initialIndex=progress.readerIndex(sectionId).coerceIn(0,groups.size+1);val listState=rememberLazyListState(initialFirstVisibleItemIndex=initialIndex,initialFirstVisibleItemScrollOffset=progress.readerOffset(sectionId));val readingProgress by remember{derivedStateOf{val total=(groups.size+1).coerceAtLeast(1);(listState.firstVisibleItemIndex.toFloat()/total).coerceIn(0f,1f)}};LaunchedEffect(section.id){progress.updateLastSection(section.id)};LaunchedEffect(sectionId,listState){snapshotFlow{listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset}.distinctUntilChanged().collect{pair->progress.saveReaderPosition(sectionId,pair.first,pair.second)}}
 Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={Column{TopAppBar(windowInsets=WindowInsets(0,0,0,0),title={Column{Text(chapterName(section.chapter)+" · Тема "+section.number+" · "+((readingProgress*100).toInt())+"%",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(section.title,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleSmall,color=CardHeadlineBrown)}},navigationIcon={IconButton(onClick=back){Icon(Icons.Outlined.ArrowBack,"Назад")}},actions={IconButton(onClick={progress.toggleBookmark(section.id)}){Icon(if(progress.bookmarks.contains(section.id))Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,"Закладка")};IconButton(onClick={sheet=ReaderSheet.NOTE}){Icon(Icons.Outlined.NoteAlt,"Заметка")};IconButton(onClick={sheet=ReaderSheet.SETTINGS}){Icon(Icons.Outlined.Settings,"Настройки чтения")}});LinearProgressIndicator(progress=readingProgress,modifier=Modifier.fillMaxWidth().height(2.dp),color=MaterialTheme.colorScheme.secondary,trackColor=MaterialTheme.colorScheme.surfaceVariant)}}){padding->LazyColumn(state=listState,modifier=Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(start=12.dp,end=12.dp,top=10.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(settings.paragraphGap.coerceIn(7f,16f).dp)){
  item{ElevatedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),elevation=CardDefaults.elevatedCardElevation(defaultElevation=1.dp)){Column(Modifier.padding(16.dp)){Kicker("Урок");Text(section.title,style=MaterialTheme.typography.titleLarge,color=CardHeadlineBrown);Spacer(Modifier.height(5.dp));Text("Правила, примеры и упражнения собраны в отдельные компактные блоки.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(10.dp));OutlinedButton(onClick={progress.toggleCompleted(section.id)},shape=RoundedCornerShape(99.dp),contentPadding=PaddingValues(horizontal=12.dp,vertical=7.dp)){Icon(Icons.Default.CheckCircle,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text(if(progress.completed.contains(section.id))"Освоено" else "Отметить тему")}}}}
  item{LessonDiagram(section,settings)}
  itemsIndexed(groups,key={index,g->g.kind.name+"_"+(g.blocks.firstOrNull()?.index?:index)}){index,g->when(g.kind){ReaderKind.EXERCISE_TITLE->ExerciseTitleCard(g);ReaderKind.ARABIC->ArabicCard(g,settings);ReaderKind.TABLE->TableCard(g,settings);ReaderKind.ANSWER_LINE->AnswerField(g,settings);ReaderKind.PARAGRAPH->ParagraphCard(g,settings,index==0)}}
  item{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){if(!progress.completed.contains(section.id))Button(onClick={progress.toggleCompleted(section.id)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Icon(Icons.Default.CheckCircle,null);Spacer(Modifier.width(6.dp));Text("Тема пройдена")};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick={adjacent.first?.let{navigate(Screen.Reader(it.id))}},enabled=adjacent.first!=null,modifier=Modifier.weight(1f)){Text("← Предыдущая")};OutlinedButton(onClick={adjacent.second?.let{navigate(Screen.Reader(it.id))}},enabled=adjacent.second!=null,modifier=Modifier.weight(1f)){Text("Следующая →")}}}}
 }}
 if(sheet!=null)ModalBottomSheet(onDismissRequest={sheet=null}){Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)){when(sheet){ReaderSheet.NOTE->{Text("Моя заметка",style=MaterialTheme.typography.titleMedium,color=CardHeadlineBrown);Spacer(Modifier.height(8.dp));OutlinedTextField(value=note,onValueChange={note=it},modifier=Modifier.fillMaxWidth(),minLines=4,label={Text("Правило своими словами, пример или вопрос")},shape=RoundedCornerShape(14.dp));Spacer(Modifier.height(10.dp));Button(onClick={progress.saveNote(section.id,note);sheet=null},modifier=Modifier.fillMaxWidth()){Text("Сохранить")}};ReaderSheet.SETTINGS->{Text("Настройки чтения",style=MaterialTheme.typography.titleMedium,color=CardHeadlineBrown);Spacer(Modifier.height(12.dp));Text("Тема",style=MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("light" to "День","dark" to "Ночь","system" to "Система").forEach{pair->FilterChip(selected=settings.theme==pair.first,onClick={progress.updateSettings(settings.copy(theme=pair.first))},label={Text(pair.second)})}};Spacer(Modifier.height(10.dp));Text("Русский текст · ${(settings.textScale*100).toInt()}%",style=MaterialTheme.typography.titleSmall);Slider(settings.textScale,{progress.updateSettings(settings.copy(textScale=it))},valueRange=.85f..1.35f);Text("Арабский текст · ${(settings.arabicScale*100).toInt()}%",style=MaterialTheme.typography.titleSmall);Slider(settings.arabicScale,{progress.updateSettings(settings.copy(arabicScale=it))},valueRange=.9f..1.5f);Text("Межстрочный интервал",style=MaterialTheme.typography.titleSmall);Slider(settings.lineHeight,{progress.updateSettings(settings.copy(lineHeight=it))},valueRange=1.25f..2f);Text("Отступы между блоками",style=MaterialTheme.typography.titleSmall);Slider(settings.paragraphGap,{progress.updateSettings(settings.copy(paragraphGap=it))},valueRange=6f..24f);Text("Русский шрифт",style=MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("sans" to "Обычный","serif" to "Книжный","mono" to "Моно").forEach{pair->FilterChip(selected=settings.fontChoice==pair.first,onClick={progress.updateSettings(settings.copy(fontChoice=pair.first))},label={Text(pair.second)})}};Text("Арабский шрифт",style=MaterialTheme.typography.titleSmall);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("naskh" to "Насх","sans" to "Современный","system" to "Системный").forEach{pair->FilterChip(selected=settings.arabicFontChoice==pair.first,onClick={progress.updateSettings(settings.copy(arabicFontChoice=pair.first))},label={Text(pair.second)})}};Text("النَّحْوُ وَالصَّرْفُ · الطَّالِبُ مُجْتَهِدٌ",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.End,style=MaterialTheme.typography.titleMedium.copy(fontFamily=arabicFont(settings),fontSize=(23f*settings.arabicScale).sp,lineHeight=(34f*settings.arabicScale).sp))};null->Unit};Spacer(Modifier.height(12.dp))}}
}

private val inlineAnswerRun=Regex("[_＿—–-]{6,}")
private val cyrillicChar=Regex("[А-Яа-яЁё]")
private val arabicChar=Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]")
private fun cleanUiLine(raw:String)=cleanBookText(raw)
private fun answerPrompt(line:String)=inlineAnswerRun.replace(line," ").replace(Regex("\\s+")," ").trim()
private fun startsArabic(line:String)=arabicChar.find(line)?.range?.first==line.indexOfFirst{!it.isWhitespace()||it=='\u00A0'}.coerceAtLeast(0)
private data class ScriptChunk(val arabic:Boolean,val text:String)
private fun splitScriptChunks(line:String):List<ScriptChunk>?{
    val clean=cleanUiLine(line)
    val firstArabic=arabicChar.find(clean)?.range?.first?:return null
    val firstCyrillic=cyrillicChar.find(clean)?.range?.first?:return null
    var arabicMode=firstArabic<firstCyrillic
    val out=mutableListOf<ScriptChunk>()
    val buffer=StringBuilder()
    fun flush(){
        val value=buffer.toString().trim()
        if(value.isNotBlank())out+=ScriptChunk(arabicMode,value)
        buffer.clear()
    }
    for(ch in clean){
        val strongMode=when{
            arabicChar.matches(ch.toString())->true
            cyrillicChar.matches(ch.toString())->false
            else->null
        }
        if(strongMode!=null&&strongMode!=arabicMode){flush();arabicMode=strongMode}
        buffer.append(ch)
    }
    flush()
    if(out.size>=2){
        for(i in 0 until out.lastIndex){
            val current=out[i]
            val next=out[i+1]
            if(current.arabic && !next.arabic && current.text.endsWith("(")){
                val cleaned=current.text.dropLast(1).trimEnd()
                val moved="("+next.text.trimStart()
                out[i]=current.copy(text=cleaned)
                out[i+1]=next.copy(text=moved)
            }
        }
    }
    return if(out.any{it.arabic}&&out.any{!it.arabic})out else null
}
@Composable private fun SeparatedMixedLine(chunks:List<ScriptChunk>,settings:ReaderSettings){
    val alternatingPairs=chunks.size>=2&&chunks.first().arabic&&chunks.withIndex().all{(i,c)->c.arabic==(i%2==0)}
    if(alternatingPairs){
        chunks.chunked(2).forEachIndexed{pairIndex,pair->
            val ar=pair.firstOrNull{it.arabic}
            val ru=pair.firstOrNull{!it.arabic}
            if(ar!=null)ArabicLine(ar.text,settings)
            if(ru!=null){Spacer(Modifier.height(2.dp));RussianOrMixedLine(ru.text,settings)}
            if(pairIndex!=chunks.chunked(2).lastIndex)Spacer(Modifier.height(5.dp))
        }
    }else{
        chunks.filter{it.arabic}.forEachIndexed{i,c->
            ArabicLine(c.text,settings)
            if(i!=chunks.count{it.arabic}-1)Spacer(Modifier.height(2.dp))
        }
        val russian=chunks.filterNot{it.arabic}.joinToString(" "){it.text}.replace(Regex("\\s+")," ").trim()
        if(russian.isNotBlank()){Spacer(Modifier.height(3.dp));RussianOrMixedLine(russian,settings)}
    }
}
@Composable private fun ArabicLine(text:String,settings:ReaderSettings,modifier:Modifier=Modifier){
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){
        Text(
            cleanUiLine(text),
            modifier.fillMaxWidth(),
            textAlign=TextAlign.End,
            style=TextStyle(
                fontFamily=arabicFont(settings),
                fontSize=(21.5f*settings.arabicScale).sp,
                lineHeight=(34f*settings.arabicScale).sp,
                textDirection=TextDirection.Rtl,
                color=MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
@Composable private fun RussianOrMixedLine(text:String,settings:ReaderSettings){
    Text(
        styledMixedText(cleanUiLine(text),settings),
        Modifier.fillMaxWidth(),
        style=MaterialTheme.typography.bodyLarge.copy(
            textDirection=TextDirection.Ltr,
            lineHeight=(22f*settings.textScale*settings.lineHeight/1.55f).sp
        ),
        color=CardHeadlineBrown
    )
}
@Composable private fun InteractiveBookBlock(block:BookBlock,settings:ReaderSettings,preferArabic:Boolean){
    val lines=remember(block.text){cleanBookText(block.text).lines()}
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(7.dp)){
        lines.forEachIndexed{lineIndex,raw->
            val line=cleanUiLine(raw)
            if(line.isBlank())return@forEachIndexed

            if(inlineAnswerRun.containsMatchIn(line)){
                val prompt=answerPrompt(line)
                if(prompt.isNotBlank()){
                    val promptChunks=splitScriptChunks(prompt)
                    if(promptChunks!=null&&preferArabic) SeparatedMixedLine(promptChunks,settings)
                    else if(arabicChar.containsMatchIn(prompt)&&!cyrillicChar.containsMatchIn(prompt)) ArabicLine(prompt,settings)
                    else RussianOrMixedLine(prompt,settings)
                    Spacer(Modifier.height(2.dp))
                }
                var answer by rememberSaveable(block.index,lineIndex){mutableStateOf("")}
                val arabicAnswer=preferArabic||(
                    arabicChar.containsMatchIn(prompt)&&!cyrillicChar.containsMatchIn(prompt)
                )
                OutlinedTextField(
                    value=answer,
                    onValueChange={answer=it},
                    modifier=Modifier.fillMaxWidth(),
                    placeholder={Text("Ваш ответ")},
                    singleLine=false,
                    minLines=1,
                    maxLines=3,
                    shape=RoundedCornerShape(14.dp),
                    textStyle=if(arabicAnswer)
                        TextStyle(
                            fontFamily=arabicFont(settings),
                            fontSize=(20f*settings.arabicScale).sp,
                            lineHeight=(30f*settings.arabicScale).sp,
                            textDirection=TextDirection.Rtl,
                            textAlign=TextAlign.End
                        )
                    else MaterialTheme.typography.bodyMedium.copy(
                        fontFamily=russianFont(settings),
                        textDirection=TextDirection.Ltr
                    )
                )
            }else{
                val chunks=splitScriptChunks(line)
                val shouldSeparate=chunks!=null&&(preferArabic||startsArabic(line))
                if(shouldSeparate){
                    SeparatedMixedLine(chunks!!,settings)
                }else if(preferArabic&&arabicChar.containsMatchIn(line)){
                    ArabicLine(line,settings)
                }else{
                    RussianOrMixedLine(line,settings)
                }
            }
        }
    }
}
@Composable private fun ParagraphCard(group:ReaderGroup,settings:ReaderSettings,first:Boolean){OutlinedCard(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp),colors=CardDefaults.outlinedCardColors(containerColor=if(group.exercise)NestedSage.copy(alpha=.55f)else MaterialTheme.colorScheme.surface),border=androidx.compose.foundation.BorderStroke(1.dp,NestedSageBorder)){Column(Modifier.padding(horizontal=14.dp,vertical=12.dp)){if(first&&!group.exercise){Text("Объяснение",color=CardHeadlineBrown,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.labelLarge);Spacer(Modifier.height(7.dp))};group.blocks.forEachIndexed{i,b->InteractiveBookBlock(b,settings,false);if(i!=group.blocks.lastIndex){Spacer(Modifier.height(8.dp));HorizontalDivider(color=NestedSageBorder.copy(alpha=.55f));Spacer(Modifier.height(8.dp))}}}}}
@Composable private fun ArabicCard(group:ReaderGroup,settings:ReaderSettings){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=if(group.exercise)NestedSageStrong else NestedSage)){Column(Modifier.padding(horizontal=14.dp,vertical=12.dp)){Text(if(group.exercise)"Задание" else "Арабский пример",color=CardHeadlineBrown,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(7.dp));InteractiveBookBlock(group.blocks.first(),settings,true)}}}
@Composable private fun TableCard(group:ReaderGroup,settings:ReaderSettings){val blocks=group.blocks;val labels=blocks.firstOrNull()?.let{tableLines(it.text)}.orEmpty();val cols=blocks.drop(1).take(3).map{b->tableLines(b.text).filterNot{it.lowercase().contains("число")}};val conj=labels.size in 4..6&&labels.any{it.lowercase().contains("лицо")}&&cols.size==3&&cols.all{it.size>=labels.size};OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp),border=androidx.compose.foundation.BorderStroke(1.dp,NestedSageBorder),colors=CardDefaults.outlinedCardColors(containerColor=NestedSage.copy(alpha=.35f))){Column(Modifier.padding(12.dp)){if(conj){Text("Спряжение",color=CardHeadlineBrown,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth()){Text("Лицо",Modifier.width(88.dp),style=MaterialTheme.typography.labelMedium);listOf("Ед.","Дв.","Мн.").forEach{h->Text(h,Modifier.weight(1f),textAlign=TextAlign.Center)}};HorizontalDivider(color=NestedSageBorder);labels.forEachIndexed{row,label->Row(Modifier.fillMaxWidth().padding(vertical=7.dp),verticalAlignment=Alignment.CenterVertically){Text(label.replace("жен.рода","жен. рода").replace("муж.рода","муж. рода"),Modifier.width(88.dp),style=MaterialTheme.typography.bodySmall,color=CardHeadlineBrown);cols.forEach{col->CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){Text(cleanBookText(col.getOrNull(row).orEmpty()),Modifier.weight(1f),textAlign=TextAlign.Center,style=TextStyle(fontFamily=arabicFont(settings),fontSize=(18f*settings.arabicScale).sp,lineHeight=(26f*settings.arabicScale).sp,textDirection=TextDirection.Rtl,color=MaterialTheme.colorScheme.onSurface))}}};if(row!=labels.lastIndex)HorizontalDivider(color=NestedSageBorder.copy(alpha=.45f))}}else{Text("Таблица",color=CardHeadlineBrown,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.SemiBold);blocks.forEachIndexed{i,b->InteractiveBookBlock(b,settings,false);if(i!=blocks.lastIndex)HorizontalDivider(color=NestedSageBorder.copy(alpha=.45f))}}}}}
@Composable private fun AnswerField(group:ReaderGroup,settings:ReaderSettings){var answer by rememberSaveable(group.blocks.firstOrNull()?.index){mutableStateOf("")};OutlinedTextField(value=answer,onValueChange={answer=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Ваш ответ")},singleLine=false,minLines=1,maxLines=3,shape=RoundedCornerShape(14.dp),textStyle=TextStyle(fontFamily=arabicFont(settings),fontSize=(20f*settings.arabicScale).sp,lineHeight=(30f*settings.arabicScale).sp))}
@Composable private fun ExerciseTitleCard(group:ReaderGroup){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=NestedSageStrong)){Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Quiz,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(19.dp));Spacer(Modifier.width(8.dp));Column{Kicker("Практика");Text(group.blocks.firstOrNull()?.text?:"Упражнения",style=MaterialTheme.typography.titleSmall,color=CardHeadlineBrown)}}}}
