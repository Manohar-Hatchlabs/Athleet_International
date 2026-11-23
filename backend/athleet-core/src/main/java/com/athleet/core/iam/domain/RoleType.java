package com.athleet.core.iam.domain;

/**
 * HEAD_OFFICE – administrative roles (see the whole organization, and its affiliates if held at a primary org).
 * CENTER – premise coaching roles, limited to one center ("Class").
 * STUDENT – app users (students/parents); no admin access.
 */
public enum RoleType { HEAD_OFFICE, CENTER, STUDENT }
