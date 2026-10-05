package com.anto.antoodin.interfaces;

// Implemented by ChannelMixin so SoundEngineMixin can re-center every playing channel
public interface MonoAudioChannel {
    void antoodin$refreshPosition();
}
