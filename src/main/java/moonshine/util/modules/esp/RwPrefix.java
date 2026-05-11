package moonshine.util.modules.esp;

public class RwPrefix {

    public static String getIconLabel(char c) {
        return switch (c) {
            case 'ꔀ' -> "PLAYER";
            case 'ꔄ' -> "HERO";
            case 'ꔈ' -> "TITAN";
            case 'ꔒ' -> "AVENGER";
            case 'ꔖ' -> "OVERLORD";
            case 'ꔠ' -> "MAGISTER";
            case 'ꔤ' -> "IMPERATOR";
            case 'ꔨ' -> "DRAGON";
            case 'ꔲ' -> "BULL";
            case 'ꕒ' -> "RABBIT";
            case 'ꔶ' -> "TIGER";
            case 'ꕄ' -> "DRACULA";
            case 'ꕖ' -> "BUNNY";
            case 'ꕀ' -> "HYDRA";
            case 'ꕈ' -> "COBRA";
            case 'ꔁ' -> "MEDIA";
            case 'ꔅ' -> "YT";
            case 'ꕠ' -> "D.HELPER";
            case 'ꔉ' -> "HELPER";
            case 'ꔓ' -> "ML.MODER";
            case 'ꔗ' -> "MODER";
            case 'ꔡ' -> "MODER+";
            case 'ꔥ' -> "ST.MODER";
            case 'ꔩ' -> "GL.MODER";
            case 'ꔳ' -> "ML.ADMIN";
            case 'ꔷ' -> "ADMIN";
            case 'ꕅ' -> "VAMPIRE";
            case 'ꕉ' -> "PEGAS";
            default -> null;
        };
    }

    public static int getPrefixColor(char c) {
        return switch (c) {
            case 'ꔀ' -> 0xFFAAAAAA; // PLAYER
            case 'ꔄ' -> 0xFF55FFFF; // HERO
            case 'ꔈ' -> 0xFFFFAA00; // TITAN
            case 'ꔒ' -> 0xFFFF6600; // AVENGER
            case 'ꔖ' -> 0xFFFF3300; // OVERLORD
            case 'ꔠ' -> 0xFFAA00AA; // MAGISTER
            case 'ꔤ' -> 0xFFFF0000; // IMPERATOR
            case 'ꔨ' -> 0xFFFF2200; // DRAGON
            case 'ꔲ' -> 0xFF00AAFF; // BULL
            case 'ꕒ' -> 0xFFFFAAAA; // RABBIT
            case 'ꔶ' -> 0xFFFF8800; // TIGER
            case 'ꕄ' -> 0xFF8B0000; // DRACULA
            case 'ꕖ' -> 0xFFFFCCCC; // BUNNY
            case 'ꕀ' -> 0xFF006633; // HYDRA
            case 'ꕈ' -> 0xFF00AA44; // COBRA
            case 'ꔁ' -> 0xFFFF4444; // MEDIA
            case 'ꔅ' -> 0xFFFF0000; // YT
            case 'ꕠ' -> 0xFF7289DA; // D.HELPER
            case 'ꔉ' -> 0xFF00AAAA; // HELPER
            case 'ꔓ' -> 0xFF5599FF; // ML.MODER
            case 'ꔗ' -> 0xFF5555FF; // MODER
            case 'ꔡ' -> 0xFF3333FF; // MODER+
            case 'ꔥ' -> 0xFF2222DD; // ST.MODER
            case 'ꔩ' -> 0xFFFF55FF; // GL.MODER
            case 'ꔳ' -> 0xFFDD0000; // ML.ADMIN
            case 'ꔷ' -> 0xFFFF0000; // ADMIN
            case 'ꕅ' -> 0xFF660000; // VAMPIRE
            case 'ꕉ' -> 0xFF88AAFF; // PEGAS
            default  -> 0xFF888888;
        };
    }

    public static boolean isIcon(char c) {
        if (getIconLabel(c) != null)
            return true;
        return (c >= '\uA000' && c <= '\uAFFF') || (c >= '\uE000' && c <= '\uF8FF')
                || (c >= '\u2400' && c <= '\u243F') || (c >= '\u2500' && c <= '\u257F');
    }

    public static String stripFormatting(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '#' && i + 7 < text.length()) {
                    i += 7;
                    continue;
                } else if ((next == 'x' || next == 'X') && i + 13 < text.length()) {
                    i += 13;
                    continue;
                } else {
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    public static ParsedName parseDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return new ParsedName("", "", "", 0xFF888888);
        }

        String clean = stripFormatting(displayName);
        StringBuilder prefix = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder clan = new StringBuilder();
        int prefixColor = 0xFF888888;

        boolean foundName = false;
        boolean inClan = false;
        int clanBracketCount = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);

            if (isIcon(c)) {
                String label = getIconLabel(c);
                if (label != null) {
                    if (prefix.length() == 0) prefixColor = getPrefixColor(c);
                    if (prefix.length() > 0) prefix.append(" ");
                    prefix.append(label);
                }
                continue;
            }

            if (!foundName && (c == ' ' || c == '[' || c == ']')) {
                continue;
            }

            if (!foundName && Character.isLetterOrDigit(c) || c == '_') {
                foundName = true;
            }

            if (foundName) {
                if (c == '[') {
                    inClan = true;
                    clanBracketCount++;
                    clan.append(c);
                    continue;
                }
                if (c == ']' && inClan) {
                    clan.append(c);
                    clanBracketCount--;
                    if (clanBracketCount <= 0) {
                        inClan = false;
                    }
                    continue;
                }
                if (inClan) {
                    clan.append(c);
                } else if (c != ' ' || name.length() > 0) {
                    if (c == ' ' && i + 1 < clean.length() && clean.charAt(i + 1) == '[') {
                        continue;
                    }
                    if (!inClan && clan.length() == 0) {
                        name.append(c);
                    }
                }
            }
        }

        String nameStr = name.toString().trim();
        if (nameStr.contains(" ")) {
            int spaceIdx = nameStr.indexOf(' ');
            String possibleClan = nameStr.substring(spaceIdx).trim();
            if (possibleClan.startsWith("[") && possibleClan.endsWith("]")) {
                clan = new StringBuilder(possibleClan);
                nameStr = nameStr.substring(0, spaceIdx);
            }
        }

        return new ParsedName(prefix.toString().trim(), nameStr.trim(), clan.toString().trim(), prefixColor);
    }

    public static class ParsedName {
        public final String prefix;
        public final String name;
        public final String clan;
        public final int prefixColor;

        public ParsedName(String prefix, String name, String clan, int prefixColor) {
            this.prefix = prefix;
            this.name = name;
            this.clan = clan;
            this.prefixColor = prefixColor;
        }

        public ParsedName(String prefix, String name, String clan) {
            this(prefix, name, clan, 0xFF888888);
        }

        public String getFullText() {
            StringBuilder sb = new StringBuilder();
            if (!prefix.isEmpty()) sb.append(prefix).append(" ");
            sb.append(name);
            if (!clan.isEmpty()) sb.append(" ").append(clan);
            return sb.toString();
        }
    }
}