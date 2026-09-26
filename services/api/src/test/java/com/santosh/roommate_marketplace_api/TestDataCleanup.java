package com.santosh.roommate_marketplace_api;

import org.springframework.test.context.jdbc.Sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Sql(
        statements = {
                "DELETE FROM messages",
                "DELETE FROM conversations",
                "DELETE FROM verification_requests",
                "DELETE FROM reports",
                "DELETE FROM favorites",
                "DELETE FROM listing_images",
                "DELETE FROM listings",
                "DELETE FROM email_verification_tokens",
                "DELETE FROM refresh_tokens",
                "DELETE FROM user_profiles",
                "DELETE FROM users"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public @interface TestDataCleanup {
}
