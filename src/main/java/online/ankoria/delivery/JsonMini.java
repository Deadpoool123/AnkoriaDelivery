package online.ankoria.delivery;

import java.util.ArrayList;
import java.util.List;

/**
 * Çok küçük JSON array parser: ["cmd1","cmd2"]
 * (Dış bağımlılık istemiyoruz diye minimal tuttuk.)
 */
public class JsonMini {
    public static List<String> parseStringArray(String json) {
        List<String> out = new ArrayList<>();
        if (json == null) return out;
        String s = json.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) return out;
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) return out;

        boolean inStr = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inStr = !inStr;
                continue;
            }
            if (!inStr && c == ',') {
                add(out, cur);
                cur.setLength(0);
                continue;
            }
            cur.append(c);
        }
        add(out, cur);
        return out;
    }

    private static void add(List<String> out, StringBuilder sb) {
        String v = sb.toString().trim();
        if (v.isEmpty()) return;
        // unescape very basic
        v = v.replace("\\\"", "\"").replace("\\\\", "\\");
        out.add(v);
    }
}
