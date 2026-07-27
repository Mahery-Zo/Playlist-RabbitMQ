# 📚 Documentation Architecture PrestaShop 8.2.6

## 🏗️ Vue d'ensemble de l'architecture

PrestaShop est une plateforme e-commerce open-source construite sur une **architecture hybride** combinant :
- **Legacy PHP** (classes historiques)
- **Symfony 4.4+** (framework moderne pour le back-office)
- **Smarty** (moteur de templates)
- **Doctrine ORM** (gestion de base de données)

---

## 📂 Arborescence complète du projet

```
prestashop/
│
├── 📁 admin107iqnyqn3npwfvr48e/          # Back-office (nom aléatoire pour sécurité)
│   ├── themes/                           # Thèmes d'administration
│   ├── filemanager/                      # Gestionnaire de fichiers
│   ├── autoupgrade/                      # Module de mise à jour automatique
│   ├── index.php                         # Point d'entrée admin
│   └── init.php                          # Initialisation admin
│
├── 📁 app/                               # Configuration Symfony
│   ├── config/                           # Fichiers de configuration
│   │   ├── config.yml                    # Configuration principale
│   │   ├── parameters.yml                # Paramètres (BDD, secrets)
│   │   ├── routing.yml                   # Routes Symfony
│   │   ├── security_*.yml                # Configuration sécurité
│   │   └── services.yml                  # Services Symfony
│   ├── Resources/                        # Ressources globales
│   │   ├── all_languages.json            # Liste des langues
│   │   └── geoip/                        # Données géolocalisation
│   ├── AppKernel.php                     # Noyau Symfony
│   └── AppCache.php                      # Gestion du cache HTTP
│
├── 📁 bin/                               # Exécutables
│   └── console                           # Console Symfony
│
├── 📁 cache/                             # Cache applicatif
│   ├── smarty/                           # Cache templates Smarty
│   │   ├── cache/                        # Cache compilé
│   │   └── compile/                      # Templates compilés
│   ├── cachefs/                          # Cache système de fichiers
│   └── tcpdf/                            # Cache PDF
│
├── 📁 classes/                           # Classes Legacy (cœur métier)
│   ├── ObjectModel.php                   # Classe mère de tous les modèles
│   ├── Context.php                       # Contexte global (session, langue, etc.)
│   ├── Tools.php                         # Fonctions utilitaires
│   ├── Validate.php                      # Validation des données
│   ├── Db.php                            # Couche d'abstraction BDD
│   ├── controller/                       # Contrôleurs de base
│   │   ├── Controller.php                # Contrôleur abstrait
│   │   ├── FrontController.php           # Contrôleur front-office
│   │   ├── AdminController.php           # Contrôleur back-office
│   │   └── ModuleAdminController.php     # Contrôleur modules admin
│   ├── cache/                            # Systèmes de cache
│   ├── db/                               # Adaptateurs BDD (MySQL, etc.)
│   ├── module/                           # Gestion des modules
│   ├── order/                            # Gestion des commandes
│   ├── product/                          # Gestion des produits
│   └── [100+ fichiers métier]            # Product, Cart, Customer, etc.
│
├── 📁 config/                            # Configuration Legacy
│   ├── config.inc.php                    # Point d'entrée configuration
│   ├── defines.inc.php                   # Constantes globales
│   ├── bootstrap.php                     # Bootstrap application
│   ├── autoload.php                      # Autoloader classes
│   └── smarty*.config.inc.php            # Configuration Smarty
│
├── 📁 controllers/                       # Contrôleurs applicatifs
│   ├── admin/                            # Contrôleurs back-office
│   │   └── Admin*Controller.php          # Un par entité (Product, Order, etc.)
│   └── front/                            # Contrôleurs front-office
│       └── *Controller.php               # Product, Cart, Order, etc.
│
├── 📁 src/                               # Code Symfony moderne
│   ├── Core/                             # Logique métier moderne
│   │   ├── Domain/                       # Domain-Driven Design
│   │   ├── Grid/                         # Grilles de données
│   │   └── Form/                         # Formulaires Symfony
│   ├── Adapter/                          # Adaptateurs Legacy ↔ Symfony
│   └── PrestaShopBundle/                 # Bundle Symfony principal
│       ├── Controller/                   # Contrôleurs Symfony
│       ├── Form/                         # Types de formulaires
│       ├── Resources/                    # Vues Twig
│       └── Service/                      # Services métier
│
├── 📁 modules/                           # Modules (extensions)
│   ├── ps_*/                             # Modules officiels PrestaShop
│   └── [custom]/                         # Modules tiers/personnalisés
│
├── 📁 themes/                            # Thèmes front-office
│   ├── classic/                          # Thème par défaut
│   │   ├── templates/                    # Templates Smarty
│   │   ├── assets/                       # CSS, JS, images
│   │   └── config/                       # Configuration thème
│   └── _libraries/                       # Bibliothèques partagées
│
├── 📁 var/                               # Données variables
│   ├── cache/                            # Cache Symfony
│   ├── logs/                             # Logs applicatifs
│   └── sessions/                         # Sessions utilisateurs
│
├── 📁 vendor/                            # Dépendances Composer
│   ├── symfony/                          # Framework Symfony
│   ├── doctrine/                         # ORM Doctrine
│   ├── twig/                             # Moteur de templates
│   └── [autres dépendances]
│
├── 📁 translations/                      # Fichiers de traduction
│   ├── fr-FR/                            # Traductions françaises
│   └── [autres langues]
│
├── 📁 img/                               # Images statiques
├── 📁 js/                                # JavaScript global
├── 📁 css/                               # CSS global
├── 📁 upload/                            # Fichiers uploadés
├── 📁 download/                          # Produits téléchargeables
│
├── index.php                             # Point d'entrée front-office
├── autoload.php                          # Autoloader Composer
├── composer.json                         # Dépendances PHP
└── .htaccess                             # Configuration Apache
```

---

## 🧱 Concepts clés et code de référence

### 1. **ObjectModel** - Classe mère de tous les modèles

Le pattern **Active Record** est au cœur de PrestaShop. Chaque entité métier hérite de `ObjectModel`.

```php
<?php
/**
 * Exemple de modèle Product simplifié
 * Hérite de ObjectModel pour bénéficier de la persistance automatique
 */
class Product extends ObjectModel
{
    /** @var int ID du produit */
    public $id;
    
    /** @var string Nom du produit */
    public $name;
    
    /** @var float Prix du produit */
    public $price;
    
    /** @var bool Produit actif ou non */
    public $active;
    
    /** @var string Date de création */
    public $date_add;
    
    /**
     * Définition de la structure de la table
     * C'est ici que PrestaShop mappe les propriétés PHP aux colonnes SQL
     */
    public static $definition = [
        'table' => 'product',              // Nom de la table (préfixe ps_ ajouté auto)
        'primary' => 'id_product',         // Clé primaire
        'multilang' => true,               // Support multilingue (nom, description)
        'multishop' => true,               // Support multi-boutique
        'fields' => [
            // Champs simples
            'price' => [
                'type' => self::TYPE_FLOAT,     // Type de données
                'validate' => 'isPrice',        // Méthode de validation
                'required' => true,             // Champ obligatoire
            ],
            'active' => [
                'type' => self::TYPE_BOOL,
                'validate' => 'isBool',
                'default' => '0',
            ],
            'date_add' => [
                'type' => self::TYPE_DATE,
                'validate' => 'isDate',
            ],
            // Champs multilingues (un par langue)
            'name' => [
                'type' => self::TYPE_STRING,
                'lang' => true,                 // Champ traduit
                'validate' => 'isCatalogName',
                'required' => true,
                'size' => 128,
            ],
            'description' => [
                'type' => self::TYPE_HTML,
                'lang' => true,
                'validate' => 'isCleanHtml',
            ],
        ],
    ];
    
    /**
     * Constructeur - charge l'objet depuis la BDD
     * 
     * @param int|null $id ID du produit à charger
     * @param int|null $id_lang ID de la langue
     * @param int|null $id_shop ID de la boutique
     */
    public function __construct($id = null, $id_lang = null, $id_shop = null)
    {
        // Appel du constructeur parent qui charge automatiquement les données
        parent::__construct($id, $id_lang, $id_shop);
    }
    
    /**
     * Sauvegarde le produit en base de données
     * Gère automatiquement INSERT ou UPDATE selon l'existence de l'ID
     * 
     * @param bool $null_values Autoriser les valeurs NULL
     * @param bool $autodate Mettre à jour date_upd automatiquement
     * @return bool Succès de l'opération
     */
    public function save($null_values = false, $autodate = true)
    {
        // Validation automatique avant sauvegarde
        if (!$this->validateFields()) {
            return false;
        }
        
        // Appel de la méthode parent qui gère INSERT/UPDATE
        return parent::save($null_values, $autodate);
    }
    
    /**
     * Supprime le produit de la base de données
     * Gère automatiquement les dépendances (images, stocks, etc.)
     * 
     * @return bool Succès de la suppression
     */
    public function delete()
    {
        // Suppression des images associées
        $this->deleteImages();
        
        // Suppression des stocks
        StockAvailable::removeProductFromStockAvailable($this->id);
        
        // Appel de la méthode parent pour suppression BDD
        return parent::delete();
    }
    
    /**
     * Méthode métier : calcul du prix final avec taxes et réductions
     * 
     * @param bool $with_tax Inclure les taxes
     * @param int|null $id_customer ID du client (pour réductions personnalisées)
     * @return float Prix final calculé
     */
    public function getPrice($with_tax = true, $id_customer = null)
    {
        $price = $this->price;
        
        // Application des réductions spécifiques
        $specific_price = SpecificPrice::getSpecificPrice(
            $this->id,
            Context::getContext()->shop->id,
            Context::getContext()->currency->id,
            Context::getContext()->country->id,
            0,
            1,
            $id_customer
        );
        
        if ($specific_price) {
            // Réduction en pourcentage
            if ($specific_price['reduction_type'] == 'percentage') {
                $price *= (1 - $specific_price['reduction']);
            }
            // Réduction en montant fixe
            else {
                $price -= $specific_price['reduction'];
            }
        }
        
        // Application des taxes si demandé
        if ($with_tax) {
            $tax_rate = Tax::getProductTaxRate($this->id);
            $price *= (1 + ($tax_rate / 100));
        }
        
        return Tools::ps_round($price, 2);
    }
}

/**
 * UTILISATION PRATIQUE
 */

// Création d'un nouveau produit
$product = new Product();
$product->name = [
    1 => 'T-Shirt Rouge',  // Français (id_lang = 1)
    2 => 'Red T-Shirt',    // Anglais (id_lang = 2)
];
$product->price = 19.99;
$product->active = true;
$product->save();  // INSERT en base

// Chargement d'un produit existant
$product = new Product(42);  // Charge le produit ID 42
echo $product->name;         // Affiche le nom dans la langue du contexte
echo $product->getPrice(true);  // Prix TTC

// Modification
$product->price = 24.99;
$product->save();  // UPDATE en base

// Suppression
$product->delete();  // DELETE en base + nettoyage dépendances
```

