import { defineStore } from 'pinia';

export const usePlayerStore = defineStore('player', {
  state: () => ({
    currentSong: null,
    isPlaying: false,
    queue: [],
    queueIndex: -1,
  }),
  getters: {
    hasNext: (state) => state.queueIndex < state.queue.length - 1,
    hasPrev: (state) => state.queueIndex > 0,
  },
  actions: {
    playSong(song, playlistQueue = null) {
      this.currentSong = song;
      this.isPlaying = true;
      
      if (playlistQueue) {
        this.queue = playlistQueue;
        this.queueIndex = playlistQueue.findIndex(s => s.id === song.id);
      } else {
        this.queue = [song];
        this.queueIndex = 0;
      }
    },
    pause() {
      this.isPlaying = false;
    },
    resume() {
      if (this.currentSong) {
        this.isPlaying = true;
      }
    },
    togglePlay() {
      this.isPlaying = !this.isPlaying;
    },
    next() {
      if (this.hasNext) {
        this.queueIndex++;
        this.currentSong = this.queue[this.queueIndex];
        this.isPlaying = true;
      }
    },
    prev() {
      if (this.hasPrev) {
        this.queueIndex--;
        this.currentSong = this.queue[this.queueIndex];
        this.isPlaying = true;
      }
    }
  }
});
