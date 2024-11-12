package com.pocket.app;

/**
 * A type of build mode that app is running in.
 */
public enum AppMode {

    DEV,
    TEAM_ALPHA,
    PRODUCTION;
    
    /**
     * @return true if this is a dev or internal beta build. <b>PLEASE</b> read the security note in {@link AppMode}'s docs.
     */
    public boolean isForInternalCompanyOnly() {
        return this == DEV || this == TEAM_ALPHA;
    }
    
    public boolean isDevBuild() {
        return this == DEV;
    }

    public boolean isPublic() {
        return this == PRODUCTION;
    }
}
