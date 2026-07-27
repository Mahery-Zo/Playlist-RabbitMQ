import { defineStore } from 'pinia';
import api from '../api/axios';

export const usePlaylistsStore = defineStore('playlists', {
  state: () => ({
    playlists: [],
    currentPlaylist: null,
    generatedSongs: [],
    loading: false,
  }),
  actions: {
    async fetchPlaylists() {
      this.loading = true;
      try {
        const res = await api.get('/playlists');
        this.playlists = res.data;
      } finally {
        this.loading = false;
      }
    },
    async fetchPlaylist(id) {
      this.loading = true;
      try {
        const res = await api.get(`/playlists/${id}`);
        this.currentPlaylist = res.data;
        return res.data;
      } finally {
        this.loading = false;
      }
    },
    async generatePlaylist(criteria) {
      this.loading = true;
      try {
        const res = await api.post('/playlists/generate', criteria);
        this.generatedSongs = res.data;
        return res.data;
      } finally {
        this.loading = false;
      }
    },
    async savePlaylist(name, songIds) {
      const res = await api.post('/playlists', { name, songIds });
      this.playlists.unshift({
        id: res.data.id,
        name: res.data.name,
        songCount: res.data.songs.length,
        totalDurationSec: res.data.totalDurationSec,
        createdAt: res.data.createdAt
      });
      return res.data;
    },
    async updatePlaylist(id, name, songIds) {
      const res = await api.put(`/playlists/${id}`, { name, songIds });
      this.currentPlaylist = res.data;
      return res.data;
    },
    async deletePlaylist(id) {
      await api.delete(`/playlists/${id}`);
      this.playlists = this.playlists.filter(p => p.id !== id);
      if (this.currentPlaylist?.id === id) {
        this.currentPlaylist = null;
      }
    },
    async mergePlaylists(name, playlistIds) {
      const res = await api.post('/playlists/merge', { name, playlistIds });
      // Ajouter la nouvelle playlist fusionnée en tête de la liste
      this.playlists.unshift({
        id: res.data.id,
        name: res.data.name,
        songCount: res.data.songs.length,
        totalDurationSec: res.data.totalDurationSec,
        createdAt: res.data.createdAt
      });
      return res.data;
    },
    getDownloadUrl(id) {
      // Need to pass token in URL since we can't use axios for direct browser download easily
      // A better way is using Blob but this is simpler
      return `/api/playlists/${id}/download`;
    }
  }
});
