package com.pocket.app;

/**
 * An User Facing App Component that might be disabled.
 */
public abstract class Feature {
	
	private final Audience audience;

	public enum Audience {
		EVERY_USER,
		POCKET_TEAM,
		ENGIES,
		NOBODY;
		
		public boolean isInternalCompany() {
			return this == POCKET_TEAM || this == ENGIES;
		}

		public static Audience from(AppMode mode) {
			switch (mode) {
				case PRODUCTION: return Feature.Audience.EVERY_USER;
				case TEAM_ALPHA: return Feature.Audience.POCKET_TEAM;
				case DEV: return Feature.Audience.ENGIES;
				default: throw new RuntimeException("unknown mode " + mode);
			}
		}
	}
	
	public Feature(AppMode mode) {
		this.audience = Audience.from(mode);
	}
	
	/**
	 * Convenience for {@link #isOn(Audience)} with the current app and context.
	 * <p />
	 * Note: Subclasses don't override/implement this directly,
	 * instead {@link #isOn(Audience)} is provided as the extension point.
	 *
	 * @return true if the feature is enabled
	 */
	public final boolean isOn() {
		return isOn(audience);
	}
	
	/**
	 * Whether or not this feature is currently on.
	 * This might change due to user settings,
	 *
	 * (If the feature is not enabled, it will always be off)
	 *
	 * By default this maps to {@link #isEnabled(Audience)}.
	 * Provided for overriding if a feature has an ability to be turned on and off by the user.
	 */
	protected boolean isOn(Audience audience) {
		return isEnabled(audience);
	}
	
	/**
	 * Checks if the feature is enabled for this build and context.
	 * Even if it is enabled, it doesn't mean it should be shown.
	 * You'll likely want {@link #isOn()} in most cases.
	 * <p>
	 * Note: Subclasses don't override/implement this directly,
	 * instead {@link #isEnabled(Audience)} is provided as the extension point.
	 * 
	 * @return true if the feature is enabled
	 */
	public final boolean isEnabled() {
		return isEnabled(audience);
	}
	
	/**
	 * Extension point for concrete features to check if they want to be enabled given the current audience and other
	 * inputs they might want to check.
	 */
	abstract protected boolean isEnabled(Audience audience);
	
	protected Audience getAudience() {
		return audience;
	}
}
