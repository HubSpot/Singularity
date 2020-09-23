import * as AgentsActions from '../../actions/ui/agents';
import Utils from '../../utils';

const initialState = {
  columns: window.localStorage.hasOwnProperty('agents.columns')
    ? JSON.parse(window.localStorage.getItem('agents.columns'))
    : Utils.DEFAULT_AGENTS_COLUMNS,
  paginated: window.localStorage.hasOwnProperty('agents.paginated')
    ? (localStorage.getItem('agents.paginated') == "true")
    : true
};

export default (state = initialState, action) => {
  if (action.type === AgentsActions.UPDATE_AGENTS_TABLE_SETTINGS) {
    return {
      columns: action.columns,
      paginated: action.paginated
    };
  } else {
    return state;
  }
};
