# 📋 Documentation complète : Listes avec Vue.js

<!-- MOTS-CLÉS pour recherche Ctrl+F -->
<!-- liste, affichage, tableau, table, v-for, boucle, itération, données, data, array, map, filter, search, recherche, tri, sort, pagination, loading, chargement, erreur, error, vide, empty, API, fetch, axios, GET, afficher, display, render -->

## 🎯 Table des matières

1. [Liste simple](#liste-simple)
2. [Liste avec API](#liste-avec-api)
3. [Liste avec recherche](#liste-avec-recherche)
4. [Liste avec tri](#liste-avec-tri)
5. [Liste avec pagination](#liste-avec-pagination)
6. [Liste avec filtres](#liste-avec-filtres)
7. [Liste avec actions](#liste-avec-actions)
8. [Tableau complet](#tableau-complet)
9. [Gestion des états](#gestion-des-etats)
10. [Patterns réutilisables](#patterns-reutilisables)

---

## 1. Liste simple {#liste-simple}

<!-- MOTS-CLÉS: liste basique, v-for, affichage simple, ul li, basic list -->

### Code réutilisable

```vue
<template>
  <div class="simple-list">
    <h2>Liste simple</h2>
    
    <!-- Liste non ordonnée -->
    <ul>
      <!-- v-for : boucle sur chaque élément -->
      <!-- :key : identifiant unique obligatoire -->
      <li v-for="item in items" :key="item.id">
        {{ item.name }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Données de la liste
const items = ref([
  { id: 1, name: 'Produit 1' },
  { id: 2, name: 'Produit 2' },
  { id: 3, name: 'Produit 3' }
]);
</script>
```

### Variantes

```vue
<!-- Liste ordonnée -->
<ol>
  <li v-for="(item, index) in items" :key="item.id">
    {{ index + 1 }}. {{ item.name }}
  </li>
</ol>

<!-- Liste avec div -->
<div v-for="item in items" :key="item.id" class="item-card">
  <h3>{{ item.name }}</h3>
  <p>{{ item.description }}</p>
</div>

<!-- Liste vide -->
<div v-if="items.length === 0" class="empty-state">
  Aucun élément à afficher
</div>
<ul v-else>
  <li v-for="item in items" :key="item.id">{{ item.name }}</li>
</ul>
```

---


## 2. Liste avec API {#liste-avec-api}

<!-- MOTS-CLÉS: API, fetch, axios, GET, async, await, loading, chargement, erreur, error, onMounted -->

### Code réutilisable complet

```vue
<template>
  <div class="api-list">
    <h2>Liste depuis API</h2>
    
    <!-- État de chargement -->
    <div v-if="loading" class="loading">
      ⏳ Chargement en cours...
    </div>
    
    <!-- État d'erreur -->
    <div v-else-if="error" class="error">
      ❌ Erreur: {{ error }}
    </div>
    
    <!-- Liste des données -->
    <ul v-else-if="items.length > 0">
      <li v-for="item in items" :key="item.id">
        {{ item.name }}
      </li>
    </ul>
    
    <!-- État vide -->
    <div v-else class="empty">
      📭 Aucune donnée disponible
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

// États réactifs
const items = ref([]);        // Données de la liste
const loading = ref(false);   // État de chargement
const error = ref(null);      // Message d'erreur

// Fonction pour charger les données
const loadItems = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    // Requête GET vers l'API
    const response = await axios.get('/api/items');
    
    // Stocker les données
    items.value = response.data;
    
  } catch (err) {
    // Gérer l'erreur
    error.value = err.message;
    console.error('Erreur de chargement:', err);
    
  } finally {
    // Toujours exécuté (succès ou erreur)
    loading.value = false;
  }
};

// Charger au montage du composant
onMounted(() => {
  loadItems();
});
</script>

<style scoped>
.loading {
  padding: 20px;
  text-align: center;
  color: #007bff;
}

.error {
  padding: 15px;
  background: #f8d7da;
  color: #721c24;
  border-radius: 4px;
}

.empty {
  padding: 20px;
  text-align: center;
  color: #6c757d;
}
</style>
```

### Service réutilisable

```javascript
// services/ItemService.js
import axios from 'axios';

export const ItemService = {
  // Récupérer tous les items
  async getAll() {
    const response = await axios.get('/api/items');
    return response.data;
  },
  
  // Récupérer un item par ID
  async getById(id) {
    const response = await axios.get(`/api/items/${id}`);
    return response.data;
  },
  
  // Créer un item
  async create(data) {
    const response = await axios.post('/api/items', data);
    return response.data;
  },
  
  // Mettre à jour un item
  async update(id, data) {
    const response = await axios.put(`/api/items/${id}`, data);
    return response.data;
  },
  
  // Supprimer un item
  async delete(id) {
    await axios.delete(`/api/items/${id}`);
  }
};
```

### Utilisation du service

```vue
<script setup>
import { ref, onMounted } from 'vue';
import { ItemService } from '@/services/ItemService';

const items = ref([]);
const loading = ref(false);
const error = ref(null);

const loadItems = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    // Utiliser le service
    items.value = await ItemService.getAll();
  } catch (err) {
    error.value = err.message;
  } finally {
    loading.value = false;
  }
};

onMounted(loadItems);
</script>
```

---


## 3. Liste avec recherche {#liste-avec-recherche}

<!-- MOTS-CLÉS: recherche, search, filter, filtrer, input, v-model, computed, toLowerCase, includes -->

### Code réutilisable

```vue
<template>
  <div class="searchable-list">
    <h2>Liste avec recherche</h2>
    
    <!-- Champ de recherche -->
    <div class="search-box">
      <input 
        v-model="searchQuery" 
        type="text" 
        placeholder="🔍 Rechercher..."
        class="search-input"
      />
      <!-- Afficher le nombre de résultats -->
      <span class="result-count">
        {{ filteredItems.length }} résultat(s)
      </span>
    </div>
    
    <!-- Liste filtrée -->
    <ul v-if="filteredItems.length > 0">
      <li v-for="item in filteredItems" :key="item.id">
        <!-- Mettre en surbrillance le texte recherché -->
        <span v-html="highlightText(item.name)"></span>
      </li>
    </ul>
    
    <!-- Aucun résultat -->
    <div v-else class="no-results">
      Aucun résultat pour "{{ searchQuery }}"
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// Données
const items = ref([
  { id: 1, name: 'Produit A', category: 'Électronique' },
  { id: 2, name: 'Produit B', category: 'Vêtements' },
  { id: 3, name: 'Produit C', category: 'Électronique' }
]);

// Requête de recherche
const searchQuery = ref('');

// Liste filtrée (computed = recalculé automatiquement)
const filteredItems = computed(() => {
  // Si pas de recherche, retourner tout
  if (!searchQuery.value) {
    return items.value;
  }
  
  // Convertir en minuscules pour recherche insensible à la casse
  const query = searchQuery.value.toLowerCase();
  
  // Filtrer les items
  return items.value.filter(item => {
    // Rechercher dans le nom
    const nameMatch = item.name.toLowerCase().includes(query);
    // Rechercher dans la catégorie
    const categoryMatch = item.category.toLowerCase().includes(query);
    
    // Retourner si match dans nom OU catégorie
    return nameMatch || categoryMatch;
  });
});

// Fonction pour mettre en surbrillance le texte recherché
const highlightText = (text) => {
  if (!searchQuery.value) return text;
  
  // Créer une regex pour trouver le texte
  const regex = new RegExp(`(${searchQuery.value})`, 'gi');
  
  // Remplacer par du HTML avec surbrillance
  return text.replace(regex, '<mark>$1</mark>');
};
</script>

<style scoped>
.search-box {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.result-count {
  color: #6c757d;
  font-size: 14px;
}

mark {
  background: yellow;
  padding: 2px 4px;
  border-radius: 2px;
}

.no-results {
  padding: 20px;
  text-align: center;
  color: #6c757d;
}
</style>
```

### Recherche avancée (multiple champs)

```vue
<script setup>
import { ref, computed } from 'vue';

const items = ref([
  { id: 1, name: 'iPhone', price: 999, brand: 'Apple' },
  { id: 2, name: 'Galaxy', price: 899, brand: 'Samsung' },
  { id: 3, name: 'Pixel', price: 799, brand: 'Google' }
]);

const searchQuery = ref('');
const selectedBrand = ref('');
const minPrice = ref(0);
const maxPrice = ref(10000);

// Filtrage multiple
const filteredItems = computed(() => {
  return items.value.filter(item => {
    // Filtre par texte
    const textMatch = !searchQuery.value || 
      item.name.toLowerCase().includes(searchQuery.value.toLowerCase());
    
    // Filtre par marque
    const brandMatch = !selectedBrand.value || 
      item.brand === selectedBrand.value;
    
    // Filtre par prix
    const priceMatch = item.price >= minPrice.value && 
      item.price <= maxPrice.value;
    
    // Tous les filtres doivent correspondre
    return textMatch && brandMatch && priceMatch;
  });
});
</script>
```

---


## 4. Liste avec tri {#liste-avec-tri}

<!-- MOTS-CLÉS: tri, sort, trier, ordre, ascendant, descendant, asc, desc, sortBy, orderBy -->

### Code réutilisable

```vue
<template>
  <div class="sortable-list">
    <h2>Liste avec tri</h2>
    
    <!-- Boutons de tri -->
    <div class="sort-controls">
      <button 
        @click="sortBy('name')" 
        :class="{ active: sortKey === 'name' }"
      >
        Nom {{ getSortIcon('name') }}
      </button>
      
      <button 
        @click="sortBy('price')" 
        :class="{ active: sortKey === 'price' }"
      >
        Prix {{ getSortIcon('price') }}
      </button>
      
      <button 
        @click="sortBy('date')" 
        :class="{ active: sortKey === 'date' }"
      >
        Date {{ getSortIcon('date') }}
      </button>
    </div>
    
    <!-- Liste triée -->
    <ul>
      <li v-for="item in sortedItems" :key="item.id">
        <strong>{{ item.name }}</strong> - 
        {{ item.price }}€ - 
        {{ formatDate(item.date) }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// Données
const items = ref([
  { id: 1, name: 'Produit C', price: 150, date: '2024-01-15' },
  { id: 2, name: 'Produit A', price: 200, date: '2024-02-20' },
  { id: 3, name: 'Produit B', price: 100, date: '2024-01-10' }
]);

// État du tri
const sortKey = ref('name');      // Clé de tri actuelle
const sortOrder = ref('asc');     // Ordre: 'asc' ou 'desc'

// Liste triée
const sortedItems = computed(() => {
  // Copier le tableau pour ne pas modifier l'original
  const sorted = [...items.value];
  
  // Trier selon la clé et l'ordre
  sorted.sort((a, b) => {
    let aVal = a[sortKey.value];
    let bVal = b[sortKey.value];
    
    // Comparaison pour les chaînes
    if (typeof aVal === 'string') {
      aVal = aVal.toLowerCase();
      bVal = bVal.toLowerCase();
    }
    
    // Comparaison
    if (aVal < bVal) return sortOrder.value === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortOrder.value === 'asc' ? 1 : -1;
    return 0;
  });
  
  return sorted;
});

// Fonction pour changer le tri
const sortBy = (key) => {
  // Si même clé, inverser l'ordre
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    // Nouvelle clé, ordre ascendant par défaut
    sortKey.value = key;
    sortOrder.value = 'asc';
  }
};

// Icône de tri
const getSortIcon = (key) => {
  if (sortKey.value !== key) return '↕️';
  return sortOrder.value === 'asc' ? '↑' : '↓';
};

// Formater la date
const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleDateString('fr-FR');
};
</script>

<style scoped>
.sort-controls {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.sort-controls button {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-controls button:hover {
  background: #f8f9fa;
}

.sort-controls button.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}
</style>
```

### Tri dans un tableau HTML

```vue
<template>
  <table>
    <thead>
      <tr>
        <!-- En-têtes cliquables pour trier -->
        <th @click="sortBy('name')" class="sortable">
          Nom {{ getSortIcon('name') }}
        </th>
        <th @click="sortBy('price')" class="sortable">
          Prix {{ getSortIcon('price') }}
        </th>
        <th @click="sortBy('stock')" class="sortable">
          Stock {{ getSortIcon('stock') }}
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="item in sortedItems" :key="item.id">
        <td>{{ item.name }}</td>
        <td>{{ item.price }}€</td>
        <td>{{ item.stock }}</td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.sortable {
  cursor: pointer;
  user-select: none;
}

.sortable:hover {
  background: #f8f9fa;
}
</style>
```

---


## 5. Liste avec pagination {#liste-avec-pagination}

<!-- MOTS-CLÉS: pagination, page, pages, suivant, précédent, next, previous, limit, offset, slice -->

### Code réutilisable

```vue
<template>
  <div class="paginated-list">
    <h2>Liste avec pagination</h2>
    
    <!-- Informations -->
    <div class="info">
      Affichage de {{ startIndex + 1 }} à {{ endIndex }} sur {{ items.length }} éléments
    </div>
    
    <!-- Liste paginée -->
    <ul>
      <li v-for="item in paginatedItems" :key="item.id">
        {{ item.name }}
      </li>
    </ul>
    
    <!-- Contrôles de pagination -->
    <div class="pagination">
      <!-- Bouton précédent -->
      <button 
        @click="previousPage" 
        :disabled="currentPage === 1"
        class="btn-page"
      >
        ← Précédent
      </button>
      
      <!-- Numéros de pages -->
      <button
        v-for="page in totalPages"
        :key="page"
        @click="goToPage(page)"
        :class="{ active: currentPage === page }"
        class="btn-page"
      >
        {{ page }}
      </button>
      
      <!-- Bouton suivant -->
      <button 
        @click="nextPage" 
        :disabled="currentPage === totalPages"
        class="btn-page"
      >
        Suivant →
      </button>
    </div>
    
    <!-- Sélecteur de taille de page -->
    <div class="page-size-selector">
      <label>Éléments par page :</label>
      <select v-model.number="pageSize">
        <option :value="5">5</option>
        <option :value="10">10</option>
        <option :value="20">20</option>
        <option :value="50">50</option>
      </select>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// Données (exemple avec 50 items)
const items = ref(
  Array.from({ length: 50 }, (_, i) => ({
    id: i + 1,
    name: `Produit ${i + 1}`
  }))
);

// État de la pagination
const currentPage = ref(1);       // Page actuelle
const pageSize = ref(10);         // Nombre d'éléments par page

// Calculs
const totalPages = computed(() => {
  return Math.ceil(items.value.length / pageSize.value);
});

const startIndex = computed(() => {
  return (currentPage.value - 1) * pageSize.value;
});

const endIndex = computed(() => {
  const end = startIndex.value + pageSize.value;
  return Math.min(end, items.value.length);
});

// Items de la page actuelle
const paginatedItems = computed(() => {
  return items.value.slice(startIndex.value, endIndex.value);
});

// Fonctions de navigation
const goToPage = (page) => {
  currentPage.value = page;
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
  }
};

const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
  }
};

// Réinitialiser à la page 1 si pageSize change
watch(pageSize, () => {
  currentPage.value = 1;
});
</script>

<style scoped>
.info {
  margin-bottom: 15px;
  color: #6c757d;
  font-size: 14px;
}

.pagination {
  display: flex;
  gap: 5px;
  margin: 20px 0;
  justify-content: center;
}

.btn-page {
  padding: 8px 12px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-page:hover:not(:disabled) {
  background: #f8f9fa;
}

.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-page.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.page-size-selector {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  margin-top: 15px;
}

.page-size-selector select {
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
```

### Pagination avec API (côté serveur)

```vue
<script setup>
import { ref, watch } from 'vue';
import axios from 'axios';

const items = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const totalItems = ref(0);
const loading = ref(false);

// Calculer le nombre total de pages
const totalPages = computed(() => {
  return Math.ceil(totalItems.value / pageSize.value);
});

// Charger les données depuis l'API
const loadItems = async () => {
  loading.value = true;
  
  try {
    // Envoyer page et limit à l'API
    const response = await axios.get('/api/items', {
      params: {
        page: currentPage.value,
        limit: pageSize.value
      }
    });
    
    // Récupérer les données et le total
    items.value = response.data.items;
    totalItems.value = response.data.total;
    
  } catch (err) {
    console.error('Erreur:', err);
  } finally {
    loading.value = false;
  }
};

// Recharger quand la page ou la taille change
watch([currentPage, pageSize], () => {
  loadItems();
});

// Charger au montage
onMounted(() => {
  loadItems();
});
</script>
```

---


## 6. Liste avec filtres {#liste-avec-filtres}

<!-- MOTS-CLÉS: filtre, filter, checkbox, select, dropdown, catégorie, category, multiple, range -->

### Code réutilisable

```vue
<template>
  <div class="filtered-list">
    <h2>Liste avec filtres</h2>
    
    <!-- Panneau de filtres -->
    <div class="filters-panel">
      <!-- Filtre par catégorie -->
      <div class="filter-group">
        <label>Catégorie :</label>
        <select v-model="selectedCategory">
          <option value="">Toutes</option>
          <option v-for="cat in categories" :key="cat" :value="cat">
            {{ cat }}
          </option>
        </select>
      </div>
      
      <!-- Filtre par prix -->
      <div class="filter-group">
        <label>Prix max : {{ maxPrice }}€</label>
        <input 
          v-model.number="maxPrice" 
          type="range" 
          min="0" 
          max="1000" 
          step="10"
        />
      </div>
      
      <!-- Filtre par disponibilité -->
      <div class="filter-group">
        <label>
          <input v-model="inStockOnly" type="checkbox" />
          En stock uniquement
        </label>
      </div>
      
      <!-- Bouton réinitialiser -->
      <button @click="resetFilters" class="btn-reset">
        🔄 Réinitialiser
      </button>
    </div>
    
    <!-- Résultats -->
    <div class="results-info">
      {{ filteredItems.length }} produit(s) trouvé(s)
    </div>
    
    <!-- Liste filtrée -->
    <div class="items-grid">
      <div v-for="item in filteredItems" :key="item.id" class="item-card">
        <h3>{{ item.name }}</h3>
        <p class="category">{{ item.category }}</p>
        <p class="price">{{ item.price }}€</p>
        <p class="stock" :class="{ 'out-of-stock': item.stock === 0 }">
          {{ item.stock > 0 ? `En stock (${item.stock})` : 'Rupture' }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// Données
const items = ref([
  { id: 1, name: 'Laptop', category: 'Électronique', price: 899, stock: 5 },
  { id: 2, name: 'T-shirt', category: 'Vêtements', price: 29, stock: 0 },
  { id: 3, name: 'Livre', category: 'Livres', price: 15, stock: 10 },
  { id: 4, name: 'Smartphone', category: 'Électronique', price: 699, stock: 3 },
  { id: 5, name: 'Chaussures', category: 'Vêtements', price: 79, stock: 8 }
]);

// États des filtres
const selectedCategory = ref('');
const maxPrice = ref(1000);
const inStockOnly = ref(false);

// Liste des catégories uniques
const categories = computed(() => {
  return [...new Set(items.value.map(item => item.category))];
});

// Items filtrés
const filteredItems = computed(() => {
  return items.value.filter(item => {
    // Filtre par catégorie
    const categoryMatch = !selectedCategory.value || 
      item.category === selectedCategory.value;
    
    // Filtre par prix
    const priceMatch = item.price <= maxPrice.value;
    
    // Filtre par stock
    const stockMatch = !inStockOnly.value || item.stock > 0;
    
    // Tous les filtres doivent correspondre
    return categoryMatch && priceMatch && stockMatch;
  });
});

// Réinitialiser les filtres
const resetFilters = () => {
  selectedCategory.value = '';
  maxPrice.value = 1000;
  inStockOnly.value = false;
};
</script>

<style scoped>
.filters-panel {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-group {
  margin-bottom: 15px;
}

.filter-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.filter-group select,
.filter-group input[type="range"] {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn-reset {
  padding: 8px 16px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.results-info {
  margin-bottom: 15px;
  font-weight: bold;
  color: #495057;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.item-card {
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
}

.category {
  color: #6c757d;
  font-size: 14px;
}

.price {
  font-size: 18px;
  font-weight: bold;
  color: #28a745;
}

.stock {
  font-size: 14px;
  color: #28a745;
}

.stock.out-of-stock {
  color: #dc3545;
}
</style>
```

---


## 7. Liste avec actions {#liste-avec-actions}

<!-- MOTS-CLÉS: actions, boutons, modifier, supprimer, edit, delete, CRUD, update, remove -->

### Code réutilisable

```vue
<template>
  <div class="list-with-actions">
    <h2>Liste avec actions</h2>
    
    <!-- Bouton ajouter -->
    <button @click="showAddForm = true" class="btn-add">
      ➕ Ajouter un élément
    </button>
    
    <!-- Formulaire d'ajout (modal simple) -->
    <div v-if="showAddForm" class="modal">
      <div class="modal-content">
        <h3>Nouvel élément</h3>
        <input v-model="newItem.name" placeholder="Nom" />
        <input v-model.number="newItem.price" type="number" placeholder="Prix" />
        <div class="modal-actions">
          <button @click="addItem" class="btn-save">Enregistrer</button>
          <button @click="showAddForm = false" class="btn-cancel">Annuler</button>
        </div>
      </div>
    </div>
    
    <!-- Liste -->
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>Nom</th>
          <th>Prix</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td>{{ item.id }}</td>
          
          <!-- Mode édition -->
          <td v-if="editingId === item.id">
            <input v-model="editForm.name" />
          </td>
          <td v-else>{{ item.name }}</td>
          
          <td v-if="editingId === item.id">
            <input v-model.number="editForm.price" type="number" />
          </td>
          <td v-else>{{ item.price }}€</td>
          
          <!-- Boutons d'action -->
          <td class="actions">
            <!-- Mode édition -->
            <template v-if="editingId === item.id">
              <button @click="saveEdit(item.id)" class="btn-icon btn-save">
                ✓
              </button>
              <button @click="cancelEdit" class="btn-icon btn-cancel">
                ✗
              </button>
            </template>
            
            <!-- Mode normal -->
            <template v-else>
              <button @click="startEdit(item)" class="btn-icon btn-edit">
                ✏️
              </button>
              <button @click="deleteItem(item.id)" class="btn-icon btn-delete">
                🗑️
              </button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Données
const items = ref([
  { id: 1, name: 'Produit A', price: 100 },
  { id: 2, name: 'Produit B', price: 200 },
  { id: 3, name: 'Produit C', price: 150 }
]);

// État pour l'ajout
const showAddForm = ref(false);
const newItem = ref({ name: '', price: 0 });

// État pour l'édition
const editingId = ref(null);
const editForm = ref({ name: '', price: 0 });

// Ajouter un élément
const addItem = () => {
  // Générer un nouvel ID
  const newId = Math.max(...items.value.map(i => i.id)) + 1;
  
  // Ajouter au tableau
  items.value.push({
    id: newId,
    name: newItem.value.name,
    price: newItem.value.price
  });
  
  // Réinitialiser le formulaire
  newItem.value = { name: '', price: 0 };
  showAddForm.value = false;
};

// Commencer l'édition
const startEdit = (item) => {
  editingId.value = item.id;
  editForm.value = { ...item }; // Copier les valeurs
};

// Sauvegarder l'édition
const saveEdit = (id) => {
  // Trouver l'index de l'élément
  const index = items.value.findIndex(item => item.id === id);
  
  if (index !== -1) {
    // Mettre à jour l'élément
    items.value[index] = {
      id: id,
      name: editForm.value.name,
      price: editForm.value.price
    };
  }
  
  // Quitter le mode édition
  editingId.value = null;
};

// Annuler l'édition
const cancelEdit = () => {
  editingId.value = null;
  editForm.value = { name: '', price: 0 };
};

// Supprimer un élément
const deleteItem = (id) => {
  // Demander confirmation
  if (confirm('Êtes-vous sûr de vouloir supprimer cet élément ?')) {
    // Filtrer pour retirer l'élément
    items.value = items.value.filter(item => item.id !== id);
  }
};
</script>

<style scoped>
.btn-add {
  padding: 10px 20px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 20px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  background: #f8f9fa;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 5px;
}

.btn-icon {
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.btn-edit {
  background: #007bff;
  color: white;
}

.btn-delete {
  background: #dc3545;
  color: white;
}

.btn-save {
  background: #28a745;
  color: white;
}

.btn-cancel {
  background: #6c757d;
  color: white;
}

/* Modal */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 8px;
  min-width: 400px;
}

.modal-content input {
  width: 100%;
  padding: 10px;
  margin: 10px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.modal-actions button {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
```

---


## 8. Tableau complet (All-in-one) {#tableau-complet}

<!-- MOTS-CLÉS: tableau complet, datatable, all-in-one, recherche tri pagination, complete table -->

### Code réutilisable - Composant complet

```vue
<template>
  <div class="data-table">
    <h2>{{ title }}</h2>
    
    <!-- Barre d'outils -->
    <div class="toolbar">
      <!-- Recherche -->
      <input 
        v-model="searchQuery" 
        type="text" 
        placeholder="🔍 Rechercher..."
        class="search-input"
      />
      
      <!-- Sélecteur de taille de page -->
      <select v-model.number="pageSize" class="page-size">
        <option :value="10">10 par page</option>
        <option :value="25">25 par page</option>
        <option :value="50">50 par page</option>
      </select>
      
      <!-- Bouton ajouter -->
      <button @click="$emit('add')" class="btn-add">
        ➕ Ajouter
      </button>
    </div>
    
    <!-- Tableau -->
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th 
              v-for="column in columns" 
              :key="column.key"
              @click="column.sortable && sortBy(column.key)"
              :class="{ sortable: column.sortable }"
            >
              {{ column.label }}
              <span v-if="column.sortable">
                {{ getSortIcon(column.key) }}
              </span>
            </th>
            <th v-if="hasActions">Actions</th>
          </tr>
        </thead>
        <tbody>
          <!-- État de chargement -->
          <tr v-if="loading">
            <td :colspan="columns.length + (hasActions ? 1 : 0)" class="loading">
              ⏳ Chargement...
            </td>
          </tr>
          
          <!-- État d'erreur -->
          <tr v-else-if="error">
            <td :colspan="columns.length + (hasActions ? 1 : 0)" class="error">
              ❌ {{ error }}
            </td>
          </tr>
          
          <!-- Données -->
          <tr v-else-if="paginatedItems.length > 0" v-for="item in paginatedItems" :key="item.id">
            <td v-for="column in columns" :key="column.key">
              <!-- Slot personnalisé si fourni -->
              <slot :name="`cell-${column.key}`" :item="item" :value="item[column.key]">
                {{ formatValue(item[column.key], column.format) }}
              </slot>
            </td>
            
            <!-- Actions -->
            <td v-if="hasActions" class="actions">
              <button @click="$emit('edit', item)" class="btn-icon btn-edit">
                ✏️
              </button>
              <button @click="$emit('delete', item)" class="btn-icon btn-delete">
                🗑️
              </button>
            </td>
          </tr>
          
          <!-- État vide -->
          <tr v-else>
            <td :colspan="columns.length + (hasActions ? 1 : 0)" class="empty">
              📭 Aucune donnée
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- Pagination -->
    <div class="pagination-wrapper">
      <div class="pagination-info">
        Affichage de {{ startIndex + 1 }} à {{ endIndex }} sur {{ filteredItems.length }}
      </div>
      
      <div class="pagination">
        <button 
          @click="currentPage--" 
          :disabled="currentPage === 1"
          class="btn-page"
        >
          ←
        </button>
        
        <button
          v-for="page in visiblePages"
          :key="page"
          @click="currentPage = page"
          :class="{ active: currentPage === page }"
          class="btn-page"
        >
          {{ page }}
        </button>
        
        <button 
          @click="currentPage++" 
          :disabled="currentPage === totalPages"
          class="btn-page"
        >
          →
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

// Props
const props = defineProps({
  title: { type: String, default: 'Tableau' },
  columns: { type: Array, required: true },
  data: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  error: { type: String, default: null },
  hasActions: { type: Boolean, default: true }
});

// Émissions
const emit = defineEmits(['add', 'edit', 'delete']);

// États
const searchQuery = ref('');
const sortKey = ref('');
const sortOrder = ref('asc');
const currentPage = ref(1);
const pageSize = ref(10);

// Items filtrés par recherche
const filteredItems = computed(() => {
  if (!searchQuery.value) return props.data;
  
  const query = searchQuery.value.toLowerCase();
  return props.data.filter(item => {
    return props.columns.some(column => {
      const value = String(item[column.key]).toLowerCase();
      return value.includes(query);
    });
  });
});

// Items triés
const sortedItems = computed(() => {
  if (!sortKey.value) return filteredItems.value;
  
  const sorted = [...filteredItems.value];
  sorted.sort((a, b) => {
    let aVal = a[sortKey.value];
    let bVal = b[sortKey.value];
    
    if (typeof aVal === 'string') {
      aVal = aVal.toLowerCase();
      bVal = bVal.toLowerCase();
    }
    
    if (aVal < bVal) return sortOrder.value === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortOrder.value === 'asc' ? 1 : -1;
    return 0;
  });
  
  return sorted;
});

// Pagination
const totalPages = computed(() => {
  return Math.ceil(sortedItems.value.length / pageSize.value);
});

const startIndex = computed(() => {
  return (currentPage.value - 1) * pageSize.value;
});

const endIndex = computed(() => {
  return Math.min(startIndex.value + pageSize.value, sortedItems.value.length);
});

const paginatedItems = computed(() => {
  return sortedItems.value.slice(startIndex.value, endIndex.value);
});

// Pages visibles (max 5)
const visiblePages = computed(() => {
  const pages = [];
  const maxVisible = 5;
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2));
  let end = Math.min(totalPages.value, start + maxVisible - 1);
  
  if (end - start < maxVisible - 1) {
    start = Math.max(1, end - maxVisible + 1);
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  
  return pages;
});

// Fonctions
const sortBy = (key) => {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortKey.value = key;
    sortOrder.value = 'asc';
  }
};

const getSortIcon = (key) => {
  if (sortKey.value !== key) return '↕️';
  return sortOrder.value === 'asc' ? '↑' : '↓';
};

const formatValue = (value, format) => {
  if (!format) return value;
  
  switch (format) {
    case 'date':
      return new Date(value).toLocaleDateString('fr-FR');
    case 'currency':
      return `${value}€`;
    case 'percent':
      return `${value}%`;
    default:
      return value;
  }
};

// Réinitialiser la page lors de changements
watch([searchQuery, pageSize], () => {
  currentPage.value = 1;
});
</script>

<style scoped>
/* Styles du tableau complet */
.data-table {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
}

.search-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.page-size {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.btn-add {
  padding: 10px 20px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  background: #f8f9fa;
  font-weight: bold;
}

th.sortable {
  cursor: pointer;
  user-select: none;
}

th.sortable:hover {
  background: #e9ecef;
}

.loading, .error, .empty {
  text-align: center;
  padding: 40px;
  color: #6c757d;
}

.error {
  color: #dc3545;
}

.actions {
  display: flex;
  gap: 5px;
}

.btn-icon {
  padding: 5px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.btn-edit {
  background: #007bff;
  color: white;
}

.btn-delete {
  background: #dc3545;
  color: white;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

.pagination-info {
  color: #6c757d;
  font-size: 14px;
}

.pagination {
  display: flex;
  gap: 5px;
}

.btn-page {
  padding: 8px 12px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.btn-page:hover:not(:disabled) {
  background: #f8f9fa;
}

.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-page.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
}
</style>
```

### Utilisation du composant

```vue
<template>
  <DataTable
    title="Liste des produits"
    :columns="columns"
    :data="products"
    :loading="loading"
    :error="error"
    @add="handleAdd"
    @edit="handleEdit"
    @delete="handleDelete"
  >
    <!-- Slot personnalisé pour le prix -->
    <template #cell-price="{ value }">
      <span class="price">{{ value }}€</span>
    </template>
    
    <!-- Slot personnalisé pour le stock -->
    <template #cell-stock="{ value }">
      <span :class="value > 0 ? 'in-stock' : 'out-of-stock'">
        {{ value > 0 ? `${value} en stock` : 'Rupture' }}
      </span>
    </template>
  </DataTable>
</template>

<script setup>
import { ref } from 'vue';
import DataTable from '@/components/DataTable.vue';

// Configuration des colonnes
const columns = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'name', label: 'Nom', sortable: true },
  { key: 'price', label: 'Prix', sortable: true, format: 'currency' },
  { key: 'stock', label: 'Stock', sortable: true },
  { key: 'date', label: 'Date', sortable: true, format: 'date' }
];

// Données
const products = ref([
  { id: 1, name: 'Produit A', price: 100, stock: 5, date: '2024-01-15' },
  { id: 2, name: 'Produit B', price: 200, stock: 0, date: '2024-02-20' }
]);

const loading = ref(false);
const error = ref(null);

// Gestionnaires d'événements
const handleAdd = () => {
  console.log('Ajouter un produit');
};

const handleEdit = (item) => {
  console.log('Éditer:', item);
};

const handleDelete = (item) => {
  if (confirm(`Supprimer ${item.name} ?`)) {
    products.value = products.value.filter(p => p.id !== item.id);
  }
};
</script>
```

---


## 9. Gestion des états {#gestion-des-etats}

<!-- MOTS-CLÉS: états, loading, error, empty, chargement, erreur, vide, skeleton, placeholder -->

### États de chargement

```vue
<template>
  <div>
    <!-- Skeleton loader -->
    <div v-if="loading" class="skeleton-list">
      <div v-for="n in 5" :key="n" class="skeleton-item">
        <div class="skeleton-line"></div>
        <div class="skeleton-line short"></div>
      </div>
    </div>
    
    <!-- Spinner -->
    <div v-if="loading" class="spinner-container">
      <div class="spinner"></div>
      <p>Chargement...</p>
    </div>
    
    <!-- Barre de progression -->
    <div v-if="loading" class="progress-bar">
      <div class="progress-fill" :style="{ width: progress + '%' }"></div>
    </div>
  </div>
</template>

<style scoped>
/* Skeleton */
.skeleton-item {
  padding: 15px;
  border-bottom: 1px solid #eee;
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 10px;
}

.skeleton-line.short {
  width: 60%;
}

@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* Spinner */
.spinner-container {
  text-align: center;
  padding: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Barre de progression */
.progress-bar {
  width: 100%;
  height: 4px;
  background: #f0f0f0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #007bff;
  transition: width 0.3s;
}
</style>
```

### États d'erreur

```vue
<template>
  <div>
    <!-- Erreur simple -->
    <div v-if="error" class="error-box">
      <p>❌ {{ error }}</p>
      <button @click="retry">Réessayer</button>
    </div>
    
    <!-- Erreur détaillée -->
    <div v-if="error" class="error-detailed">
      <div class="error-icon">⚠️</div>
      <h3>Une erreur est survenue</h3>
      <p class="error-message">{{ error }}</p>
      <div class="error-actions">
        <button @click="retry" class="btn-primary">Réessayer</button>
        <button @click="goBack" class="btn-secondary">Retour</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.error-box {
  padding: 20px;
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 8px;
  color: #721c24;
}

.error-detailed {
  text-align: center;
  padding: 40px;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

.error-message {
  color: #721c24;
  margin: 15px 0;
}

.error-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin-top: 20px;
}
</style>
```

### État vide

```vue
<template>
  <div>
    <!-- État vide simple -->
    <div v-if="items.length === 0" class="empty-state">
      <p>📭 Aucun élément</p>
    </div>
    
    <!-- État vide avec illustration -->
    <div v-if="items.length === 0" class="empty-state-detailed">
      <div class="empty-icon">📦</div>
      <h3>Aucun produit trouvé</h3>
      <p>Commencez par ajouter votre premier produit</p>
      <button @click="addFirst" class="btn-add">
        ➕ Ajouter un produit
      </button>
    </div>
    
    <!-- État vide après recherche -->
    <div v-if="filteredItems.length === 0 && searchQuery" class="no-results">
      <div class="no-results-icon">🔍</div>
      <h3>Aucun résultat</h3>
      <p>Aucun élément ne correspond à "{{ searchQuery }}"</p>
      <button @click="searchQuery = ''" class="btn-clear">
        Effacer la recherche
      </button>
    </div>
  </div>
</template>

<style scoped>
.empty-state-detailed {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.empty-state-detailed h3 {
  color: #495057;
  margin-bottom: 10px;
}

.empty-state-detailed p {
  color: #6c757d;
  margin-bottom: 20px;
}

.no-results {
  text-align: center;
  padding: 40px;
}

.no-results-icon {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.5;
}
</style>
```

---


## 10. Patterns réutilisables {#patterns-reutilisables}

<!-- MOTS-CLÉS: patterns, composables, hooks, réutilisable, useList, usePagination, useSort -->

### Composable useList

```javascript
// composables/useList.js
import { ref, computed } from 'vue';

export function useList(initialData = []) {
  // États
  const items = ref(initialData);
  const loading = ref(false);
  const error = ref(null);
  
  // Ajouter un élément
  const addItem = (item) => {
    const newId = items.value.length > 0 
      ? Math.max(...items.value.map(i => i.id)) + 1 
      : 1;
    
    items.value.push({ ...item, id: newId });
  };
  
  // Mettre à jour un élément
  const updateItem = (id, updates) => {
    const index = items.value.findIndex(item => item.id === id);
    if (index !== -1) {
      items.value[index] = { ...items.value[index], ...updates };
    }
  };
  
  // Supprimer un élément
  const deleteItem = (id) => {
    items.value = items.value.filter(item => item.id !== id);
  };
  
  // Trouver un élément
  const findItem = (id) => {
    return items.value.find(item => item.id === id);
  };
  
  // Charger depuis une API
  const loadItems = async (fetchFn) => {
    loading.value = true;
    error.value = null;
    
    try {
      items.value = await fetchFn();
    } catch (err) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  };
  
  return {
    items,
    loading,
    error,
    addItem,
    updateItem,
    deleteItem,
    findItem,
    loadItems
  };
}
```

### Composable useSearch

```javascript
// composables/useSearch.js
import { ref, computed } from 'vue';

export function useSearch(items, searchFields = []) {
  const searchQuery = ref('');
  
  const filteredItems = computed(() => {
    if (!searchQuery.value) return items.value;
    
    const query = searchQuery.value.toLowerCase();
    
    return items.value.filter(item => {
      return searchFields.some(field => {
        const value = String(item[field]).toLowerCase();
        return value.includes(query);
      });
    });
  });
  
  const clearSearch = () => {
    searchQuery.value = '';
  };
  
  return {
    searchQuery,
    filteredItems,
    clearSearch
  };
}
```

### Composable useSort

```javascript
// composables/useSort.js
import { ref, computed } from 'vue';

export function useSort(items) {
  const sortKey = ref('');
  const sortOrder = ref('asc');
  
  const sortedItems = computed(() => {
    if (!sortKey.value) return items.value;
    
    const sorted = [...items.value];
    
    sorted.sort((a, b) => {
      let aVal = a[sortKey.value];
      let bVal = b[sortKey.value];
      
      if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();
      }
      
      if (aVal < bVal) return sortOrder.value === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortOrder.value === 'asc' ? 1 : -1;
      return 0;
    });
    
    return sorted;
  });
  
  const sortBy = (key) => {
    if (sortKey.value === key) {
      sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
    } else {
      sortKey.value = key;
      sortOrder.value = 'asc';
    }
  };
  
  const getSortIcon = (key) => {
    if (sortKey.value !== key) return '↕️';
    return sortOrder.value === 'asc' ? '↑' : '↓';
  };
  
  return {
    sortKey,
    sortOrder,
    sortedItems,
    sortBy,
    getSortIcon
  };
}
```

### Composable usePagination

```javascript
// composables/usePagination.js
import { ref, computed } from 'vue';

export function usePagination(items, initialPageSize = 10) {
  const currentPage = ref(1);
  const pageSize = ref(initialPageSize);
  
  const totalPages = computed(() => {
    return Math.ceil(items.value.length / pageSize.value);
  });
  
  const startIndex = computed(() => {
    return (currentPage.value - 1) * pageSize.value;
  });
  
  const endIndex = computed(() => {
    return Math.min(startIndex.value + pageSize.value, items.value.length);
  });
  
  const paginatedItems = computed(() => {
    return items.value.slice(startIndex.value, endIndex.value);
  });
  
  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page;
    }
  };
  
  const nextPage = () => {
    goToPage(currentPage.value + 1);
  };
  
  const previousPage = () => {
    goToPage(currentPage.value - 1);
  };
  
  const resetPage = () => {
    currentPage.value = 1;
  };
  
  return {
    currentPage,
    pageSize,
    totalPages,
    startIndex,
    endIndex,
    paginatedItems,
    goToPage,
    nextPage,
    previousPage,
    resetPage
  };
}
```

### Utilisation combinée des composables

```vue
<template>
  <div class="advanced-list">
    <!-- Recherche -->
    <input v-model="searchQuery" placeholder="Rechercher..." />
    
    <!-- Tri -->
    <button @click="sortBy('name')">
      Nom {{ getSortIcon('name') }}
    </button>
    
    <!-- Liste -->
    <ul>
      <li v-for="item in paginatedItems" :key="item.id">
        {{ item.name }}
      </li>
    </ul>
    
    <!-- Pagination -->
    <div class="pagination">
      <button @click="previousPage" :disabled="currentPage === 1">
        Précédent
      </button>
      <span>Page {{ currentPage }} / {{ totalPages }}</span>
      <button @click="nextPage" :disabled="currentPage === totalPages">
        Suivant
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useList } from '@/composables/useList';
import { useSearch } from '@/composables/useSearch';
import { useSort } from '@/composables/useSort';
import { usePagination } from '@/composables/usePagination';

// Utiliser les composables
const { items, loading, error, loadItems } = useList();
const { searchQuery, filteredItems } = useSearch(items, ['name', 'description']);
const { sortedItems, sortBy, getSortIcon } = useSort(filteredItems);
const { 
  currentPage, 
  totalPages, 
  paginatedItems, 
  nextPage, 
  previousPage,
  resetPage 
} = usePagination(sortedItems);

// Réinitialiser la page lors de la recherche
watch(searchQuery, () => {
  resetPage();
});

// Charger les données
onMounted(async () => {
  await loadItems(async () => {
    const response = await fetch('/api/items');
    return response.json();
  });
});
</script>
```

---

## 📚 Ressources supplémentaires

### Bibliothèques recommandées

- **TanStack Table** : Tableau avancé avec toutes les fonctionnalités
- **Vue Good Table** : Tableau Vue.js complet
- **PrimeVue DataTable** : Composant de tableau riche
- **AG Grid** : Grille de données professionnelle

### Liens utiles

- [Documentation Vue.js - Rendu de listes](https://vuejs.org/guide/essentials/list.html)
- [Vue.js Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Patterns de composables](https://vueuse.org/)

---

## 🎯 Checklist pour une liste complète

- [ ] Affichage des données
- [ ] État de chargement
- [ ] Gestion des erreurs
- [ ] État vide
- [ ] Recherche/Filtrage
- [ ] Tri
- [ ] Pagination
- [ ] Actions (CRUD)
- [ ] Responsive design
- [ ] Accessibilité (ARIA)
- [ ] Performance (virtualisation si > 1000 items)

---

**Créé pour** : Projet PrestaShop NewApp  
**Version** : 1.0  
**Dernière mise à jour** : 2024  
**Mots-clés** : liste, tableau, vue.js, v-for, recherche, tri, pagination, filtres, actions, CRUD, API, composables, patterns réutilisables