**Utilité profonde** :
- **Abstraction BDD** : Pas besoin d'écrire de SQL, tout est géré automatiquement
- **Validation automatique** : Les données sont validées avant sauvegarde
- **Multilingue natif** : Support transparent de plusieurs langues
- **Multi-boutique** : Gestion de plusieurs boutiques avec une seule base de code
- **Hooks intégrés** : Déclenchement automatique d'événements (actionObjectProductAddAfter, etc.)

---

### 2. **Context** - Contexte global de l'application

Le `Context` est un **singleton** qui centralise toutes les informations de la requête courante.

```php
<?php
/**
 * Classe Context - Singleton contenant l'état global de l'application
 * Accessible partout via Context::getContext()
 */
class Context
{
    /** @var Context Instance unique (pattern Singleton) */
    protected static $instance;
    
    /** @var Cart Panier du client */
    public $cart;
    
    /** @var Customer Client connecté */
    public $customer;
    
    /** @var Cookie Cookies de session */
    public $cookie;
    
    /** @var Language Langue courante */
    public $language;
    
    /** @var Currency Devise courante */
    public $currency;
    
    /** @var Country Pays du client */
    public $country;
    
    /** @var Shop Boutique courante (multi-shop) */
    public $shop;
    
    /** @var Employee Employé connecté (back-office) */
    public $employee;
    
    /** @var Controller Contrôleur en cours d'exécution */
    public $controller;
    
    /** @var Link Générateur de liens */
    public $link;
    
    /** @var Smarty Moteur de templates */
    public $smarty;
    
    /** @var ContainerInterface Container Symfony */
    public $container;
    
    /**
     * Récupère l'instance unique du Context (Singleton)
     * 
     * @return Context Instance unique
     */
    public static function getContext()
    {
        if (!self::$instance) {
            self::$instance = new Context();
        }
        
        return self::$instance;
    }
    
    /**
     * Clone le contexte (utile pour les tests ou traitements parallèles)
     * 
     * @return Context Nouveau contexte cloné
     */
    public function cloneContext()
    {
        return clone $this;
    }
}

/**
 * EXEMPLES D'UTILISATION PRATIQUE
 */

// Récupération du contexte global
$context = Context::getContext();

// Accès aux informations du client
if ($context->customer->isLogged()) {
    echo "Bonjour " . $context->customer->firstname;
    echo "Email : " . $context->customer->email;
}

// Accès au panier
$cart = $context->cart;
$total = $cart->getOrderTotal(true, Cart::BOTH);  // Total TTC
echo "Votre panier : " . Tools::displayPrice($total, $context->currency);

// Accès à la langue courante
$id_lang = $context->language->id;
$iso_code = $context->language->iso_code;  // 'fr', 'en', etc.

// Génération de liens
$link = $context->link;
$product_url = $link->getProductLink(42);  // URL du produit ID 42
$category_url = $link->getCategoryLink(5);  // URL de la catégorie ID 5

// Accès à la boutique (multi-shop)
$shop_name = $context->shop->name;
$shop_url = $context->shop->getBaseURL();

// Vérification des droits (back-office)
if ($context->employee && $context->employee->isSuperAdmin()) {
    // Actions réservées aux super-admins
}

/**
 * Exemple complet : Affichage d'un message personnalisé
 */
function displayWelcomeMessage()
{
    $context = Context::getContext();
    
    // Construction du message selon le contexte
    $message = '';
    
    if ($context->customer->isLogged()) {
        // Client connecté
        $message = sprintf(
            'Bonjour %s, bienvenue sur %s !',
            $context->customer->firstname,
            $context->shop->name
        );
        
        // Ajout du nombre d'articles dans le panier
        $nb_products = $context->cart->nbProducts();
        if ($nb_products > 0) {
            $message .= sprintf(
                ' Vous avez %d article(s) dans votre panier.',
                $nb_products
            );
        }
    } else {
        // Visiteur non connecté
        $message = sprintf(
            'Bienvenue sur %s ! Connectez-vous pour profiter de vos avantages.',
            $context->shop->name
        );
    }
    
    return $message;
}
```

**Utilité profonde** :
- **Accès centralisé** : Toutes les infos importantes en un seul endroit
- **Évite les paramètres multiples** : Plus besoin de passer langue, devise, etc. partout
- **Thread-safe** : Chaque requête a son propre contexte
- **Testabilité** : Possibilité de mocker le contexte pour les tests

---

### 3. **Controller** - Architecture MVC

PrestaShop utilise le pattern **MVC** avec deux types de contrôleurs.

