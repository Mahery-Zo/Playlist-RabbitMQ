import { defineStore } from 'pinia';
import api from '../api/axios';

export const useSongsStore = defineStore('songs', {
  state: () => ({
    songs: [],
    genres: [],
    artists: [],
    loading: false,
    error: null,
  }),
  actions: {
    async fetchSongs(query = '') {
      this.loading = true;
      try {
        const res = await api.get(`/songs`, { params: { q: query } });
        this.songs = res.data;
      } catch (err) {
        this.error = "Erreur lors du chargement des chansons.";
        console.error(err);
      } finally {
        this.loading = false;
      }
    },
    async fetchGenresAndArtists() {
      try {
        const [genresRes, artistsRes] = await Promise.all([
          api.get('/songs/genres'),
          api.get('/songs/artists')
        ]);
        this.genres = genresRes.data;
        this.artists = artistsRes.data;
      } catch (err) {
        console.error("Erreur chargement filtres", err);
      }
    },
    async uploadSong(file, metadata) {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));
      
      const res = await api.post('/songs', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      this.songs.push(res.data);
      this.fetchGenresAndArtists(); // Refresh filtres potentiels
      return res.data;
    },
    async updateSong(id, data) {
      const res = await api.put(`/songs/${id}`, data);
      const index = this.songs.findIndex(s => s.id === id);
      if (index !== -1) {
        this.songs[index] = res.data;
      }
      return res.data;
    },
    async deleteSong(id) {
      await api.delete(`/songs/${id}`);
      this.songs = this.songs.filter(s => s.id !== id);
    }
  }
});
