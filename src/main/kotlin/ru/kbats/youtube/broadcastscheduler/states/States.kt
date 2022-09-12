package ru.kbats.youtube.broadcastscheduler.states

sealed class BotUserState {
    object Default : BotUserState()
    object CreatingNewLiveStream : BotUserState()
}