```php
<?php
/**
 * Contrôleur Front-Office - Exemple : Page produit
 * Hérite de FrontController pour bénéficier des fonctionnalités front
 */
class ProductController extends FrontController
{
    /** @var string Template Smarty à utiliser */
    public $php_self = 'product';
    
    /** @var Product Produit chargé */
    protected $product;
    
    /**
     * Initialisation du contrôleur
     * Appelé automatiquement avant toute action
     */
    public function init()
    {
        // Appel de l'initialisation parente
        parent::init();
        
        // Chargement du produit depuis l'URL (?id_product=42)
        $id_product = (int)Tools::getValue('id_product');
        
        if (!$id_product) {
            // Produit non trouvé, redirection 404
            Tools::redirect('index.php?controller=404');
        }
        
        // Chargement du produit avec la langue du contexte
        $this->product = new Product(
            $id_product,
            true,  // Charger les données multilingues
            $this->context->language->id
        );
        
        // Vérification que le produit existe et est actif
        if (!Validate::isLoadedObject($this->product) || !$this->product->active) {
            Tools::redirect('index.php?controller=404');
        }
    }
    
    /**
     * Définit le fil d'Ariane (breadcrumb)
     */
    protected function getBreadcrumbLinks()
    {
        $breadcrumb = parent::getBreadcrumbLinks();
        
        // Ajout des catégories parentes
        $category = new Category($this->product->id_category_default, $this->context->language->id);
        $breadcrumb['links'][] = [
            'title' => $category->name,
            'url' => $this->context->link->getCategoryLink($category),
        ];
        
        // Ajout du produit (page courante)
        $breadcrumb['links'][] = [
            'title' => $this->product->name,
            'url' => $this->context->link->getProductLink($this->product),
        ];
        
        return $breadcrumb;
    }
    
    /**
     * Assigne les variables au template Smarty
     * C'est ici que les données sont préparées pour l'affichage
     */
    public function initContent()
    {
        parent::initContent();
        
        // Préparation des données produit
        $product_data = [
            'id' => $this->product->id,
            'name' => $this->product->name,
            'description' => $this->product->description,
            'price' => $this->product->getPrice(true),  // Prix TTC
            'price_without_tax' => $this->product->getPrice(false),  // Prix HT
            'images' => $this->getProductImages(),
            'features' => $this->getProductFeatures(),
            'quantity' => $this->getProductQuantity(),
            'add_to_cart_url' => $this->context->link->getAddToCartURL($this->product->id),
        ];
        
        // Chargement des produits similaires
        $accessories = $this->product->getAccessories($this->context->language->id);
        
        // Chargement des avis clients
        $reviews = ProductComment::getByProduct($this->product->id, 1, 10);
        
        // Assignment des variables au template Smarty
        $this->context->smarty->assign([
            'product' => $product_data,
            'accessories' => $accessories,
            'reviews' => $reviews,
            'allow_oosp' => (int)Configuration::get('PS_ORDER_OUT_OF_STOCK'),
        ]);
        
        // Définition du template à utiliser
        $this->setTemplate('catalog/product.tpl');
    }
    
    /**
     * Récupère les images du produit
     * 
     * @return array Liste des images avec leurs URLs
     */
    protected function getProductImages()
    {
        $images = [];
        $product_images = $this->product->getImages($this->context->language->id);
        
        foreach ($product_images as $image) {
            $images[] = [
                'id' => $image['id_image'],
                'url_small' => $this->context->link->getImageLink(
                    $this->product->link_rewrite,
                    $image['id_image'],
                    'small_default'
                ),
                'url_large' => $this->context->link->getImageLink(
                    $this->product->link_rewrite,
                    $image['id_image'],
                    'large_default'
                ),
                'legend' => $image['legend'],
            ];
        }
        
        return $images;
    }
    
    /**
     * Traitement AJAX : Ajout au panier
     * Appelé quand le client clique sur "Ajouter au panier"
     */
    public function displayAjaxAdd()
    {
        // Récupération des paramètres
        $id_product = (int)Tools::getValue('id_product');
        $quantity = (int)Tools::getValue('qty', 1);
        $id_product_attribute = (int)Tools::getValue('id_product_attribute');
        
        // Validation
        if (!$id_product) {
            $this->ajaxDie(json_encode([
                'hasError' => true,
                'errors' => ['Produit invalide'],
            ]));
        }
        
        // Ajout au panier
        $cart = $this->context->cart;
        $result = $cart->updateQty(
            $quantity,
            $id_product,
            $id_product_attribute,
            false,  // Pas de combinaison personnalisée
            'up'    // Augmenter la quantité
        );
        
        if ($result < 0) {
            // Erreur (stock insuffisant, etc.)
            $this->ajaxDie(json_encode([
                'hasError' => true,
                'errors' => ['Stock insuffisant'],
            ]));
        }
        
        // Succès - Retour des infos du panier
        $this->ajaxDie(json_encode([
            'hasError' => false,
            'cart' => [
                'products' => $cart->getProducts(),
                'nbProducts' => $cart->nbProducts(),
                'total' => Tools::displayPrice(
                    $cart->getOrderTotal(true, Cart::BOTH),
                    $this->context->currency
                ),
            ],
        ]));
    }
}

/**
 * Contrôleur Back-Office - Exemple : Gestion des produits
 * Hérite de AdminController pour les fonctionnalités d'administration
 */
class AdminProductsController extends AdminController
{
    /**
     * Constructeur - Configuration du contrôleur
     */
    public function __construct()
    {
        // Définition de la table et du modèle
        $this->bootstrap = true;
        $this->table = 'product';
        $this->className = 'Product';
        $this->lang = true;  // Support multilingue
        
        // Appel du constructeur parent
        parent::__construct();
        
        // Configuration de la liste des produits
        $this->fields_list = [
            'id_product' => [
                'title' => 'ID',
                'align' => 'center',
                'class' => 'fixed-width-xs',
            ],
            'image' => [
                'title' => 'Image',
                'image' => 'p',
                'orderby' => false,
                'search' => false,
            ],
            'name' => [
                'title' => 'Nom',
                'filter_key' => 'b!name',
            ],
            'reference' => [
                'title' => 'Référence',
            ],
            'price' => [
                'title' => 'Prix',
                'type' => 'price',
                'align' => 'right',
            ],
            'active' => [
                'title' => 'Actif',
                'active' => 'status',
                'type' => 'bool',
                'align' => 'center',
            ],
        ];
        
        // Actions en masse
        $this->bulk_actions = [
            'delete' => [
                'text' => 'Supprimer',
                'icon' => 'icon-trash',
                'confirm' => 'Supprimer les produits sélectionnés ?',
            ],
            'enableSelection' => [
                'text' => 'Activer',
            ],
            'disableSelection' => [
                'text' => 'Désactiver',
            ],
        ];
    }
    
    /**
     * Rendu du formulaire d'édition/création
     */
    public function renderForm()
    {
        // Configuration des champs du formulaire
        $this->fields_form = [
            'legend' => [
                'title' => 'Produit',
                'icon' => 'icon-shopping-cart',
            ],
            'input' => [
                [
                    'type' => 'text',
                    'label' => 'Nom',
                    'name' => 'name',
                    'lang' => true,  // Champ multilingue
                    'required' => true,
                    'hint' => 'Nom du produit visible par les clients',
                ],
                [
                    'type' => 'textarea',
                    'label' => 'Description',
                    'name' => 'description',
                    'lang' => true,
                    'autoload_rte' => true,  // Éditeur WYSIWYG
                    'rows' => 10,
                ],
                [
                    'type' => 'text',
                    'label' => 'Prix HT',
                    'name' => 'price',
                    'suffix' => '€',
                    'required' => true,
                ],
                [
                    'type' => 'switch',
                    'label' => 'Actif',
                    'name' => 'active',
                    'values' => [
                        ['id' => 'active_on', 'value' => 1, 'label' => 'Oui'],
                        ['id' => 'active_off', 'value' => 0, 'label' => 'Non'],
                    ],
                ],
            ],
            'submit' => [
                'title' => 'Enregistrer',
            ],
        ];
        
        return parent::renderForm();
    }
    
    /**
     * Traitement après sauvegarde
     */
    protected function afterUpdate($object)
    {
        // Régénération du cache
        Cache::clean('product_*');
        
        // Déclenchement d'un hook
        Hook::exec('actionProductUpdate', ['id_product' => $object->id]);
        
        return true;
    }
}
```

**Utilité profonde** :
- **Séparation des responsabilités** : Logique métier séparée de la présentation
- **Réutilisabilité** : Méthodes communes dans les classes parentes
- **Extensibilité** : Facile d'ajouter de nouvelles pages
- **AJAX natif** : Support intégré des requêtes asynchrones

---

### 4. **Module System** - Architecture modulaire

Les modules sont le système d'extension de PrestaShop. Chaque module peut ajouter des fonctionnalités.

