package ru.kbats.youtube.broadcastscheduler.states

class UserStateStorage {
    private val states = mutableMapOf<Long, BotUserState>()

    operator fun get(id: Long?) = id?.let { states[it] } ?: BotUserState.Default
    operator fun set(id: Long, newState: BotUserState) {
        states[id] = newState
    }
}
