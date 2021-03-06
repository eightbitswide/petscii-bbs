package eu.sblendorio.bbs.tenants;

import static eu.sblendorio.bbs.core.Colors.BLUE;
import static eu.sblendorio.bbs.core.Colors.CYAN;
import static eu.sblendorio.bbs.core.Colors.GREEN;
import static eu.sblendorio.bbs.core.Colors.GREY1;
import static eu.sblendorio.bbs.core.Colors.GREY2;
import static eu.sblendorio.bbs.core.Colors.GREY3;
import static eu.sblendorio.bbs.core.Colors.LIGHT_BLUE;
import static eu.sblendorio.bbs.core.Colors.LIGHT_GREEN;
import static eu.sblendorio.bbs.core.Colors.LIGHT_RED;
import static eu.sblendorio.bbs.core.Colors.PURPLE;
import static eu.sblendorio.bbs.core.Colors.RED;
import static eu.sblendorio.bbs.core.Colors.WHITE;
import static eu.sblendorio.bbs.core.Colors.YELLOW;
import static eu.sblendorio.bbs.core.Keys.DEL;
import static eu.sblendorio.bbs.core.Keys.RETURN;
import static eu.sblendorio.bbs.core.Keys.REVOFF;
import static eu.sblendorio.bbs.core.Keys.REVON;
import static eu.sblendorio.bbs.core.Keys.RIGHT;
import static eu.sblendorio.bbs.core.Keys.SPACE_CHAR;
import static eu.sblendorio.bbs.core.Keys.UP;
import static eu.sblendorio.bbs.core.Utils.filterPrintableWithNewline;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.rometools.rome.io.FeedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

import com.google.common.collect.ImmutableMap;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import eu.sblendorio.bbs.core.HtmlUtils;
import eu.sblendorio.bbs.core.PetsciiThread;

public class TelevideoRai extends PetsciiThread {
    static final String HR_TOP = StringUtils.repeat(chr(163), 39);

    static final String PREFIX = "http://www.servizitelevideo.rai.it/televideo/pub/";
    protected int screenRows = 19;

    static class NewsSection {
        final int color;
        final String title;
        final String url;
        final byte[] logo;

        public NewsSection(int color, String title, String url, byte[] logo) {
            this.color = color; this.title = title; this.url = url; this.logo = logo;
        }
    }

    static class NewsFeed {
        final Date publishedDate;
        final String title;
        final String description;
        final String uri;

        public NewsFeed(Date publishedDate, String title, String description, String uri) {
            this.publishedDate = publishedDate; this.title = title; this.description = description; this.uri = uri;
        }

        @Override
        public String toString() {
            return "Title: "+title+"\nDate:"+publishedDate+"\nDescription:"+description+"\n";
        }
    }

    static Map<String, NewsSection> sections = new ImmutableMap.Builder<String, NewsSection>()
        .put("101", new NewsSection(WHITE, "Ultim'Ora", PREFIX + "rss101.xml", Logos.LOGO_ULTIMORA))
        .put("102", new NewsSection(CYAN, "24h No Stop", PREFIX + "rss102.xml", Logos.LOGO_NOSTOP24H))
        .put("110", new NewsSection(RED, "Primo Piano", PREFIX + "rss110.xml", Logos.LOGO_PRIMOPIANO))
        .put("120", new NewsSection(GREEN, "Politica", PREFIX + "rss120.xml", Logos.LOGO_POLITICA))
        .put("130", new NewsSection(BLUE, "Economia", PREFIX + "rss130.xml", Logos.LOGO_ECONOMIA))
        .put("140", new NewsSection(GREY2, "Dall'Italia", PREFIX + "rss140.xml", Logos.LOGO_DALLITALIA))
        .put("150", new NewsSection(LIGHT_BLUE, "Dal Mondo", PREFIX + "rss150.xml", Logos.LOGO_DALMONDO))
        .put("160", new NewsSection(LIGHT_RED, "Culture", PREFIX + "rss160.xml", Logos.LOGO_CULTURE))
        .put("170", new NewsSection(PURPLE, "Cittadini", PREFIX + "rss170.xml", Logos.LOGO_CITTADINI))
        .put("180", new NewsSection(GREY3, "Speciale", PREFIX + "rss180.xml", Logos.LOGO_SPECIALE))
        .put("190", new NewsSection(LIGHT_RED, "Atlante Crisi", PREFIX + "rss190.xml", Logos.LOGO_ATLANTECRISI))
        .put("229", new NewsSection(LIGHT_GREEN, "Brevi Calcio", PREFIX + "rss229.xml", Logos.LOGO_BREVICALCIO))
        .put("230", new NewsSection(YELLOW, "CalcioSquadre", PREFIX + "rss230.xml", Logos.LOGO_CALCIOSQUADRE))
        .put("260", new NewsSection(GREEN, "Altri Sport", PREFIX + "rss260.xml", Logos.LOGO_ALTRISPORT))
        .put("299", new NewsSection(GREY1, "Brevissime", PREFIX + "rss299.xml", Logos.LOGO_SPORTBREVISSIME))
        .put("810", new NewsSection(GREY2, "Motori", PREFIX + "rss810.xml", Logos.LOGO_MOTORI))
        .build();

