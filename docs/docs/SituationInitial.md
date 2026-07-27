# Situation Initiale — PrestaShop 8.2.6

> **Mots-clés globaux** : `PrestaShop`, `8.2.6`, `Symfony`, `Smarty`, `ObjectModel`, `Hook`, `Module`, `FrontController`, `AdminController`, `Dispatcher`, `XAMPP`

---

## 1. Informations générales

| Élément | Valeur |
|---------|--------|
| **Version** | `8.2.6` (fichier `src/Core/Version.php`) |
| **Kernel Symfony** | `AppKernel` (fichier `app/AppKernel.php`) — MAJOR_VERSION = 8 |
| **Serveur** | XAMPP — `c:\xampp\htdocs\prestashop` |
| **Licence** | OSL 3.0 |
| **Thème actif** | `classic` (`themes/classic/`) |
| **Dossier override** | **Vide** — aucune surcharge active |

```php
// src/Core/Version.php
final class Version
{
    public const VERSION = '8.2.6';
    public const MAJOR_VERSION = 8;
    public const MINOR_VERSION = 2;
    public const RELEASE_VERSION = 6;
}
```

> **Mots-clés** : `Version`, `8.2.6`, `MAJOR_VERSION`, `AppKernel`

---

## 2. Arborescence du projet

```text
prestashop/
├── admin/                  # Back-office (point d'entrée BO)
├── app/                    # Kernel Symfony (AppKernel.php, config, Resources)
│   ├── AppKernel.php       # Kernel principal — enregistre les bundles
│   ├── config/             # Configuration Symfony (YAML)
│   └── Resources/          # Templates Twig du BO
├── bin/                    # Commandes console (Symfony)
├── cache/                  # Cache Smarty / legacy
├── classes/                # Classes PHP legacy (107 fichiers — ObjectModel, Tools, Hook…)
│   ├── controller/         # FrontController.php, AdminController.php
│   ├── ObjectModel.php     # ORM maison (2371 lignes)
│   ├── Product.php         # Modèle Produit (318 Ko)
│   ├── Cart.php            # Modèle Panier (221 Ko)
│   ├── Hook.php            # Système de hooks (1400 lignes)
│   ├── Tools.php           # Utilitaires (140 Ko)
│   └── Validate.php        # Validation (46 Ko)
├── config/                 # Configuration legacy
│   ├── config.inc.php      # Bootstrap principal (309 lignes)
│   ├── defines.inc.php     # Constantes (_PS_ROOT_DIR_, etc.)
│   └── smarty.config.inc.php
├── controllers/            # Contrôleurs
│   ├── admin/              # 42 contrôleurs back-office
│   └── front/              # 32 contrôleurs front-office + listing/
├── docs/                   # Documentation (ce fichier)
├── img/                    # Images statiques
├── install/                # Scripts d'installation
├── modules/                # 77 modules installés
├── override/               # Surcharges (actuellement VIDE)
├── src/                    # Code moderne (namespaced)
│   ├── Adapter/            # Bridge legacy ↔ Symfony (91 sous-dossiers)
│   ├── Core/               # Domaine métier (69 sous-dossiers)
│   └── PrestaShopBundle/   # Bundle Symfony (23 sous-dossiers)
├── templates/              # Templates Smarty globaux
├── themes/                 # Thèmes
│   └── classic/            # Thème par défaut (templates, assets, config)
├── translations/           # Fichiers de traduction
├── var/                    # Logs Symfony, cache Symfony
├── vendor/                 # Dépendances Composer
├── webservice/             # API REST legacy
└── index.php               # Point d'entrée front
```

> **Mots-clés** : `classes/`, `controllers/`, `modules/`, `src/`, `themes/`, `override/`, `config/`, `vendor/`

---

## 3. Architecture — Double couche Legacy + Symfony

### 3.1 Point d'entrée — `index.php`

```php
// index.php (29 lignes)
require dirname(__FILE__).'/config/config.inc.php';
Dispatcher::getInstance()->dispatch();
```

Le `Dispatcher` analyse l'URL, identifie le contrôleur front, l'instancie et appelle `run()`.

> **Mots-clés** : `index.php`, `Dispatcher`, `dispatch()`

### 3.2 Bootstrap — `config/config.inc.php`

Fichier de 309 lignes qui initialise tout le contexte :

