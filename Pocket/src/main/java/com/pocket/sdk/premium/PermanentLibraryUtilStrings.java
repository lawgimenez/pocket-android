package com.pocket.sdk.premium;

/**
 */
public class PermanentLibraryUtilStrings {

    // Salt obsfuscation: "Piles of books by the bedside"
    protected static Object sSaltPieces = new String[] {
            "er",
            "ok",
            "pi",
            "t",
            ".",
            " ",
            "e",
            "s ",
            "bo",
            "of",
            "e",
            "d",
            "R",
            "si",
            "Android",
            " u",
            "es",
            "e",
            ":",
            "h"
    };

    private static String sSaltPiece15 = "P";
    private static final String sSaltPiece3 = "es";
    protected static final String sDelim3 = ":";

    protected static String sSaltBaseKey;

    static {
        StringBuilder builder = new StringBuilder();
        String[] saltPieces12and17 = new String[]{"b", "b"};
        String saltPiece2 = "y";

        // Build Salt from Obsfucated code.
        sSaltBaseKey = builder
                .append(sSaltPiece15) 					// P
                .append("il")							// il
                .append(sSaltPiece3)					// es
                .append(((String[]) sSaltPieces)[5])	// _
                .append(((String[]) sSaltPieces)[9])	// of
                .append(((String[]) sSaltPieces)[5])	// _
                .append(((String[]) sSaltPieces)[8])	// bo
                .append(((String[]) sSaltPieces)[1])	// ok
                .append(((String[]) sSaltPieces)[7])	// s_
                .append(saltPieces12and17[0])			// b
                .append(saltPiece2)						// y
                .append(" ")							// _
                .append(((String[]) sSaltPieces)[3])	// t
                .append(((String[]) sSaltPieces)[19])	// h
                .append(((String[]) sSaltPieces)[6])	// e
                .append(((String[]) sSaltPieces)[5])	// _
                .append(saltPieces12and17[1])			// b
                .append(((String[]) sSaltPieces)[17])	// e
                .append(((String[]) sSaltPieces)[11])	// d
                .append(((String[]) sSaltPieces)[13])	// si
                .append(((String[]) sSaltPieces)[11])	// d
                .append(((String[]) sSaltPieces)[17])	// e
                .toString();
    }
}