    private void printChannelList() {
        gotoXY(0, 5);
        List<String> keys = new LinkedList<>(sections.keySet());
        Collections.sort(keys);
        for (int i=0; i<8; ++i) {
            int even = i;
            if (even >= keys.size()) break;
            String key = keys.get(even);
            NewsSection value = sections.get(key);
            write(RIGHT, value.color, REVON, SPACE_CHAR);
            print(key); write(SPACE_CHAR, REVOFF, SPACE_CHAR);
            String title = substring(value.title + "                    ", 0, 12);
            print(title);
            print(" ");

            int odd = even+8;
            if (odd < keys.size()) {
                key = keys.get(odd);
                value = sections.get(key);
                write(value.color, REVON, SPACE_CHAR);
                print(key);
                write(SPACE_CHAR, REVOFF, SPACE_CHAR);
                print(value.title);
            } else {
                write(WHITE, REVON, SPACE_CHAR);
                print(" . ");
                write(SPACE_CHAR, REVOFF, SPACE_CHAR);
                print("Fine");
            }
            newline();
            newline();

        }
        write(RIGHT, WHITE, REVON, SPACE_CHAR);
        print(" . ");
        write(SPACE_CHAR, REVOFF, SPACE_CHAR);
        print("Fine");
        write(GREY3, RETURN, RETURN);
        flush();
    }