```php
// config/config.inc.php — extraits clés
require_once __DIR__ . '/defines.inc.php';       // Constantes globales
require_once _PS_CONFIG_DIR_ . 'autoload.php';   // Autoload
require_once __DIR__ . '/bootstrap.php';          // Symfony bootstrap

$context = Context::getContext();
$context->shop = Shop::initialize();              // Initialise la boutique
Language::loadLanguages();                        // Charge les langues
$context->cookie = $cookie;                       // Cookie (session)
$context->link = new Link($https_link, $https_link); // Générateur d'URLs
require_once __DIR__ . '/smarty.config.inc.php';  // Moteur de templates
```

> **Mots-clés** : `config.inc.php`, `Context`, `Shop::initialize`, `Cookie`, `Language::loadLanguages`

### 3.3 Kernel Symfony — `app/AppKernel.php`

```php
// app/AppKernel.php — bundles enregistrés
class AppKernel extends Kernel
{
    const MAJOR_VERSION = 8;

    public function registerBundles()
    {
        $bundles = array(
            new Symfony\Bundle\FrameworkBundle\FrameworkBundle(),
            new Symfony\Bundle\SecurityBundle\SecurityBundle(),
            new Symfony\Bundle\TwigBundle\TwigBundle(),
            new Doctrine\Bundle\DoctrineBundle\DoctrineBundle(),
            new ApiPlatform\Symfony\Bundle\ApiPlatformBundle(),
            new PrestaShopBundle\PrestaShopBundle(),        // Bundle PS
            new League\Tactician\Bundle\TacticianBundle(),  // Command Bus
            new FOS\JsRoutingBundle\FOSJsRoutingBundle(),
        );
        // ...
    }
}
```

> **Mots-clés** : `AppKernel`, `registerBundles`, `PrestaShopBundle`, `Doctrine`, `ApiPlatform`, `Tactician`

---

## 4. Couche Modèle — `ObjectModel`

Toutes les entités legacy héritent de `ObjectModelCore` (2371 lignes). C'est l'ORM maison de PrestaShop.

```php
// classes/ObjectModel.php
abstract class ObjectModelCore implements EntityInterface
{
    public const TYPE_INT = 1;
    public const TYPE_BOOL = 2;
    public const TYPE_STRING = 3;
    public const TYPE_FLOAT = 4;
    public const TYPE_DATE = 5;
    public const TYPE_HTML = 6;

    public static $definition = [];  // Définition du schéma

    // CRUD
    public function save($null_values = false, $auto_date = true)
    {
        return (int) $this->id > 0 ? $this->update($null_values) : $this->add($auto_date, $null_values);
    }

    public function add($auto_date = true, $null_values = false)
    {
        Hook::exec('actionObjectAddBefore', ['object' => $this]);
        // ... INSERT via Db::getInstance()->insert(...)
        Hook::exec('actionObjectAddAfter', ['object' => $this]);
    }

    public function update($null_values = false)
    {
        Hook::exec('actionObjectUpdateBefore', ['object' => $this]);
        // ... UPDATE via Db::getInstance()->update(...)
    }

    public function delete() { /* ... */ }
}
```

Les tables sont préfixées `ps_` (configurable). Les champs multilingues sont stockés dans `ps_<table>_lang`.

> **Mots-clés** : `ObjectModel`, `$definition`, `save()`, `add()`, `update()`, `delete()`, `TYPE_INT`, `TYPE_STRING`, `Db::getInstance`

---

## 5. Couche Contrôleur

### 5.1 Front-office — `FrontController`

```php
// classes/controller/FrontController.php
class FrontControllerCore extends Controller
{
    public function init()
    {
        // SSL, cookie, cart, géolocation, cart rules...
        Hook::exec('actionFrontControllerInitBefore', ['controller' => $this]);
        $this->sslRedirection();
        $this->recoverCart();
        CartRule::autoRemoveFromCart($this->context);
        CartRule::autoAddToCart($this->context);
        Hook::exec('actionFrontControllerInitAfter', ['controller' => $this]);
    }

    public function initContent()
    {
        $this->assignGeneralPurposeVariables();
        $this->context->smarty->assign([
            'HOOK_HEADER' => Hook::exec('displayHeader'),
        ]);
    }
}
```

