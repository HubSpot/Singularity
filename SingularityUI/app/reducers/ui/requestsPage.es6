import * as RequestsPageActions from '../../actions/ui/requestsPage';

const initialState = {
  textFilter: '',
  stateFilter: ['ACTIVE', 'SYSTEM_COOLDOWN', 'PAUSED'],
  typeFilter: 'ALL'
};

export default function requestsPage(state = initialState, action) {
  switch (action.type) {
    case RequestsPageActions.TOGGLE_STATE_FILTER:
      let newStateFilter;
      if (state.stateFilter.indexOf(action.value) > -1) {
        // disable
        newStateFilter = state.stateFilter.filter((v) => v !== action.value);
      } else {
        // enable
        newStateFilter = [...state.stateFilter, action.value];
      }
      return Object.assign({}, state, {
        stateFilter: newStateFilter
      });
    case RequestsPageActions.CHANGE_TYPE_FILTER:
      return Object.assign({}, state, {
        typeFilter: action.value
      });
    default:
      return state;
  }
}
