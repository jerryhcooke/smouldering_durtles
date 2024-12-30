/*
 * Copyright 2024 The Smouldering Durtles Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.util;

import java.util.HashMap;
import java.util.function.Function;

public class InflectionUtil {

    static class VerbEnding {
        public StemForm stemForm;
        public String ending;

        VerbEnding(StemForm stemForm, String ending) {
            this.stemForm = stemForm;
            this.ending = ending;
        }

        String conjugate(String verb, VerbType verbType) {
            String stem = getStem(verb, verbType);
            return stem == null ? verb : stem + ending;
        }

        String getStem(String verb, VerbType verbType) {
            if (this.stemForm == StemForm.DICTIONARY)
                return verb;

            String prefix = verb.substring(0, verb.length() - 1);
            String lastChar = verb.substring(verb.length() - 1, verb.length());

            if (verbType == VerbType.ICHIDAN) {
                if (!lastChar.equals("る")) return null;
                if (stemForm == StemForm.TE) return prefix + "て";
                if (stemForm == StemForm.TA) return prefix + "た";
                if (stemForm == StemForm.VOLITIONAL) return prefix + "よ";
                return prefix;
            }

            if (verbType == VerbType.SURU) {
                switch (this.stemForm) {
                    case DICTIONARY: return "する";
                    case CONJUNCTIVE: return "し";
                    case NAI: return "しな";
                    case IMPERATIVE: return "しろ";
                    case VOLITIONAL: return "しよう";
                    case TE: return "して";
                    case TA: return "した";
                    case HYPOTHETICAL:
                        // TODO conditional: すれば, potential: できる
                        return null;
                }
            }

            HashMap<String, String[]> endings = new HashMap<>();
            endings.put("う", new String[] { "い", "わ", "え", "お", "って" });
            endings.put("つ", new String[] { "ち", "た", "て", "と", "って" });
            endings.put("る", new String[] { "り", "ら", "れ", "ろ", "って" });
            endings.put("ぬ", new String[] { "に", "な", "ね", "の", "んで" });
            endings.put("む", new String[] { "み", "ま", "め", "も", "んで" });
            endings.put("ぶ", new String[] { "び", "ば", "べ", "ぼ", "んで" });
            endings.put("く", new String[] { "き", "か", "け", "こ", "いて" });
            endings.put("ぐ", new String[] { "ぎ", "が", "げ", "ご", "いで" });
            endings.put("す", new String[] { "し", "さ", "せ", "そ", "して" });
            Function<StemForm, Integer> formToIndex = form -> {
                switch (form) {
                    case CONJUNCTIVE: return 0;
                    case NAI: return 1;
                    case IMPERATIVE: return 2;
                    case VOLITIONAL: return 3;
                    case TE: return 4;
                    default: throw new IllegalArgumentException("no index for this form");
                }
            };

            if (endings.get(lastChar) == null) return null;

            if (this.stemForm == StemForm.TA) {
                String teEnding = endings.get(lastChar)[formToIndex.apply(StemForm.TE)];
                return prefix + teEnding.replace("て", "た").replace("で", "だ");
            }

            return prefix + endings.get(lastChar)[formToIndex.apply(this.stemForm)];
        }
    }

    enum StemForm {
        DICTIONARY,
        CONJUNCTIVE,
        NAI,
        IMPERATIVE,
        HYPOTHETICAL,
        VOLITIONAL,
        TE,
        TA,
    }

    // we leave くる out -- it is not assigned a special verb in wanikani
    //　just する is also considered a する verb
    public enum VerbType {
        ICHIDAN,
        GODAN,
        SURU,
    }

    enum VerbConjugation {
        NEGATIVE_IMPERATIVE,
        TE,
        TAI,
        PAST,
        POTENTIAL,
        PASSIVE,
        CAUSATIVE,
        CAUSATIVE_PASSIVE
    }

    public enum AdjectiveType {
        I,
        NA_PLAIN,
        NA_POLITE
    }

    static void addDictionary(String name, String ending) {
        verbEndings.put(name, new VerbEnding(StemForm.DICTIONARY, ending));
    }

    static void addConjunctive(String name, String ending) {
        verbEndings.put(name, new VerbEnding(StemForm.CONJUNCTIVE, ending));
    }

    static void addImperative(String name, String ending) {
        verbEndings.put(name, new VerbEnding(StemForm.IMPERATIVE, ending));
    }

    static void addNai(String name, String ending) {
        verbEndings.put(name, new VerbEnding(StemForm.NAI, ending));
    }

    static void addTe(String name, String ending) {
        verbEndings.put(name, new VerbEnding(StemForm.TE, ending));
    }

    static void addTa(String name, String ending) {
        verbEndings.put(name, new VerbEnding(StemForm.TA, ending));
    }

    static HashMap<String, VerbEnding> verbEndings = new HashMap<String, VerbEnding>();
    static {
        addConjunctive("polite non past", "ます");
        addConjunctive("polite past", "ました");
        addConjunctive("polite negative", "ません");
        addConjunctive("polite negative past", "ませんでした");
        addConjunctive("polite imperative", "なさい");
        addConjunctive("polite imperative, short", "な");
        addConjunctive("polite volitional", "ましょう");
        addConjunctive("polite conjunction", "まして");

        addDictionary("nominalization", "こと");
        addDictionary("sometimes", "ことがある");
        addDictionary("decide on", "ことにする");
        addDictionary("is decided", "ことになる");

        addDictionary("\"before\"", "前に");

        addDictionary("hypothetical if", "なら(ば)");
        addDictionary("if (sure)", "と");
        addDictionary("\"should be\"", "はず");
        addDictionary("\"seems like\"", "みたい");
        addDictionary("\"I hear\"", "そう");
        addDictionary("\"apparently\"", "らしい");

        addConjunctive("\"[motion] and [do]\"", "に + motion verb");
        addConjunctive("\"easy to\"", "やすい");
        addConjunctive("\"difficult to\"", "にくい");
        addConjunctive("\"too much\"", "すぎる");
        addConjunctive("speaker's desire", "たい");
        addConjunctive("someone's desire", "たがる");
        addConjunctive("\"while\"", "ながら");
        addConjunctive("\"often\"", "がち");
        addConjunctive("\"way of\"", "方");
        addConjunctive("\"looks like ...\"", "そう");

        addNai("negative", "ない");
        addNai("negative て-form", "なくて");
        addNai("past negative", "なかった");
        addNai("\"don't do please\"", "ないでください");
        addNai("\"without\"", "ないで");
        addNai("must (informal)", "なきゃ");
        addNai("must (formal)", "なくてはいけない");

        addImperative("imperative", "");

        addTa("past", "");
        addTa("\"have before\"", "ことがある");

        addTe("continuing", "いる");
        addTe("continuing (informal)", "る");
        addTe("resulting", "ある");
        addTe("\"try\"", "みる");
        addTe("\"please\"", "ください");
    };

    public static String getConjugatedVerb(String verb, VerbType verbType, String endingName) {
        return verbEndings.get(endingName).conjugate(verb, verbType);
    }

    static class AdjectiveEnding {
        String i;
        String naPlain;
        String naPolite;

        AdjectiveEnding(String i, String naPlain, String naPolite) {
            this.i = i;
            this.naPlain = naPlain;
            this.naPolite = naPolite;
        }

        String decline(String adjective, AdjectiveType adjectiveType) {
            switch (adjectiveType) {
                case I: return adjective.substring(0, adjective.length() - 1) + i;
                case NA_PLAIN: return adjective + naPlain;
                case NA_POLITE: return adjective + naPolite;
            }
            return adjective;
        }
    }

    static HashMap<String, AdjectiveEnding> adjectiveEndings = new HashMap<String, AdjectiveEnding>();
    static {
        adjectiveEndings.put("past", new AdjectiveEnding("かった", "だった", "でした"));
        adjectiveEndings.put("conjunctive", new AdjectiveEnding("くて", "で", "でして"));
        // FIXME as is, this will show (spoken/written) for い-adjectives, too, which may confuse people
        adjectiveEndings.put("negative (spoken)", new AdjectiveEnding("くない", "じゃない", "じゃありません"));
        adjectiveEndings.put("negative (written)", new AdjectiveEnding("くない", "ではない", "ではありません"));
        adjectiveEndings.put("negative past (spoken)", new AdjectiveEnding("くなかった", "じゃなかった", "じゃありませんでした"));
        adjectiveEndings.put("negative past (written)", new AdjectiveEnding("くなかった", "ではなかった", "ではありませんでした"));
        adjectiveEndings.put("negative conjunctive", new AdjectiveEnding("くなくて", "で", "でして"));
    }

    public static String getDeclinedAdjective(String adjective, AdjectiveType adjectiveType, String declension) {
        return adjectiveEndings.get(declension).decline(adjective, adjectiveType);
    }

    public static String getRandomAdjectiveDeclension() {
        int index = (int) (Math.random() * adjectiveEndings.size());
        return (String) adjectiveEndings.keySet().toArray()[index];
    }

    public static String getRandomVerbConjugation() {
        int index = (int) (Math.random() * verbEndings.size());
        return (String) verbEndings.keySet().toArray()[index];
    }

    // for testing purposes
    public static void main(String[] args) {
        System.out.println(getDeclinedAdjective("大人しい", AdjectiveType.I, "past"));
        System.out.println(getDeclinedAdjective("不安", AdjectiveType.NA_PLAIN, "past"));
        System.out.println(getDeclinedAdjective("不安", AdjectiveType.NA_POLITE, "past"));
        System.out.println(getConjugatedVerb("集中する", VerbType.SURU, "speaker's desire"));
        System.out.println(getConjugatedVerb("食", VerbType.ICHIDAN, "speaker's desire"));
        System.out.println(getConjugatedVerb("飲む", VerbType.GODAN, "past"));
    }
}