**32 contrôleurs front** dans `controllers/front/` : `ProductController`, `CartController`, `OrderController`, `AuthController`, `CategoryController`, etc.

> **Mots-clés** : `FrontController`, `init()`, `initContent()`, `assignGeneralPurposeVariables`, `displayHeader`

### 5.2 Exemple concret — `ProductController`

```php
// controllers/front/ProductController.php
class ProductControllerCore extends ProductPresentingFrontControllerCore
{
    public $php_self = 'product';
    protected $product;

    public function init()
    {
        parent::init();
        $this->id_product = (int) Tools::getValue('id_product');
        $this->product = new Product($this->id_product, true, $this->context->language->id);

        if (!Validate::isLoadedObject($this->product)) {
            header('HTTP/1.1 404 Not Found');
            $this->setTemplate('errors/404');
            return;
        }
        $this->setTemplate('catalog/product', ['entity' => 'product', 'id' => $this->id_product]);
    }

    public function initContent()
    {
        // Prix, combinaisons, packs, accessoires...
        $this->assignPriceAndTax();
        $this->assignAttributesCombinations();
        $product_for_template = $this->getTemplateVarProduct();

        // Hook chaîné
        $filteredProduct = Hook::exec('filterProductContent',
            ['object' => $product_for_template], null, false, true, false, null, true);

        $this->context->smarty->assign(['product' => $product_for_template]);
        parent::initContent();
    }
}
```

> **Mots-clés** : `ProductController`, `Tools::getValue`, `setTemplate`, `assignPriceAndTax`, `assignAttributesCombinations`, `filterProductContent`

### 5.3 Back-office — `AdminController`

**42 contrôleurs admin** dans `controllers/admin/`. Pattern : `AdminXxxControllerCore extends AdminController`.

```php
// controllers/admin/AdminCartsController.php
class AdminCartsControllerCore extends AdminController
{
    public function __construct()
    {
        $this->bootstrap = true;
        $this->table = 'cart';
        $this->className = 'Cart';

        parent::__construct();

        $this->addRowAction('view');
        $this->addRowAction('delete');

        $this->_select = 'CONCAT(LEFT(c.`firstname`, 1), \'. \', c.`lastname`) `customer`, ...';
        $this->_join = 'LEFT JOIN '._DB_PREFIX_.'customer c ON (c.id_customer = a.id_customer) ...';

        $this->fields_list = [
            'id_cart' => ['title' => $this->trans('ID', [], 'Admin.Global')],
            'customer' => ['title' => $this->trans('Customer', [], 'Admin.Global')],
            // ...
        ];
    }

    public function renderView() { /* ... chargement Cart, Currency, Products ... */ }

    public function ajaxProcessUpdateQty()
    {
        $qty = Tools::getValue('qty');
        $id_product = (int) Tools::getValue('id_product');
        $this->context->cart->updateQty($qty, $id_product, ...);
        echo json_encode($this->ajaxReturnVars());
    }
}
```

> **Mots-clés** : `AdminController`, `$this->table`, `$this->className`, `fields_list`, `_select`, `_join`, `addRowAction`, `ajaxProcess`, `renderView`

---

## 6. Système de Hooks

Le fichier `classes/Hook.php` (1400 lignes) gère les points d'extension.

```php
// classes/Hook.php
class HookCore extends ObjectModel
{
    public static $definition = [
        'table' => 'hook',
        'primary' => 'id_hook',
        'fields' => [
            'name' => ['type' => self::TYPE_STRING, 'required' => true, 'size' => 191],
            'title' => ['type' => self::TYPE_STRING],
            'description' => ['type' => self::TYPE_HTML],
        ],
    ];

    // Enregistrer un module sur un hook
    public static function registerHook($module_instance, $hook_name, $shop_list = null) { /* ... */ }

    // Déclencher un hook
    public static function exec($hook_name, $hook_args = [], ...) { /* ... */ }

    // Vérifier si un module répond à un hook
    public static function isHookCallableOn(Module $module, string $hookName): bool { /* ... */ }
}
```

**Hooks fréquents** :
- `displayHeader` — injection dans le `<head>`
- `displayHome` — page d'accueil
- `actionObjectAddAfter` — après un INSERT
- `actionFrontControllerInitBefore` / `After`
- `filterProductContent` — filtrage chaîné du produit