```php
<?php
/**
 * Module personnalisé - Exemple : Système de fidélité
 * Tous les modules héritent de la classe Module
 */
class MyLoyaltyModule extends Module
{
    /**
     * Constructeur - Définition des métadonnées du module
     */
    public function __construct()
    {
        // Informations du module
        $this->name = 'myloyalty';  // Nom technique (doit correspondre au dossier)
        $this->tab = 'front_office_features';  // Catégorie dans le back-office
        $this->version = '1.0.0';
        $this->author = 'Mon Entreprise';
        $this->need_instance = 0;  // Pas besoin d'instancier pour la config
        $this->ps_versions_compliancy = [
            'min' => '8.0.0',
            'max' => _PS_VERSION_,
        ];
        $this->bootstrap = true;  // Utiliser Bootstrap dans le BO
        
        parent::__construct();
        
        // Textes affichés dans le back-office
        $this->displayName = $this->l('Programme de fidélité');
        $this->description = $this->l('Récompensez vos clients fidèles avec des points');
        
        $this->confirmUninstall = $this->l('Êtes-vous sûr de vouloir désinstaller ce module ?');
    }
    
    /**
     * Installation du module
     * Création des tables, enregistrement des hooks, etc.
     * 
     * @return bool Succès de l'installation
     */
    public function install()
    {
        // Vérification de la compatibilité
        if (!parent::install()) {
            return false;
        }
        
        // Création de la table pour stocker les points
        if (!$this->createTables()) {
            return false;
        }
        
        // Enregistrement des hooks (points d'accroche)
        if (!$this->registerHook('actionValidateOrder') ||  // Après validation commande
            !$this->registerHook('displayCustomerAccount') ||  // Page compte client
            !$this->registerHook('displayHeader')) {  // Header (pour CSS/JS)
            return false;
        }
        
        // Création des valeurs de configuration par défaut
        Configuration::updateValue('MYLOYALTY_POINTS_PER_EURO', 10);  // 10 points par euro
        Configuration::updateValue('MYLOYALTY_POINTS_FOR_REGISTRATION', 100);  // Bonus inscription
        
        return true;
    }
    
    /**
     * Désinstallation du module
     * Suppression des tables, désinscription des hooks, etc.
     * 
     * @return bool Succès de la désinstallation
     */
    public function uninstall()
    {
        // Suppression des tables
        if (!$this->deleteTables()) {
            return false;
        }
        
        // Suppression de la configuration
        Configuration::deleteByName('MYLOYALTY_POINTS_PER_EURO');
        Configuration::deleteByName('MYLOYALTY_POINTS_FOR_REGISTRATION');
        
        // Désinstallation parente (désinscrit automatiquement les hooks)
        return parent::uninstall();
    }
    
    /**
     * Création des tables en base de données
     * 
     * @return bool Succès de la création
     */
    protected function createTables()
    {
        $sql = [];
        
        // Table pour stocker les points des clients
        $sql[] = 'CREATE TABLE IF NOT EXISTS `' . _DB_PREFIX_ . 'loyalty_points` (
            `id_loyalty_points` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `id_customer` INT(11) UNSIGNED NOT NULL,
            `points` INT(11) NOT NULL DEFAULT 0,
            `date_add` DATETIME NOT NULL,
            `date_upd` DATETIME NOT NULL,
            PRIMARY KEY (`id_loyalty_points`),
            KEY `id_customer` (`id_customer`)
        ) ENGINE=' . _MYSQL_ENGINE_ . ' DEFAULT CHARSET=utf8;';
        
        // Table pour l'historique des transactions de points
        $sql[] = 'CREATE TABLE IF NOT EXISTS `' . _DB_PREFIX_ . 'loyalty_history` (
            `id_loyalty_history` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
            `id_customer` INT(11) UNSIGNED NOT NULL,
            `id_order` INT(11) UNSIGNED NULL,
            `points` INT(11) NOT NULL,
            `type` ENUM("earn", "spend", "expire") NOT NULL,
            `description` VARCHAR(255) NOT NULL,
            `date_add` DATETIME NOT NULL,
            PRIMARY KEY (`id_loyalty_history`),
            KEY `id_customer` (`id_customer`)
        ) ENGINE=' . _MYSQL_ENGINE_ . ' DEFAULT CHARSET=utf8;';
        
        // Exécution des requêtes
        foreach ($sql as $query) {
            if (!Db::getInstance()->execute($query)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Suppression des tables
     * 
     * @return bool Succès de la suppression
     */
    protected function deleteTables()
    {
        $sql = [
            'DROP TABLE IF EXISTS `' . _DB_PREFIX_ . 'loyalty_points`',
            'DROP TABLE IF EXISTS `' . _DB_PREFIX_ . 'loyalty_history`',
        ];
        
        foreach ($sql as $query) {
            if (!Db::getInstance()->execute($query)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Hook : Après validation d'une commande
     * Attribution des points de fidélité au client
     * 
     * @param array $params Paramètres du hook (contient la commande)
     */
    public function hookActionValidateOrder($params)
    {
        /** @var Order $order */
        $order = $params['order'];
        $customer = new Customer($order->id_customer);
        
        // Calcul des points gagnés (10 points par euro dépensé)
        $points_per_euro = (int)Configuration::get('MYLOYALTY_POINTS_PER_EURO');
        $points_earned = (int)($order->total_paid * $points_per_euro);
        
        // Attribution des points
        $this->addPoints($customer->id, $points_earned, $order->id, 'Commande #' . $order->id);
        
        // Envoi d'un email de notification
        $this->sendPointsNotificationEmail($customer, $points_earned);
    }
    
    /**
     * Hook : Affichage dans le compte client
     * Affiche le solde de points du client
     * 
     * @param array $params Paramètres du hook
     * @return string HTML à afficher
     */
    public function hookDisplayCustomerAccount($params)
    {
        $customer = $this->context->customer;
        
        // Récupération du solde de points
        $points = $this->getCustomerPoints($customer->id);
        
        // Assignment des variables au template
        $this->context->smarty->assign([
            'loyalty_points' => $points,
            'loyalty_history' => $this->getCustomerHistory($customer->id, 10),
        ]);
        
        // Retour du HTML généré
        return $this->display(__FILE__, 'views/templates/hook/customer-account.tpl');
    }
    
    /**
     * Hook : Header (pour ajouter CSS/JS)
     * 
     * @param array $params Paramètres du hook
     */
    public function hookDisplayHeader($params)
    {
        // Ajout du CSS du module
        $this->context->controller->addCSS($this->_path . 'views/css/front.css');
        
        // Ajout du JS du module
        $this->context->controller->addJS($this->_path . 'views/js/front.js');
    }
    
    /**
     * Ajoute des points à un client
     * 
     * @param int $id_customer ID du client
     * @param int $points Nombre de points à ajouter
     * @param int|null $id_order ID de la commande (optionnel)
     * @param string $description Description de la transaction
     * @return bool Succès de l'opération
     */
    protected function addPoints($id_customer, $points, $id_order = null, $description = '')
    {
        // Mise à jour du solde
        $sql = 'INSERT INTO `' . _DB_PREFIX_ . 'loyalty_points`
                (`id_customer`, `points`, `date_add`, `date_upd`)
                VALUES (' . (int)$id_customer . ', ' . (int)$points . ', NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                `points` = `points` + ' . (int)$points . ',
                `date_upd` = NOW()';
        
        if (!Db::getInstance()->execute($sql)) {
            return false;
        }
        
        // Ajout dans l'historique
        $sql = 'INSERT INTO `' . _DB_PREFIX_ . 'loyalty_history`
                (`id_customer`, `id_order`, `points`, `type`, `description`, `date_add`)
                VALUES (
                    ' . (int)$id_customer . ',
                    ' . ($id_order ? (int)$id_order : 'NULL') . ',
                    ' . (int)$points . ',
                    "earn",
                    "' . pSQL($description) . '",
                    NOW()
                )';
        
        return Db::getInstance()->execute($sql);
    }
    
    /**
     * Récupère le solde de points d'un client
     * 
     * @param int $id_customer ID du client
     * @return int Nombre de points
     */
    protected function getCustomerPoints($id_customer)
    {
        $sql = 'SELECT `points`
                FROM `' . _DB_PREFIX_ . 'loyalty_points`
                WHERE `id_customer` = ' . (int)$id_customer;
        
        $result = Db::getInstance()->getValue($sql);
        
        return $result ? (int)$result : 0;
    }
    
    /**
     * Page de configuration du module dans le back-office
     * 
     * @return string HTML du formulaire de configuration
     */
    public function getContent()
    {
        $output = '';
        
        // Traitement du formulaire
        if (Tools::isSubmit('submitLoyaltyConfig')) {
            $points_per_euro = (int)Tools::getValue('MYLOYALTY_POINTS_PER_EURO');
            $points_registration = (int)Tools::getValue('MYLOYALTY_POINTS_FOR_REGISTRATION');
            
            // Validation
            if ($points_per_euro < 1 || $points_registration < 0) {
                $output .= $this->displayError($this->l('Valeurs invalides'));
            } else {
                // Sauvegarde
                Configuration::updateValue('MYLOYALTY_POINTS_PER_EURO', $points_per_euro);
                Configuration::updateValue('MYLOYALTY_POINTS_FOR_REGISTRATION', $points_registration);
                
                $output .= $this->displayConfirmation($this->l('Configuration enregistrée'));
            }
        }
        
        // Affichage du formulaire
        return $output . $this->renderConfigForm();
    }
    
    /**
     * Génère le formulaire de configuration
     * 
     * @return string HTML du formulaire
     */
    protected function renderConfigForm()
    {
        $helper = new HelperForm();
        
        // Configuration du helper
        $helper->module = $this;
        $helper->name_controller = $this->name;
        $helper->token = Tools::getAdminTokenLite('AdminModules');
        $helper->currentIndex = AdminController::$currentIndex . '&configure=' . $this->name;
        $helper->submit_action = 'submitLoyaltyConfig';
        
        // Champs du formulaire
        $fields_form = [
            'form' => [
                'legend' => [
                    'title' => $this->l('Configuration'),
                    'icon' => 'icon-cogs',
                ],
                'input' => [
                    [
                        'type' => 'text',
                        'label' => $this->l('Points par euro dépensé'),
                        'name' => 'MYLOYALTY_POINTS_PER_EURO',
                        'required' => true,
                        'class' => 'fixed-width-xs',
                    ],
                    [
                        'type' => 'text',
                        'label' => $this->l('Points bonus à l\'inscription'),
                        'name' => 'MYLOYALTY_POINTS_FOR_REGISTRATION',
                        'required' => true,
                        'class' => 'fixed-width-xs',
                    ],
                ],
                'submit' => [
                    'title' => $this->l('Enregistrer'),
                ],
            ],
        ];
        
        // Valeurs par défaut
        $helper->fields_value['MYLOYALTY_POINTS_PER_EURO'] = Configuration::get('MYLOYALTY_POINTS_PER_EURO');
        $helper->fields_value['MYLOYALTY_POINTS_FOR_REGISTRATION'] = Configuration::get('MYLOYALTY_POINTS_FOR_REGISTRATION');
        
        return $helper->generateForm([$fields_form]);
    }
}
```

**Utilité profonde** :
- **Extensibilité sans modification du core** : Ajout de fonctionnalités sans toucher au code source
- **Système de hooks** : Points d'accroche pour intercepter les événements
- **Isolation** : Chaque module est indépendant
- **Marketplace** : Écosystème de modules tiers

---

### 5. **Symfony Integration** - Architecture moderne

PrestaShop 8 intègre Symfony pour moderniser le back-office.

