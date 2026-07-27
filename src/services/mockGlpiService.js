import * as mockData from './mockData';

function getCollection(itemtype) {
  const clean = itemtype.toLowerCase();
  if (clean.includes('computer')) return mockData.computers;
  if (clean.includes('monitor')) return mockData.monitors;
  if (clean.includes('phone')) return mockData.phones;
  if (clean.includes('user')) return mockData.users;
  if (clean.includes('state')) return mockData.states;
  if (clean.includes('location')) return mockData.locations;
  if (clean.includes('manufacturer')) return mockData.manufacturers;
  if (clean.includes('computermodel')) return mockData.computerModels;
  if (clean.includes('monitormodel')) return mockData.monitorModels;
  if (clean.includes('phonemodel')) return mockData.phoneModels;
  if (clean.includes('ticket')) return mockData.tickets;
  return null;
}

export function headers() {
  return {
    'Authorization': 'Bearer demo-token',
    'App-Token': 'demo-app-token',
  };
}

export function getToken() {
  return 'demo-token';
}

export async function getAll(itemtype, isdeleted) {
  console.log(`[MOCK] getAll: ${itemtype}`);
  const list = getCollection(itemtype);
  if (!list) return [];
  // For demo, we don't care about isdeleted, just return the active items
  if (isdeleted) return [];
  return JSON.parse(JSON.stringify(list)); // Return copy to prevent direct modification
}

export async function getOne(itemType, id) {
  console.log(`[MOCK] getOne: ${itemType} ID: ${id}`);
  const list = getCollection(itemType);
  if (!list) return null;
  const item = list.find(item => item.id === Number(id));
  return item ? JSON.parse(JSON.stringify(item)) : null;
}

export async function createItem(itemtype, data) {
  console.log(`[MOCK] createItem: ${itemtype}`, data);
  const list = getCollection(itemtype);
  if (!list) throw new Error(`Collection non trouvée pour ${itemtype}`);
  
  const id = list.length > 0 ? Math.max(...list.map(i => i.id)) + 1 : 1;
  const newItem = { 
    id, 
    ...data,
    // Format status, location etc nicely as mock objects if they are numbers
    status: typeof data.status === 'number' ? mockData.states.find(s => s.id === data.status) : data.status,
    location: typeof data.location === 'number' ? mockData.locations.find(l => l.id === data.location) : data.location,
    manufacturer: typeof data.manufacturer === 'number' ? mockData.manufacturers.find(m => m.id === data.manufacturer) : data.manufacturer,
    model: typeof data.model === 'number' ? getCollection(itemtype + 'Model')?.find(m => m.id === data.model) : data.model,
    user: typeof data.user === 'number' ? mockData.users.find(u => u.id === data.user) : data.user,
  };
  list.push(newItem);
  return newItem;
}

export async function CreateObj(type, data) {
  console.log(`[MOCK] CreateObj: ${type}`, data);
  if (type.toLowerCase().includes('ticket') && !type.toLowerCase().includes('cost')) {
    const list = mockData.tickets;
    const id = list.length > 0 ? Math.max(...list.map(i => i.id)) + 1 : 101;
    // Map priorities / status / type if numeric
    const newTicket = {
      id,
      name: data.name,
      content: data.content,
      date: data.date || new Date().toISOString().slice(0, 19),
      status: data.status || { id: 1 },
      priority: data.priority || 3,
      type: data.type || 1
    };
    list.push(newTicket);
    mockData.ticketItems[id] = [];
    mockData.ticketCosts[id] = [];
    return newTicket;
  }
  
  // Custom logic for generic objects
  const list = getCollection(type);
  if (list) {
    const id = list.length > 0 ? Math.max(...list.map(i => i.id)) + 1 : 1;
    const newItem = { id, ...data };
    list.push(newItem);
    return newItem;
  }
  
  return { id: 999, ...data };
}

export async function deleteItem(itemtype, id) {
  console.log(`[MOCK] deleteItem: ${itemtype} ID: ${id}`);
  const list = getCollection(itemtype);
  if (list) {
    const index = list.findIndex(i => i.id === Number(id));
    if (index !== -1) {
      list.splice(index, 1);
    }
  }
}

export function buildlookup(items) {
  const map = {}
  for (const item of items) {
    if (item.name) map[item.name.toLowerCase()] = item.id
  }
  return map
}

export async function linkItemToTicket(ticketId, itemtype, itemsId) {
  console.log(`[MOCK] linkItemToTicket: Ticket ${ticketId} to ${itemtype} ID ${itemsId}`);
  if (!mockData.ticketItems[ticketId]) {
    mockData.ticketItems[ticketId] = [];
  }
  mockData.ticketItems[ticketId].push({ itemtype, items_id: Number(itemsId) });
  return { id: Math.floor(Math.random() * 1000) };
}

export async function uploadDocument(filename, blob, itemtype, itemsId) {
  console.log(`[MOCK] uploadDocument: ${filename} to ${itemtype} ID ${itemsId}`);
  return { id: Math.floor(Math.random() * 1000) };
}

export async function getTicketItems(ticketId) {
  console.log(`[MOCK] getTicketItems: ${ticketId}`);
  return mockData.ticketItems[ticketId] || [];
}

export async function getTicketCosts(ticketId) {
  console.log(`[MOCK] getTicketCosts: ${ticketId}`);
  return mockData.ticketCosts[ticketId] || [];
}

export async function assignTechnician(ticketId, userId) {
  console.log(`[MOCK] assignTechnician: Ticket ${ticketId} to User ${userId}`);
  if (!mockData.ticketAssignees[ticketId]) {
    mockData.ticketAssignees[ticketId] = [];
  }
  mockData.ticketAssignees[ticketId].push({ id: Math.random(), users_id: userId, type: 2 });
  return { id: Math.floor(Math.random() * 1000) };
}

export async function updateTicketStatus(ticket_id, statusid) {
  console.log(`[MOCK] updateTicketStatus: Ticket ${ticket_id} to Status ${statusid}`);
  const ticket = mockData.tickets.find(t => t.id === Number(ticket_id));
  if (ticket) {
    ticket.status = { id: statusid };
  }
  return { id: ticket_id, status: { id: statusid } };
}

export async function getTicketAssignees(ticketId) {
  console.log(`[MOCK] getTicketAssignees: ${ticketId}`);
  return mockData.ticketAssignees[ticketId] || [];
}

export async function getTicketSolutions(ticketId) {
  console.log(`[MOCK] getTicketSolutions: ${ticketId}`);
  return mockData.ticketSolutions[ticketId] || [];
}

export async function addSolution(ticketId, content) {
  console.log(`[MOCK] addSolution: Ticket ${ticketId}`);
  if (!mockData.ticketSolutions[ticketId]) {
    mockData.ticketSolutions[ticketId] = [];
  }
  mockData.ticketSolutions[ticketId].push({ id: Math.random(), content });
}

export async function getTicketByExternalId(externalID) {
  console.log(`[MOCK] getTicketByExternalId: ${externalID}`);
  // In csv imports, Ref_Ticket is usually used as external_id. Let's find by name or ID
  const ticket = mockData.tickets.find(t => t.id === Number(externalID) || t.name === externalID);
  return ticket || mockData.tickets[0];
}
