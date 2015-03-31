package ru.eaze.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: zenden
 * Date: 01.12.11
 * Time: 15:04
 * To change this template use File | Settings | File Templates.
 */
public class RegexpUtils {
     /**
     * Интерфейс который должен реализовать тот кто хочет выполнить обработку, замену каждого вхождения программно
     */
    public static interface Replacer {
        /**
         * Метод должен вернуть строку на которую будет выполнена замена найденного regexp-ом фрагмента
         *
         * @param matches список с информацией об найденном фрагменте, нулевой элемент списка
         * содержит весь текст "совпадения"
         *                остальные же элементы 1,2, ... содержат значения для групп внутри регулярного выражения
         * @return
         */
        public String onMatch(List<String> matches);
    }

    /**
     * Кэш, в котором хранятся скомпилированные regexp-выражения
     */
    private static HashMap<String, Pattern> cache = new HashMap<String, Pattern>();

    /**
     * Очиска кэша скомпилированных regexp-выражений
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Выполнить поиск в строке шаблона и заменить его на новую величину вычисляемую динамически, пользователем
     *
     * @param pattern шаблон (regexp)
     * @param input   строка, где выполнить поиск
     * @param by      объект Replacer - задает значение на что выполнить замену
     * @return  строка после замены
     */
    public static String preg_replace_callback(String pattern, String input, Replacer by) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        StringBuffer sb = new StringBuffer();
        ArrayList<String> row = new ArrayList<String>();

        while (m.find()) {
            try {
                row.clear();
                for (int i = 0; i <= gcount; i++)
                    row.add(m.group(i));
                m.appendReplacement(sb, by.onMatch(row));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//end -- while --
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Выполнить поиск в строке шаблона и заменить его на новую величину вычисляемую средствами Regexp-выражения
     * @param pattern шаблон (regexp)
     * @param input строка, где выполнить поиск
     * @param by      строка, на которую нужно заменить найденное значение
     * @return  строка после замены
     */
    public static String preg_replace(String pattern, String input, String by) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                m.appendReplacement(sb, by);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }//end -- while --
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Проверка того ассоциирутся ли строка с шаблоном
     * @param pattern  шаблон (regexp)
     * @param input строка, где выполнить поиск
     * @param rez Список куда будет помещена информация об совпадении:
     * нулевой элемент списка содержит весь текст совпадения
     * 1, 2, ... содержат значения групп
     * @return булево выражение - признак того что ассоциация произошла
     */
    public static boolean preg_match(String pattern, String input, List <String> rez) {
        Pattern p = compile(pattern, false);
        Matcher m = p.matcher(input);
        if (rez != null)
            rez.clear();
        if (m.matches()) {
            final int gcount = m.groupCount();
            for (int i = 0; i <= gcount; i++) {
                if (rez != null)
                    rez.add(m.group(i));
            }
            return true;
        }
        return false;
    }

    /**
     * Проверка того что в строке содержится некоторый шаблон и возвращается список со всеми найденными группами совпадений
     * @param pattern  шаблон (regexp)
     * @param input строка, где выполнить поиск
     * @param rez список, куда будут помещены все найденные соответвия, список двухуровневый: первый уровень
     * содержит перечисление объектов-списков, каждый из которых содержит информацию об
     * очередном совпадении в таком же формате как и метод preg_match
     * @return
     */
    public static boolean preg_match_all(String pattern, String input, List <List <String>> rez) {
        Pattern p = compile(pattern, true);
        Matcher m = p.matcher(input);
        final int gcount = m.groupCount();
        if (rez != null)
            rez.clear();
        while (m.find()) {
            ArrayList row = new ArrayList();
            for (int i = 0; i <= gcount; i++) {
                if (rez != null)
                    row.add(m.group(i));
            }
            if (rez != null)
                rez.add(row);
        }
        return rez.size() > 0;
    }


    /**
     * Слежебный метод выполняющий компиляцию regexp-а и сохранение его в кэш
     * @param pattern текст регулярного выражения
     * @param surroundBy признак того нужно ли выражение окружить .*?
     * @return скомпилированный Pattern
     */
    private static Pattern compile(String pattern, boolean surroundBy) {
        if (cache.containsKey(pattern)) return cache.get(pattern);
        final String pattern_orig = pattern;

        final char firstChar = pattern.charAt(0);
        char endChar = firstChar;
        if (firstChar == '(') endChar = ')';
        if (firstChar == '[') endChar = ']';
        if (firstChar == '{') endChar = '}';
        if (firstChar == '<') endChar = '>';

        int lastPos = pattern.lastIndexOf(endChar);
        if (lastPos == -1)
            throw new RuntimeException("Invalid pattern: " + pattern);

        char[] modifiers = pattern.substring(lastPos + 1).toCharArray();
        int mod = 0;
        for (int i = 0; i < modifiers.length; i++) {
            char modifier = modifiers[i];
            switch (modifier) {
                case 'i':
                    mod |= Pattern.CASE_INSENSITIVE;
                    break;
                case 'd':
                    mod |= Pattern.UNIX_LINES;
                    break;
                case 'x':
                    mod |= Pattern.COMMENTS;
                    break;
                case 'm':
                    mod |= Pattern.MULTILINE;
                    break;
                case 's':
                    mod |= Pattern.DOTALL;
                    break;
                case 'u':
                    mod |= Pattern.UNICODE_CASE;
                    break;
            }
        }
        pattern = pattern.substring(1, lastPos);
        if (surroundBy) {
            if (pattern.charAt(0) != '^')
                pattern = ".*?" + pattern;
            if (pattern.charAt(pattern.length() - 1) != '$')
                pattern = pattern + ".*?";
        }

        final Pattern rezPattern = Pattern.compile(pattern, mod);
        cache.put(pattern_orig, rezPattern);
        return rezPattern;
    }
}
