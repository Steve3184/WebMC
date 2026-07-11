package java.util;

public class Locale {
    private final String language;
    private final String country;
    private final String variant;
    private final String script;

    public static final Locale ENGLISH = new Locale("en", "", "");
    public static final Locale CHINESE = new Locale("zh", "", "");
    public static final Locale SIMPLIFIED_CHINESE = new Locale("zh", "CN", "");
    public static final Locale TRADITIONAL_CHINESE = new Locale("zh", "TW", "");

    private static volatile Locale defaultLocale = new Locale("en", "US", "");

    public Locale(String language) {
        this(language, "", "");
    }

    public Locale(String language, String country) {
        this(language, country, "");
    }

    public Locale(String language, String country, String variant) {
        this.language = language != null ? language : "";
        this.country = country != null ? country : "";
        this.variant = variant != null ? variant : "";
        this.script = "";
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public String getVariant() {
        return variant;
    }

    public String getScript() {
        return script;
    }

    public String getDisplayLanguage() {
        return language;
    }

    public String getDisplayCountry() {
        return country;
    }

    public String getDisplayVariant() {
        return variant;
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (!language.isEmpty()) {
            sb.append(language);
        }
        if (!country.isEmpty()) {
            if (sb.length() > 0) sb.append("_");
            sb.append(country);
        }
        if (!variant.isEmpty()) {
            if (sb.length() > 0) sb.append("_");
            sb.append(variant);
        }
        return sb.toString();
    }

    public String getExtension(char key) {
        return null;
    }

    public Set<Character> getExtensionKeys() {
        return Collections.emptySet();
    }

    public Set<String> getUnicodeLocaleAttributes() {
        return Collections.emptySet();
    }

    public Set<String> getUnicodeLocaleKeys() {
        return Collections.emptySet();
    }

    public String getUnicodeLocaleType(String key) {
        return null;
    }

    public static Locale forLanguageTag(String languageTag) {
        if (languageTag == null || languageTag.isEmpty()) {
            return new Locale("");
        }
        String[] parts = languageTag.split("[-_]");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        } else if (parts.length == 2) {
            return new Locale(parts[0], parts[1]);
        } else {
            return new Locale(parts[0], parts[1], parts[2]);
        }
    }

    public String toLanguageTag() {
        StringBuilder sb = new StringBuilder();
        sb.append(language);
        if (!country.isEmpty()) {
            sb.append("-").append(country);
        }
        if (!variant.isEmpty()) {
            sb.append("-").append(variant);
        }
        return sb.toString();
    }

    public static Locale getDefault() {
        return defaultLocale;
    }

    public static void setDefault(Locale locale) {
        defaultLocale = locale;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(language);
        if (!country.isEmpty()) {
            sb.append("_").append(country);
        }
        if (!variant.isEmpty()) {
            sb.append("_").append(variant);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Locale)) return false;
        Locale other = (Locale) obj;
        return language.equals(other.language) &&
               country.equals(other.country) &&
               variant.equals(other.variant);
    }

    @Override
    public int hashCode() {
        return language.hashCode() ^ country.hashCode() ^ variant.hashCode();
    }
}
