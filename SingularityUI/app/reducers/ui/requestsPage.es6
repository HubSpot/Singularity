import { TOGGLE_STATE_FILTER } from '../../actions/ui/requestsPage';

const initialState = {
  search: {
    text: '',
    state: ['ACTIVE', 'SYSTEM_COOLDOWN', 'PAUSED'],
    type: null
  }
};

export default function requestsPage(state = initialState, action) {
  switch (action.type) {
    case TOGGLE_STATE_FILTER:
      let newStateFilter;
      if (state.search.state.indexOf(action.value) > -1) {
        // disable
        newStateFilter = state.search.state.filter((v) => v !== action.value);
      } else {
        // enable
        newStateFilter = [...state.search.state, action.value];
      }
      return Object.assign({}, state, {
        search: {
          state: newStateFilter
        }
      });
    default:
      return state;
  }
}