```php
<?php
/**
 * Contrôleur Symfony moderne - Exemple : API REST pour les produits
 * Utilise les annotations et l'injection de dépendances
 */

namespace PrestaShopBundle\Controller\Admin\Sell\Catalog;

use PrestaShop\PrestaShop\Core\Domain\Product\Command\AddProductCommand;
use PrestaShop\PrestaShop\Core\Domain\Product\Query\GetProductForEditing;
use PrestaShop\PrestaShop\Core\Domain\Product\QueryResult\ProductForEditing;
use PrestaShopBundle\Controller\Admin\FrameworkBundleAdminController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

/**
 * Contrôleur moderne pour la gestion des produits
 * Utilise CQRS (Command Query Responsibility Segregation)
 * 
 * @Route("/api/products", name="api_products_")
 */
class ProductApiController extends FrameworkBundleAdminController
{
    /**
     * Liste des produits avec pagination et filtres
     * 
     * @Route("/", name="list", methods={"GET"})
     * 
     * @param Request $request Requête HTTP
     * @return JsonResponse Réponse JSON
     */
    public function listAction(Request $request): JsonResponse
    {
        // Récupération des paramètres de requête
        $page = $request->query->getInt('page', 1);
        $limit = $request->query->getInt('limit', 20);
        $filters = $request->query->get('filters', []);
        
        // Utilisation du Query Bus (CQRS)
        $query = new GetProductsList($page, $limit, $filters);
        
        /** @var ProductsList $result */
        $result = $this->getQueryBus()->handle($query);
        
        // Transformation en format API
        $products = array_map(function ($product) {
            return [
                'id' => $product->getId(),
                'name' => $product->getName(),
                'price' => $product->getPrice(),
                'active' => $product->isActive(),
                'stock' => $product->getStock(),
            ];
        }, $result->getProducts());
        
        return $this->json([
            'data' => $products,
            'total' => $result->getTotal(),
            'page' => $page,
            'pages' => ceil($result->getTotal() / $limit),
        ]);
    }
    
    /**
     * Récupère un produit par son ID
     * 
     * @Route("/{productId}", name="get", methods={"GET"}, requirements={"productId"="\d+"})
     * 
     * @param int $productId ID du produit
     * @return JsonResponse Réponse JSON
     */
    public function getAction(int $productId): JsonResponse
    {
        try {
            // Query pour récupérer le produit
            $query = new GetProductForEditing($productId);
            
            /** @var ProductForEditing $product */
            $product = $this->getQueryBus()->handle($query);
            
            // Transformation en format API
            return $this->json([
                'id' => $product->getProductId(),
                'name' => $product->getBasicInformation()->getName(),
                'description' => $product->getBasicInformation()->getDescription(),
                'price' => $product->getPrices()->getPrice(),
                'active' => $product->isActive(),
                'categories' => $product->getCategoriesInformation()->getCategoryIds(),
            ]);
            
        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Product not found',
                'message' => $e->getMessage(),
            ], Response::HTTP_NOT_FOUND);
        }
    }
    
    /**
     * Crée un nouveau produit
     * 
     * @Route("/", name="create", methods={"POST"})
     * 
     * @param Request $request Requête HTTP avec les données JSON
     * @return JsonResponse Réponse JSON
     */
    public function createAction(Request $request): JsonResponse
    {
        // Récupération des données JSON
        $data = json_decode($request->getContent(), true);
        
        // Validation des données
        if (!isset($data['name']) || !isset($data['price'])) {
            return $this->json([
                'error' => 'Missing required fields',
                'required' => ['name', 'price'],
            ], Response::HTTP_BAD_REQUEST);
        }
        
        try {
            // Command pour créer le produit (CQRS)
            $command = new AddProductCommand(
                $data['name'],
                $data['type'] ?? 'standard'
            );
            
            // Exécution de la commande via le Command Bus
            $productId = $this->getCommandBus()->handle($command);
            
            // Retour de la réponse avec l'ID du produit créé
            return $this->json([
                'id' => $productId->getValue(),
                'message' => 'Product created successfully',
            ], Response::HTTP_CREATED);
            
        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Failed to create product',
                'message' => $e->getMessage(),
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Met à jour un produit existant
     * 
     * @Route("/{productId}", name="update", methods={"PUT", "PATCH"}, requirements={"productId"="\d+"})
     * 
     * @param int $productId ID du produit
     * @param Request $request Requête HTTP avec les données JSON
     * @return JsonResponse Réponse JSON
     */
    public function updateAction(int $productId, Request $request): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        
        try {
            // Command pour mettre à jour le produit
            $command = new UpdateProductCommand($productId);
            
            // Application des modifications
            if (isset($data['name'])) {
                $command->setName($data['name']);
            }
            if (isset($data['price'])) {
                $command->setPrice($data['price']);
            }
            if (isset($data['active'])) {
                $command->setActive($data['active']);
            }
            
            // Exécution de la commande
            $this->getCommandBus()->handle($command);
            
            return $this->json([
                'message' => 'Product updated successfully',
            ]);
            
        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Failed to update product',
                'message' => $e->getMessage(),
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Supprime un produit
     * 
     * @Route("/{productId}", name="delete", methods={"DELETE"}, requirements={"productId"="\d+"})
     * 
     * @param int $productId ID du produit
     * @return JsonResponse Réponse JSON
     */
    public function deleteAction(int $productId): JsonResponse
    {
        try {
            // Command pour supprimer le produit
            $command = new DeleteProductCommand($productId);
            
            $this->getCommandBus()->handle($command);
            
            return $this->json([
                'message' => 'Product deleted successfully',
            ]);
            
        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Failed to delete product',
                'message' => $e->getMessage(),
            ], Response::HTTP_INTERNAL_SERVER_ERROR);
        }
    }
}

/**
 * Service Symfony - Exemple : Gestionnaire de stock
 * Utilise l'injection de dépendances
 */

namespace PrestaShop\PrestaShop\Core\Stock;

use Doctrine\ORM\EntityManagerInterface;
use PrestaShop\PrestaShop\Core\Domain\Product\ValueObject\ProductId;
use Psr\Log\LoggerInterface;

/**
 * Service de gestion du stock
 * Injecté automatiquement par le container Symfony
 */
class StockManager
{
    /** @var EntityManagerInterface Gestionnaire d'entités Doctrine */
    private $entityManager;
    
    /** @var LoggerInterface Logger pour tracer les opérations */
    private $logger;
    
    /** @var StockRepository Repository pour accéder aux stocks */
    private $stockRepository;
    
    /**
     * Constructeur avec injection de dépendances
     * Les dépendances sont automatiquement injectées par Symfony
     * 
     * @param EntityManagerInterface $entityManager
     * @param LoggerInterface $logger
     * @param StockRepository $stockRepository
     */
    public function __construct(
        EntityManagerInterface $entityManager,
        LoggerInterface $logger,
        StockRepository $stockRepository
    ) {
        $this->entityManager = $entityManager;
        $this->logger = $logger;
        $this->stockRepository = $stockRepository;
    }
    
    /**
     * Ajoute du stock pour un produit
     * 
     * @param ProductId $productId ID du produit
     * @param int $quantity Quantité à ajouter
     * @param string $reason Raison de l'ajout (réapprovisionnement, retour, etc.)
     * @return void
     * 
     * @throws \Exception Si la quantité est invalide
     */
    public function addStock(ProductId $productId, int $quantity, string $reason = 'restock'): void
    {
        // Validation
        if ($quantity <= 0) {
            throw new \InvalidArgumentException('Quantity must be positive');
        }
        
        // Début de transaction
        $this->entityManager->beginTransaction();
        
        try {
            // Récupération du stock actuel
            $stock = $this->stockRepository->findByProductId($productId);
            
            if (!$stock) {
                // Création d'un nouveau stock
                $stock = new Stock($productId, $quantity);
            } else {
                // Mise à jour du stock existant
                $stock->addQuantity($quantity);
            }
            
            // Sauvegarde
            $this->entityManager->persist($stock);
            
            // Création d'une entrée dans l'historique
            $movement = new StockMovement(
                $productId,
                $quantity,
                StockMovement::TYPE_ADD,
                $reason
            );
            $this->entityManager->persist($movement);
            
            // Commit de la transaction
            $this->entityManager->flush();
            $this->entityManager->commit();
            
            // Log de l'opération
            $this->logger->info('Stock added', [
                'product_id' => $productId->getValue(),
                'quantity' => $quantity,
                'reason' => $reason,
            ]);
            
        } catch (\Exception $e) {
            // Rollback en cas d'erreur
            $this->entityManager->rollback();
            
            $this->logger->error('Failed to add stock', [
                'product_id' => $productId->getValue(),
                'error' => $e->getMessage(),
            ]);
            
            throw $e;
        }
    }
    
    /**
     * Retire du stock pour un produit
     * 
     * @param ProductId $productId ID du produit
     * @param int $quantity Quantité à retirer
     * @param string $reason Raison du retrait (vente, casse, etc.)
     * @return void
     * 
     * @throws \Exception Si stock insuffisant
     */
    public function removeStock(ProductId $productId, int $quantity, string $reason = 'sale'): void
    {
        if ($quantity <= 0) {
            throw new \InvalidArgumentException('Quantity must be positive');
        }
        
        $this->entityManager->beginTransaction();
        
        try {
            $stock = $this->stockRepository->findByProductId($productId);
            
            if (!$stock) {
                throw new \Exception('Product not found in stock');
            }
            
            // Vérification du stock disponible
            if ($stock->getQuantity() < $quantity) {
                throw new \Exception('Insufficient stock');
            }
            
            // Retrait du stock
            $stock->removeQuantity($quantity);
            $this->entityManager->persist($stock);
            
            // Historique
            $movement = new StockMovement(
                $productId,
                -$quantity,
                StockMovement::TYPE_REMOVE,
                $reason
            );
            $this->entityManager->persist($movement);
            
            $this->entityManager->flush();
            $this->entityManager->commit();
            
            $this->logger->info('Stock removed', [
                'product_id' => $productId->getValue(),
                'quantity' => $quantity,
                'reason' => $reason,
            ]);
            
        } catch (\Exception $e) {
            $this->entityManager->rollback();
            
            $this->logger->error('Failed to remove stock', [
                'product_id' => $productId->getValue(),
                'error' => $e->getMessage(),
            ]);
            
            throw $e;
        }
    }
    
    /**
     * Récupère le stock disponible pour un produit
     * 
     * @param ProductId $productId ID du produit
     * @return int Quantité disponible
     */
    public function getAvailableStock(ProductId $productId): int
    {
        $stock = $this->stockRepository->findByProductId($productId);
        
        return $stock ? $stock->getQuantity() : 0;
    }
}
```

**Configuration du service dans `config/services.yml`** :

```yaml
services:
    # Configuration par défaut
    _defaults:
        autowire: true      # Injection automatique des dépendances
        autoconfigure: true # Configuration automatique des tags
        public: false       # Services privés par défaut

    # Enregistrement du service StockManager
    PrestaShop\PrestaShop\Core\Stock\StockManager:
        arguments:
            $entityManager: '@doctrine.orm.entity_manager'
            $logger: '@logger'
            $stockRepository: '@PrestaShop\PrestaShop\Core\Stock\StockRepository'
        tags:
            - { name: 'monolog.logger', channel: 'stock' }
```

**Utilité profonde** :
- **Architecture moderne** : CQRS, DDD, injection de dépendances
- **Testabilité** : Services facilement mockables
- **Maintenabilité** : Code découplé et réutilisable
- **Performance** : Cache HTTP, lazy loading, etc.

---

## 🔄 Flux de données étape par étape

### Flux 1 : Requête Front-Office (Affichage d'un produit)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FLUX FRONT-OFFICE : PAGE PRODUIT                 │
└─────────────────────────────────────────────────────────────────────┘

