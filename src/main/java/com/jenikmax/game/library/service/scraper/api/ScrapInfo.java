package com.jenikmax.game.library.service.scraper.api;

public class ScrapInfo {

    private String url;
    private String source;

    private boolean isTitleAttr;
    private boolean isPosterAttr;
    private boolean isDescriptionAttr;
    private boolean isYearAttrAttr;
    private boolean isGenresAttr;
    private boolean isScreensAttr;

    public ScrapInfo(String url, String source, boolean isTitleAttr,
                     boolean isPosterAttr, boolean isDescriptionAttr,
                     boolean isYearAttrAttr, boolean isGenresAttr,
                     boolean isScreensAttr) {
        this.url = url;
        this.source = source;
        this.isTitleAttr = isTitleAttr;
        this.isPosterAttr = isPosterAttr;
        this.isDescriptionAttr = isDescriptionAttr;
        this.isYearAttrAttr = isYearAttrAttr;
        this.isGenresAttr = isGenresAttr;
        this.isScreensAttr = isScreensAttr;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public boolean isTitleAttr() {
        return isTitleAttr;
    }

    public boolean isPosterAttr() {
        return isPosterAttr;
    }

    public boolean isDescriptionAttr() {
        return isDescriptionAttr;
    }

    public boolean isYearAttrAttr() {
        return isYearAttrAttr;
    }

    public boolean isGenresAttr() {
        return isGenresAttr;
    }

    public boolean isScreensAttr() {
        return isScreensAttr;
    }
}
