// Mock data for GLPI offline demo
export const states = [
  { id: 1, name: "Production" },
  { id: 2, name: "Stock" },
  { id: 3, name: "Rupture" },
  { id: 4, name: "Maintenance" }
];

export const locations = [
  { id: 1, name: "Antananarivo - Siège" },
  { id: 2, name: "Tamatave - Bureau" },
  { id: 3, name: "Majunga - Agence" },
  { id: 4, name: "Fianarantsoa - Agence" }
];

export const manufacturers = [
  { id: 1, name: "Dell" },
  { id: 2, name: "HP" },
  { id: 3, name: "Samsung" },
  { id: 4, name: "Apple" },
  { id: 5, name: "Lenovo" }
];

export const computerModels = [
  { id: 1, name: "OptiPlex 7090" },
  { id: 2, name: "Latitude 5420" },
  { id: 3, name: "ThinkPad L14" },
  { id: 4, name: "MacBook Pro M1" }
];

export const monitorModels = [
  { id: 1, name: "UltraSharp 24" },
  { id: 2, name: "SyncMaster 22" },
  { id: 3, name: "Pro Display XDR" }
];

export const phoneModels = [
  { id: 1, name: "iPhone 13" },
  { id: 2, name: "Galaxy S21" },
  { id: 3, name: "Redmi Note 10" }
];

export const users = [
  { id: 1, username: "glpi", realname: "Administrateur", firstname: "GLPI" },
  { id: 2, username: "rakoto", realname: "Rakoto", firstname: "Jean" },
  { id: 3, username: "rabe", realname: "Rabe", firstname: "Pierre" },
  { id: 4, username: "soanjara", realname: "Soanjara", firstname: "Marie" }
];

export const computers = [
  {
    id: 1,
    name: "PC-DIR-001",
    otherserial: "INV-COMP-001",
    status: { id: 1, name: "Production" },
    location: { id: 1, name: "Antananarivo - Siège" },
    manufacturer: { id: 1, name: "Dell" },
    model: { id: 2, name: "Latitude 5420" },
    user: { id: 2, name: "Rakoto Jean" }
  },
  {
    id: 2,
    name: "PC-DEV-002",
    otherserial: "INV-COMP-002",
    status: { id: 1, name: "Production" },
    location: { id: 1, name: "Antananarivo - Siège" },
    manufacturer: { id: 5, name: "Lenovo" },
    model: { id: 3, name: "ThinkPad L14" },
    user: { id: 3, name: "Rabe Pierre" }
  },
  {
    id: 3,
    name: "PC-STOCK-003",
    otherserial: "INV-COMP-003",
    status: { id: 2, name: "Stock" },
    location: { id: 3, name: "Majunga - Agence" },
    manufacturer: { id: 2, name: "HP" },
    model: { id: 1, name: "OptiPlex 7090" },
    user: null
  }
];

export const monitors = [
  {
    id: 1,
    name: "ECR-DIR-001",
    otherserial: "INV-MON-001",
    status: { id: 1, name: "Production" },
    location: { id: 1, name: "Antananarivo - Siège" },
    manufacturer: { id: 1, name: "Dell" },
    model: { id: 1, name: "UltraSharp 24" },
    user: { id: 2, name: "Rakoto Jean" }
  },
  {
    id: 2,
    name: "ECR-DEV-002",
    otherserial: "INV-MON-002",
    status: { id: 1, name: "Production" },
    location: { id: 1, name: "Antananarivo - Siège" },
    manufacturer: { id: 3, name: "Samsung" },
    model: { id: 2, name: "SyncMaster 22" },
    user: { id: 3, name: "Rabe Pierre" }
  }
];

export const phones = [
  {
    id: 1,
    name: "TEL-DIR-001",
    otherserial: "INV-TEL-001",
    status: { id: 1, name: "Production" },
    location: { id: 1, name: "Antananarivo - Siège" },
    manufacturer: { id: 4, name: "Apple" },
    model: { id: 1, name: "iPhone 13" },
    user: { id: 2, name: "Rakoto Jean" }
  }
];

export const tickets = [
  {
    id: 101,
    name: "Problème d'accès internet",
    content: "Impossible de se connecter au réseau local et à internet depuis ce matin.",
    date: "2026-07-27T08:30:00",
    status: { id: 1, name: "nouveau" },
    priority: 4, // High
    type: 1 // Incident
  },
  {
    id: 102,
    name: "Installation de logiciels de dev",
    content: "Demande d'installation de Docker, NodeJS et VS Code sur le nouveau poste de dev.",
    date: "2026-07-27T09:15:00",
    status: { id: 2, name: "assigned" },
    priority: 3, // Medium
    type: 2 // Request
  },
  {
    id: 103,
    name: "Écran noir après démarrage",
    content: "L'écran ne s'allume pas, la LED reste orange.",
    date: "2026-07-26T14:00:00",
    status: { id: 6, name: "closed" },
    priority: 2, // Low
    type: 1 // Incident
  }
];

// Map of ticket ID to list of items associated
export const ticketItems = {
  101: [{ itemtype: "Computer", items_id: 1 }, { itemtype: "Monitor", items_id: 1 }],
  102: [{ itemtype: "Computer", items_id: 2 }],
  103: [{ itemtype: "Monitor", items_id: 2 }]
};

export const ticketCosts = {
  101: [],
  102: [
    { id: 1, name: "Main d'œuvre installation", cost_time: 15.0, duration: 7200, cost_fixed: 0.0 }
  ],
  103: [
    { id: 2, name: "Remplacement câble HDMI", cost_time: 0.0, duration: 0, cost_fixed: 25.0 }
  ]
};

export const ticketSolutions = {
  103: [{ id: 1, content: "Changement du câble HDMI défectueux. Résolu." }]
};

export const ticketAssignees = {
  102: [{ id: 1, users_id: 4, type: 2 }] // Tech Assigned
};
