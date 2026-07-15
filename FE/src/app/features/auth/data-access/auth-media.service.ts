import { Injectable } from '@angular/core';

export const AUTH_LIGHT_BACKGROUND_VIDEO_SRC = 'assets/videos/Pikachu Pixel Animated Loop.mp4';
export const AUTH_DARK_BACKGROUND_VIDEO_SRC = 'assets/videos/pikachu-night.mp4';
export const BATTLE_BACKGROUND_AUDIO_SRC = '/assets/audio/enemy-approaching_4wxI6Chf.mp3';

const BACKGROUND_AUDIO_ENABLED_KEY = 'pokemon-register-background-audio-enabled';
const BACKGROUND_AUDIO_VOLUME_KEY = 'pokemon-register-background-audio-volume';
const BACKGROUND_AUDIO_TIME_KEY = 'pokemon-register-background-audio-time';
const BACKGROUND_VIDEO_TIME_KEY = 'pokemon-register-background-video-time';
const DEFAULT_BACKGROUND_AUDIO_VOLUME = 0.6;

@Injectable({ providedIn: 'root' })
export class AuthMediaService {
  private audio?: HTMLAudioElement;
  private battleAudio?: HTMLAudioElement;
  private currentVolume = this.readStoredVolume();

  constructor() {
    window.addEventListener('beforeunload', () => this.saveAudioTime());
  }

  enableAudio(syncTime?: number): void {
    this.markAudioEnabled();
    this.playIfEnabled(syncTime);
  }

  markAudioEnabled(): void {
    localStorage.setItem(BACKGROUND_AUDIO_ENABLED_KEY, 'true');
  }

  playIfEnabled(syncTime?: number): void {
    this.battleAudio?.pause();

    if (!this.isAudioEnabled()) {
      return;
    }

    const audio = this.getAudio();

    this.restoreAudioTime(audio, syncTime);

    audio.muted = false;
    audio.volume = this.appliedGain();

    if (audio.paused) {
      void audio.play().catch(() => undefined);
    }
  }

  isAudioEnabled(): boolean {
    return localStorage.getItem(BACKGROUND_AUDIO_ENABLED_KEY) === 'true';
  }

  playBattleTrack(): void {
    this.audio?.pause();

    const battleAudio = this.getBattleAudio();

    battleAudio.muted = false;
    battleAudio.volume = this.appliedGain();

    if (battleAudio.paused) {
      void battleAudio.play().catch(() => undefined);
    }
  }

  stopBattleTrack(): void {
    if (this.battleAudio) {
      this.battleAudio.pause();
      this.battleAudio.currentTime = 0;
    }

    this.playIfEnabled();
  }

  volume(): number {
    return this.currentVolume;
  }

  setVolume(volume: number): void {
    this.currentVolume = Math.min(1, Math.max(0, volume));
    localStorage.setItem(BACKGROUND_AUDIO_VOLUME_KEY, this.currentVolume.toString());

    if (this.audio) {
      this.audio.volume = this.appliedGain();
      this.audio.muted = this.currentVolume === 0;
    }

    if (this.battleAudio) {
      this.battleAudio.volume = this.appliedGain();
      this.battleAudio.muted = this.currentVolume === 0;
    }
  }

  private appliedGain(): number {
    return this.currentVolume * this.currentVolume;
  }

  saveBackgroundVideoTime(video?: HTMLVideoElement): void {
    if (!video || !Number.isFinite(video.currentTime)) {
      return;
    }

    sessionStorage.setItem(BACKGROUND_VIDEO_TIME_KEY, video.currentTime.toString());
  }

  restoreBackgroundVideoTime(video?: HTMLVideoElement): void {
    const savedTime = Number(sessionStorage.getItem(BACKGROUND_VIDEO_TIME_KEY));

    if (!video || !Number.isFinite(savedTime) || savedTime <= 0) {
      return;
    }

    video.currentTime = savedTime;
  }

  private getAudio(): HTMLAudioElement {
    if (!this.audio) {
      this.audio = new Audio(AUTH_LIGHT_BACKGROUND_VIDEO_SRC);
      this.audio.loop = true;
      this.audio.preload = 'auto';
      this.audio.volume = this.appliedGain();
      this.audio.addEventListener('timeupdate', () => this.saveAudioTime());
      this.restoreAudioTime(this.audio);
    }

    return this.audio;
  }

  private getBattleAudio(): HTMLAudioElement {
    if (!this.battleAudio) {
      this.battleAudio = new Audio(BATTLE_BACKGROUND_AUDIO_SRC);
      this.battleAudio.loop = true;
      this.battleAudio.preload = 'auto';
      this.battleAudio.volume = this.appliedGain();
    }

    return this.battleAudio;
  }

  private saveAudioTime(): void {
    if (!this.audio || !Number.isFinite(this.audio.currentTime)) {
      return;
    }

    localStorage.setItem(BACKGROUND_AUDIO_TIME_KEY, this.audio.currentTime.toString());
  }

  private restoreAudioTime(audio: HTMLAudioElement, fallbackTime?: number): void {
    if (audio.currentTime > 0) {
      return;
    }

    const storedTime = Number(localStorage.getItem(BACKGROUND_AUDIO_TIME_KEY));
    const nextTime = Number.isFinite(storedTime) && storedTime > 0 ? storedTime : fallbackTime;

    if (Number.isFinite(nextTime) && nextTime! > 0) {
      audio.currentTime = nextTime!;
    }
  }

  private readStoredVolume(): number {
    const storedVolume = Number(localStorage.getItem(BACKGROUND_AUDIO_VOLUME_KEY));

    return Number.isFinite(storedVolume)
      ? Math.min(1, Math.max(0, storedVolume))
      : DEFAULT_BACKGROUND_AUDIO_VOLUME;
  }
}
