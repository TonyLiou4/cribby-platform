package com.santosh.roommate_marketplace_api.report.dto;

import com.santosh.roommate_marketplace_api.user.entity.User;

public record AdminReportUserSummary(
        Long id,
        String displayName,
        String email
) {
    public static AdminReportUserSummary from(User user) {
        if (user == null) {
            return null;
        }

        return new AdminReportUserSummary(
                user.getId(),
                buildDisplayName(user),
                user.getEmail()
        );
    }

    private static String buildDisplayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (firstName == null || firstName.isBlank()) {
            return null;
        }
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName.substring(0, 1) + ".";
    }
}
