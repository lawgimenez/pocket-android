package com.pocket.app.premium;

import android.content.Context;
import android.graphics.Typeface;

import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.http.HttpClientDelegate;
import com.pocket.sdk.http.NetworkStatus;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpUtil;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.util.java.PktFileUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okio.BufferedSink;
import okio.Okio;

@Singleton
public class PremiumFonts implements AppLifecycle {
    
    public interface FontsListener {
        void onFontsReady();
        void onFontsUnavailable();
    }

    private final Context appContext;
    private final List<FontsListener> fontListeners = new ArrayList<>();
    private final HttpClientDelegate http;
    private final AppThreads threads;
    private final Pocket pocket;
    private final PocketCache pktcache;
    private final NetworkStatus networkStatus;

    private File fontsDir;
    private AtomicBoolean downloading = new AtomicBoolean(false);
    private List<String> fontCssPaths = null;

    @Inject
    public PremiumFonts(
            HttpClientDelegate http,
            AppThreads threads,
            Pocket pocket,
            PocketCache pktcache,
            @ApplicationContext Context context,
            NetworkStatus networkStatus,
            AppLifecycleEventDispatcher dispatcher
    ) {
        dispatcher.registerAppLifecycleObserver(this);
        this.http = http;
        this.threads = threads;
        this.pocket = pocket;
        this.pktcache = pktcache;
        this.appContext = context;
        this.networkStatus = networkStatus;
        this.fontsDir = new File(appContext.getFilesDir(), "premiumfonts/"); // Note: This location MUST be within the app's sandbox. We inject these files directly into the article view so there must be no chance other apps could have modified these files.
    }

    private Map<Font, Typeface> cache = new HashMap<>();

    public enum Font {

        IDEAL_SANS_BOOK("IdealSansSSm-Book.otf"),
        IDEAL_SANS_BOOK_ITALIC("IdealSansSSm-BookItalic.otf"),
        IDEAL_SANS_SEMIBOLD("IdealSansSSm-Semibold.otf"),
        IDEAL_SANS_SEMIBOLD_ITALIC("IdealSansSSm-SemiboldItalic.otf"),

        INTER_REGULAR("Inter-Regular.otf"),
        INTER_ITALIC("Inter-Italic.otf"),
        INTER_BOLD("Inter-Bold.otf"),
        INTER_BOLD_ITALIC("Inter-BoldItalic.otf"),

        IBM_PLEX_SANS_REGULAR("IBMPlexSans-Regular.ttf"),
        IBM_PLEX_SANS_ITALIC("IBMPlexSans-Italic.ttf"),
        IBM_PLEX_SANS_SEMIBOLD("IBMPlexSans-SemiBold.ttf"),
        IBM_PLEX_SANS_SEMIBOLD_ITALIC("IBMPlexSans-SemiBoldItalic.ttf"),

        SENTINEL_BOOK("SentinelSSm-Book.otf"),
        SENTINEL_BOOK_ITALIC("SentinelSSm-BookItalic.otf"),
        SENTINEL_SEMIBOLD("SentinelSSm-Semibold.otf"),
        SENTINEL_SEMIBOLD_ITALIC("SentinelSSm-SemiboldItalic.otf"),

        TIEMPOS_REGULAR("TiemposTextApp-Regular.ttf"),
        TIEMPOS_REGULAR_ITALIC("TiemposTextApp-RegularItalic.ttf"),
        TIEMPOS_SEMIBOLD("TiemposTextApp-Semibold.ttf"),
        TIEMPOS_SEMIBOLD_ITALIC("TiemposTextApp-SemiboldItalic.ttf"),

        VOLLKORN_REGULAR("Vollkorn-Regular.ttf"),
        VOLLKORN_ITALIC("Vollkorn-Italic.ttf"),
        VOLLKORN_BOLD("Vollkorn-Bold.ttf"),
        VOLLKORN_BOLD_ITALIC("Vollkorn-BoldItalic.ttf"),

        WHITNEY_BOOK("WhitneySSm-Book-Bas.otf"),
        WHITNEY_BOOK_ITALIC("WhitneySSm-BookItalic-Bas.otf"),
        WHITNEY_SEMIBOLD("WhitneySSm-Semibold-Bas.otf"),
        WHITNEY_SEMIBOLD_ITALIC("WhitneySSm-SemiboldItalic-Bas.otf"),

        ZILLA_SLAB_REGULAR("ZillaSlab-Regular.ttf"),
        ZILLA_SLAB_ITALIC("ZillaSlab-Italic.ttf"),
        ZILLA_SLAB_SEMIBOLD("ZillaSlab-SemiBold.ttf"),
        ZILLA_SLAB_SEMIBOLD_ITALIC("ZillaSlab-SemiBoldItalic.ttf");

