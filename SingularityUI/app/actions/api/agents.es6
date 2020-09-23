import { buildApiAction, buildJsonApiAction } from './base';

export const FetchAgents = buildApiAction(
  'FETCH_SLAVES',
  {url: '/slaves'}
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

export const RemoveExpiringSlaveState = buildJsonApiAction(
  'REMOVE_EXPIRING_SLAVE_STATE',
  'DELETE',
  (slaveId) => ({
    url: `/slaves/slave/${slaveId}/expiring`
  })
);

export const FetchSlaveUsages = buildApiAction(
  'FETCH_SLAVE_USAGES',
  {url : '/usage/slaves'}
);

export const ClearInactiveSlaves = buildApiAction(
  'FETCH_SLAVE_USAGES',
  {
    method: 'DELETE',
    url : '/slaves/dead'
  }
);
