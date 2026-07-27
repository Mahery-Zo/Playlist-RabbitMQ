# Documentation : Manipulation des Données XML de l'API PrestaShop

Lors de la récupération de données depuis l'API PrestaShop, les données sont renvoyées au format XML. Dans ce projet, la librairie Javascript `fast-xml-parser` est utilisée pour convertir ce XML en objets JavaScript/JSON exploitables par votre application Vue.js.

Cette documentation explique la structure des objets générés et pourquoi vous vous retrouvez parfois à devoir utiliser `valeur` et d'autres fois `valeur["#text"]`.

---

## 1. Comprendre le comportement de `fast-xml-parser`

Lorsque le parser convertit le XML, son comportement dépend de **la présence ou non d'attributs** dans les balises XML ciblées. Dans PrestaShop, énormément de balises contiennent des attributs tels que `xlink:href` (pour les liens relationnels) ou `notFilterable`.

### Cas A : Balise simple sans attributs
Si l'API renvoie un XML totalement basique sans attributs :
```xml
<id_attribute>12</id_attribute>
```
Le parser va simplement le convertir en une valeur directe (chaîne ou nombre) car il n'a aucune information supplémentaire à stocker :
```javascript
// Résultat JS
{
  id_attribute: 12
}

// Utilisation
const id = product.id_attribute; // 12
```

### Cas B : Balise avec attributs
Si l'API renvoie un XML contenant des attributs (ce qui est le comportement par défaut de l'API PrestaShop) :
```xml
<id_attribute xlink:href="http://monsite/api/product_options/12">12</id_attribute>
```
Le parser est obligé de créer un objet (Object) complexe pour pouvoir conserver et stocker à la fois l'attribut (généralement préfixé par `@_` selon la configuration) et le contenu texte (qui sera par conséquent stocké dans la clé `#text`) :
```javascript
// Résultat JS
{
  id_attribute: {
    "@_xlink:href": "http://monsite/api/product_options/12",
    "#text": 12
  }
}

// Utilisation
const id = product.id_attribute["#text"]; // 12
```

---

## 2. Le problème rencontré lors du développement

Parce que l'API PrestaShop peut parfois renvoyer une balise avec attributs et d'autres fois sans (selon le niveau de détail `display=full`, selon les champs, ou si l'on requête un objet individuel vs une liste), **la structure de la donnée JS finale devient imprévisible**.

```javascript
// ❌ Risqué : Peut retourner "undefined" ou "[object Object]"
const idAttr = order.prestashop.product.id_attribute; 

// ❌ Risqué : Va provoquer un plantage JS (Cannot read properties of undefined)
// si id_attribute est un simple nombre ou une string
const idAttr = order.prestashop.product.id_attribute["#text"];
```

---

## 3. La Solution : Fonctions Réutilisables (Helpers)

Pour éviter tous les bugs liés à cette différence de format, il ne faut jamais accéder directement à la valeur en supposant qu'elle est toujours de la même forme. 

Voici des fonctions (Helpers) réutilisables à placer dans vos fichiers (par exemple dans `NewApp/src/utils/xmlHelpers.js` ou directement dans vos services) pour extraire la valeur en toute sécurité, peu importe comment elle a été parsée.

### Code Réutilisable (Helpers JS)

```javascript
/**
 * Extrait la valeur d'un nœud XML parsé de manière sécurisée.
 * Gère à la fois le format primitif et le format objet avec attributs.
 * 
 * @param {any} node - Le nœud récupéré depuis fast-xml-parser
 * @param {any} defaultValue - La valeur par défaut si le nœud est null/undefined
 * @returns {any} La valeur extraite
 */
export const extractXmlValue = (node, defaultValue = '') => {
  if (node === null || node === undefined) {
    return defaultValue;
  }
  
  // Si le parser a créé un objet contenant la propriété #text
  if (typeof node === 'object' && '#text' in node) {
    return node['#text'];
  }
  
  // S'il s'agit d'un objet vide ou autre (ex: <tag/> balise auto-fermante)
  if (typeof node === 'object' && Object.keys(node).length === 0) {
    return defaultValue;
  }

  // Si c'est déjà une valeur primitive (string, number, etc.)
  return node;
};

// =========================================================================
// Versions courtes (ES6) très pratiques et couramment utilisées (recommandé)
// =========================================================================

// Pour le texte (Optional Chaining + Nullish Coalescing)
export const txt = (v) => v?.['#text'] ?? v ?? '';

// Pour les nombres
export const num = (v) => parseFloat(v?.['#text'] ?? v) || 0;
```

### Exemples d'utilisation

#### Exemple 1 : Récupérer des valeurs simples (ID, Montants, Noms)
```javascript
import { txt, num } from '../utils/xmlHelpers';

// ❌ Au lieu de faire un "if" ou de prendre le risque :
// let price = typeof product.price === 'object' ? product.price["#text"] : product.price;

// ✅ Faites simplement ceci, c'est robuste à 100% :
const price = num(product.price);
const idAttr = txt(product.id_attribute);
const name = txt(product.name.language); 
```

#### Exemple 2 : Le piège des Listes / Tableaux (Arrays)
Un autre piège classique intimement lié à la conversion XML -> JSON : si une liste XML ne contient **qu'un seul élément**, le parser n'a aucun moyen de deviner que c'est une liste. Il renvoie donc un Objet au lieu d'un Tableau (`Array`). Voici le code pour sécuriser cela :

```javascript
/**
 * Assure qu'un nœud XML est toujours traité comme un tableau (Array).
 */
export const ensureArray = (node) => {
  if (node === null || node === undefined || node === '') return [];
  return Array.isArray(node) ? node : [node];
};

// Utilisation :
// ❌ Ne jamais faire directement un .forEach sur un nœud XML qui peut être unique !
// ✅ Sécuriser d'abord la conversion en tableau :
const cartRows = ensureArray(cart.associations.cart_rows.cart_row);

cartRows.forEach(row => {
    const productId = txt(row.id_product);
});
```

---

## 📋 Résumé des Bonnes Pratiques
1. Ne **jamais** supposer qu'une valeur se trouve exactement sous `clé` ou sous `clé["#text"]`.
2. Utilisez **toujours** une fonction helper (`txt()`, `num()`, ou `extractXmlValue()`) pour lire la donnée de manière asynchrone et sécurisée.
3. Pour les listes (ex: `<order>`, `<cart_row>`, `<product>`), utilisez **toujours** le validateur `ensureArray()` avant de faire une boucle `.map()` ou `.forEach()`, afin d'éviter un crash critique lorsque l'API ne retourne qu'une seule ligne.
