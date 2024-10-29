package com.pocket.sdk.api.endpoint;

/**
 * Sensitive strings used by ApiRequest.
 * TODO move back into {@link Endpoint}, they were moved out when we thought we were going to use dexguard.
 */
public class EndpointStrings {

    private static Object mSaltPieces = new String[]{
            "yde",
            "r a ebi",
            "rp o",
            "tco",
            "a n",
            "er",
            "t lli",
            "I"
    };

    private static String mSaltPiece = "rcse";
    private static final String mSaltPiece2 = "om eka";

    protected static String API_REQUEST_SALT;

    static {
        String[] saltPieces2 = new String[]{"t ro", "w t"};

        StringBuilder builder = new StringBuilder();

        // Build Salt from Obsfucated code.
        // Should end up with: "It will take more than a doctor to prescribe a remedy"
        API_REQUEST_SALT = builder
                .append(((String[]) mSaltPieces)[0])
                .append("me")
                .append(((String[]) mSaltPieces)[1])
                .append(mSaltPiece)
                .append(((String[]) mSaltPieces)[2])
                .append(saltPieces2[0])
                .append(((String[]) mSaltPieces)[3])
                .append("d ")
                .append(((String[]) mSaltPieces)[4])
                .append("aht ")
                .append(((String[]) mSaltPieces)[5])
                .append(mSaltPiece2)
                .append(((String[]) mSaltPieces)[6])
                .append(saltPieces2[1])
                .append(((String[]) mSaltPieces)[7])
                .reverse()
                .toString();
    }

}