> **Mots-clés** : `Hook::exec`, `registerHook`, `unregisterHook`, `displayHeader`, `actionObject`, `hookDisplayHeader`

---

## 7. Modules installés (77 modules)

### Modules principaux

| Module | Rôle |
|--------|------|
| `ps_checkout` | Paiement (PayPal) |
| `ps_facebook` | Intégration Facebook |
| `ps_mbo` | Marketplace back-office |
| `ps_accounts` | Comptes PrestaShop |
| `ps_eventbus` | Event bus cloud |
| `ps_facetedsearch` | Navigation à facettes |
| `ps_emailsubscription` | Newsletter |
| `ps_googleanalytics` | Google Analytics |
| `contactform` | Formulaire de contact |
| `blockreassurance` | Blocs de réassurance |
| `productcomments` | Avis produits |
| `psgdpr` | Conformité RGPD |

### Modules statistiques

`statsbestproducts`, `statsbestcustomers`, `statscatalog`, `statssales`, `statsstock`, `statscarrier`, `statsforecast`, etc.

> **Mots-clés** : `ps_checkout`, `ps_facebook`, `ps_mbo`, `ps_accounts`, `ps_facetedsearch`, `psgdpr`, `blockreassurance`

---

## 8. Code moderne — `src/`

### 8.1 Structure de `src/`

```text
src/
├── Adapter/           # 91 sous-dossiers — Pont legacy ↔ Symfony
│   ├── Product/       # ProductDataProvider, etc.
│   ├── Cart/
│   ├── Order/
│   ├── LegacyContext.php
│   └── ContainerBuilder.php
├── Core/              # 69 sous-dossiers — Domaine métier
│   ├── Domain/        # CQRS Commands/Queries
│   ├── CommandBus/    # Bus de commandes (Tactician)
│   ├── Grid/          # Grilles admin
│   ├── Form/          # Form types
│   ├── Product/       # Value Objects produit
│   └── Hook/          # HookModuleFilter
└── PrestaShopBundle/  # 23 sous-dossiers — Bundle Symfony
    ├── Controller/    # Contrôleurs Symfony (BO moderne)
    ├── Entity/        # Entités Doctrine
    ├── Form/          # FormTypes
    ├── Twig/          # Extensions Twig
    └── Resources/     # Vues, config services
```

### 8.2 Pattern CQRS dans `src/Core/Domain/`

Les nouvelles fonctionnalités utilisent le pattern **Command/Query** via **Tactician** :

```php
// Exemple conceptuel
// src/Core/Domain/Product/Command/AddProductCommand.php
// src/Core/Domain/Product/CommandHandler/AddProductCommandHandler.php
// src/Core/Domain/Product/Query/GetProductForEditing.php
// src/Core/Domain/Product/QueryHandler/GetProductForEditingHandler.php
```

> **Mots-clés** : `Adapter`, `Core/Domain`, `CQRS`, `CommandBus`, `Tactician`, `PrestaShopBundle`, `Entity`, `Grid`

---

## 9. Thème `classic`

```text
themes/classic/
├── assets/        # CSS/JS compilés
├── config/        # theme.yml
├── modules/       # Surcharges de templates par module
├── plugins/       # Plugins JS
├── templates/     # Templates Smarty (.tpl)
│   ├── catalog/   # product.tpl, listing/
│   ├── checkout/  # cart.tpl, order.tpl
│   ├── customer/  # account, addresses
│   ├── layouts/   # layout-both-columns.tpl
│   └── errors/    # 404.tpl, maintenance.tpl
├── preview.png
├── preview-mobile.png
└── preview-tablet.png
```

Les templates utilisent **Smarty** (`.tpl`) côté front et **Twig** (`.html.twig`) côté back-office Symfony.

> **Mots-clés** : `classic`, `Smarty`, `.tpl`, `Twig`, `layout`, `catalog/product.tpl`

---

## 10. Sécurité et validation

```php
// Validation — classes/Validate.php (46 Ko)
Validate::isEmail($email);
Validate::isUnsignedId($id);
Validate::isLoadedObject($object);
Validate::isMessage($text);

// Échappement SQL — pSQL()
$safe = pSQL($user_input);

// Échappement HTML — Tools::safeOutput()
$safe_html = Tools::safeOutput($value);

// Token CSRF — via AdminController
$token = Tools::getAdminTokenLite('AdminProducts');

// Vérification dans un contrôleur admin
if (!Tools::isSubmit('submitForm')) { die('Invalid'); }
```

