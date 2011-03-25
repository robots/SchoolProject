/** Objekt tokenu. */
class Token implements Cloneable {

  /* Vlajky tokenu. TF = Token Flag. */

  /** Vlajka tokenu "nic". */
  public static final int TF_NONE               = 0;
  /** Vlajka tokenu "zacina radek". */
  public static final int TF_BEGINS_LINE  	= (1 << 0); 
  /** Vlajka tokenu "ukoncuje radek". */
  public static final int TF_ENDS_LINE      	= (1 << 1); 
  /** Vlajka tokenu "povoleny upravy casu rezervovanych slov". */
  public static final int TF_RESERVED_WORDS_ON  = (1 << 2); 
  /** Vlajka tokenu "povoleny upravy casu direktiv". */
  public static final int TF_DIRECTIVES_ON	= (1 << 3); 
  /** Vlajka tokenu "povoleny upravy casu identifikatoru". */
  public static final int TF_IDENTIFIERS_ON     = (1 << 4); 
  /** Vlajka tokenu "povoleno vkladani mezer na vhodna mista". */
  public static final int TF_INSERT_SPACES_ON	= (1 << 5); 
  /** Vlajka tokenu "povoleno odsazovani". */
  public static final int TF_INDENT_ON         = (1 << 6); 
  /** Vlajka tokenu "povoleno zalamovani radku". */
  public static final int TF_WRAP_ON           = (1 << 7); 
  /** Vlajka tokenu "povoleno vkladani prazdnych radku". */
  public static final int TF_BLANK_LINES_ON    = (1 << 8); 
  
  
  public int flags;
  /** Radek, kde zacina (indexovano od 0). */
  public int row;
  /** Sloupec, kde zacina (indexovano od 0). */
  public int col;
  /** Odkaz na predchozi token ve spojaku. */
  public Token prev;
  /** Odkaz na nasledujici token ve spojaku. */
  public Token next;
  /** Text. */
  public String text;
  /** Trida. */
  public String klass;
  /** Vlajky. */
 
  /**
   * Vytvori token podle zadanych parametru.
   * 
   * @param text text
   * @param klass trida
   * @param flags vlajky
   * @param row radek, kde zacina (indexovano od 0)
   * @param col sloupec, kde zacina (indexovano od 0)
   * @param prev odkaz na predchozi token ve spojaku
   * @param next odkaz na nasledujici token ve spojaku
   */
  public Token(String text, String klass, int flags, int row, int col, Token prev, Token next) {
    this.text  = text;
    this.klass = klass;
    this.flags = flags;
    this.row   = row;
    this.col   = col;
    this.prev  = prev;
    this.next  = next;  
  }

  /**
   * Odpovida token dane tride a textu?
   * 
   * @param aClass trida
   * @param aText text
   * @return <code>true</code> pokud token odpovida dane tride a textu;
   *         <code>false</code> pokud token neodpovida dane tride a textu
   */
    boolean match(String desiredKlass, String desiredText) {
        boolean matchTexts = text.equalsIgnoreCase(desiredText);

        return isSameKlassAs(desiredKlass) && matchTexts;
    }

  /**
   * Vytvori klon objektu.
   *
   * @return klon objektu
   */
  public Object clone() {
    return new Token(text, klass, flags, row, col, null, null);
  }

  public boolean isSameKlassAs(String desiredKlass) {
      return this.klass.equals(desiredKlass);
  }

  public boolean hasFlag(int flag) {
    if( (this.flags & flag) == flag)
            return true;
    
    return false;
  }


}

/** Zajistuje spravne odsazovani. */
public class Indent {
    public static final String NEWLINE = "\n";
    public static final String KLASS_WHITESPACE = "whitespace";
    public static final String KLASS_COMMENT = "comment";
    public static final String KLASS_RESERVED_WORD = "reserved-word";
    public static final String KLASS_VIRTUAL_ROUND_BRACKET = "virtual-round-bracket";


  /**
   * Vrati <code>1</code> nebo <code>0</code> podle toho, jestli je dany druh odsazeni
   * jen realny nebo virtualni.
   * 
   * Vracena hodnota je ve volajici funkci pouzita jako koeficint, coz ma efektivne za 
   * nasledek, ze se virtualni druh odsazovani fyzicky neodsazuje.
   *   
   * @param klass druh odsazeni
   * @return <code>1</code> pokud je druh odsazeni realny;
   *         <code>0</code> pokud je druh odsazeni virtualni
   */
    private static int indentLevel(String klass) {
        return klass.equals(KLASS_VIRTUAL_ROUND_BRACKET) ? 1 : 0;
    }
  