1. 📥 REQUÊTE HTTP
   └─> URL : https://monsite.com/produit/t-shirt-rouge-42.html
   └─> Serveur Web (Apache/Nginx) reçoit la requête
   └─> .htaccess redirige vers index.php

2. 🚀 BOOTSTRAP APPLICATION
   └─> index.php
       ├─> config/config.inc.php (chargement configuration)
       ├─> Autoloader (classes/PrestaShopAutoload.php)
       ├─> Initialisation Context (langue, devise, client, panier)
       └─> Dispatcher (classes/Dispatcher.php)

3. 🎯 ROUTING & DISPATCH
   └─> Dispatcher analyse l'URL
       ├─> Détecte le contrôleur : "product"
       ├─> Extrait l'ID produit : 42
       └─> Instancie ProductController

4. 🎮 CONTRÔLEUR (controllers/front/ProductController.php)
   └─> init()
       ├─> Chargement du produit : new Product(42, true, $id_lang)
       │   └─> ObjectModel::__construct()
       │       ├─> Requête SQL : SELECT * FROM ps_product WHERE id_product = 42
       │       ├─> Requête SQL : SELECT * FROM ps_product_lang WHERE id_product = 42
       │       └─> Hydratation de l'objet Product
       │
       ├─> Vérification : produit actif ? existe ?
       └─> Chargement des données associées
           ├─> Images : Product::getImages()
           ├─> Catégories : Product::getCategories()
           ├─> Caractéristiques : Product::getFeatures()
           └─> Prix : Product::getPrice(true)
               ├─> Calcul des réductions (SpecificPrice)
               ├─> Application des taxes (Tax)
               └─> Conversion devise (Currency)

5. 📊 PRÉPARATION DES DONNÉES
   └─> initContent()
       ├─> Assemblage des données produit
       ├─> Chargement des produits similaires
       ├─> Chargement des avis clients
       └─> Assignment Smarty
           └─> $smarty->assign('product', $product_data)

6. 🎨 RENDU TEMPLATE (Smarty)
   └─> themes/classic/templates/catalog/product.tpl
       ├─> Inclusion du header : _partials/header.tpl
       ├─> Affichage du produit
       │   ├─> Nom, description, prix
       │   ├─> Images (carousel)
       │   ├─> Bouton "Ajouter au panier"
       │   └─> Caractéristiques
       ├─> Inclusion du footer : _partials/footer.tpl
       └─> Compilation Smarty → HTML

7. 📤 RÉPONSE HTTP
   └─> HTML généré envoyé au navigateur
   └─> Code HTTP 200 OK
   └─> Headers (Cache-Control, Content-Type, etc.)

┌─────────────────────────────────────────────────────────────────────┐
│                    TEMPS TOTAL : ~100-300ms                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Flux 2 : Requête AJAX (Ajout au panier)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FLUX AJAX : AJOUT AU PANIER                      │
└─────────────────────────────────────────────────────────────────────┘

1. 📥 REQUÊTE AJAX
   └─> JavaScript (front.js) déclenche l'événement
   └─> POST : /index.php?controller=cart&ajax=1&action=update
   └─> Données : { id_product: 42, qty: 1, add: 1 }

2. 🚀 BOOTSTRAP (identique au flux 1)
   └─> index.php → config.inc.php → Context → Dispatcher

3. 🎯 ROUTING
   └─> Dispatcher détecte :
       ├─> Contrôleur : CartController
       ├─> Mode AJAX : ajax=1
       └─> Action : update

4. 🎮 CONTRÔLEUR (controllers/front/CartController.php)
   └─> processChangeProductInCart()
       ├─> Récupération des paramètres
       │   ├─> id_product = 42
       │   ├─> qty = 1
       │   └─> add = 1 (ajouter)
       │
       ├─> Validation
       │   ├─> Produit existe ? Product::existsInDatabase(42)
       │   ├─> Produit actif ? $product->active
       │   └─> Stock disponible ? StockAvailable::getQuantityAvailableByProduct(42)
       │
       └─> Mise à jour du panier
           └─> Cart::updateQty(1, 42, null, false, 'up')
               ├─> Vérification des règles métier
               │   ├─> Quantité minimale/maximale
               │   ├─> Produit déjà dans le panier ?
               │   └─> Compatibilité avec autres produits
               │
               ├─> Mise à jour BDD
               │   ├─> INSERT/UPDATE ps_cart_product
               │   └─> UPDATE ps_cart (date_upd)
               │
               ├─> Recalcul du panier
               │   ├─> Prix total HT
               │   ├─> Prix total TTC
               │   ├─> Frais de port
               │   └─> Réductions applicables
               │
               └─> Déclenchement des hooks
                   └─> Hook::exec('actionCartSave', ['cart' => $cart])
                       └─> Modules écoutant ce hook sont notifiés

5. 📊 PRÉPARATION RÉPONSE JSON
   └─> Assemblage des données
       ├─> Succès : true
       ├─> Nombre de produits : $cart->nbProducts()
       ├─> Total panier : $cart->getOrderTotal(true, Cart::BOTH)
       ├─> Liste des produits : $cart->getProducts()
       └─> Messages : "Produit ajouté au panier"

6. 📤 RÉPONSE JSON
   └─> {
         "success": true,
         "cart": {
           "products": [...],
           "nbProducts": 3,
           "total": "89.97 €",
           "total_tax_exc": "74.98 €"
         },
         "message": "Produit ajouté au panier"
       }

7. 🎨 MISE À JOUR INTERFACE
   └─> JavaScript reçoit la réponse
       ├─> Mise à jour du compteur panier (header)
       ├─> Affichage d'une notification
       └─> Animation du bouton "Ajouter au panier"

