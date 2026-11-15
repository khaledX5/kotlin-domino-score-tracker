package io.github.khaledx5.dominoscore.domain.model

/** A person who has sat at the table, under the name they were first written with. */
data class Profile(val id: ProfileId, val name: PlayerName)
