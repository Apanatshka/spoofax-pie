package mb.tiger;

import mb.jsglr1.common.JSGLR1ParseResult;
import mb.jsglr1.common.JSGLR1Parser;

public class TigerParser {
    private final JSGLR1Parser parser;

    public TigerParser(TigerParseTable parseTable) {
        this.parser = new JSGLR1Parser(parseTable.parseTable);
    }

    public JSGLR1ParseResult parse(String text, String startSymbol) throws InterruptedException {
        return parser.parse(text, startSymbol);
    }
}
