import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAgents = buildApiAction(
  'FETCH_AGENTS',
  {url: '/agents'}
);

export const FreezeAgent = buildJsonApiAction(
  'FREEZE_AGENT',
  'POST',
  (agentId, data) => ({
    url: `/agents/agent/${agentId}/freeze`,
    body: data || {}
  })
);

export const DecommissionAgent = buildJsonApiAction(
  'DECOMMISSION_AGENT',
  'POST',
  (agentId, data) => ({
    url: `/agents/agent/${agentId}/decommission`,
    body: data || {}
  })
);

export const RemoveAgent = buildJsonApiAction(
  'REMOVE_AGENT',
  'DELETE',
  (agentId, data) => ({
    url: `/agents/agent/${agentId}`,
    body: data || {}
  })
);

export const ReactivateAgent = buildJsonApiAction(
  'ACTIVATE_AGENT',
  'POST',
  (agentId, data) => ({
    url: `/agents/agent/${agentId}/activate`,
    body: data || {}
  })
);

export const FetchExpiringAgentStates = buildApiAction(
  'FETCH_EXPIRING_AGENT_STATES',
  {url: '/agnets/expiring'}
);

export const RemoveExpiringAgentState = buildJsonApiAction(
  'REMOVE_EXPIRING_AGENT_STATE',
  'DELETE',
  (agentId) => ({
    url: `/agents/agent/${agentId}/expiring`
  })
);

export const FetchAgentUsages = buildApiAction(
  'FETCH_AGENT_USAGES',
  {url : '/usage/agents'}
);

export const ClearInactiveAgents = buildApiAction(
  'FETCH_AGENT_USAGES',
  {
    method: 'DELETE',
    url : '/agents/dead'
  }
);
