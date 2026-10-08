package com.xalidmuslim.arabicgrammar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.xalidmuslim.arabicgrammar.R
import com.xalidmuslim.arabicgrammar.data.ReaderSettings

val CardHeadlineBrown: Color
    @Composable get() = MaterialTheme.colorScheme.secondary
val NestedSage: Color
    @Composable get() = MaterialTheme.colorScheme.primaryContainer
val NestedSageStrong: Color
    @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
val NestedSageBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

private val Light = lightColorScheme(
    primary=Color(0xFF1F4D43), onPrimary=Color(0xFFFFFFFF), primaryContainer=Color(0xFFD8E8E0), onPrimaryContainer=Color(0xFF15352D),
    secondary=Color(0xFF6F4B35), onSecondary=Color(0xFFFFFFFF), secondaryContainer=Color(0xFFF0E1D4), onSecondaryContainer=Color(0xFF352318),
    tertiary=Color(0xFF526A75), onTertiary=Color(0xFFFFFFFF), tertiaryContainer=Color(0xFFDEE8ED), onTertiaryContainer=Color(0xFF263840),
    background=Color(0xFFF6F1E8), onBackground=Color(0xFF1C2421), surface=Color(0xFFFFFDF8), onSurface=Color(0xFF1C2421),
    surfaceVariant=Color(0xFFE9E4DA), onSurfaceVariant=Color(0xFF56615B), surfaceDim=Color(0xFFE1DDD4), surfaceBright=Color(0xFFFFFDF8),
    surfaceContainerLowest=Color(0xFFFFFFFF), surfaceContainerLow=Color(0xFFFAF7F0), surfaceContainer=Color(0xFFF2EEE5),
    surfaceContainerHigh=Color(0xFFEAE6DD), surfaceContainerHighest=Color(0xFFE2DED5), outline=Color(0xFF8F9B94), outlineVariant=Color(0xFFC5CEC9),
    error=Color(0xFFA54036), errorContainer=Color(0xFFF7DEDA), onErrorContainer=Color(0xFF4D1F1A)
)
private val Dark = darkColorScheme(
    primary=Color(0xFFACD7C5), onPrimary=Color(0xFF11372E), primaryContainer=Color(0xFF264D42), onPrimaryContainer=Color(0xFFD8F0E5),
    secondary=Color(0xFFE2C0A2), onSecondary=Color(0xFF402B1C), secondaryContainer=Color(0xFF523927), onSecondaryContainer=Color(0xFFF8E3D1),
    tertiary=Color(0xFFBCD2DE), onTertiary=Color(0xFF243943), tertiaryContainer=Color(0xFF354D58), onTertiaryContainer=Color(0xFFE0EEF4),
    background=Color(0xFF101613), onBackground=Color(0xFFEDE9E2), surface=Color(0xFF171F1B), onSurface=Color(0xFFF2EEE8),
    surfaceVariant=Color(0xFF2A332F), onSurfaceVariant=Color(0xFFCBD5CF), surfaceDim=Color(0xFF0D1210), surfaceBright=Color(0xFF323A36),
    surfaceContainerLowest=Color(0xFF0B100E), surfaceContainerLow=Color(0xFF141B18), surfaceContainer=Color(0xFF1A231F), surfaceContainerHigh=Color(0xFF202B26),
    surfaceContainerHighest=Color(0xFF27332D), outline=Color(0xFF819188), outlineVariant=Color(0xFF46564F),
    error=Color(0xFFFFB4AA), errorContainer=Color(0xFF8C1D18), onErrorContainer=Color(0xFFFFDAD5)
)
private val ArabicSegment=Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]+(?:\\s+[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF]+)*")
private val NaskhFont = FontFamily(Font(R.font.noto_naskh_arabic_regular))
fun russianFont(settings:ReaderSettings)=when(settings.fontChoice){"serif"->FontFamily.Serif;"mono"->FontFamily.Monospace;else->FontFamily.SansSerif}
fun arabicFont(settings:ReaderSettings)=when(settings.arabicFontChoice){"sans"->FontFamily.SansSerif;"system"->FontFamily.Default;else->NaskhFont}
fun styledMixedText(text:String,settings:ReaderSettings):AnnotatedString=buildAnnotatedString{var cursor=0;ArabicSegment.findAll(text).forEach{m->if(m.range.first>cursor)append(text.substring(cursor,m.range.first));pushStyle(SpanStyle(fontFamily=arabicFont(settings)));append(m.value);pop();cursor=m.range.last+1};if(cursor<text.length)append(text.substring(cursor))}
@Composable fun ArabicGrammarTheme(settings:ReaderSettings,darkSystem:Boolean,content: @Composable () -> Unit){val dark=settings.theme=="dark"||(settings.theme=="system"&&darkSystem);val base=russianFont(settings);val s=settings.textScale;val t=MaterialTheme.typography.copy(bodyLarge=TextStyle(fontFamily=base,fontSize=(15.5f*s).sp,lineHeight=(22.5f*s).sp),bodyMedium=TextStyle(fontFamily=base,fontSize=(14f*s).sp,lineHeight=(20.5f*s).sp),bodySmall=TextStyle(fontFamily=base,fontSize=(12.5f*s).sp,lineHeight=(18f*s).sp),titleLarge=TextStyle(fontFamily=FontFamily.Serif,fontSize=(24f*s).sp,lineHeight=(28f*s).sp),titleMedium=TextStyle(fontFamily=FontFamily.Serif,fontSize=(19.5f*s).sp,lineHeight=(23f*s).sp),titleSmall=TextStyle(fontFamily=base,fontSize=(15f*s).sp,lineHeight=(19.5f*s).sp),labelLarge=TextStyle(fontFamily=base,fontSize=(13f*s).sp),labelMedium=TextStyle(fontFamily=base,fontSize=(11.5f*s).sp));MaterialTheme(colorScheme=if(dark)Dark else Light,typography=t,content=content)}