        private final String filename;

        Font(String filename) {
            this.filename = filename;
        }
    }
    
    @Override
    public void onUserPresent() {
        initFonts(false);
    }

    @Override
    public void onLoggedIn(boolean isNewUser) {
        initFonts(false);
    }

    public Typeface get(Font font) {
        Typeface typeFace = cache.get(font);
        if (typeFace == null) {
            try {
                typeFace = Typeface.createFromFile(fontsDir.getAbsolutePath() + "/" + font.filename);
                cache.put(font, typeFace);
            } catch (RuntimeException e) {
                // we'll return null
            }
        }
        return typeFace;
    }

    private boolean fontsAvailable() {
        int fontcount = Font.values().length;
        int filecount = (fontcount / 4) + fontcount; // x font files + 1 css file per font-family
        File[] files = fontsDir.listFiles();

        return files != null && files.length >= filecount; // should be at *least* this many files, maybe more because of hidden system files
    }

    private synchronized void setFontCssPaths() {
        if (fontCssPaths == null) {
            final List<String> paths = new ArrayList<>();
            for (File file : FileUtils.listFiles(fontsDir, new String[]{"css"}, true)) {
                paths.add(file.getAbsolutePath());
            }
            fontCssPaths = paths;
        }
    }

    public boolean fontsReady() {
        return getFontCssPaths() != null;
    }

    public synchronized List<String> getFontCssPaths() {
        return fontCssPaths;
    }

    public void addFontListener(FontsListener listener) {
        if (listener != null && !fontListeners.contains(listener)) {
            fontListeners.add(listener);
        }
    }

    public void removeFontListener(FontsListener listener) {
        fontListeners.remove(listener);
    }

    public void initFonts(final boolean forceDownload) {

        if (getFontCssPaths() != null) {

            onFontsReady();

        } else {
            threads.async(() -> {

                if (fontsAvailable()) {

                    // set the css paths list
                    setFontCssPaths();

                    // all set!
                    onFontsReady();
                } else {

                    // otherwise, if we should download them, go ahead and do that
                    if (forceDownload || (pktcache.isLoggedIn() && networkStatus.isUnmetered())) {
                        downloadFonts();
                    } else {
                        // no fonts yet...
                        onFontsUnavailable();
                    }
                }
            });
        }
    }

    private void downloadFonts() {

        if (!downloading.compareAndSet(false, true)) {
            return;
        }

        final String diskPath = appContext.getFilesDir().getAbsolutePath();
        final String filename = "premiumfonts.zip";

        pocket.sync(pocket.spec().things().getPostAuthPayload().build())
                .onSuccess(payload -> threads.async(() -> {
                    File zip = new File(diskPath, filename);
                    EclecticHttp client = http.getClient();
                    boolean downloaded;
                    try {
                        downloaded = (Boolean) client.get(client.buildRequest(payload.payloads.premium_fonts_zip.url), (stream, response) -> {
                            if (response.getStatusCode() != 200) return false;
                            if (!StringUtils.equalsIgnoreCase(EclecticHttpUtil.getMimeType(response), "application/zip")) return false;
                            
                            zip.delete(); // delete the file if it already exists
                            zip.getParentFile().mkdirs(); // ensure its directory structure exists
                            zip.createNewFile(); // create the file to output to
                            BufferedSink sink = Okio.buffer(Okio.sink(zip));
                            sink.writeAll(stream.okioBuffer());
                            sink.close();
                            return true;
                        }).getResponse();
                    } catch (Throwable ignore) {
                        downloaded = false;
                    }
                    
                    if (downloaded) {
                        // unzip to the current directory
                        if (PktFileUtils.unzip(diskPath + "/" + filename)) {
                            if (fontsAvailable()) {
                                setFontCssPaths();
                                onFontsReady();
                            } else {
                                onFontsUnavailable();
                            }
    
                        } else {
                            onFontsUnavailable();
                        }
                        zip.delete(); // don't need the zip file anymore, in any case
                        
                    } else {
                        onFontsUnavailable();
                    }
                    downloading.set(false);
                }))
                .onFailure(e -> {
                    onFontsUnavailable();
                    downloading.set(false);
                });
    }

    private void onFontsReady() {
        threads.runOrPostOnUiThread(() -> {
            for (FontsListener l : new ArrayList<>(fontListeners)) {
                l.onFontsReady();
            }
        });
    }

    private void onFontsUnavailable() {
        threads.runOrPostOnUiThread(() -> {
            for (FontsListener l : new ArrayList<>(fontListeners)) {
                l.onFontsUnavailable();
            }
        });
    }

}