    public static List<NewsFeed> getFeeds(String urlString) throws IOException, FeedException {
        URL url = new URL(urlString);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(url));
        List<NewsFeed> result = new LinkedList<>();
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry e : entries)
            result.add(new NewsFeed(e.getPublishedDate(), e.getTitle(), e.getDescription().getValue(), e.getUri()));
        return result;
    }

    @Override
    public void doLoop() throws Exception {
        log("Entered TelevideoRai");
        while (true) {
            cls();
            drawLogo();
            printChannelList();
            String command = null;
            NewsSection choice;
            boolean inputFail;
            do {
                print(" > ");
                flush();
                resetInput();
                command = readLine(3);
                choice = sections.get(command);
                inputFail = (choice == null && !trim(command).equals("."));
                if (inputFail) {
                    write(UP); println("        "); write(UP);
                }
            } while (inputFail);
            if (trim(command).equals(".")) {
                break;
            }
            log("Televideo choice = " + command + " " + (choice == null ? EMPTY : choice.title));
            view(choice);
        }
        log("Televideo-EXIT");
    }

    private void view(NewsSection section) throws IOException, FeedException {
        if (section == null) {
            return;
        }

        cls();
        waitOn();
        List<NewsFeed> feeds = getFeeds(section.url);
        String text = EMPTY;
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        for (NewsFeed feed: feeds) {
            String post = EMPTY;
            post += feed.title + "<br>" + HR_TOP + "<br>";
            post += dateFormat.format(feed.publishedDate) + " " + feed.description + "<br>";
            int lineFeeds = (screenRows - (wordWrap(post).length % screenRows)) % screenRows;

            post += StringUtils.repeat("&c64nbsp;<br>", lineFeeds);
            text += post;
        }
        waitOff();

        boolean interruptByUser = displayText(text, screenRows, section.logo);
        if (!interruptByUser) {
            gotoXY(0, 24); write(WHITE); print(" ENTER = MAIN MENU                    ");
            flush(); resetInput(); readKey();
        }
    }

    protected boolean displayText(String text, int screenRows, byte[] logo) throws IOException {
        cls();
        write(defaultIfNull(logo, LOGO_TELEVIDEO));
        write(GREY3);

        String[] rows = wordWrap(text);
        int page = 1;
        int j = 0;
        boolean forward = true;
        while (j < rows.length) {
            if (j>0 && j % screenRows == 0 && forward) {
                println();
                write(WHITE);
                print("-PAGE " + page + "-  SPACE=NEXT  -=PREV  .=EXIT");
                write(GREY3);

                resetInput(); int ch = readKey();
                if (ch == '.') {
                    return true;
                }
                if (ch == '-' && page > 1) {
                    j -= (screenRows * 2);
                    --page;
                    forward = false;
                    cls();
                    write(logo == null ? LOGO_TELEVIDEO : logo);
                    write(GREY3);
                    continue;
                } else {
                    ++page;
                }
                cls();
                write(logo == null ? LOGO_TELEVIDEO : logo);
                write(GREY3);
            }
            String row = rows[j];
            println(row.replace("&c64nbsp;", EMPTY));
            forward = true;
            ++j;
        }
        println();
        return false;
    }

    protected String[] wordWrap(String s) {
        String[] cleaned = filterPrintableWithNewline(HtmlUtils.htmlClean(s)).replaceAll(" +", " ").split("\n");
        List<String> result = new LinkedList<>();
        for (String item: cleaned) {
            String[] wrappedLine = WordUtils
                    .wrap(item, 39, "\n", true)
                    .split("\n");
            result.addAll(asList(wrappedLine));
        }
        return Arrays.copyOf(result.toArray(), result.size(), String[].class);
    }

    private void drawLogo() {
        write(LOGO_TELEVIDEO);
        write(GREY3);
    }

    private static final byte[] LOGO_TELEVIDEO = new byte[] {
        32, 32, 18, -98, 32, 32, 32, 32, -110, -95, 18, 32, 32, 32, -110, -95,
        18, 32, -110, -95, 32, 32, 18, 32, 32, 32, -110, -95, 18, 32, -110, -95,
        32, 18, 32, -95, 32, -95, 32, 32, -68, -110, 32, 18, 32, 32, 32, -110,
        -95, 18, -66, 32, 32, -68, -110, 13, 32, 32, 18, -94, -69, 32, -94, -110,
        -66, 18, 32, -68, -110, -94, 32, 18, 32, -110, -95, 32, 32, 18, 32, -68,
        -110, -94, 32, 18, -69, -110, -95, 32, 18, -84, -95, 32, -95, 32, -94, -69,
        -110, -95, 18, 32, -68, -110, -94, 32, 18, 32, -84, -94, 32, -110, 13, 32,
        32, 32, 18, -95, 32, -110, 32, 32, 18, 32, -68, -110, -94, -69, 18, 32,
        32, 32, -110, -95, 18, 32, -68, -110, -94, -69, -68, 18, 32, 32, -110, -66,
        18, -95, 32, -95, 32, 32, 32, -110, -95, 18, 32, -68, -110, -94, -69, 18,
        32, 32, 32, 32, -110, 13, 32, 32, 32, -68, 18, -94, -110, 32, 32, 18,
        -94, -94, -94, -110, -66, 18, -94, -94, -94, -110, -66, 18, -94, -94, -94, -110,
        -66, 32, 18, -94, -94, -110, 32, -68, 18, -94, -110, -68, 18, -94, -94, -94,
        -110, 32, 18, -94, -94, -94, -110, -66, -68, 18, -94, -94, -110, -66, 13
    };

    protected void waitOn() {
        print("PLEASE WAIT...");
        flush();
    }

    protected void waitOff() {
        for (int i=0; i<14; ++i) write(DEL);
        flush();
    }

    static class Logos {
        static final byte[] LOGO_ULTIMORA = new byte[] {
            32, 32, 32, 18, -98, 32, -110, -95, 32, 18, 32, -95, 32, -110, 32, 18,
            32, 32, 32, 32, -110, -95, 18, 32, -110, -95, 18, 32, -68, -110, -84, 18,
            32, -95, -110, -84, 18, 32, 32, 32, -110, -69, 18, 32, 32, 32, 32, -110,
            -69, 18, -66, 32, 32, -68, -110, 13, 32, 32, 32, 18, 32, -110, -95, 32,
            18, 32, -95, 32, -110, 32, 18, -94, -69, 32, -94, -110, -66, 18, 32, -110,
            -95, 18, 32, 32, 32, 32, -110, 32, 18, -95, 32, -94, -69, -110, -95, 18,
            32, -68, -110, -94, 18, -66, -110, -66, 18, 32, -84, -94, 32, -110, 13, 32,
            32, 32, 18, 32, 32, 32, 32, -95, 32, 32, 32, -95, 32, -110, 32, 32,
            18, 32, -110, -95, 18, 32, -110, -95, -95, 18, 32, -110, 32, 18, -95, 32,
            32, 32, -110, -95, 18, 32, -110, -95, 18, -95, -110, -94, -69, 18, 32, -84,
            -94, 32, -110, 13, 32, 32, 32, -68, 18, -94, -94, -110, -66, -68, 18, -94,
            -94, -94, -110, -68, 18, -94, -110, 32, 32, 18, -94, -110, -66, 18, -94, -110,
            -66, 32, 18, -94, -110, 32, 32, 18, -94, -94, -94, -110, 32, 18, -94, -110,
            -66, -68, 18, -94, -110, -66, 18, -94, -110, -66, 32, 18, -94, -110, 13
        };

        static final byte[] LOGO_POLITICA = new byte[] {
            32, 32, 32, 32, 32, 18, -98, 32, 32, 32, -68, -110, -84, 18, 32, 32,
            32, -110, -69, 18, 32, -110, -95, 32, 32, 18, 32, -110, -95, 18, 32, 32,
            32, 32, -110, -95, 18, 32, -110, -95, 18, -66, 32, 32, 32, -110, -84, 18,
            32, 32, 32, -110, -69, 13, 32, 32, 32, 32, 32, 18, 32, -84, -94, 32,
            -95, 32, -94, -69, -110, -95, 18, 32, -110, -95, 32, 32, 18, 32, -110, -95,
            18, -94, -69, 32, -94, -110, -66, 18, 32, -110, -95, 18, 32, -84, -94, -94,
            -95, 32, -94, -69, -110, -95, 13, 32, 32, 32, 32, 32, 18, 32, -84, -94,
            -110, -66, 18, -95, 32, 32, 32, -110, -95, 18, 32, -68, -110, -94, -69, 18,
            32, -110, -95, 32, 18, -95, 32, -110, 32, 32, 18, 32, -110, -95, 18, 32,
            32, 32, 32, -95, 32, -94, -69, -110, -95, 13, 32, 32, 32, 32, 32, 18,
            -94, -110, -66, 32, 32, 32, 18, -94, -94, -94, -110, 32, 18, -94, -94, -94,
            -110, -66, 18, -94, -110, -66, 32, -68, 18, -94, -110, 32, 32, 18, -94, -110,
            -66, -68, 18, -94, -94, -94, -110, -68, 18, -94, -110, 32, -68, -66, 13
        };

        static final byte[] LOGO_ECONOMIA = new byte[] {
            32, 32, 32, 32, 18, -98, 32, 32, 32, -110, -95, 18, -66, 32, 32, -110,
            -95, 18, -66, 32, 32, -68, -95, 32, -110, -69, 18, 32, -110, -95, 18, -66,
            32, 32, -68, -95, 32, -110, -69, 18, -66, -110, -95, 18, 32, -110, -95, 18,
            -66, 32, 32, -68, -110, 13, 32, 32, 32, 32, 18, 32, -68, -110, -94, 32,
            18, 32, -84, -94, -110, -66, 18, 32, -84, -94, 32, -95, 32, 32, 32, -110,
            -95, 18, 32, -84, -94, 32, -95, 32, 32, 32, -110, -95, 18, 32, -110, -95,
            18, 32, -84, -94, 32, -110, 13, 32, 32, 32, 32, 18, 32, -68, -110, -94,
            -69, 18, 32, 32, 32, -110, -95, 18, 32, 32, 32, 32, -95, 32, -69, 32,
            -110, -95, 18, 32, 32, 32, 32, -95, 32, -95, -95, -110, -95, 18, 32, -110,
            -95, 18, 32, -84, -94, 32, -110, 13, 32, 32, 32, 32, 18, -94, -94, -94,
            -110, -66, -68, 18, -94, -94, -110, -66, -68, 18, -94, -94, -110, -66, -68, 18,
            -94, -110, 32, 18, -94, -110, -66, -68, 18, -94, -94, -110, -66, -68, 18, -94,
            -110, 32, -68, -66, 18, -94, -110, -66, 18, -94, -110, -66, 32, 18, -94, -110,
            13
        };

        static final byte[] LOGO_DALLITALIA = new byte[] {
            18, -98, 32, 32, 32, -110, -69, -84, 18, 32, 32, 32, -110, -69, 18, 32,
            -110, -95, 32, 32, 18, 32, -110, -95, 32, 18, -95, -110, -95, 18, 32, -110,
            -95, 18, 32, 32, 32, 32, -110, -95, 18, -66, 32, 32, -68, -95, 32, -110,
            32, 32, 18, -95, 32, -110, -84, 18, 32, 32, 32, -110, -69, 18, 32,
            -84, -94, 32, -95, 32, -94, -69, -110, -95, 18, 32, -110, -95, 32, 32, 18,
            32, -110, -95, 32, 32, 32, 18, 32, -110, -95, 18, -94, -69, 32, -94, -110,
            -66, 18, 32, -84, -94, 32, -95, 32, -110, 32, 32, 18, -95, 32, -95, 32,
            -94, -69, -110, -95, 18, 32, 32, 32, 32, -95, 32, -94, -69, -110, -95,
            18, 32, 32, 32, -110, -95, 18, 32, 32, 32, -110, -95, 32, 18, 32, -110,
            -95, 32, 18, -95, 32, -110, 32, 32, 18, 32, -84, -94, 32, -95, 32, 32,
            32, -95, 32, -95, 32, -94, -69, -110, -95, 18, -94, -94, -94, -110, -66,
            -68, 18, -94, -110, 32, -68, -66, 18, -94, -94, -94, -110, -66, 18, -94, -94,
            -94, -110, -66, 32, 18, -94, -110, -66, 32, -68, 18, -94, -110, 32, 32, 18,
            -94, -110, -66, 32, 18, -94, -110, -68, 18, -94, -94, -94, -110, -68, 18, -94,
            -110, -68, 18, -94, -110, 32, -68, -66
        };

        static final byte[] LOGO_DALMONDO = new byte[] {
            32, 32, 18, -98, 32, 32, 32, -110, -69, -84, 18, 32, 32, 32, -110, -69,
            18, 32, -110, -95, 32, 32, 32, 18, -95, 32, -110, -69, 18, -66, -110, -95,
            18, -66, 32, 32, -68, -95, 32, -110, -69, 18, 32, -95, 32, 32, -68, -110,
            32, 18, -66, 32, 32, -68, -110, 13, 32, 32, 18, 32, -84, -94, 32, -95,
            32, -94, -69, -110, -95, 18, 32, -110, -95, 32, 32, 32, 18, -95, 32, 32,
            32, -110, -95, 18, 32, -84, -94, 32, -95, 32, 32, 32, -95, 32, -94, -69,
            -110, -95, 18, 32, -84, -94, 32, -110, 13, 32, 32, 18, 32, 32, 32, 32,
            -95, 32, -94, -69, -110, -95, 18, 32, 32, 32, -110, -95, 32, 18, -95, 32,
            -95, -95, -110, -95, 18, 32, 32, 32, 32, -95, 32, -69, 32, -95, 32, 32,
            32, -110, -95, 18, 32, 32, 32, 32, -110, 13, 32, 32, 18, -94, -94, -94,
            -110, -66, -68, 18, -94, -110, 32, -68, -66, 18, -94, -94, -94, -110, -66, 32,
            -68, 18, -94, -110, 32, -68, -66, -68, 18, -94, -94, -110, -66, -68, 18, -94,
            -110, 32, 18, -94, -110, -68, 18, -94, -94, -94, -110, 32, -68, 18, -94, -94,
            -110, -66, 13
        };

        static final byte[] LOGO_CULTURE = new byte[] {
            32, 32, 32, 32, 32, 32, 18, -98, -66, 32, 32, -110, -95, 18, 32, -110,
            -95, 32, 18, 32, -95, 32, -110, 32, 18, 32, 32, 32, 32, -110, -95, 18,
            32, -110, -95, 32, 18, 32, -95, 32, 32, -68, -110, -69, 18, 32, 32, 32,
            -110, -95, 13, 32, 32, 32, 32, 32, 32, 18, 32, -84, -94, -110, -66, 18,
            32, -110, -95, 32, 18, 32, -95, 32, -110, 32, 18, -94, -69, 32, -94, -110,
            -66, 18, 32, -110, -95, 32, 18, 32, -95, 32, -110, -94, 18, -66, -110, -66,
            18, 32, -68, -110, -94, 13, 32, 32, 32, 32, 32, 32, 18, 32, 32, 32,
            -110, -95, 18, 32, 32, 32, 32, -95, 32, 32, 32, -95, 32, -110, 32, 32,
            18, 32, 32, 32, 32, -95, 32, -95, -110, -94, -69, 18, 32, -68, -110, -94,
            -69, 13, 32, 32, 32, 32, 32, 32, -68, 18, -94, -94, -110, -66, -68, 18,
            -94, -94, -110, -66, -68, 18, -94, -94, -94, -110, -68, 18, -94, -110, 32, 32,
            -68, 18, -94, -94, -110, -66, -68, 18, -94, -110, -68, 18, -94, -110, -66, 18,
            -94, -94, -94, -110, -66, 13
        };

        static final byte[] LOGO_BREVICALCIO = new byte[] {
            32, 32, 32, 32, 32, 18, -98, -66, 32, 32, -110, -95, 18, -66, 32, 32,
            -68, -95, 32, -110, 32, 32, -84, 18, 32, 32, 32, -95, -110, -95, 18, -66,
            32, 32, -68, -110, 13, 32, 32, 32, 32, 32, 18, 32, -84, -94, -110, -66,
            18, 32, -84, -94, 32, -95, 32, -110, 32, 32, 18, -95, 32, -94, -94, -95,
            -110, -95, 18, 32, -84, -94, 32, -110, 32, 32, 32, 5, -62, -46, -59, -42,
            -55, 13, 32, 32, 32, 32, 32, 18, -98, 32, 32, 32, -110, -95, 18, 32,
            -84, -94, 32, -95, 32, 32, 32, -95, 32, 32, 32, -95, -110, -95, 18, 32,
            32, 32, 32, -110, 32, 32, 32, 5, -94, -94, -94, -94, -94, 13, 32, 32,
            32, 32, 32, -98, -68, 18, -94, -94, -110, -66, 18, -94, -110, -66, 32, 18,
            -94, -110, -68, 18, -94, -94, -94, -110, 32, 18, -94, -94, -94, -110, -68, -66,
            -68, 18, -94, -94, -110, -66, 13
        };

        static final byte[] LOGO_CALCIOSQUADRE = new byte[] {
            32, 32, 32, 32, 18, -98, -66, 32, 32, -110, -95, 18, -66, 32, 32, -68,
            -95, 32, -110, 32, 32, -84, 18, 32, 32, 32, -95, -110, -95, 18, -66, 32,
            32, -68, -110, 13, 32, 32, 32, 32, 18, 32, -84, -94, -110, -66, 18, 32,
            -84, -94, 32, -95, 32, -110, 32, 32, 18, -95, 32, -94, -94, -95, -110, -95,
            18, 32, -84, -94, 32, -110, 32, 32, 32, 5, -45, -47, -43, -63, -60, -46,
            -59, 13, 32, 32, 32, 32, 18, -98, 32, 32, 32, -110, -95, 18, 32, -84,
            -94, 32, -95, 32, 32, 32, -95, 32, 32, 32, -95, -110, -95, 18, 32, 32,
            32, 32, -110, 32, 32, 32, 5, -94, -94, -94, -94, -94, -94, -94, 13, 32,
            32, 32, 32, -98, -68, 18, -94, -94, -110, -66, 18, -94, -110, -66, 32, 18,
            -94, -110, -68, 18, -94, -94, -94, -110, 32, 18, -94, -94, -94, -110, -68, -66,
            -68, 18, -94, -94, -110, -66, 13
        };

        static final byte[] LOGO_ALTRISPORT = new byte[] {
            32, 32, 32, 32, 32, 32, 32, 32, 32, 18, -98, -66, 32, 32, -110, 32,
            18, 32, 32, 32, -68, -110, 5, -63, -52, -44, -46, -55, 18, -98, -95, 32,
            32, -68, -95, 32, 32, 32, 32, -110, 13, 32, 32, 32, 32, 32, 32, 32,
            32, 32, 18, -69, -68, -110, -94, 32, 18, 32, -94, -69, 32, -110, -84, 18,
            32, 32, 32, -68, -95, -68, -110, -94, 18, -84, -110, 32, 32, 18, 32, -110,
            -95, 13, 32, 32, 32, 32, 32, 32, 32, 32, 32, -94, -94, 18, -66, -110,
            -95, 18, 32, -94, -94, -110, -66, 18, -95, 32, -110, -94, 18, -66, 32, -95,
            -110, -95, 18, -95, -110, -94, 32, 32, 18, 32, -110, -95, 13, 32, 32, 32,
            32, 32, 32, 32, 32, 32, 18, -94, -94, -94, -110, 32, 18, -94, -110, 32,
            32, 32, 32, 18, -94, -94, -94, -110, -66, -68, -66, -68, 18, -94, -110, 32,
            32, 18, -94, -110, -66, 13
        };

        static final byte[] LOGO_MOTORI = new byte[] {
            32, 32, 32, 32, 32, 32, 18, -98, -95, 32, -110, -69, 18, -66, 32, -110,
            -84, 18, 32, 32, 32, -110, -69, 18, 32, 32, 32, 32, -110, -95, 18, -66,
            32, 32, -68, -95, 32, 32, 32, -68, -95, 32, -110, 13, 32, 32, 32, 32,
            32, 32, 18, -95, 32, 32, 32, 32, -95, 32, -94, -69, -110, -95, 18, -94,
            -69, 32, -94, -110, -66, 18, 32, -84, -94, 32, -95, 32, -110, -94, 18, -66,
            -84, -95, 32, -110, 13, 32, 32, 32, 32, 32, 32, 18, -95, 32, -95, -95,
            32, -95, 32, 32, 32, -110, -95, 32, 18, -95, 32, -110, 32, 32, 18, 32,
            32, 32, 32, -95, 32, -110, 32, 18, -68, -110, -94, 18, -95, 32, -110, 13,
            32, 32, 32, 32, 32, 32, -68, 18, -94, -110, 32, -68, 18, -94, -110, 32,
            18, -94, -94, -94, -110, 32, 32, -68, 18, -94, -110, 32, 32, -68, 18, -94,
            -94, -110, -66, -68, 18, -94, -110, 32, 18, -94, -94, -110, -68, 18, -94, -110,
            13
        };

        static final byte[] LOGO_SPORTBREVISSIME = new byte[] {
            32, 32, 32, 18, -98, -66, 32, 32, -110, 32, 18, 32, 32, 32, -68, -110,
            -84, 18, 32, 32, 32, -68, -95, 32, 32, -68, -95, 32, 32, 32, 32, -110,
            13, 32, 32, 32, 18, -69, -68, -110, -94, 32, 18, 32, -94, -69, 32, -95,
            32, -94, -94, 32, -95, -68, -110, -94, 18, -84, -110, 32, 32, 18, 32, -110,
            -95, 32, 32, 5, -62, -46, -59, -42, -55, -45, -45, -55, -51, -59, 13, 32,
            32, 32, -98, -94, -94, 18, -66, -110, -95, 18, 32, -94, -94, -110, -66, 18,
            -95, 32, 32, 32, 32, -95, -110, -95, 18, -95, -110, -94, 32, 32, 18, 32,
            -110, -95, 32, 32, 5, -94, -94, -94, -94, -94, -94, -94, -94, -94, -94, 13,
            32, 32, 32, 18, -98, -94, -94, -94, -110, 32, 18, -94, -110, 32, 32, 32,
            32, 18, -94, -94, -94, -110, -66, -68, -66, -68, 18, -94, -110, 32, 32, 18,
            -94, -110, -66, 13
        };

        static final byte[] LOGO_PRIMOPIANO = new byte[] {
            18, -98, 32, 32, 32, -68, -95, 32, 32, 32, -110, -69, 18, 32, -110, -95,
            18, 32, -68, -110, -84, 18, 32, -110, -84, 18, 32, 32, 32, -110, -69, 32,
            18, 32, 32, 32, -68, -95, 32, -110, -84, 18, 32, 32, -68, -95, -68, -95,
            -110, -95, 18, -66, 32, 32, -68, -110, 13, 18, 32, -84, -94, 32, -95, 32,
            -94, -69, -110, -95, 18, 32, -110, -95, 18, 32, 32, 32, 32, -95, 32, -94,
            -69, -110, -95, 32, 18, 32, -84, -94, 32, -95, 32, -95, -84, -94, 32, -95,
            32, 32, -110, -95, 18, 32, -94, -94, 32, -110, 13, 18, 32, -84, -94, -110,
            -66, 18, -95, 32, -94, 32, -110, 32, 18, 32, -110, -95, 18, 32, -110, -95,
            -95, 18, 32, -95, 32, 32, 32, -110, -95, 32, 18, 32, -84, -94, -110, -66,
            18, -95, 32, -95, -84, -94, 32, -95, -110, -95, 18, -69, -110, -95, 18, 32,
            32, 32, 32, -110, 13, 18, -94, -110, -66, 32, 32, -68, 18, -94, -110, 32,
            18, -94, -110, -66, 18, -94, -110, -66, 18, -94, -110, -66, 32, 18, -94, -110,
            32, 18, -94, -94, -94, -110, 32, 32, 18, -94, -110, -66, 32, 32, -68, 18,
            -94, -110, -68, -66, 32, 18, -94, -110, -68, -66, -68, -66, -68, 18, -94, -94,
            -110, -66, 13
        };

        static final byte[] LOGO_NOSTOP24H = new byte[] {
            32, 18, -98, -66, 32, 32, -110, -69, 18, -95, -110, -95, 32, 32, 18, -95,
            -110, -95, 32, 32, 32, 32, 18, 5, -95, -68, -95, -110, -95, 18, -66, -94,
            -68, -110, 32, 32, -84, 18, -84, -69, -110, -69, 18, -94, 32, -94, -110, -84,
            18, -84, -69, -110, -69, 18, 32, -94, -68, -110, 13, 32, 18, -98, -94, -110,
            -84, 18, 32, -110, -66, 18, -95, -68, 32, -110, -69, 18, -95, 32, 32, -110,
            -69, 32, 32, 18, 5, -95, -84, 32, -110, -95, 18, 32, -110, 32, 18, 32,
            -110, 32, 32, -68, 18, -68, -110, -94, 32, 32, 18, 32, -110, 32, 18, -95,
            -110, -95, 18, -95, -110, -95, 18, 32, -110, -94, 18, -84, -110, 13, 32, -98,
            -84, 18, 32, -68, -110, -69, -68, 18, -94, 32, -110, -66, 18, -95, -110, -95,
            18, -95, -110, -95, 32, 32, 18, 5, -95, -110, -95, 18, -95, -110, -95, 18,
            32, -110, 32, 18, 32, -110, 32, 32, -84, -69, 18, -95, -110, -95, 32, 18,
            32, -110, 32, 18, -95, -110, -95, 18, -95, -110, -95, 18, 32, -110, 13, 32,
            18, -98, -94, -94, -94, -110, -66, 32, 32, 18, -94, -110, 32, -68, -66, -68,
            -66, 32, 32, 5, -68, -66, -68, -66, -68, 18, -94, -110, -66, 32, 32, 32,
            18, -94, -94, -110, 32, 32, 18, -94, -110, 32, 32, 18, -94, -94, -110, 32,
            18, -94, -110, 13
        };

        static final byte[] LOGO_SPECIALE = new byte[] {
            32, 32, 32, 32, 18, -98, -66, 32, -110, -95, 18, -95, 32, 32, -110, -69,
            18, -95, 32, 32, -110, -95, -84, 18, 32, 32, -68, -110, 32, 18, 32, 32,
            -110, 32, 18, -66, 32, 32, -110, -69, 18, -95, -110, -95, 32, 32, 18, -95,
            32, 32, -110, -95, 13, 32, 32, 32, 32, 18, -69, -110, -94, 32, 18, -95,
            -68, -66, -110, -95, 18, -95, -68, -110, -69, 32, 18, -95, -110, -95, 32, 18,
            -94, -110, 32, 18, -95, -110, -95, 32, 18, 32, -110, 32, 18, -95, -110, -95,
            18, -95, -110, -95, 32, 32, 18, -95, -68, -110, -69, 13, 32, 32, 32, 32,
            -94, 18, -66, -110, -95, 18, -95, -84, -94, -110, 32, 18, -95, -68, -110, -94,
            -69, 18, -95, -68, -110, -94, 18, 32, -110, 32, 18, -66, -68, -110, 32, 18,
            32, -94, -69, -110, -95, 18, -95, -68, -110, -94, -69, 18, -95, -68, -110, -94,
            -69, 13, 32, 32, 32, 32, 18, -94, -94, -110, 32, -68, -66, 32, 32, -68,
            18, -94, -94, -110, -66, 32, 18, -94, -94, -110, -66, 32, 18, -94, -94, -110,
            32, 18, -94, -110, 32, -68, -66, -68, 18, -94, -94, -110, -66, -68, 18, -94,
            -94, -110, -66, 13
        };

        static final byte[] LOGO_ATLANTECRISI = new byte[] {
            18, -98, -66, 32, -68, -95, 32, 32, 32, -95, -110, -95, 32, 32, 18, -66,
            32, -68, -95, -68, -95, -110, -95, 18, 32, 32, 32, -110, -95, 18, 32, 32,
            -110, -95, 32, 18, -66, 32, -68, -95, 32, 32, -110, -69, 18, 32, -110, -84,
            18, 32, 32, -110, -95, 18, 32, -110, 13, 18, 32, -110, 32, 18, 32, -110,
            32, 18, -95, 32, -110, 32, 18, -95, -110, -95, 32, 32, 18, 32, -110, 32,
            18, 32, -95, 32, 32, -110, -95, 32, 18, 32, -110, -95, 32, 18, 32, -110,
            -94, 32, 32, 18, 32, -110, 32, 18, -94, -95, -110, -95, 18, -95, -110, -95,
            18, 32, -110, -68, 18, -68, -110, -94, 32, 18, 32, -110, 13, 18, 32, -94,
            32, -110, 32, 18, -95, 32, -110, 32, 18, -95, -68, -110, -94, -69, 18, 32,
            -94, 32, -95, -110, -95, 18, -69, -110, -95, 32, 18, 32, -110, -95, 32, 18,
            32, -110, -94, -69, 32, 18, 32, -110, -94, 18, 32, -95, -84, -69, -110, -69,
            18, 32, -110, -84, -94, 18, -66, -110, -95, 18, 32, -110, 13, 18, -94, -110,
            32, 18, -94, -110, 32, -68, 18, -94, -110, 32, -68, 18, -94, -94, -110, -66,
            18, -94, -110, 32, 18, -94, -110, -68, -66, -68, -66, 32, 18, -94, -110, -66,
            32, 18, -94, -94, -110, -66, 32, -68, 18, -94, -110, -66, -68, -66, -68, -66,
            18, -94, -110, -68, 18, -94, -94, -110, 32, 18, -94, -110, 13
        };

        static final byte[] LOGO_CITTADINI = new byte[] {
            32, 32, 32, 18, -98, -66, 32, 32, -110, -95, 18, 32, -110, -95, 18, 32,
            32, 32, 32, 32, -95, 32, 32, 32, 32, -110, -84, 18, 32, 32, 32, -110,
            -69, 18, 32, 32, 32, -110, -69, 18, -95, 32, -95, 32, -110, -69, 18, 32,
            -95, 32, -110, 13, 32, 32, 32, 18, 32, -84, -94, -110, -66, 18, 32, -110,
            -95, 18, -94, -69, 32, -84, -94, -110, -68, 18, -94, 32, -84, -94, -95, 32,
            -94, -69, -110, -95, 18, 32, -84, -94, 32, -95, 32, -95, 32, 32, 32, -95,
            32, -110, 13, 32, 32, 32, 18, 32, 32, 32, -110, -95, 18, 32, -110, -95,
            32, 18, -95, 32, -110, -95, 32, 32, 32, 18, 32, -110, -95, 32, 18, -95,
            32, -94, -69, -110, -95, 18, 32, 32, 32, 32, -95, 32, -95, 32, -110, -68,
            18, 32, -95, 32, -110, 13, 32, 32, 32, -68, 18, -94, -94, -110, -66, 18,
            -94, -110, -66, 32, -68, 18, -94, -110, -66, 32, 32, 32, 18, -94, -110, -66,
            32, -68, 18, -94, -110, 32, -68, -66, 18, -94, -94, -94, -110, -66, -68, 18,
            -94, -110, -68, 18, -94, -110, 32, 18, -94, -110, -68, 18, -94, -110, 13
        };
    }
}