> **Mots-clés** : `Validate::`, `pSQL`, `Tools::safeOutput`, `getAdminTokenLite`, `CSRF`, `isLoadedObject`

---

## 11. Base de données

- Préfixe des tables : `ps_` (défini dans `defines.inc.php`)
- Accès via : `Db::getInstance()->executeS($sql)` / `getRow()` / `getValue()` / `insert()` / `update()`
- ORM legacy : `ObjectModel` avec `$definition` statique
- ORM moderne : **Doctrine** dans `src/PrestaShopBundle/Entity/`

```php
// Accès DB direct
$result = Db::getInstance()->executeS(
    'SELECT * FROM `'._DB_PREFIX_.'product` WHERE active = 1'
);

// Via ObjectModel
$product = new Product(42, true, $id_lang);
$product->name[$id_lang] = 'Mon produit';
$product->price = 29.99;
$product->save();  // INSERT ou UPDATE automatique
```

> **Mots-clés** : `Db::getInstance`, `executeS`, `getRow`, `_DB_PREFIX_`, `ps_product`, `ps_cart`, `ps_orders`, `Doctrine`

---

## 12. Contexte global — `Context`

Le singleton `Context` contient toute la session courante :

```php
$context = Context::getContext();
$context->shop;       // Boutique active
$context->language;   // Langue active
$context->currency;   // Devise active
$context->customer;   // Client connecté
$context->cart;       // Panier courant
$context->cookie;     // Cookie de session
$context->link;       // Générateur d'URLs
$context->smarty;     // Moteur de templates
$context->employee;   // Employé BO connecté
$context->controller; // Contrôleur courant
```

> **Mots-clés** : `Context::getContext()`, `$context->shop`, `$context->cart`, `$context->customer`, `$context->cookie`

---

## 13. Points d'attention

| # | Domaine | Constat | Remarque |
|---|---------|---------|----------|
| 1 | **Override** | Dossier `override/` vide | Aucune surcharge custom — bon signe |
| 2 | **Modules** | 77 modules installés | Vérifier lesquels sont réellement actifs |
| 3 | **Version** | PS 8.2.6 | Version récente, support PHP 8.1+ |
| 4 | **Architecture** | Double couche legacy + Symfony | Migration progressive en cours |
| 5 | **Templates** | Smarty (front) + Twig (back) | Deux moteurs à maintenir |
| 6 | **CQRS** | Présent dans `src/Core/Domain/` | Pattern moderne pour les nouvelles fonctionnalités |

---

## 14. Index de recherche rapide (CTRL+F)

| Mot-clé | Localisation / Concept |
|---------|----------------------|
| `ObjectModel` | ORM legacy — `classes/ObjectModel.php` |
| `FrontController` | Contrôleur front — `classes/controller/FrontController.php` |
| `AdminController` | Contrôleur admin — `classes/controller/AdminController.php` |
| `Hook::exec` | Déclenchement de hook — `classes/Hook.php` |
| `registerHook` | Inscription d'un module sur un hook |
| `Dispatcher` | Routage URL — `classes/Dispatcher.php` |
| `Context` | Singleton de session — `classes/Context.php` |
| `Tools::getValue` | Récupération paramètre GET/POST |
| `Validate::` | Validation — `classes/Validate.php` |
| `pSQL` | Échappement SQL |
| `Db::getInstance` | Accès base de données |
| `_DB_PREFIX_` | Préfixe tables (`ps_`) |
| `AppKernel` | Kernel Symfony — `app/AppKernel.php` |
| `PrestaShopBundle` | Bundle Symfony — `src/PrestaShopBundle/` |
| `Adapter` | Pont legacy ↔ Symfony — `src/Adapter/` |
| `Core/Domain` | CQRS Commands/Queries |
| `Smarty` | Moteur templates front |
| `Twig` | Moteur templates back-office |
| `classic` | Thème par défaut — `themes/classic/` |
| `modules/` | 77 modules installés |
| `override/` | Surcharges (vide) |
| `config.inc.php` | Bootstrap principal |
| `ajax` | `ajaxProcess*` méthodes des contrôleurs admin |

---

*Rapport généré le 5 mai 2026 à partir de l'analyse directe du code source du projet.*
