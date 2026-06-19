package dev.qcore.auth.common.constants;

public final class ApiPaths {
    private ApiPaths() {
    }

    public static final String AUTH_BASE = "/api/auth";
    public static final String REGISTER = "/register";
    public static final String LOGIN = "/login";
    public static final String VALIDATE = "/validate";
    public static final String USERS = "/users";

    public static final String REGISTER_ENDPOINT = AUTH_BASE + REGISTER;
    public static final String LOGIN_ENDPOINT = AUTH_BASE + LOGIN;
    public static final String VALIDATE_ENDPOINT = AUTH_BASE + VALIDATE;
    public static final String USERS_ENDPOINT = AUTH_BASE + USERS;

    public static final String[] PUBLIC_AUTH_ENDPOINTS = {
            REGISTER_ENDPOINT,
            LOGIN_ENDPOINT,
            VALIDATE_ENDPOINT
    };
}
