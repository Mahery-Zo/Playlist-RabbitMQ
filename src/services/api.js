import * as realHelpers from './glpiHelpers.js';
import * as mockHelpers from './mockGlpiService.js';
import * as realAuth from './glpiAuth.js';
import * as mockAuth from './mockAuth.js';

const DEMO = import.meta.env.VITE_DEMO_MODE === 'true';

console.log(`[GLPI API] Mode démo actif : ${DEMO}`);

// Helpers re-export
export const getAll = DEMO ? mockHelpers.getAll : realHelpers.getAll;
export const getOne = DEMO ? mockHelpers.getOne : realHelpers.getOne;
export const createItem = DEMO ? mockHelpers.createItem : realHelpers.createItem;
export const CreateObj = DEMO ? mockHelpers.CreateObj : realHelpers.CreateObj;
export const deleteItem = DEMO ? mockHelpers.deleteItem : realHelpers.deleteItem;
export const buildlookup = DEMO ? mockHelpers.buildlookup : realHelpers.buildlookup;
export const linkItemToTicket = DEMO ? mockHelpers.linkItemToTicket : realHelpers.linkItemToTicket;
export const uploadDocument = DEMO ? mockHelpers.uploadDocument : realHelpers.uploadDocument;
export const getTicketItems = DEMO ? mockHelpers.getTicketItems : realHelpers.getTicketItems;
export const getTicketCosts = DEMO ? mockHelpers.getTicketCosts : realHelpers.getTicketCosts;
export const assignTechnician = DEMO ? mockHelpers.assignTechnician : realHelpers.assignTechnician;
export const updateTicketStatus = DEMO ? mockHelpers.updateTicketStatus : realHelpers.updateTicketStatus;
export const getTicketAssignees = DEMO ? mockHelpers.getTicketAssignees : realHelpers.getTicketAssignees;
export const getTicketSolutions = DEMO ? mockHelpers.getTicketSolutions : realHelpers.getTicketSolutions;
export const addSolution = DEMO ? mockHelpers.addSolution : realHelpers.addSolution;
export const getTicketByExternalId = DEMO ? mockHelpers.getTicketByExternalId : realHelpers.getTicketByExternalId;
export const headers = DEMO ? mockHelpers.headers : realHelpers.headers;
export const getToken = DEMO ? mockHelpers.getToken : realHelpers.getToken;

// Auth re-export
export const login = DEMO ? mockAuth.login : realAuth.login;
export const logout = DEMO ? mockAuth.logout : realAuth.logout;
export const isAuthenticated = DEMO ? mockAuth.isAuthenticated : realAuth.isAuthenticated;

// Node API Endpoint
export const NODE_API = import.meta.env.VITE_NODE_API || 'http://localhost:8083';
export const DEMO_MODE = DEMO;
