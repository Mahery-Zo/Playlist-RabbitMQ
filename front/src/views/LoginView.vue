<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const isLogin = ref(true);
const username = ref('');
const password = ref('');
const loading = ref(false);

const submit = async () => {
  loading.value = true;
  try {
    if (isLogin.value) {
      await authStore.login(username.value, password.value);
      window.$toast('Connexion réussie', 'success');
    } else {
      await authStore.register(username.value, password.value);
      window.$toast('Inscription réussie', 'success');
    }
    router.push({ name: 'library' });
  } catch (err) {
    const msg = err.response?.data?.message || err.message || 'Erreur inconnue';
    window.$toast(msg, 'error');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="auth-page">
    <div class="auth-card glass-card">
      <div class="text-center mb-8">
        <h1 class="text-gradient">Naina Playlist</h1>
        <p class="text-secondary">{{ isLogin ? 'Connectez-vous à votre compte' : 'Créez votre compte' }}</p>
      </div>

      <form @submit.prevent="submit">
        <div class="form-group">
          <label class="form-label">Nom d'utilisateur</label>
          <input type="text" v-model="username" class="form-control" required placeholder="Ex: johndoe" />
        </div>
        <div class="form-group mb-8">
          <label class="form-label">Mot de passe</label>
          <input type="password" v-model="password" class="form-control" required placeholder="••••••••" />
        </div>

        <button type="submit" class="btn btn-primary w-full" :disabled="loading">
          <span v-if="loading">Chargement...</span>
          <span v-else>{{ isLogin ? 'Se connecter' : 'S\'inscrire' }}</span>
        </button>
      </form>

      <div class="text-center mt-8">
        <p class="text-secondary">
          {{ isLogin ? "Pas encore de compte ?" : "Déjà un compte ?" }}
          <a href="#" @click.prevent="isLogin = !isLogin" class="auth-link">
            {{ isLogin ? "S'inscrire" : "Se connecter" }}
          </a>
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 4rem);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
  animation: slideUp 0.5s ease;
}

.w-full {
  width: 100%;
}

.auth-link {
  color: var(--accent-primary);
  text-decoration: none;
  font-weight: 500;
}

.auth-link:hover {
  text-decoration: underline;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