  /**
   * Preskoci bile mezery a komentare, ktere nasleduji za <code>start</code>.
   *
   * @param start token, za kterym se preskakuji bile mezery a komentare, sam to
   *         nemusi byt bily komentar nebo mezera
   * @return nejblizsi token za <code>start</code>, ktery neni bila mezera ani komentar;
   *          <code>null</code> pokud takovy uz ve spojaku neni 
   */
    private static Token skipWhitespaceAndComments(Token skipFrom) {
        Token currToken = skipFrom.next;

        if(currToken == null)
               return null;

        while ( currToken.isSameKlassAs(KLASS_WHITESPACE)
                || currToken.isSameKlassAs(KLASS_COMMENT)) {
            
            currToken = currToken.next;

            if(currToken == null)
                break;
        }
    
        return currToken;
    }

    /**
    * Preskoci vsechny tokeny od <code>token</code> (vcetne) a nemaji tridu
    * <code>klass</code> a text <code>text</code>. Porovnavani tridy tokenu je zde pro efektivitu,
    * aby se nemusely porad porovnavat retezce. Misto toho se nejdrive porovnaji tridy a az kdyz
    * jsou shodne, porovnavaji se retezce.
    *
    * @param token token, za kterym se preskakuji neodpovidajici tokeny
    * @param klass trida hledaneho tokenu
    * @param text text hledaneho tokenu
    * @return nejblizsi token za <code>token</code> (vcetne) se tridou <code>klass</code>
    *          a textem <code>text</code>; <code>null</code> pokud takovy uz ve spojaku neni
    */
    private static Token skipUntil(Token skipFrom, String desiredKlass, String desiredText) {
        Token currToken = skipFrom;

        while ((currToken != null) 
                && (!currToken.match(desiredKlass, desiredText))) {

            currToken = currToken.next;
        }

        return currToken;
    }

    /**
    * Zmeni hodnotu polozky <code>col</code> o <code>delta</code> u vsech tokenu od
    * <code>start</code> az do konce radku.
    *
    * @param start prni token, kde se ma menit hodnota <code>col</code>
    * @param delta o kolik se ma zmenit hodnota <code>col</code>
    */
    static void changeColUntilEOL(Token startToken, int delta) {
        Token currToken = startToken;

        while ((currToken != null) && (currToken.row == startToken.row)) {
            currToken.col += delta;

            currToken = currToken.next;
        }
    }

    /**
    * Zmeni hodnotu polozky <code>row</code> o <code>delta</code> u vsech tokenu od
    * <code>start</code> az do konce spojaku tokenu.
    *
    * @param start prni token, kde se ma menit hodnota <code>row</code>
    * @param delta o kolik se ma zmenit hodnota <code>row</code>
    */
    static void changeRowUntilEOF(Token startToken, int delta) {
        Token currToken;

        for (currToken = startToken; currToken != null; currToken = currToken.next) {
            currToken.row += delta;
        }
    }
  
    /**
    * Zaridi, aby za danym tokenem byl prazdny radek.
    *
    * @param token token, za kterym ma byt prazdna radka
    */
    private static void ensureBlankLineAfter(Token token) {
        if (token.next == null)
            return;
        while (token != null) {
            if ((token.flags & Token.TF_ENDS_LINE) == Token.TF_ENDS_LINE)
                break;

            token = token.next;
        }

        if (token == null || token.next == null)
            return;

        if ((token.next.flags & Token.TF_ENDS_LINE) == Token.TF_ENDS_LINE)
            return;

        Token newToken = new Token(NEWLINE, KLASS_WHITESPACE, token.next.flags, token.next.row, 0, token, token.next);

        changeRowUntilEOF(1, token.next);
        token.next.prev = newToken;
        token.next = newToken;
    }
  
    /**
    * Odsadi radek zacinajici tokenem <code>start</code> na uroven <code>level</code>.
    *
    * @param start prvni token na radce
    * @param l uroven, na kterou ma byt tato radka odsazena
    * @return token, kterym radka zacina po odsazeni (neni nutne stejny jako <code>start</code>)
    */
    private static Token indentLine(Token firstToken, int level) {
        Token newToken;
        Token result;
        String text;
        int delta = 0;

        result = firstToken;

        if (firstToken.isSameKlassAs(KLASS_WHITESPACE)) {
            if (firstToken.hasFlag(Token.TF_ENDS_LINE)) {
                if (firstToken.text.length() != level) {
                    if (level > 0) {
                        delta = level - firstToken.text.length();
                        firstToken.text = "";

                        for (int i = 1; i <= level; i++)
                            firstToken.text += " ";

                        changeColUntilEOL(firstToken.next, delta);
                    }
                    else {
                        delta = -(int)firstToken.text.length();

                        if (firstToken.prev != null)
                            firstToken.prev.next = firstToken.next;

                        if (firstToken.next != null)
                            firstToken.next.prev = firstToken.prev;

                        firstToken.next.flags |= Token.TF_BEGINS_LINE;
                        changeColUntilEOL(firstToken.next, delta);
                        result = firstToken.next;
                    }
                }
            }
        }
        else if (level > 0) {
            text = "";
            for (int i = 1; i <= level; i++)
            text += ' ';
            int flags = (firstToken.flags & ~Token.TF_ENDS_LINE) | Token.TF_BEGINS_LINE;
            newToken = new Token(text, KLASS_WHITESPACE, flags, firstToken.row, firstToken.col, firstToken.prev, firstToken);
            changeColUntilEOL(firstToken, level);
            if (firstToken.prev != null)
                firstToken.prev.next = newToken;
            firstToken.prev = newToken;
            firstToken.flags &= ~Token.TF_BEGINS_LINE;
        }

	return result;
    }

