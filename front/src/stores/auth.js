import { defineStore } from 'pinia';
import api from '../api/axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: null,
    user: null,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
  },
  actions: {
    initAuth() {
      const token = localStorage.getItem('token');
      const user = localStorage.getItem('user');
      if (token && user) {
        this.token = token;
        this.user = JSON.parse(user);
      }
    },
    async login(username, password) {
      const res = await api.post('/auth/login', { username, password });
      this.setAuthData(res.data);
    },
    async register(username, password) {
      const res = await api.post('/auth/register', { username, password });
      this.setAuthData(res.data);
    },
    setAuthData(data) {
      this.token = data.token;
      this.user = { id: data.userId, username: data.username };
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(this.user));
    },
    logout() {
      this.token = null;
      this.user = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }
});
