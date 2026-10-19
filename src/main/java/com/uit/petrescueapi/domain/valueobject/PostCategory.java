package com.uit.petrescueapi.domain.valueobject;

/** What a community post is about (DA2 Intelligent Post Processing). */
public enum PostCategory {
    /** An animal needs help now (injured, abandoned, in danger). */
    RESCUE,
    /** Someone offers an animal for adoption or wants to adopt. */
    ADOPTION,
    /** An owner lost their pet. */
    LOST,
    /** Someone found an animal that may have an owner. */
    FOUND,
    /** Anything else: stories, advice, questions. */
    GENERAL
}