    /**
    * Odsadi urcite typy komentaru podle toho, k cemu patri.
    *
    * @param tokens spojovy seznam tokenu, kde se maji najit a odsadit komentare
    */
    static void indentComments(Token tokens) {
        Token token, t, first;

        for (token = tokens; token != null; token = token.next)
            if (token.isSameKlassAs(KLASS_COMMENT)) {

                /* Nejdrive zjistime, zda je komentar standalone */
                boolean isStandalone = true;
                first = token;
                if ( ! token.hasFlag(Token.TF_BEGINS_LINE)) {
                    for (t = token.prev; t != null; t = t.prev)
                        if (! t.isSameKlassAs(KLASS_WHITESPACE)) {
                            isStandalone = false;
                            break;
                        }
                        else {
                            if (t.hasFlag(Token.TF_BEGINS_LINE)) {
                                first = t;
                                break;
                            }
                        }
                }
                if (!isStandalone) continue;

                /* Ted najdeme neco, k cemu by se mohl tento komentar vztahovat. */
                for (t = token.next; t != null && (t.isSameKlassAs(KLASS_COMMENT) || t.isSameKlassAs(KLASS_WHITESPACE)); t = t.next)
                    ;

                if (t == null || t.match(KLASS_RESERVED_WORD, "end")|| t.match(KLASS_RESERVED_WORD, "until"))
                    continue;

                indentLine(first, t.col);
            }
       
     }

    /** Trida popisujici jednu uroven odsazeni. Slouzi jako jedna polozka v zasobniku odsazeni. */
    private static final class IndentLvl {

        /** Druh odsazeni. */
        public String klass;
        /** Ukazatel na dalsi polozku zasobniku, */
        public IndentLvl next;

        IndentLvl(String klass) {
            this.klass = klass;
        }
    }

        /** Trida popisujici aktualni stav odsazovani v prubehu algoritmu. */
    private final static class IndentContext {

        /** Zasobnik <code>Indent</code>u. */
        public IndentLvl top;
        /** Aktualni uroven odsazeni. */
        public int currLevel;
        /** Minimalni uroven odsazeni na teto radce. */
        public int minLevel;

        /**
        * Odebere polozku ze zasobniku <code>Indent</code>u.
        *
        * @return odebrana polozka
        */
        public IndentLvl pop() {
            IndentLvl poped;

            if (top == null) {
                return null;
            }

            poped = top;
            top = top.next;
            currLevel -= indentLevel(poped.klass) * 4;

            if (minLevel > currLevel)
            minLevel = currLevel;

            return poped;
        }

        /**
        * Prida polozku na zasobnik <code>Indent</code>u.
        *
        * @param indent pridavana polozka
        */
        public void push(IndentLvl indent) {
            indent.next = null;

            if (top != null)
                indent.next = top;

            top = indent;
            currLevel += indentLevel(indent.klass) * 4;
        }

        /**
        * Odsadi o jednu uroven, jejiz druh je urcen parametrem <code>klass</code>.
        *
        * @param klass druh odsazeni
        */
        public void indent(String klass) {
            IndentLvl indent;

            indent = new IndentLvl(klass);
            push(indent);
        }

        /** Odsadi zpatky. */
        public void unindent() {
            pop();
        }

        /** Odsadi zpatky, ale nesnizi pritom <code>minLevel</code>. */
        public void unindentNext() {
            int tmpMin;

            tmpMin = minLevel;
            pop();
            minLevel = tmpMin;
        }

        /**
        * Zjistuje, zda je druh odsazeni na vrcholu zasobniku <code>klass</code>.
        *
        * @param klass druh odsazeni, se kterym se porovnava druh odsazeni na vrcholu zasobniku
        * @return <code>true</code> pokud je zasobnik neprazdny a druh odsazeni na jeho vrcholu
        *          je <code>klass</code>;
        *          <code>false</code> pokud je zasobnik prazdny nebo je druh odsazeni na jeho vrcholu
        *          jiny nez <code>klass</code>
        */
        public boolean topClassIs(String klass) {
            if (top == null)
                return false;

            if (top.klass.equals(klass))
                return true;

            return false;
        }
    };
}