┌─────────────────────────────────────────────────────────────────────┐
│                    TEMPS TOTAL : ~50-150ms                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Flux 3 : Requête Back-Office (Modification d'un produit)

```
┌─────────────────────────────────────────────────────────────────────┐
│              FLUX BACK-OFFICE : ÉDITION PRODUIT (Symfony)           │
└─────────────────────────────────────────────────────────────────────┘

1. 📥 REQUÊTE HTTP
   └─> URL : /admin/products/42/edit
   └─> Serveur Web → admin/index.php

2. 🚀 BOOTSTRAP SYMFONY
   └─> admin/index.php
       ├─> app/AppKernel.php (Kernel Symfony)
       ├─> Chargement des bundles
       ├─> Initialisation du container DI
       └─> Routing Symfony (app/config/routing.yml)

3. 🎯 ROUTING SYMFONY
   └─> Analyse de la route : /admin/products/{id}/edit
       ├─> Contrôleur : ProductController::editAction
       ├─> Paramètre : id = 42
       └─> Méthode : GET

4. 🎮 CONTRÔLEUR SYMFONY
   └─> PrestaShopBundle\Controller\Admin\ProductController::editAction(42)
       │
       ├─> Vérification des droits
       │   └─> Security::isGranted('ROLE_ADMIN_PRODUCTS_UPDATE')
       │
       ├─> Query Bus (CQRS)
       │   └─> GetProductForEditing(42)
       │       ├─> QueryHandler récupère les données
       │       ├─> Requêtes SQL optimisées
       │       │   ├─> SELECT * FROM ps_product WHERE id_product = 42
       │       │   ├─> SELECT * FROM ps_product_lang WHERE id_product = 42
       │       │   ├─> SELECT * FROM ps_product_shop WHERE id_product = 42
       │       │   └─> SELECT * FROM ps_image WHERE id_product = 42
       │       └─> Retourne ProductForEditing (DTO)
       │
       ├─> Création du formulaire Symfony
       │   └─> FormFactory::create(ProductType::class, $product)
       │       ├─> Champs : nom, description, prix, etc.
       │       ├─> Validation : constraints Symfony
       │       └─> Transformation des données
       │
       └─> Rendu de la vue Twig
           └─> @PrestaShop/Admin/Product/edit.html.twig

5. 🎨 RENDU TEMPLATE TWIG
   └─> templates/Admin/Product/edit.html.twig
       ├─> Extends : @PrestaShop/Admin/layout.html.twig
       ├─> Blocks : content, javascripts, stylesheets
       ├─> Affichage du formulaire
       │   ├─> {{ form_start(form) }}
       │   ├─> {{ form_row(form.name) }}
       │   ├─> {{ form_row(form.price) }}
       │   └─> {{ form_end(form) }}
       └─> Compilation Twig → HTML

6. 📤 RÉPONSE HTTP
   └─> HTML généré envoyé au navigateur
   └─> Code HTTP 200 OK

┌─────────────────────────────────────────────────────────────────────┐
│                    SOUMISSION DU FORMULAIRE                         │
└─────────────────────────────────────────────────────────────────────┘

7. 📥 REQUÊTE POST
   └─> POST : /admin/products/42/edit
   └─> Données : { name: "T-Shirt Bleu", price: 24.99, ... }

8. 🎮 TRAITEMENT DU FORMULAIRE
   └─> ProductController::editAction(42)
       │
       ├─> Récupération de la requête
       │   └─> $form->handleRequest($request)
       │
       ├─> Validation du formulaire
       │   └─> if ($form->isSubmitted() && $form->isValid())
       │       ├─> Validation des contraintes
       │       ├─> Validation métier
       │       └─> Transformation des données
       │
       ├─> Command Bus (CQRS)
       │   └─> UpdateProductCommand(42, $data)
       │       ├─> CommandHandler traite la commande
       │       ├─> Validation métier supplémentaire
       │       ├─> Mise à jour en base de données
       │       │   ├─> UPDATE ps_product SET ...
       │       │   ├─> UPDATE ps_product_lang SET ...
       │       │   └─> UPDATE ps_product_shop SET ...
       │       ├─> Invalidation du cache
       │       │   └─> Cache::clean('product_*')
       │       └─> Déclenchement des hooks
       │           └─> Hook::exec('actionProductUpdate', ['id_product' => 42])
       │
       └─> Redirection avec message de succès
           └─> return $this->redirectToRoute('admin_products_index')

9. 📤 RÉPONSE HTTP
   └─> Redirection 302 vers /admin/products
   └─> Flash message : "Produit mis à jour avec succès"

┌─────────────────────────────────────────────────────────────────────┐
│                    TEMPS TOTAL : ~200-500ms                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Flux 4 : Système de Hooks (Événements)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FLUX HOOKS : SYSTÈME D'ÉVÉNEMENTS                │
└─────────────────────────────────────────────────────────────────────┘

1. 🎯 DÉCLENCHEMENT D'UN HOOK
   └─> Dans le code : Hook::exec('actionProductUpdate', ['id_product' => 42])
       │
       ├─> Hook::exec() (classes/Hook.php)
       │   ├─> Récupération des modules enregistrés pour ce hook
       │   │   └─> SELECT * FROM ps_hook_module
       │   │       WHERE id_hook = (SELECT id_hook FROM ps_hook WHERE name = 'actionProductUpdate')
       │   │       ORDER BY position
       │   │
       │   └─> Pour chaque module enregistré :
       │       ├─> Module 1 : myloyalty
       │       ├─> Module 2 : myanalytics
       │       └─> Module 3 : mynotifications

2. 🔄 EXÉCUTION DES MODULES
   └─> Pour chaque module dans l'ordre de position :
       │
       ├─> Module : myloyalty
       │   └─> Appel : $module->hookActionProductUpdate($params)
       │       ├─> Récupération de l'ID produit : $params['id_product']
       │       ├─> Logique métier du module
       │       │   └─> Mise à jour des statistiques de fidélité
       │       └─> Retour (optionnel)
       │
       ├─> Module : myanalytics
       │   └─> Appel : $module->hookActionProductUpdate($params)
       │       ├─> Envoi d'un événement à Google Analytics
       │       ├─> Mise à jour des rapports internes
       │       └─> Retour (optionnel)
       │
       └─> Module : mynotifications
           └─> Appel : $module->hookActionProductUpdate($params)
               ├─> Envoi d'une notification aux administrateurs
               ├─> Mise à jour du flux d'activité
               └─> Retour (optionnel)

3. 📊 AGRÉGATION DES RÉSULTATS
   └─> Hook::exec() collecte les retours de tous les modules
       ├─> Certains hooks retournent du HTML (displayHeader, displayFooter)
       ├─> D'autres modifient des données (actionCartSave)
       └─> Certains sont purement informatifs (actionProductUpdate)

4. ✅ FIN DE L'EXÉCUTION
   └─> Le code appelant continue son exécution
   └─> Les modules ont pu :
       ├─> Modifier des données
       ├─> Envoyer des notifications
       ├─> Logger des événements
       └─> Déclencher d'autres actions

┌─────────────────────────────────────────────────────────────────────┐
│              HOOKS PRINCIPAUX ET LEURS UTILISATIONS                 │
└─────────────────────────────────────────────────────────────────────┘

📌 HOOKS FRONT-OFFICE :
   ├─> displayHeader : Ajout de CSS/JS dans le <head>
   ├─> displayFooter : Ajout de scripts avant </body>
   ├─> displayHome : Contenu sur la page d'accueil
   ├─> displayProductAdditionalInfo : Infos supplémentaires produit
   └─> displayShoppingCart : Contenu dans le panier

📌 HOOKS ACTIONS (événements) :
   ├─> actionValidateOrder : Après validation d'une commande
   ├─> actionProductUpdate : Après mise à jour d'un produit
   ├─> actionCustomerAccountAdd : Après création d'un compte client
   ├─> actionCartSave : Après sauvegarde du panier
   └─> actionObjectProductDeleteAfter : Après suppression d'un produit

📌 HOOKS BACK-OFFICE :
   ├─> displayBackOfficeHeader : CSS/JS dans le BO
   ├─> displayAdminProductsExtra : Onglet supplémentaire dans la fiche produit
   └─> displayAdminOrder : Contenu dans la page de commande

┌─────────────────────────────────────────────────────────────────────┐
│                    TEMPS TOTAL : Variable (0-500ms)                 │
│                    Dépend du nombre de modules actifs               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 💡 Bonnes pratiques (Do's and Don'ts)

### ✅ À FAIRE (Best Practices)

#### 🏗️ Architecture & Organisation

| Pratique | Description | Exemple |
|----------|-------------|---------|
| **Utiliser les ObjectModel** | Toujours hériter de `ObjectModel` pour les entités métier | `class MyEntity extends ObjectModel` |
| **Respecter la structure MVC** | Séparer logique métier, contrôleurs et vues | Logique dans les classes, pas dans les contrôleurs |
| **Utiliser le Context** | Accéder aux infos globales via `Context::getContext()` | `$context = Context::getContext();` |
| **Créer des modules** | Étendre PrestaShop via des modules, pas en modifiant le core | Créer `modules/mymodule/` |
| **Utiliser les hooks** | S'accrocher aux événements via le système de hooks | `$this->registerHook('actionProductUpdate')` |
| **Nommer correctement** | Suivre les conventions de nommage PrestaShop | `AdminProductsController`, `Product.php` |

#### 🔒 Sécurité

| Pratique | Description | Exemple |
|----------|-------------|---------|
| **Valider toutes les entrées** | Utiliser `Validate::` et `Tools::getValue()` | `$id = (int)Tools::getValue('id_product');` |
| **Échapper les sorties** | Utiliser `pSQL()` pour les requêtes SQL | `WHERE name = "' . pSQL($name) . '"` |
| **Vérifier les permissions** | Contrôler les droits d'accès dans le BO | `if (!$this->access('edit')) return;` |
| **Utiliser les tokens** | Protéger les formulaires avec des tokens CSRF | `Tools::getAdminTokenLite('AdminProducts')` |
| **Hasher les mots de passe** | Utiliser `password_hash()` et `password_verify()` | `password_hash($password, PASSWORD_BCRYPT)` |
| **Limiter les requêtes SQL** | Utiliser des requêtes préparées ou l'ORM | `Db::getInstance()->getValue($sql, false)` |

#### 💾 Base de données

| Pratique | Description | Exemple |
|----------|-------------|---------|
| **Utiliser les préfixes** | Toujours utiliser `_DB_PREFIX_` | `_DB_PREFIX_ . 'product'` |
| **Définir les index** | Ajouter des index sur les colonnes fréquemment recherchées | `KEY id_customer (id_customer)` |
| **Gérer les transactions** | Utiliser des transactions pour les opérations critiques | `Db::getInstance()->execute('START TRANSACTION')` |
| **Optimiser les requêtes** | Éviter les `SELECT *`, limiter les résultats | `SELECT id_product, name FROM ... LIMIT 100` |
| **Utiliser le cache** | Mettre en cache les requêtes coûteuses | `Cache::getInstance()->get('products_list')` |

#### 🎨 Templates & Front-end

| Pratique | Description | Exemple |
|----------|-------------|---------|
| **Utiliser Smarty** | Respecter la syntaxe Smarty pour les templates | `{$product.name}`, `{foreach}`, `{if}` |
| **Échapper les variables** | Utiliser les modificateurs Smarty | `{$product.name|escape:'html':'UTF-8'}` |
| **Séparer CSS/JS** | Externaliser CSS et JS dans des fichiers séparés | `$this->addCSS('module.css')` |
| **Responsive design** | Utiliser Bootstrap et les media queries | `col-xs-12 col-md-6` |
| **Optimiser les assets** | Minifier et combiner CSS/JS en production | Configuration dans le BO |

#### 🧪 Tests & Qualité

| Pratique | Description | Exemple |
|----------|-------------|---------|
| **Écrire des tests** | Créer des tests unitaires et fonctionnels | PHPUnit, Behat |
| **Documenter le code** | Utiliser PHPDoc pour documenter les méthodes | `/** @param int $id */` |
| **Respecter PSR** | Suivre les standards PSR-1, PSR-2, PSR-4 | Indentation, nommage, autoloading |
| **Utiliser les logs** | Logger les erreurs et événements importants | `PrestaShopLogger::addLog($message)` |
| **Versionner le code** | Utiliser Git avec des commits clairs | `git commit -m "Fix: product price calculation"` |

#### ⚡ Performance

| Pratique | Description | Exemple |
|----------|-------------|---------|
| **Activer le cache** | Utiliser le cache Smarty et le cache de classes | Configuration BO > Performance |
| **Lazy loading** | Charger les données uniquement quand nécessaire | Ne pas charger toutes les images d'un coup |
| **Pagination** | Paginer les listes longues | `LIMIT $start, $limit` |
| **Optimiser les images** | Compresser et redimensionner les images | WebP, lazy loading |
| **CDN pour les assets** | Utiliser un CDN pour CSS/JS/images | Configuration dans `defines.inc.php` |

---

### ❌ À ÉVITER (Bad Practices)

#### 🚫 Architecture & Organisation

| Erreur | Pourquoi c'est mal | Alternative |
|--------|-------------------|-------------|
| **Modifier le core** | Perte des modifications lors des mises à jour | Utiliser les modules et l'override |
| **Requêtes SQL directes** | Contourne la logique métier et les validations | Utiliser ObjectModel ou Query Builder |
| **Variables globales** | Pollution de l'espace global, conflits | Utiliser le Context ou l'injection de dépendances |
| **Code dans les templates** | Mélange logique et présentation | Préparer les données dans le contrôleur |
| **Dupliquer du code** | Maintenance difficile, bugs multiples | Créer des méthodes réutilisables |
| **Ignorer les hooks** | Modules incompatibles entre eux | Toujours utiliser les hooks appropriés |

#### 🔓 Sécurité

| Erreur | Pourquoi c'est mal | Alternative |
|--------|-------------------|-------------|
| **`$_GET` / `$_POST` direct** | Vulnérabilité XSS et injection SQL | `Tools::getValue()` avec validation |
| **Pas d'échappement SQL** | Injection SQL possible | `pSQL()`, requêtes préparées |
| **Pas de validation** | Données corrompues en base | `Validate::isInt()`, `Validate::isEmail()` |
| **Afficher les erreurs** | Révèle des infos sensibles | Logger les erreurs, afficher un message générique |
| **Mots de passe en clair** | Compromission des comptes | `password_hash()` avec bcrypt |
| **Pas de token CSRF** | Attaques CSRF possibles | Toujours vérifier les tokens |

#### 💾 Base de données

| Erreur | Pourquoi c'est mal | Alternative |
|--------|-------------------|-------------|
| **`SELECT *`** | Charge des données inutiles | Spécifier les colonnes nécessaires |
| **Pas de LIMIT** | Surcharge mémoire et temps | Toujours limiter les résultats |
| **Requêtes dans les boucles** | N+1 queries, très lent | Utiliser des JOINs ou charger en masse |
| **Pas d'index** | Requêtes lentes sur grandes tables | Ajouter des index sur les clés étrangères |
| **Pas de transaction** | Données incohérentes en cas d'erreur | Utiliser BEGIN/COMMIT/ROLLBACK |
| **Oublier le préfixe** | Erreur si préfixe personnalisé | Toujours utiliser `_DB_PREFIX_` |

#### 🎨 Templates & Front-end

| Erreur | Pourquoi c'est mal | Alternative |
|--------|-------------------|-------------|
| **PHP dans les templates** | Mélange logique et présentation | Utiliser Smarty uniquement |
| **Inline CSS/JS** | Difficile à maintenir, pas de cache | Fichiers externes |
| **Pas d'échappement** | Vulnérabilité XSS | `{$var|escape:'html':'UTF-8'}` |
| **Images non optimisées** | Temps de chargement long | Compression, formats modernes (WebP) |
| **Trop de requêtes HTTP** | Ralentit le chargement | Combiner CSS/JS, sprites CSS |
| **Pas de responsive** | Mauvaise expérience mobile | Bootstrap, media queries |

#### 🐛 Erreurs courantes

| Erreur | Pourquoi c'est mal | Alternative |
|--------|-------------------|-------------|
| **Ignorer les erreurs** | Bugs silencieux, difficiles à déboguer | Logger et gérer les exceptions |
| **Pas de gestion d'erreurs** | Application plante | `try/catch`, vérifications |
| **Cache non invalidé** | Affiche des données obsolètes | `Cache::clean()` après modifications |
| **Hardcoder des valeurs** | Pas flexible, difficile à maintenir | Utiliser Configuration ou constantes |
| **Pas de tests** | Régressions non détectées | Tests unitaires et fonctionnels |
| **Commits sans message** | Historique incompréhensible | Messages clairs et descriptifs |

#### ⚠️ Performance

| Erreur | Pourquoi c'est mal | Alternative |
|--------|-------------------|-------------|
| **Charger tout en mémoire** | Out of memory sur grandes données | Pagination, streaming |
| **Pas de cache** | Recalcul à chaque requête | Cache Smarty, Redis, Memcached |
| **Images non compressées** | Bande passante gaspillée | Compression, lazy loading |
| **Trop de modules actifs** | Ralentit l'application | Désactiver les modules inutilisés |
| **Debug en production** | Ralentit et expose des infos | `_PS_MODE_DEV_ = false` |
| **Pas de CDN** | Serveur surchargé | CDN pour assets statiques |

---

### 📋 Checklist de développement

#### Avant de commencer

- [ ] Lire la documentation officielle PrestaShop
- [ ] Configurer un environnement de développement local
- [ ] Activer le mode debug (`_PS_MODE_DEV_ = true`)
- [ ] Installer les outils de développement (PHPStan, PHP-CS-Fixer)
- [ ] Créer une branche Git pour la fonctionnalité

#### Pendant le développement

- [ ] Respecter les conventions de nommage PrestaShop
- [ ] Valider toutes les entrées utilisateur
- [ ] Échapper toutes les sorties
- [ ] Utiliser les ObjectModel pour les entités
- [ ] Enregistrer les hooks nécessaires
- [ ] Documenter le code avec PHPDoc
- [ ] Tester manuellement chaque fonctionnalité
- [ ] Vérifier la compatibilité multi-boutique
- [ ] Vérifier la compatibilité multilingue

#### Avant de déployer

- [ ] Exécuter les tests unitaires
- [ ] Vérifier les logs d'erreurs
- [ ] Tester sur différents navigateurs
- [ ] Tester en mode responsive
- [ ] Optimiser les requêtes SQL
- [ ] Minifier CSS/JS
- [ ] Compresser les images
- [ ] Désactiver le mode debug
- [ ] Vider le cache
- [ ] Créer une sauvegarde de la base de données

---

### 🎯 Exemples de code à éviter vs. à privilégier

#### ❌ MAUVAIS : Requête SQL directe non sécurisée

```php
// NE JAMAIS FAIRE ÇA !
$id_product = $_GET['id_product'];
$sql = "SELECT * FROM ps_product WHERE id_product = $id_product";
$result = Db::getInstance()->executeS($sql);
```

#### ✅ BON : Utilisation de ObjectModel avec validation

```php
// FAIRE ÇA À LA PLACE
$id_product = (int)Tools::getValue('id_product');

if (!Validate::isUnsignedId($id_product)) {
    throw new PrestaShopException('Invalid product ID');
}

$product = new Product($id_product);

if (!Validate::isLoadedObject($product)) {
    throw new PrestaShopException('Product not found');
}
```

---

#### ❌ MAUVAIS : Modification du core

```php
// NE JAMAIS MODIFIER classes/Product.php directement !
// Fichier : classes/Product.php
class Product extends ObjectModel
{
    // Ajout d'une méthode personnalisée directement dans le core
    public function myCustomMethod()
    {
        // ...
    }
}
```

#### ✅ BON : Utilisation de l'override

```php
// CRÉER UN OVERRIDE À LA PLACE
// Fichier : override/classes/Product.php
class Product extends ProductCore
{
    // Surcharge ou ajout de méthodes
    public function myCustomMethod()
    {
        // ...
    }
}
```

---

#### ❌ MAUVAIS : Logique métier dans le template

```smarty
{* NE PAS FAIRE ÇA ! *}
{if $product.price > 100}
    {assign var="discount" value=$product.price * 0.1}
    {assign var="final_price" value=$product.price - $discount}
    <p>Prix final : {$final_price} €</p>
{/if}
```

#### ✅ BON : Préparation des données dans le contrôleur

```php
// Dans le contrôleur
$discount = 0;
if ($product->price > 100) {
    $discount = $product->price * 0.1;
}
$final_price = $product->price - $discount;

$this->context->smarty->assign([
    'discount' => $discount,
    'final_price' => $final_price,
]);
```

```smarty
{* Dans le template *}
{if $discount > 0}
    <p>Réduction : {$discount|string_format:"%.2f"} €</p>
{/if}
<p>Prix final : {$final_price|string_format:"%.2f"} €</p>
```

---

## 📚 Ressources complémentaires

### Documentation officielle
- **DevDocs PrestaShop** : https://devdocs.prestashop-project.org/
- **API Reference** : https://api.prestashop.com/
- **GitHub** : https://github.com/PrestaShop/PrestaShop

### Outils de développement
- **PHP-CS-Fixer** : Formateur de code PHP
- **PHPStan** : Analyse statique du code
- **Xdebug** : Débogueur PHP
- **PrestaShop Validator** : Validation des modules

### Communauté
- **Forum officiel** : https://www.prestashop.com/forums/
- **Slack PrestaShop** : https://www.prestashop-project.org/slack/
- **Stack Overflow** : Tag `prestashop`

---

## 🎓 Conclusion

PrestaShop est une plateforme e-commerce puissante avec une architecture hybride combinant :

1. **Legacy PHP** pour la compatibilité et la stabilité
2. **Symfony** pour la modernité et les bonnes pratiques
3. **Système de modules** pour l'extensibilité
4. **Hooks** pour l'intégration entre composants

**Points clés à retenir** :
- ✅ Toujours utiliser les ObjectModel pour les entités
- ✅ Valider et échapper toutes les données
- ✅ Étendre via des modules, jamais en modifiant le core
- ✅ Utiliser les hooks pour s'intégrer aux événements
- ✅ Respecter l'architecture MVC
- ✅ Optimiser les performances (cache, requêtes, assets)

**Prochaines étapes** :
1. Configurer un environnement de développement local
2. Créer votre premier module PrestaShop
3. Explorer les hooks disponibles
4. Étudier les modules officiels comme exemples
5. Contribuer à la communauté PrestaShop

---

> **Note** : Cette documentation est basée sur PrestaShop 8.2.6. Certaines fonctionnalités peuvent varier selon les versions.

> **Langage** : PHP 7.4+ / Symfony 4.4+ / Smarty 3.1+ / MySQL 5.7+

---

*Documentation générée le 6 mai 2026 - PrestaShop 8.2.6*
