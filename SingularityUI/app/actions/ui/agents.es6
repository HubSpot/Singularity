import { FetchAgents, FreezeAgent, DecommissionAgent, RemoveAgent, ReactivateAgent, FetchExpiringAgentStates } from '../../actions/api/agents';
import { FetchInactiveHosts } from '../api/inactive';

export const UPDATE_AGENTS_TABLE_SETTINGS = 'UPDATE_AGENTS_TABLE_SETTINGS';

export const UpdateAgentsTableSettings = (columns, paginated) => {
  return (dispatch) => {
    localStorage['agents.columns'] = JSON.stringify(columns);
    localStorage['agents.paginated'] = paginated;
    dispatch({
      columns: columns,
      paginated: paginated,
      type: UPDATE_AGENTS_TABLE_SETTINGS
    });
  };
};

export const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchAgents.trigger()),
    dispatch(FetchExpiringAgentStates.trigger()),
    dispatch(FetchInactiveHosts.trigger()),
  ]);

export const initialize = () => (dispatch) =>
  Promise.all([
    dispatch(FreezeAgent.clear()),
    dispatch(DecommissionAgent.clear()),
    dispatch(RemoveAgent.clear()),
    dispatch(ReactivateAgent.clear())
  ]).then(() => dispatch(refresh()));
