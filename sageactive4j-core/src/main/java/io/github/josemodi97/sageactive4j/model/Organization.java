package io.github.josemodi97.sageactive4j.model;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * An organization (business) the signed-in user can access. Since the
 * 2026-07 release this carries selection fields only; the full
 * configuration is {@link OrganizationDetail}.
 */
public final class Organization extends SageObject {

    public Organization(Map<String, Object> json) {
        super(json);
    }

    public String getSocialName() { return string("socialName"); }
    /** {@code FR}, {@code ES}, {@code DE} or {@code PT}. */
    public String getLegislationCode() { return string("legislationCode"); }
    /** {@code READY}, {@code PENDING}, {@code BLOCKED}, {@code CANCELLED}, {@code EXPIRED}, {@code NO_LICENSE}, ... */
    public String getStatus() { return string("status"); }
    public Boolean getOnboardingCompleted() { return bool("onboardingCompleted"); }
    public OffsetDateTime getOnboardingDateCompleted() { return dateTime("onboardingDateCompleted"); }
    public OffsetDateTime getCreationDate() { return dateTime("creationDate"); }
    public OffsetDateTime getModificationDate() { return dateTime("modificationDate"); }

    /**
     * Whether the public API may be used against it: Sage Active only hands
     * out a usable id when {@code status = READY} and onboarding is complete.
     */
    public boolean isUsable() {
        return "READY".equals(getStatus()) && Boolean.TRUE.equals(getOnboardingCompleted());
    }
}
