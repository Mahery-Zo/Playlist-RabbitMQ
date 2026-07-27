# 📝 Documentation complète : Formulaires de création avec Vue.js

<!-- MOTS-CLÉS pour recherche Ctrl+F -->
<!-- formulaire, form, création, create, input, textarea, select, checkbox, radio, validation, v-model, submit, POST, API, axios, erreur, error, champ, field, required, obligatoire, optionnel, bouton, button, enregistrer, save -->

## 🎯 Table des matières

1. [Formulaire simple](#formulaire-simple)
2. [Formulaire avec validation](#formulaire-avec-validation)
3. [Formulaire avec API](#formulaire-avec-api)
4. [Types de champs](#types-de-champs)
5. [Formulaire multi-étapes](#formulaire-multi-etapes)
6. [Upload de fichiers](#upload-fichiers)
7. [Formulaire dynamique](#formulaire-dynamique)
8. [Gestion des erreurs](#gestion-erreurs)
9. [Patterns réutilisables](#patterns-reutilisables)
10. [Service de création](#service-creation)

---

## 1. Formulaire simple {#formulaire-simple}

<!-- MOTS-CLÉS: formulaire basique, v-model, input, submit, basic form, simple -->

### Code réutilisable

```vue
<template>
  <div class="simple-form">
    <h2>Créer un produit</h2>
    
    <!-- Formulaire -->
    <form @submit.prevent="handleSubmit">
      <!-- Champ nom -->
      <div class="form-group">
        <label for="name">Nom du produit *</label>
        <input 
          id="name"
          v-model="form.name" 
          type="text" 
          placeholder="Ex: iPhone 15"
          required
        />
      </div>
      
      <!-- Champ prix -->
      <div class="form-group">
        <label for="price">Prix (€) *</label>
        <input 
          id="price"
          v-model.number="form.price" 
          type="number" 
          step="0.01"
          placeholder="Ex: 999.99"
          required
        />
      </div>

      <!-- Champ description -->
      <div class="form-group">
        <label for="description">Description</label>
        <textarea 
          id="description"
          v-model="form.description" 
          rows="4"
          placeholder="Description du produit..."
        ></textarea>
      </div>
      
      <!-- Boutons -->
      <div class="form-actions">
        <button type="submit" class="btn-submit">
          Créer le produit
        </button>
        <button type="button" @click="resetForm" class="btn-cancel">
          Annuler
        </button>
      </div>
    </form>
    
    <!-- Message de succès -->
    <div v-if="successMessage" class="success-message">
      ✅ {{ successMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Données du formulaire
const form = ref({
  name: '',
  price: 0,
  description: ''
});

// Message de succès
const successMessage = ref('');

// Soumettre le formulaire
const handleSubmit = () => {
  console.log('📤 Données du formulaire:', form.value);
  
  // Afficher un message de succès
  successMessage.value = `Produit "${form.value.name}" créé avec succès !`;
  
  // Réinitialiser le formulaire après 2 secondes
  setTimeout(() => {
    resetForm();
    successMessage.value = '';
  }, 2000);
};

// Réinitialiser le formulaire
const resetForm = () => {
  
};
</script>

<style scoped>
.simple-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #007bff;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.btn-submit {
  flex: 1;
  padding: 12px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-submit:hover {
  background: #218838;
}

.btn-cancel {
  flex: 1;
  padding: 12px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #5a6268;
}

.success-message {
  margin-top: 20px;
  padding: 15px;
  background: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 4px;
  color: #155724;
}
</style>
```

---

## 2. Formulaire avec validation {#formulaire-avec-validation}

<!-- MOTS-CLÉS: validation, règles, rules, required, min, max, email, pattern, erreur, error message -->

### Code réutilisable complet

```vue
<template>
  <div class="validated-form">
    <h2>Créer un compte</h2>
    
    <form @submit.prevent="handleSubmit">
      <!-- Email -->
      <div class="form-group" :class="{ 'has-error': errors.email }">
        <label for="email">Email *</label>
        <input 
          id="email"
          v-model="form.email" 
          type="email"
          @blur="validateField('email')"
          placeholder="exemple@email.com"
        />
        <span v-if="errors.email" class="error-text">
          {{ errors.email }}
        </span>
      </div>
      
      <!-- Mot de passe -->
      <div class="form-group" :class="{ 'has-error': errors.password }">
        <label for="password">Mot de passe *</label>
        <input 
          id="password"
          v-model="form.password" 
          type="password"
          @blur="validateField('password')"
          placeholder="Minimum 8 caractères"
        />
        <span v-if="errors.password" class="error-text">
          {{ errors.password }}
        </span>
      </div>
      
      <!-- Confirmation mot de passe -->
      <div class="form-group" :class="{ 'has-error': errors.confirmPassword }">
        <label for="confirmPassword">Confirmer le mot de passe *</label>
        <input 
          id="confirmPassword"
          v-model="form.confirmPassword" 
          type="password"
          @blur="validateField('confirmPassword')"
          placeholder="Retapez le mot de passe"
        />
        <span v-if="errors.confirmPassword" class="error-text">
          {{ errors.confirmPassword }}
        </span>
      </div>
      
      <!-- Téléphone -->
      <div class="form-group" :class="{ 'has-error': errors.phone }">
        <label for="phone">Téléphone</label>
        <input 
          id="phone"
          v-model="form.phone" 
          type="tel"
          @blur="validateField('phone')"
          placeholder="06 12 34 56 78"
        />
        <span v-if="errors.phone" class="error-text">
          {{ errors.phone }}
        </span>
      </div>
      
      <!-- Conditions -->
      <div class="form-group" :class="{ 'has-error': errors.terms }">
        <label class="checkbox-label">
          <input 
            v-model="form.terms" 
            type="checkbox"
            @change="validateField('terms')"
          />
          J'accepte les conditions d'utilisation *
        </label>
        <span v-if="errors.terms" class="error-text">
          {{ errors.terms }}
        </span>
      </div>
      
      <!-- Bouton submit -->
      <button type="submit" :disabled="!isFormValid" class="btn-submit">
        Créer mon compte
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';

// Données du formulaire
const form = ref({
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  terms: false
});

// Erreurs de validation
const errors = ref({
  email: '',
  password: '',
  confirmPassword: '',
  phone: '',
  terms: ''
});

// Règles de validation
const validationRules = {
  email: (value) => {
    if (!value) return 'L\'email est obligatoire';
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) return 'Email invalide';
    return '';
  },
  
  password: (value) => {
    if (!value) return 'Le mot de passe est obligatoire';
    if (value.length < 8) return 'Minimum 8 caractères';
    if (!/[A-Z]/.test(value)) return 'Doit contenir une majuscule';
    if (!/[0-9]/.test(value)) return 'Doit contenir un chiffre';
    return '';
  },
  
  confirmPassword: (value) => {
    if (!value) return 'Veuillez confirmer le mot de passe';
    if (value !== form.value.password) return 'Les mots de passe ne correspondent pas';
    return '';
  },
  
  phone: (value) => {
    if (!value) return ''; // Optionnel
    const phoneRegex = /^0[1-9](\d{2}){4}$/;
    const cleanPhone = value.replace(/\s/g, '');
    if (!phoneRegex.test(cleanPhone)) return 'Numéro de téléphone invalide';
    return '';
  },
  
  terms: (value) => {
    if (!value) return 'Vous devez accepter les conditions';
    return '';
  }
};

// Valider un champ spécifique
const validateField = (fieldName) => {
  const rule = validationRules[fieldName];
  if (rule) {
    errors.value[fieldName] = rule(form.value[fieldName]);
  }
};

// Valider tout le formulaire
const validateForm = () => {
  let isValid = true;
  
  Object.keys(validationRules).forEach(fieldName => {
    validateField(fieldName);
    if (errors.value[fieldName]) {
      isValid = false;
    }
  });
  
  return isValid;
};

// Vérifier si le formulaire est valide
const isFormValid = computed(() => {
  return Object.values(errors.value).every(error => error === '') &&
         form.value.email &&
         form.value.password &&
         form.value.confirmPassword &&
         form.value.terms;
});

// Soumettre le formulaire
const handleSubmit = () => {
  if (validateForm()) {
    console.log('✅ Formulaire valide:', form.value);
    alert('Compte créé avec succès !');
  } else {
    console.log('❌ Formulaire invalide');
  }
};
</script>

<style scoped>
.validated-form {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group.has-error input {
  border-color: #dc3545;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.error-text {
  display: block;
  margin-top: 5px;
  color: #dc3545;
  font-size: 13px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: auto;
}

.btn-submit {
  width: 100%;
  padding: 12px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-submit:hover:not(:disabled) {
  background: #0056b3;
}

.btn-submit:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
```

---

## 3. Formulaire avec API {#formulaire-avec-api}

<!-- MOTS-CLÉS: API, POST, axios, async, await, loading, chargement, erreur, error, success -->

### Code réutilisable complet

```vue
<template>
  <div class="api-form">
    <h2>Créer un produit</h2>
    
    <form @submit.prevent="handleSubmit">
      <!-- Nom -->
      <div class="form-group">
        <label for="name">Nom du produit *</label>
        <input 
          id="name"
          v-model="form.name" 
          type="text"
          :disabled="loading"
          required
        />
      </div>
      
      <!-- Prix -->
      <div class="form-group">
        <label for="price">Prix (€) *</label>
        <input 
          id="price"
          v-model.number="form.price" 
          type="number"
          step="0.01"
          :disabled="loading"
          required
        />
      </div>
      
      <!-- Catégorie -->
      <div class="form-group">
        <label for="category">Catégorie *</label>
        <select 
          id="category"
          v-model="form.id_category" 
          :disabled="loading"
          required
        >
          <option value="">Sélectionnez une catégorie</option>
          <option 
            v-for="cat in categories" 
            :key="cat.id" 
            :value="cat.id"
          >
            {{ cat.name }}
          </option>
        </select>
      </div>
      
      <!-- Description -->
      <div class="form-group">
        <label for="description">Description</label>
        <textarea 
          id="description"
          v-model="form.description" 
          rows="4"
          :disabled="loading"
        ></textarea>
      </div>
      
      <!-- Stock -->
      <div class="form-group">
        <label for="stock">Stock *</label>
        <input 
          id="stock"
          v-model.number="form.stock" 
          type="number"
          min="0"
          :disabled="loading"
          required
        />
      </div>
      
      <!-- Bouton submit -->
      <button type="submit" :disabled="loading" class="btn-submit">
        {{ loading ? '⏳ Création en cours...' : 'Créer le produit' }}
      </button>
    </form>
    
    <!-- Message de succès -->
    <div v-if="successMessage" class="success-message">
      ✅ {{ successMessage }}
    </div>
    
    <!-- Message d'erreur -->
    <div v-if="errorMessage" class="error-message">
      ❌ {{ errorMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { XMLBuilder } from 'fast-xml-parser';

// Données du formulaire
const form = ref({
  name: '',
  price: 0,
  id_category: '',
  description: '',
  stock: 0
});

// États
const loading = ref(false);
const successMessage = ref('');
const errorMessage = ref('');
const categories = ref([]);

// Charger les catégories au montage
onMounted(async () => {
  try {
    const response = await axios.get('/api/categories', {
      auth: {
        username: 'VOTRE_CLE_API',
        password: ''
      }
    });
    
    // Parser la réponse XML
    const parser = new XMLParser({
      ignoreAttributes: false,
      attributeNamePrefix: '@_'
    });
    
    const data = parser.parse(response.data);
    categories.value = data.prestashop.categories.category || [];
    
  } catch (err) {
    console.error('Erreur chargement catégories:', err);
  }
});

// Soumettre le formulaire
const handleSubmit = async () => {
  loading.value = true;
  successMessage.value = '';
  errorMessage.value = '';
  
  try {
    // Construire le XML pour PrestaShop
    const builder = new XMLBuilder({
      ignoreAttributes: false,
      format: true
    });
    
    const xmlObj = {
      prestashop: {
        product: {
          name: {
            language: {
              '@_id': 1,
              '#text': form.value.name
            }
          },
          price: form.value.price,
          id_category_default: form.value.id_category,
          description: {
            language: {
              '@_id': 1,
              '#text': form.value.description
            }
          },
          quantity: form.value.stock,
          active: 1,
          state: 1
        }
      }
    };
    
    const xmlContent = builder.build(xmlObj);
    
    // Envoyer à l'API
    const response = await axios.post('/api/products', xmlContent, {
      auth: {
        username: 'VOTRE_CLE_API',
        password: ''
      },
      headers: {
        'Content-Type': 'application/xml'
      }
    });
    
    console.log('✅ Produit créé:', response.data);
    
    successMessage.value = `Produit "${form.value.name}" créé avec succès !`;
    
    // Réinitialiser le formulaire après 2 secondes
    setTimeout(() => {
      resetForm();
      successMessage.value = '';
    }, 2000);
    
  } catch (err) {
    console.error('❌ Erreur:', err);
    
    // Extraire le message d'erreur
    let message = 'Une erreur est survenue';
    
    if (err.response && err.response.data) {
      if (typeof err.response.data === 'string') {
        const match = err.response.data.match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/);
        if (match) {
          message = match[1];
        }
      }
    }
    
    errorMessage.value = message;
    
  } finally {
    loading.value = false;
  }
};

// Réinitialiser le formulaire
const resetForm = () => {
  form.value = {
    name: '',
    price: 0,
    id_category: '',
    description: '',
    stock: 0
  };
};
</script>

<style scoped>
.api-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group input:disabled,
.form-group select:disabled,
.form-group textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.btn-submit {
  width: 100%;
  padding: 12px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-submit:hover:not(:disabled) {
  background: #218838;
}

.btn-submit:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.success-message {
  margin-top: 20px;
  padding: 15px;
  background: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 4px;
  color: #155724;
}

.error-message {
  margin-top: 20px;
  padding: 15px;
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  color: #721c24;
}
</style>
```

---

## 4. Types de champs {#types-de-champs}

<!-- MOTS-CLÉS: input, textarea, select, checkbox, radio, date, file, number, email, tel, url -->

### Tous les types de champs réutilisables

```vue
<template>
  <div class="field-types">
    <h2>Tous les types de champs</h2>
    
    <form @submit.prevent="handleSubmit">
      <!-- INPUT TEXT -->
      <div class="form-group">
        <label>Texte simple</label>
        <input v-model="form.text" type="text" placeholder="Texte..." />
      </div>
      
      <!-- INPUT EMAIL -->
      <div class="form-group">
        <label>Email</label>
        <input v-model="form.email" type="email" placeholder="email@exemple.com" />
      </div>
      
      <!-- INPUT PASSWORD -->
      <div class="form-group">
        <label>Mot de passe</label>
        <input v-model="form.password" type="password" placeholder="••••••••" />
      </div>
      
      <!-- INPUT NUMBER -->
      <div class="form-group">
        <label>Nombre</label>
        <input v-model.number="form.number" type="number" min="0" max="100" step="1" />
      </div>
      
      <!-- INPUT RANGE (Slider) -->
      <div class="form-group">
        <label>Slider ({{ form.range }})</label>
        <input v-model.number="form.range" type="range" min="0" max="100" />
      </div>
      
      <!-- INPUT DATE -->
      <div class="form-group">
        <label>Date</label>
        <input v-model="form.date" type="date" />
      </div>
      
      <!-- INPUT TIME -->
      <div class="form-group">
        <label>Heure</label>
        <input v-model="form.time" type="time" />
      </div>
      
      <!-- INPUT COLOR -->
      <div class="form-group">
        <label>Couleur</label>
        <input v-model="form.color" type="color" />
      </div>
      
      <!-- INPUT TEL -->
      <div class="form-group">
        <label>Téléphone</label>
        <input v-model="form.phone" type="tel" placeholder="06 12 34 56 78" />
      </div>
      
      <!-- INPUT URL -->
      <div class="form-group">
        <label>URL</label>
        <input v-model="form.url" type="url" placeholder="https://exemple.com" />
      </div>
      
      <!-- TEXTAREA -->
      <div class="form-group">
        <label>Texte long</label>
        <textarea v-model="form.textarea" rows="4" placeholder="Description..."></textarea>
      </div>
      
      <!-- SELECT (Liste déroulante) -->
      <div class="form-group">
        <label>Liste déroulante</label>
        <select v-model="form.select">
          <option value="">Sélectionnez...</option>
          <option value="option1">Option 1</option>
          <option value="option2">Option 2</option>
          <option value="option3">Option 3</option>
        </select>
      </div>
      
      <!-- SELECT MULTIPLE -->
      <div class="form-group">
        <label>Sélection multiple (Ctrl+clic)</label>
        <select v-model="form.multiSelect" multiple size="4">
          <option value="a">Choix A</option>
          <option value="b">Choix B</option>
          <option value="c">Choix C</option>
          <option value="d">Choix D</option>
        </select>
      </div>
      
      <!-- CHECKBOX (Case à cocher unique) -->
      <div class="form-group">
        <label class="checkbox-label">
          <input v-model="form.checkbox" type="checkbox" />
          J'accepte les conditions
        </label>
      </div>
      
      <!-- CHECKBOX MULTIPLE -->
      <div class="form-group">
        <label>Intérêts (plusieurs choix)</label>
        <label class="checkbox-label">
          <input v-model="form.interests" type="checkbox" value="sport" />
          Sport
        </label>
        <label class="checkbox-label">
          <input v-model="form.interests" type="checkbox" value="musique" />
          Musique
        </label>
        <label class="checkbox-label">
          <input v-model="form.interests" type="checkbox" value="lecture" />
          Lecture
        </label>
      </div>
      
      <!-- RADIO BUTTONS -->
      <div class="form-group">
        <label>Genre (un seul choix)</label>
        <label class="radio-label">
          <input v-model="form.gender" type="radio" value="homme" />
          Homme
        </label>
        <label class="radio-label">
          <input v-model="form.gender" type="radio" value="femme" />
          Femme
        </label>
        <label class="radio-label">
          <input v-model="form.gender" type="radio" value="autre" />
          Autre
        </label>
      </div>
      
      <!-- INPUT FILE -->
      <div class="form-group">
        <label>Fichier</label>
        <input type="file" @change="handleFileUpload" />
        <span v-if="form.fileName">{{ form.fileName }}</span>
      </div>
      
      <!-- Bouton submit -->
      <button type="submit" class="btn-submit">
        Envoyer
      </button>
    </form>
    
    <!-- Affichage des valeurs -->
    <div class="values-display">
      <h3>Valeurs du formulaire :</h3>
      <pre>{{ JSON.stringify(form, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Données du formulaire
const form = ref({
  text: '',
  email: '',
  password: '',
  number: 0,
  range: 50,
  date: '',
  time: '',
  color: '#000000',
  phone: '',
  url: '',
  textarea: '',
  select: '',
  multiSelect: [],
  checkbox: false,
  interests: [],
  gender: '',
  fileName: ''
});

// Gérer l'upload de fichier
const handleFileUpload = (event) => {
  const file = event.target.files[0];
  if (file) {
    form.value.fileName = file.name;
  }
};

// Soumettre le formulaire
const handleSubmit = () => {
  console.log('📤 Données:', form.value);
  alert('Formulaire soumis ! Voir la console.');
};
</script>

<style scoped>
.field-types {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #333;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.checkbox-label,
.radio-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"],
.radio-label input[type="radio"] {
  width: auto;
}

.btn-submit {
  width: 100%;
  padding: 12px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.values-display {
  margin-top: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.values-display pre {
  background: white;
  padding: 15px;
  border-radius: 4px;
  overflow-x: auto;
}
</style>
```


---

## 5. Formulaire multi-étapes {#formulaire-multi-etapes}

<!-- MOTS-CLÉS: multi-étapes, wizard, steps, étapes, navigation, suivant, précédent, next, previous -->

### Code réutilisable

```vue
<template>
  <div class="multi-step-form">
    <h2>Inscription en 3 étapes</h2>
    
    <!-- Indicateur d'étapes -->
    <div class="steps-indicator">
      <div 
        v-for="(step, index) in steps" 
        :key="index"
        class="step"
        :class="{ 
          active: currentStep === index + 1,
          completed: currentStep > index + 1
        }"
      >
        <div class="step-number">{{ index + 1 }}</div>
        <div class="step-label">{{ step }}</div>
      </div>
    </div>
    
    <!-- Formulaire -->
    <form @submit.prevent="handleSubmit">
      <!-- Étape 1: Informations personnelles -->
      <div v-show="currentStep === 1" class="step-content">
        <h3>Informations personnelles</h3>
        
        <div class="form-group">
          <label>Prénom *</label>
          <input v-model="form.firstName" type="text" required />
        </div>
        
        <div class="form-group">
          <label>Nom *</label>
          <input v-model="form.lastName" type="text" required />
        </div>
        
        <div class="form-group">
          <label>Date de naissance *</label>
          <input v-model="form.birthDate" type="date" required />
        </div>
      </div>
      
      <!-- Étape 2: Coordonnées -->
      <div v-show="currentStep === 2" class="step-content">
        <h3>Coordonnées</h3>
        
        <div class="form-group">
          <label>Email *</label>
          <input v-model="form.email" type="email" required />
        </div>
        
        <div class="form-group">
          <label>Téléphone *</label>
          <input v-model="form.phone" type="tel" required />
        </div>
        
        <div class="form-group">
          <label>Adresse *</label>
          <textarea v-model="form.address" rows="3" required></textarea>
        </div>
      </div>
      
      <!-- Étape 3: Confirmation -->
      <div v-show="currentStep === 3" class="step-content">
        <h3>Confirmation</h3>
        
        <div class="summary">
          <h4>Récapitulatif</h4>
          <p><strong>Nom :</strong> {{ form.firstName }} {{ form.lastName }}</p>
          <p><strong>Date de naissance :</strong> {{ form.birthDate }}</p>
          <p><strong>Email :</strong> {{ form.email }}</p>
          <p><strong>Téléphone :</strong> {{ form.phone }}</p>
          <p><strong>Adresse :</strong> {{ form.address }}</p>
        </div>
        
        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="form.terms" type="checkbox" required />
            J'accepte les conditions d'utilisation *
          </label>
        </div>
      </div>
      
      <!-- Boutons de navigation -->
      <div class="form-navigation">
        <button 
          v-if="currentStep > 1"
          type="button" 
          @click="previousStep"
          class="btn-previous"
        >
          ← Précédent
        </button>
        
        <button 
          v-if="currentStep < 3"
          type="button" 
          @click="nextStep"
          class="btn-next"
        >
          Suivant →
        </button>
        
        <button 
          v-if="currentStep === 3"
          type="submit"
          class="btn-submit"
        >
          Valider l'inscription
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue';

// Étapes
const steps = ['Informations', 'Coordonnées', 'Confirmation'];
const currentStep = ref(1);

// Données du formulaire
const form = ref({
  firstName: '',
  lastName: '',
  birthDate: '',
  email: '',
  phone: '',
  address: '',
  terms: false
});

// Navigation
const nextStep = () => {
  if (currentStep.value < 3) {
    currentStep.value++;
  }
};

const previousStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--;
  }
};

// Soumettre
const handleSubmit = () => {
  console.log('✅ Inscription validée:', form.value);
  alert('Inscription réussie !');
};
</script>

<style scoped>
.multi-step-form {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

.steps-indicator {
  display: flex;
  justify-content: space-between;
  margin-bottom: 40px;
  position: relative;
}

.steps-indicator::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 10%;
  right: 10%;
  height: 2px;
  background: #ddd;
  z-index: 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ddd;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  transition: all 0.3s;
}

.step.active .step-number {
  background: #007bff;
  color: white;
}

.step.completed .step-number {
  background: #28a745;
  color: white;
}

.step-label {
  font-size: 14px;
  color: #666;
}

.step.active .step-label {
  color: #007bff;
  font-weight: bold;
}

.step-content {
  min-height: 300px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.summary {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.summary h4 {
  margin-top: 0;
  color: #007bff;
}

.summary p {
  margin: 10px 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
}

.form-navigation {
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.btn-previous,
.btn-next,
.btn-submit {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}

.btn-previous {
  background: #6c757d;
  color: white;
}

.btn-next {
  background: #007bff;
  color: white;
}

.btn-submit {
  background: #28a745;
  color: white;
}
</style>
```

---

## 6. Upload de fichiers {#upload-fichiers}

<!-- MOTS-CLÉS: upload, fichier, file, image, drag drop, glisser déposer, preview, aperçu -->

### Code réutilisable

```vue
<template>
  <div class="file-upload">
    <h2>Upload de fichiers</h2>
    
    <!-- Zone de drag & drop -->
    <div 
      class="drop-zone"
      :class="{ 'drag-over': isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <div class="drop-zone-content">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="17 8 12 3 7 8"></polyline>
          <line x1="12" y1="3" x2="12" y2="15"></line>
        </svg>
        <p>Glissez-déposez vos fichiers ici</p>
        <p class="or">ou</p>
        <label for="file-input" class="btn-browse">
          Parcourir
        </label>
        <input 
          id="file-input"
          type="file" 
          multiple
          accept="image/*"
          @change="handleFileSelect"
          style="display: none;"
        />
      </div>
    </div>
    
    <!-- Liste des fichiers -->
    <div v-if="files.length > 0" class="files-list">
      <h3>Fichiers sélectionnés ({{ files.length }})</h3>
      
      <div 
        v-for="(file, index) in files" 
        :key="index"
        class="file-item"
      >
        <!-- Aperçu de l'image -->
        <div class="file-preview">
          <img v-if="file.preview" :src="file.preview" alt="Preview" />
          <div v-else class="file-icon">📄</div>
        </div>
        
        <!-- Informations -->
        <div class="file-info">
          <div class="file-name">{{ file.name }}</div>
          <div class="file-size">{{ formatFileSize(file.size) }}</div>
          
          <!-- Barre de progression -->
          <div v-if="file.uploading" class="progress-bar">
            <div 
              class="progress-fill" 
              :style="{ width: file.progress + '%' }"
            ></div>
          </div>
          
          <!-- Statut -->
          <div v-if="file.uploaded" class="file-status success">
            ✓ Uploadé
          </div>
          <div v-else-if="file.error" class="file-status error">
            ✗ Erreur
          </div>
        </div>
        
        <!-- Bouton supprimer -->
        <button 
          @click="removeFile(index)" 
          class="btn-remove"
          :disabled="file.uploading"
        >
          🗑️
        </button>
      </div>
    </div>
    
    <!-- Bouton upload -->
    <button 
      v-if="files.length > 0 && !allUploaded"
      @click="uploadFiles"
      :disabled="uploading"
      class="btn-upload"
    >
      {{ uploading ? '⏳ Upload en cours...' : `📤 Uploader ${files.length} fichier(s)` }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';

// États
const files = ref([]);
const isDragging = ref(false);
const uploading = ref(false);

// Vérifier si tous les fichiers sont uploadés
const allUploaded = computed(() => {
  return files.value.length > 0 && 
         files.value.every(f => f.uploaded);
});

// Gérer la sélection de fichiers
const handleFileSelect = (event) => {
  const selectedFiles = Array.from(event.target.files);
  addFiles(selectedFiles);
};

// Gérer le drop
const handleDrop = (event) => {
  isDragging.value = false;
  const droppedFiles = Array.from(event.dataTransfer.files);
  addFiles(droppedFiles);
};

// Ajouter des fichiers
const addFiles = (newFiles) => {
  newFiles.forEach(file => {
    // Créer un aperçu pour les images
    const fileObj = {
      name: file.name,
      size: file.size,
      file: file,
      preview: null,
      uploading: false,
      uploaded: false,
      error: false,
      progress: 0
    };
    
    // Générer l'aperçu si c'est une image
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        fileObj.preview = e.target.result;
      };
      reader.readAsDataURL(file);
    }
    
    files.value.push(fileObj);
  });
};

// Supprimer un fichier
const removeFile = (index) => {
  files.value.splice(index, 1);
};

// Formater la taille du fichier
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

// Uploader les fichiers
const uploadFiles = async () => {
  uploading.value = true;
  
  for (const fileObj of files.value) {
    if (fileObj.uploaded) continue;
    
    try {
      fileObj.uploading = true;
      fileObj.error = false;
      
      // Créer FormData
      const formData = new FormData();
      formData.append('file', fileObj.file);
      
      // Envoyer à l'API
      await axios.post('/api/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          // Mettre à jour la progression
          fileObj.progress = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          );
        }
      });
      
      fileObj.uploaded = true;
      fileObj.uploading = false;
      
    } catch (err) {
      console.error('Erreur upload:', err);
      fileObj.error = true;
      fileObj.uploading = false;
    }
  }
  
  uploading.value = false;
};
</script>

<style scoped>
.file-upload {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px;
}

.drop-zone {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 40px;
  text-align: center;
  transition: all 0.3s;
  cursor: pointer;
}

.drop-zone.drag-over {
  border-color: #007bff;
  background: #e7f3ff;
}

.drop-zone-content svg {
  color: #6c757d;
  margin-bottom: 20px;
}

.drop-zone-content p {
  margin: 10px 0;
  color: #6c757d;
}

.or {
  font-size: 14px;
  color: #999;
}

.btn-browse {
  display: inline-block;
  padding: 10px 20px;
  background: #007bff;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-browse:hover {
  background: #0056b3;
}

.files-list {
  margin-top: 30px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.file-preview {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-icon {
  font-size: 30px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-weight: bold;
  margin-bottom: 5px;
}

.file-size {
  font-size: 14px;
  color: #6c757d;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  margin-top: 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #007bff;
  transition: width 0.3s;
}

.file-status {
  margin-top: 5px;
  font-size: 14px;
}

.file-status.success {
  color: #28a745;
}

.file-status.error {
  color: #dc3545;
}

.btn-remove {
  padding: 8px 12px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 18px;
}

.btn-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-upload {
  width: 100%;
  padding: 12px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  margin-top: 20px;
}

.btn-upload:hover:not(:disabled) {
  background: #218838;
}

.btn-upload:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
```

---

## 7. Service de création réutilisable {#service-creation}

<!-- MOTS-CLÉS: service, API, POST, create, axios, XML, JSON, builder -->

### Service générique pour PrestaShop

```javascript
// services/CreateService.js
import axios from 'axios';
import { XMLBuilder, XMLParser } from 'fast-xml-parser';

const API_KEY = 'VOTRE_CLE_API';

export const CreateService = {
  /**
   * CRÉER: Un produit
   */
  async createProduct(productData) {
    const builder = new XMLBuilder({
      ignoreAttributes: false,
      format: true
    });
    
    const xmlObj = {
      prestashop: {
        product: {
          name: {
            language: {
              '@_id': 1,
              '#text': productData.name
            }
          },
          price: productData.price,
          id_category_default: productData.id_category,
          description: {
            language: {
              '@_id': 1,
              '#text': productData.description || ''
            }
          },
          quantity: productData.stock || 0,
          active: 1,
          state: 1
        }
      }
    };
    
    const xmlContent = builder.build(xmlObj);
    
    const response = await axios.post('/api/products', xmlContent, {
      auth: { username: API_KEY, password: '' },
      headers: { 'Content-Type': 'application/xml' }
    });
    
    const parser = new XMLParser({
      ignoreAttributes: false,
      attributeNamePrefix: '@_'
    });
    
    const result = parser.parse(response.data);
    return result.prestashop.product.id;
  },
  
  /**
   * CRÉER: Un client
   */
  async createCustomer(customerData) {
    const builder = new XMLBuilder({
      ignoreAttributes: false,
      format: true
    });
    
    const xmlObj = {
      prestashop: {
        customer: {
          firstname: customerData.firstname,
          lastname: customerData.lastname,
          email: customerData.email,
          passwd: customerData.password,
          id_default_group: 3, // Groupe client par défaut
          active: 1
        }
      }
    };
    
    const xmlContent = builder.build(xmlObj);
    
    const response = await axios.post('/api/customers', xmlContent, {
      auth: { username: API_KEY, password: '' },
      headers: { 'Content-Type': 'application/xml' }
    });
    
    const parser = new XMLParser({
      ignoreAttributes: false,
      attributeNamePrefix: '@_'
    });
    
    const result = parser.parse(response.data);
    return result.prestashop.customer.id;
  },
  
  /**
   * CRÉER: Une adresse
   */
  async createAddress(addressData) {
    const builder = new XMLBuilder({
      ignoreAttributes: false,
      format: true
    });
    
    const xmlObj = {
      prestashop: {
        address: {
          id_customer: addressData.id_customer,
          id_country: addressData.id_country || 8, // France par défaut
          alias: addressData.alias || 'Mon adresse',
          firstname: addressData.firstname,
          lastname: addressData.lastname,
          address1: addressData.address1,
          address2: addressData.address2 || '',
          postcode: addressData.postcode,
          city: addressData.city,
          phone: addressData.phone || '',
          phone_mobile: addressData.phone_mobile || ''
        }
      }
    };
    
    const xmlContent = builder.build(xmlObj);
    
    const response = await axios.post('/api/addresses', xmlContent, {
      auth: { username: API_KEY, password: '' },
      headers: { 'Content-Type': 'application/xml' }
    });
    
    const parser = new XMLParser({
      ignoreAttributes: false,
      attributeNamePrefix: '@_'
    });
    
    const result = parser.parse(response.data);
    return result.prestashop.address.id;
  },
  
  /**
   * CRÉER: Une catégorie
   */
  async createCategory(categoryData) {
    const builder = new XMLBuilder({
      ignoreAttributes: false,
      format: true
    });
    
    const xmlObj = {
      prestashop: {
        category: {
          name: {
            language: {
              '@_id': 1,
              '#text': categoryData.name
            }
          },
          link_rewrite: {
            language: {
              '@_id': 1,
              '#text': categoryData.slug || categoryData.name.toLowerCase().replace(/\s+/g, '-')
            }
          },
          description: {
            language: {
              '@_id': 1,
              '#text': categoryData.description || ''
            }
          },
          id_parent: categoryData.id_parent || 2, // Catégorie racine par défaut
          active: 1
        }
      }
    };
    
    const xmlContent = builder.build(xmlObj);
    
    const response = await axios.post('/api/categories', xmlContent, {
      auth: { username: API_KEY, password: '' },
      headers: { 'Content-Type': 'application/xml' }
    });
    
    const parser = new XMLParser({
      ignoreAttributes: false,
      attributeNamePrefix: '@_'
    });
    
    const result = parser.parse(response.data);
    return result.prestashop.category.id;
  },
  
  /**
   * GÉRER: Les erreurs API
   */
  handleError(error) {
    let message = 'Une erreur est survenue';
    
    if (error.response && error.response.data) {
      if (typeof error.response.data === 'string') {
        const match = error.response.data.match(/<message><!\[CDATA\[(.*?)\]\]><\/message>/);
        if (match) {
          message = match[1];
        }
      } else if (error.response.data.errors) {
        message = JSON.stringify(error.response.data.errors);
      }
    }
    
    return message;
  }
};
```

### Utilisation du service

```vue
<script setup>
import { ref } from 'vue';
import { CreateService } from '@/services/CreateService';

const form = ref({
  name: '',
  price: 0,
  id_category: 2,
  description: '',
  stock: 0
});

const loading = ref(false);
const error = ref('');
const success = ref('');

const handleSubmit = async () => {
  loading.value = true;
  error.value = '';
  success.value = '';
  
  try {
    const productId = await CreateService.createProduct(form.value);
    success.value = `Produit créé avec l'ID: ${productId}`;
  } catch (err) {
    error.value = CreateService.handleError(err);
  } finally {
    loading.value = false;
  }
};
</script>
```

---

## 8. Patterns réutilisables {#patterns-reutilisables}

<!-- MOTS-CLÉS: composable, hook, réutilisable, useForm, useValidation -->

### Composable useForm

```javascript
// composables/useForm.js
import { ref, computed } from 'vue';

export function useForm(initialValues = {}) {
  const form = ref({ ...initialValues });
  const errors = ref({});
  const touched = ref({});
  
  // Réinitialiser le formulaire
  const reset = () => {
    form.value = { ...initialValues };
    errors.value = {};
    touched.value = {};
  };
  
  // Marquer un champ comme touché
  const touch = (fieldName) => {
    touched.value[fieldName] = true;
  };
  
  // Définir une erreur
  const setError = (fieldName, message) => {
    errors.value[fieldName] = message;
  };
  
  // Effacer une erreur
  const clearError = (fieldName) => {
    errors.value[fieldName] = '';
  };
  
  // Vérifier si le formulaire est valide
  const isValid = computed(() => {
    return Object.values(errors.value).every(error => !error);
  });
  
  return {
    form,
    errors,
    touched,
    reset,
    touch,
    setError,
    clearError,
    isValid
  };
}
```

### Utilisation

```vue
<script setup>
import { useForm } from '@/composables/useForm';

const { form, errors, reset, setError, isValid } = useForm({
  name: '',
  email: '',
  password: ''
});

const validateEmail = () => {
  if (!form.value.email) {
    setError('email', 'Email requis');
  } else if (!/\S+@\S+\.\S+/.test(form.value.email)) {
    setError('email', 'Email invalide');
  }
};

const handleSubmit = () => {
  if (isValid.value) {
    console.log('Formulaire valide:', form.value);
  }
};
</script>
```

---

## 🎓 Résumé des bonnes pratiques

### ✅ À faire
- Utiliser `v-model` pour lier les champs au modèle
- Valider les données côté client ET serveur
- Afficher des messages d'erreur clairs
- Désactiver le bouton submit pendant l'envoi
- Gérer les états de chargement
- Réinitialiser le formulaire après succès
- Utiliser `@submit.prevent` pour éviter le rechargement de page

### ❌ À éviter
- Ne pas valider les données
- Oublier de gérer les erreurs API
- Laisser le bouton submit actif pendant l'envoi
- Ne pas donner de feedback à l'utilisateur
- Oublier les champs obligatoires
- Ne pas nettoyer les données avant envoi

---

**📚 Documentation créée à partir des composants existants dans NewApp/**
